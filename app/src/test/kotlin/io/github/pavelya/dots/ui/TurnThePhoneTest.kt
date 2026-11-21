package io.github.pavelya.dots.ui

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import dots.campaign.LevelRecord
import io.github.pavelya.dots.DotsApplication
import io.github.pavelya.dots.MainActivity
import io.github.pavelya.dots.ReviewPrompt
import io.github.pavelya.dots.data.GameMode
import io.github.pavelya.dots.ui.board.BoardGeometry
import io.github.pavelya.dots.ui.screen.BOARD_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Turning the phone, or any other restart of the activity, keeps the screen the player is on. */
@RunWith(RobolectricTestRunner::class)
class TurnThePhoneTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val app get() = RuntimeEnvironment.getApplication() as DotsApplication

    /** Google Play's review prompt does nothing in a test. */
    @Before
    fun `no review prompt`() {
        app.reviews = ReviewPrompt { }
    }

    private fun waitFor(text: String) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    private fun waitForSide(description: String) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty() }

    private fun gone(text: String) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isEmpty() }

    @Test
    fun `Result stays on screen when the activity is recreated, and Retry plays the level again`() {
        // A new player's first start opens Level 1-1; one dot, dragged below the computer's, wins it.
        waitForSide("You, score 0, to move")
        compose.onNodeWithTag(BOARD_TAG).performTouchInput {
            val board = BoardGeometry(7, 7, width.toFloat(), height.toFloat())
            down(board.center(0, 0))
            moveTo(board.center(3, 4))
            up()
        }
        waitFor("You won!")
        // The game has ended, so the store holds no campaign game to resume.
        compose.waitUntil(timeoutMillis = 5_000) { app.games.saved(GameMode.CAMPAIGN) == null }

        compose.activityRule.scenario.recreate()
        compose.waitForIdle()

        // Result is still there, over the same board, not the map.
        waitFor("You won!")
        compose.waitForIdle()
        compose.onNodeWithText("You won!").assertExists()
        assertTrue(compose.onAllNodes(hasText("Today’s puzzle")).fetchSemanticsNodes().isEmpty())

        // Retry starts a new game of the same level.
        compose.onNodeWithContentDescription("Retry").performClick()
        gone("You won!")
        waitForSide("You, score 0, to move")
        assertEquals("1-1", (app.games.saved(GameMode.CAMPAIGN)?.record as? LevelRecord)?.level?.code)
    }
}
