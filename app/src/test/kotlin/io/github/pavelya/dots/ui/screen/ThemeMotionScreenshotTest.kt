package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.ai.Hints
import dots.engine.GameState
import dots.engine.MoveResult
import dots.engine.Player
import dots.engine.Pos
import io.github.pavelya.dots.game.BoardMoment
import io.github.pavelya.dots.game.ThreatMark
import io.github.pavelya.dots.ui.board.BoardStyle
import io.github.pavelya.dots.ui.board.BoardView
import io.github.pavelya.dots.ui.theme.ThemeCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The moments Star chart and Neon arcade change (docs/design/motion.md, "Star chart" and "Neon
 * arcade"), on the board at the exact times of each theme's strip (`strip-capture-star-chart.png`
 * and the others): the time is handed to the board, not stepped on a clock, so a 40 ms flicker
 * shows where the strip shows it. The boards are the canvas's game of Level 7 ([CanvasGame]).
 * Frames are named `motion_<moment>_<theme>_<time>`. Reference images in
 * app/src/test/screenshots; they are recorded in CI (CLAUDE.md, "Recording screenshots").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h360dp-xhdpi")
class ThemeMotionScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun dangers(state: GameState) = Player.entries.flatMap { Hints.dangers(state, it) }

    /**
     * The board after [move] in [case]'s light version, with the moment of [by]'s [dot] at each of
     * [times] ms; the board showed [before], with the pinhole on [pinholeBefore], until then.
     */
    private fun frames(name: String, case: ThemeCase, before: GameState, move: MoveResult, dot: Pos, by: Player, pinholeBefore: Pos?, vararg times: Int) {
        val after = dangers(move.state)
        val known = dangers(before).map(ThreatMark::of).toSet()
        val moment = BoardMoment(
            id = 1,
            dot = dot,
            by = by,
            announced = by == Player.BLUE,
            captures = move.captures,
            pinholeBefore = pinholeBefore,
            freshThreats = after.map(ThreatMark::of).filter { it !in known }.toSet(),
        )
        var now by mutableFloatStateOf(0f)
        compose.setContent {
            Box(Modifier.size(360.dp)) {
                BoardView(
                    move.state, BoardStyle.of(case.theme, dark = false), "board", Modifier.fillMaxSize(),
                    lastMove = dot, dangers = after, moment = moment, time = { now },
                    edgeMarks = dot.takeIf { by == Player.BLUE },
                )
            }
        }
        for (time in times) {
            now = time.toFloat()
            compose.waitForIdle()
            compose.onRoot().captureRoboImage("src/test/screenshots/motion_${name}_${case.key}_$time.png")
        }
    }

    /** Red closes its ring at (4, 9): two dots (the motion page's "Capture"). */
    private fun capture(case: ThemeCase, vararg times: Int) =
        frames("capture", case, CanvasGame.beforeSmallCapture, CanvasGame.smallCapture, Pos(4, 9), Player.RED, Pos(11, 2), *times)

    /** Red closes the big ring at (5, 7): six dots at once (the motion page's "Big capture"). */
    private fun bigCapture(case: ThemeCase, vararg times: Int) =
        frames("big_capture", case, CanvasGame.threat.state, CanvasGame.bigCapture, Pos(5, 7), Player.RED, Pos(7, 7), *times)

    /** Star chart: the line traced star to star, each star twinkling as it is reached, the haze blending in. */
    @Test
    fun captureInStarChart() = capture(ThemeCase.STAR_CHART, 0, 200, 300, 400, 550, 650, 1250)

    @Test
    fun bigCaptureInStarChart() = bigCapture(ThemeCase.STAR_CHART, 0, 250, 400, 550, 650, 750, 1400)

    /** Neon arcade: the tube drawn unlit, lit at 550 ms, dark again from 590 to 630 ms, sparks; the captured go dark. */
    @Test
    fun captureInNeonArcade() = capture(ThemeCase.NEON_ARCADE, 0, 360, 560, 600, 640, 720, 1250)

    @Test
    fun bigCaptureInNeonArcade() = bigCapture(ThemeCase.NEON_ARCADE, 0, 400, 660, 700, 740, 820, 1400)

    /** Neon arcade: Red's disc at (10, 9) grows unlit, then lights up as it settles. */
    @Test
    fun placingInNeonArcade() {
        val before = CanvasGame.threat.state
        frames("placing", ThemeCase.NEON_ARCADE, before, before.play(Pos(10, 9)), Pos(10, 9), Player.RED, Pos(7, 7), 0, 80, 120, 160, 200)
    }

    /** Neon arcade: the computer's triangle at (7, 7) lands with its ripples and lights up the same way. */
    @Test
    fun computersMoveInNeonArcade() =
        frames("turn", ThemeCase.NEON_ARCADE, CanvasGame.smallCapture.state, CanvasGame.threat, Pos(7, 7), Player.BLUE, Pos(4, 9), 0, 120, 160, 200, 450)
}
