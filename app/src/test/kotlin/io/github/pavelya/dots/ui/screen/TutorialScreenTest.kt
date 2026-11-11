package io.github.pavelya.dots.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import dots.ai.SearchAi
import dots.campaign.Level
import dots.campaign.LevelResult
import dots.campaign.Tutorial
import dots.engine.Player
import io.github.pavelya.dots.data.CampaignJournal
import io.github.pavelya.dots.data.LevelEnd
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.levelGame
import io.github.pavelya.dots.ui.board.BoardGeometry
import io.github.pavelya.dots.ui.theme.DotsTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/**
 * The tutorial (A-06) on the game screen, by finger, with both placing methods (decision 0024):
 * a player who only does what the step note and the hand say passes levels 1 to 3, played as
 * practice, which saves nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class TutorialScreenTest {
    @get:Rule
    val compose = createComposeRule()

    /** Counts what a game would save to the campaign: nothing, in practice. */
    private class Journal : CampaignJournal {
        var saved = 0

        override suspend fun finished(level: Level, result: LevelResult): LevelEnd {
            saved++
            error("a practice game saved its end")
        }
    }

    private fun practice(journal: CampaignJournal = Journal()) = GameViewModel(
        start = levelGame(Tutorial.levels.first(), threatHighlight = false, now = 0, practice = true),
        nextGame = { error("a practice game starts its next game itself") },
        computerFor = { entry -> SearchAi((entry.record as dots.campaign.LevelRecord).level.ai, Random(1)) },
        campaign = journal,
        computerDispatcher = Dispatchers.Default,
        replyDelayMillis = 0,
        clock = { 0L },
    )

    /** Waits for the player's turn with a point to tap, or for Result. */
    private fun waitForTurn(vm: GameViewModel) = compose.waitUntil(timeoutMillis = 10_000) {
        val ui = vm.ui.value
        ui.showsResult || (!ui.thinking && ui.state.toMove == Player.RED && ui.guide?.point != null)
    }

    private fun waitFor(text: String) =
        compose.waitUntil(timeoutMillis = 10_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    /** Places a dot where the hand points, by [method]. */
    private fun followTheHand(vm: GameViewModel, method: PlacingMethod) {
        val state = vm.ui.value.state
        val point = vm.ui.value.guide!!.point!!
        compose.onNodeWithTag(BOARD_TAG).performTouchInput {
            val board = BoardGeometry(state.width, state.height, width.toFloat(), height.toFloat())
            when (method) {
                PlacingMethod.TWO_TAPS -> {
                    click(board.center(point))
                    click(board.center(point))
                }
                PlacingMethod.DRAG -> {
                    down(board.center(0, 0))
                    moveTo(board.center(point))
                    up()
                }
            }
        }
        compose.waitForIdle()
    }

    private fun playTheTutorial(method: PlacingMethod) {
        val journal = Journal()
        val vm = practice(journal)
        var toMap = 0
        compose.setContent { DotsTheme { GameScreen(vm, onToMap = { toMap++ }, onRules = {}, placing = method) } }

        for ((i, level) in Tutorial.levels.withIndex()) {
            assertEquals(level, vm.ui.value.level)
            assertTrue(vm.ui.value.practice)
            waitFor("STEP ${i + 1} OF 3 · ${listOf("PLACING", "LINKING", "CAPTURING")[i]}")
            while (true) {
                waitForTurn(vm)
                if (vm.ui.value.showsResult) break
                followTheHand(vm, method)
            }
            waitFor("You won!")
            compose.onNodeWithText("Next").performClick()
            compose.waitForIdle()
        }
        assertEquals(1, toMap)
        assertEquals(0, journal.saved)
    }

    @Test
    fun `with two taps, following the hand passes levels 1 to 3, and practice saves nothing`() {
        playTheTutorial(PlacingMethod.TWO_TAPS)
    }

    @Test
    fun `with drag, following the hand passes levels 1 to 3, and practice saves nothing`() {
        playTheTutorial(PlacingMethod.DRAG)
    }

    @Test
    fun `level 1 teaches two taps and says that drag can be chosen in Settings`() {
        val vm = practice()
        compose.setContent { DotsTheme { GameScreen(vm, onToMap = {}, onRules = {}) } }
        waitFor("Tap the circled point.")
        compose.onNodeWithText("Rather drag than tap twice? Choose it in Settings.").assertIsDisplayed()

        val point = vm.ui.value.guide!!.point!!
        compose.onNodeWithTag(BOARD_TAG).performTouchInput {
            click(BoardGeometry(7, 7, width.toFloat(), height.toFloat()).center(point))
        }
        waitFor("Tap it again to place your dot.")
    }

    @Test
    fun `with drag, level 1 says how to drag, and nothing about Settings`() {
        val vm = practice()
        compose.setContent { DotsTheme { GameScreen(vm, onToMap = {}, onRules = {}, placing = PlacingMethod.DRAG) } }
        waitFor("Press, slide to the circled point and lift.")
        compose.onNodeWithText("Rather drag than tap twice? Choose it in Settings.").assertDoesNotExist()
    }

    @Test
    fun `Skip hides the steps and says so, and the board still plays`() {
        val vm = practice()
        var skipped = 0
        compose.setContent { DotsTheme { GameScreen(vm, onToMap = {}, onRules = {}, onSkipGuides = { skipped++ }) } }
        waitFor("Tap the circled point.")

        compose.onNodeWithText("Skip").performClick()
        compose.waitForIdle()
        assertEquals(1, skipped)
        assertNull(vm.ui.value.guide)
        compose.onNodeWithText("Tap the circled point.").assertDoesNotExist()
    }

    @Test
    fun `steps skipped earlier stay hidden`() {
        val vm = practice()
        compose.setContent { DotsTheme { GameScreen(vm, onToMap = {}, onRules = {}, guidesSkipped = true) } }
        compose.waitUntil(timeoutMillis = 5_000) { vm.ui.value.guide != null }
        compose.mainClock.advanceTimeBy(OPENING_MILLIS + 100)
        compose.onNodeWithText("Tap the circled point.").assertDoesNotExist()
        compose.onNodeWithText("Skip").assertDoesNotExist()
    }
}
