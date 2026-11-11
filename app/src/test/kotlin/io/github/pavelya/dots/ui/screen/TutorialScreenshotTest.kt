package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.ai.Ai
import dots.campaign.Campaign
import dots.engine.Action
import dots.engine.GameState
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.levelGame
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The tutorial's levels with their step note and hand (the wireframes' "Tutorial level"), How to
 * play (the wireframes' "Rules") and Settings with Replay the tutorial, shown next to their frames
 * in the PR. Reference images in app/src/test/screenshots; they are recorded in CI (CLAUDE.md,
 * "Recording screenshots").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class TutorialScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /** Draws [content] as the phone would: 24 dp of status bar at the top, 12 dp of gesture bar at the bottom. */
    private fun show(content: @Composable () -> Unit) {
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface { Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp)) { content() } }
            }
        }
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")

    private fun waitFor(text: String) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    /** The computer places on the first free point, the top row, far from the rings. */
    private val firstFreePoint = object : Ai {
        override fun choose(state: GameState): Action = Action.Place(state.legalMoves().first())
    }

    private fun tutorialLevel(number: Int) = Campaign.level(1, number).let { level ->
        GameViewModel(
            start = levelGame(level, threatHighlight = false, now = 0),
            nextGame = { levelGame(level, threatHighlight = false, now = 0) },
            computerFor = { firstFreePoint },
            computerDispatcher = Dispatchers.Unconfined,
            replyDelayMillis = 0,
        )
    }

    /** Places a dot where the hand points, and waits for the computer's reply. */
    private fun followTheHand(vm: GameViewModel) {
        val point = vm.ui.value.guide!!.point!!
        compose.runOnUiThread { repeat(2) { vm.onTap(point) } }
        compose.waitUntil(timeoutMillis = 5_000) { !vm.ui.value.thinking }
    }

    @Test
    fun tutorialPlacing() {
        show { GameScreen(tutorialLevel(1), onToMap = {}, onRules = {}) }
        waitFor("Tap the circled point.")
        capture("tutorial_placing")
    }

    @Test
    fun tutorialPlacingByDrag() {
        show { GameScreen(tutorialLevel(1), onToMap = {}, onRules = {}, placing = PlacingMethod.DRAG) }
        waitFor("Press, slide to the circled point and lift.")
        capture("tutorial_placing_drag")
    }

    @Test
    fun tutorialLinking() {
        show { GameScreen(tutorialLevel(2), onToMap = {}, onRules = {}) }
        waitFor("One dot can join two rings: tap the circled point.")
        capture("tutorial_linking")
    }

    /** Level 3 one dot from the first ring: the wireframe's step 3 of 3. */
    private fun capturing() {
        val vm = tutorialLevel(3)
        show { GameScreen(vm, onToMap = {}, onRules = {}) }
        waitFor("Start a ring around a blue dot: tap the circled point.")
        followTheHand(vm)
        waitFor("Close the ring: tap the circled point.")
    }

    @Test
    fun tutorialCapturing() {
        capturing()
        capture("tutorial_capturing")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun tutorialCapturingOnASmallPhone() {
        capturing()
        capture("tutorial_capturing_small_phone")
    }

    @Test
    fun rules() {
        show { RulesScreen(onBack = {}, onPlayTutorial = {}) }
        capture("rules_screen")
    }

    @Test
    fun settings() {
        show { SettingsScreen(AppSettings(), onChange = {}, version = "1.0", onDetails = {}, onDeveloperMenu = {}, onReplayTutorial = {}, onBack = {}) }
        capture("settings_screen")
    }
}
