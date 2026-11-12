package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.campaign.Campaign
import dots.campaign.Progress
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.GameMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Play a game over the map (the wireframes' frame "Play a game", with and without a saved game),
 * and its setup, shown next to the frame in the PR. Reference images in app/src/test/screenshots;
 * they are recorded in CI (CLAUDE.md, "Recording screenshots").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class PlayAGameScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /** The note over the map of a player halfway through World 1, the map dimmed behind it, with 24 dp of status bar and 12 of gesture bar. */
    private fun note(saved: Set<GameMode> = emptySet()) {
        val map = Campaign.map(Progress(Campaign.levels.take(6).associate { it.code to 2 }))
        val actions = MapActions(onLevel = {}, onDaily = {}, onPlay = {}, onRules = {}, onStats = {}, onShop = {}, onSettings = {})
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface {
                    Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp)) {
                        MapScreen(map, actions)
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                        PlayAGameNote(saved = saved, settings = AppSettings(), onSettings = {}, onPlay = { _, _ -> }, onClose = {})
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

    @Test
    fun playAGameSetup() {
        note()
        compose.onNodeWithText("Board 15 × 15 · Short · change").performClick()
        capture("play_a_game_setup")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun playAGameSetupOnASmallPhone() {
        note()
        compose.onNodeWithText("Board 15 × 15 · Short · change").performClick()
        capture("play_a_game_setup_small_phone")
    }
}
