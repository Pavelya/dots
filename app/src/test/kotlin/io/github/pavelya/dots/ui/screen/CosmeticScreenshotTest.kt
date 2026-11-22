package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.engine.MoveResult
import dots.engine.Player
import io.github.pavelya.dots.data.PieceStyleId
import io.github.pavelya.dots.data.RingEffectId
import io.github.pavelya.dots.ui.board.BoardInput
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.ThemeCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The piece styles and ring effects on the game screen (docs/design/cosmetics.md), in every theme
 * ([ThemeCase]), light and dark, with the usual and with high-contrast pieces. As on the canvas
 * frames (`cosmetic-hearts-and-clubs-notebook-light.png` and the others), a piece style is shown on
 * the threat, which has every piece and mark, and a ring effect after the big capture, which has
 * three rings. Images are named `game_threat_<style>` and `game_big_capture_<effect>`, then the
 * theme, `_contrast` and `_dark` ([ThemeCase.name]). Each is shown next to its frame in the PR.
 * Reference images in app/src/test/screenshots; they are recorded in CI (CLAUDE.md, "Recording
 * screenshots").
 *
 * These boards have no moment playing: they are also what the board shows with the phone's
 * animations removed, when a ring effect keeps its still look (cosmetics.md, Rules).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class CosmeticScreenshotTest(private val case: ThemeCase) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes(): List<Array<Any>> = ThemeCase.parameters()
    }

    @get:Rule
    val compose = createComposeRule()

    /** The game screen after [move], as GameScreenshotTest draws it: 24 dp of status bar at the top, 12 dp of gesture bar at the bottom. */
    private fun show(name: String, move: MoveResult, dark: Boolean, highContrast: Boolean, pieces: PieceStyleId = PieceStyleId.STANDARD, rings: RingEffectId = RingEffectId.STANDARD) {
        compose.setContent {
            DotsTheme(case.theme, dark, highContrast, pieces = pieces, rings = rings) {
                Surface {
                    Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp)) {
                        val ui = CanvasGame.ui(move)
                        GameBoard(ui, Player.RED, CanvasGame.parts(ui.state), input = BoardInput.NONE, onUndo = {}, onPause = {})
                    }
                }
            }
        }
        val suffix = (if (highContrast) "_contrast" else "") + (if (dark) "_dark" else "")
        compose.onRoot().captureRoboImage("src/test/screenshots/${case.name(name + suffix)}.png")
    }

    private fun pieces(style: PieceStyleId, dark: Boolean, highContrast: Boolean = false) =
        show("game_threat_${style.key.replace('-', '_')}", CanvasGame.threat, dark, highContrast, pieces = style)

    private fun rings(effect: RingEffectId, dark: Boolean, highContrast: Boolean = false) =
        show("game_big_capture_${effect.key.replace('-', '_')}", CanvasGame.bigCapture, dark, highContrast, rings = effect)

    @Test
    fun heartsAndClubs() = pieces(PieceStyleId.HEARTS_AND_CLUBS, dark = false)

    @Test
    fun heartsAndClubsDark() = pieces(PieceStyleId.HEARTS_AND_CLUBS, dark = true)

    @Test
    fun heartsAndClubsHighContrast() = pieces(PieceStyleId.HEARTS_AND_CLUBS, dark = false, highContrast = true)

    @Test
    fun heartsAndClubsHighContrastDark() = pieces(PieceStyleId.HEARTS_AND_CLUBS, dark = true, highContrast = true)

    @Test
    fun flameAndFrost() = pieces(PieceStyleId.FLAME_AND_FROST, dark = false)

    @Test
    fun flameAndFrostDark() = pieces(PieceStyleId.FLAME_AND_FROST, dark = true)

    @Test
    fun flameAndFrostHighContrast() = pieces(PieceStyleId.FLAME_AND_FROST, dark = false, highContrast = true)

    @Test
    fun flameAndFrostHighContrastDark() = pieces(PieceStyleId.FLAME_AND_FROST, dark = true, highContrast = true)

    @Test
    fun lasso() = rings(RingEffectId.LASSO, dark = false)

    @Test
    fun lassoDark() = rings(RingEffectId.LASSO, dark = true)

    @Test
    fun lassoHighContrast() = rings(RingEffectId.LASSO, dark = false, highContrast = true)

    @Test
    fun lassoHighContrastDark() = rings(RingEffectId.LASSO, dark = true, highContrast = true)

    @Test
    fun rubberStamp() = rings(RingEffectId.RUBBER_STAMP, dark = false)

    @Test
    fun rubberStampDark() = rings(RingEffectId.RUBBER_STAMP, dark = true)

    @Test
    fun rubberStampHighContrast() = rings(RingEffectId.RUBBER_STAMP, dark = false, highContrast = true)

    @Test
    fun rubberStampHighContrastDark() = rings(RingEffectId.RUBBER_STAMP, dark = true, highContrast = true)
}
