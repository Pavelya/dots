package io.github.pavelya.dots.ui

import androidx.compose.ui.test.assertIsDisplayed
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
            "Play the computer" to "The game screen arrives in task P-05.",
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
}
