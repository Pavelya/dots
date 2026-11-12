package io.github.pavelya.dots.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.BoardSize
import io.github.pavelya.dots.data.GameLength
import io.github.pavelya.dots.data.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Play a game over the map: the two cards, the setup behind the link, and the question before a saved game is replaced. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class PlayAGameTest {
    @get:Rule
    val compose = createComposeRule()

    private var latest = AppSettings()
    private val played = mutableListOf<Pair<GameMode, Boolean>>()

    private fun show(saved: Set<GameMode> = emptySet()) {
        compose.setContent {
            var settings by remember { mutableStateOf(AppSettings()) }
            PlayAGameNote(
                saved = saved,
                settings = settings,
                onSettings = { change -> settings = change(settings); latest = settings },
                onPlay = { mode, replace -> played += mode to replace },
                onClose = {},
            )
        }
    }

    @Test
    fun `a card starts its game at once, with the defaults`() {
        show()
        compose.onNodeWithText("at your campaign strength").assertIsDisplayed()
        compose.onNodeWithText("pass the phone, pure rules").assertIsDisplayed()

        compose.onNodeWithText("Against the computer").performClick()
        compose.onNodeWithText("Two players").performClick()

        assertEquals(listOf(GameMode.FREE_PLAY to false, GameMode.TWO_PLAYERS to false), played)
    }

    @Test
    fun `the link opens the setup, where a bigger board updates the dots and the game can be open-ended`() {
        show()
        compose.onNodeWithText("Board 15 × 15 · Short · change").performClick()
        compose.onNodeWithText("15 × 15").assertIsSelected()
        compose.onNodeWithText("33 dots").assertIsDisplayed()

        compose.onNodeWithText("20 × 20").performClick()
        assertEquals(BoardSize.LARGE, latest.boardSize)
        compose.onNodeWithText("60 dots").assertIsDisplayed()
        compose.onNodeWithText("120 dots").assertIsDisplayed()

        compose.onNodeWithText("Open-ended").performClick()
        assertEquals(GameLength.OPEN_ENDED, latest.length)
        compose.onNodeWithText("Open-ended").assertIsSelected()
    }

    @Test
    fun `a saved game resumes, and with the setup open, a new game asks before it replaces the saved one`() {
        show(saved = setOf(GameMode.FREE_PLAY))
        compose.onNodeWithText("resume your game").assertIsDisplayed()
        compose.onNodeWithText("Against the computer").performClick()
        assertEquals(GameMode.FREE_PLAY to false, played.single())

        compose.onNodeWithText("Board 15 × 15 · Short · change").performClick()
        compose.onNodeWithText("Against the computer").performClick()
        compose.onNodeWithText("Replace your saved game?").assertIsDisplayed()
        compose.onNodeWithText("Keep it").performClick()
        assertEquals(1, played.size)

        compose.onNodeWithText("Against the computer").performClick()
        compose.onNodeWithText("New game").performClick()
        assertEquals(GameMode.FREE_PLAY to true, played.last())

        // Two players has nothing saved: no question.
        compose.onNodeWithText("Keep it").performClick()
        compose.onNodeWithText("Two players").performClick()
        assertEquals(GameMode.TWO_PLAYERS to true, played.last())
        assertTrue(played.size == 3)
    }
}
