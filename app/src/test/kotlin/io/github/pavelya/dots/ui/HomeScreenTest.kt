package io.github.pavelya.dots.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.performScrollTo
import dots.engine.GameSettings
import dots.log.GameLogEntry
import dots.log.Outcome
import io.github.pavelya.dots.DotsApplication
import org.junit.Assert.assertTrue
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.pavelya.dots.ui.screen.HomeScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val entries = listOf("Play the computer", "Two players", "Rules", "Settings")

    @Test
    fun `home offers the four POC entries and each button reports its own click`() {
        val clicked = mutableListOf<String>()
        compose.setContent {
            HomeScreen(
                canContinue = false,
                onContinue = {},
                onPlayComputer = { clicked += "computer" },
                onTwoPlayers = { clicked += "two" },
                onRules = { clicked += "rules" },
                onSettings = { clicked += "settings" },
            )
        }
        for (entry in entries) compose.onNodeWithText(entry).assertIsDisplayed().performClick()

        assertEquals(listOf("computer", "two", "rules", "settings"), clicked)
    }

    @Test
    fun `every entry opens its screen and back returns home`() {
        compose.setContent { DotsApp() }
        val notes = mapOf(
            "Play the computer" to "Your turn · 33 dots left",
            "Two players" to "Red to move · 33 dots left",
            "Rules" to "Capture more of the other player’s dots than they capture of yours.",
            "Settings" to "Game length",
        )
        for ((entry, note) in notes) {
            compose.onNodeWithText(entry).performClick()
            // Some screens first read the saved settings, so give them a moment to appear.
            compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasText(note)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(note).assertIsDisplayed()
            compose.onNodeWithText("Back").performClick()
            compose.onNodeWithText("Proof of concept").assertIsDisplayed()
        }
    }

    private fun waitFor(text: String) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun `a game left for the home screen can be continued`() {
        compose.setContent { DotsApp() }
        compose.onNodeWithText("Play the computer").performClick()
        waitFor("Your turn · 33 dots left")
        compose.onNodeWithText("Back").performClick()

        waitFor("Continue game")
        compose.onNodeWithText("Continue game").performClick()
        waitFor("Your turn · 33 dots left")
    }

    @Test
    fun `the game log is offered to the share sheet as a file`() {
        val app = RuntimeEnvironment.getApplication() as DotsApplication
        val finished = GameLogEntry.start(GameSettings.POC, "easy", threatHighlight = false, now = 0).copy(outcome = Outcome.FINISHED)
        app.games.finish(finished)
        compose.setContent { DotsApp() }
        compose.onNodeWithText("Settings").performClick()
        waitFor("1 game in the log, kept on this phone until you share it. It holds your moves and results, nothing else.")

        compose.onNodeWithText("Share the game log").performScrollTo().assertIsEnabled().performClick()

        compose.waitUntil(timeoutMillis = 5_000) { shadowOf(app).peekNextStartedActivity() != null }
        val chooser = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val send = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        val uri = send.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)!!
        assertEquals("content://io.github.pavelya.dots.logs/games/game-log.txt", uri.toString())
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
