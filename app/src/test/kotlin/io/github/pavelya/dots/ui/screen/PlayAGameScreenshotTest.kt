package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.campaign.Campaign
import dots.campaign.Progress
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.GameLength
import io.github.pavelya.dots.data.GameMode
import io.github.pavelya.dots.game.FreeGame
import io.github.pavelya.dots.ui.theme.DotsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Play a game over the map (the wireframes' frame "Play a game", with and without a saved game),
 * and its setup as the canvas frames `play-setup`, `play-replace` and `small-play-setup` draw it,
 * with Open-ended chosen; each is shown next to its frame in the PR. Reference images in
 * app/src/test/screenshots; they are recorded in CI (CLAUDE.md, "Recording screenshots").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class PlayAGameScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /** The note over the map of a player halfway through World 1, the map dimmed behind it, with 24 dp of status bar and 12 of gesture bar. */
    private fun note(saved: Set<GameMode> = emptySet(), settings: AppSettings = AppSettings(), dark: Boolean = false) {
        val map = Campaign.map(Progress(Campaign.levels.take(6).associate { it.code to 2 }))
        val actions = MapActions(onLevel = {}, onDaily = {}, onPlay = {}, onRules = {}, onStats = {}, onShop = {}, onSettings = {})
        compose.setContent {
            DotsTheme(dark = dark) {
                Surface {
                    Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp)) {
                        MapScreen(map, actions)
                        PlayAGameNote(saved = saved.associateWith { FreeGame.saved }, settings = settings, onSettings = {}, onPlay = { _, _ -> }, onClose = {})
                    }
                }
            }
        }
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")

    @Test
    fun playAGame() {
        note()
        capture("play_a_game")
    }

    @Test
    fun playAGameWithASavedGame() {
        note(saved = setOf(GameMode.FREE_PLAY))
        capture("play_a_game_saved")
    }

    /** Canvas frame `play-setup`: the link opened the setup, with Open-ended chosen. */
    private fun setup(dark: Boolean, saved: Set<GameMode> = emptySet()) {
        note(saved, AppSettings(length = GameLength.OPEN_ENDED), dark)
        compose.onNodeWithText("Board 15 × 15 · Open-ended · change").performClick()
    }

    @Test
    fun playSetup() {
        setup(dark = false)
        capture("play_setup")
    }

    @Test
    fun playSetupDark() {
        setup(dark = true)
        capture("play_setup_dark")
    }

    /** Canvas frame `play-replace`: with the setup open, Against the computer asks before it replaces the saved game. */
    private fun replace(dark: Boolean) {
        setup(dark, saved = setOf(GameMode.FREE_PLAY))
        compose.onNodeWithText("Against the computer").performClick()
        compose.onNodeWithText("Replace your saved game?").assertExists()
    }

    @Test
    fun playReplace() {
        replace(dark = false)
        capture("play_replace")
    }

    @Test
    fun playReplaceDark() {
        replace(dark = true)
        capture("play_replace_dark")
    }

    /** Canvas frame `small-play-setup`: the setup at 320 x 568. */
    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun smallPlaySetup() {
        setup(dark = false)
        capture("small_play_setup")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun smallPlaySetupDark() {
        setup(dark = true)
        capture("small_play_setup_dark")
    }
}
