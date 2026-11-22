package io.github.pavelya.dots.ui.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.ai.Danger
import dots.ai.Hints
import dots.engine.GameSettings
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import io.github.pavelya.dots.ui.theme.GameTheme
import io.github.pavelya.dots.ui.theme.ThemeCase
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Boards in each theme ([ThemeCase]): Notebook's images keep their names, the other themes' take
 * the theme's name. Reference images in app/src/test/screenshots; they are recorded in CI
 * (CLAUDE.md, "Recording screenshots").
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h400dp-xhdpi")
class BoardScreenshotTest(private val case: ThemeCase) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes(): List<Array<Any>> = ThemeCase.parameters()
    }

    @get:Rule
    val compose = createComposeRule()

    private val style = BoardStyle.of(case.theme, dark = false)

    private fun capture(
        name: String,
        state: GameState,
        lastMove: Pos? = null,
        preview: Pos? = null,
        dangers: List<Danger> = emptyList(),
        guides: Boolean = false,
    ) {
        compose.setContent {
            Box(Modifier.size(360.dp)) {
                BoardView(state, style, "board", Modifier.fillMaxSize(), lastMove, preview, dangers = dangers, guides = guides)
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/${case.name(name)}.png")
    }

    @Test
    fun startOfAPocGame() = capture("board_15x15_start", GameSettings.POC.copy(seed = 1).newGame())

    @Test
    fun captureWithAreaAndLastMove() {
        val start = GameState.start(
            15, 15, dotsPerPlayer = 33,
            dots = mapOf(
                Pos(6, 5) to Player.RED, Pos(5, 6) to Player.RED, Pos(7, 6) to Player.RED,
                Pos(6, 6) to Player.BLUE, Pos(3, 3) to Player.BLUE, Pos(10, 10) to Player.BLUE,
            ),
        )
        capture("board_15x15_capture", start.play(Pos(6, 7)).state, lastMove = Pos(6, 7))
    }

    @Test
    fun largeBoardWithPreview() {
        val state = GameSettings(width = 20, height = 20, dotsPerPlayer = 60, seed = 2).newGame()
        capture("board_20x20_preview", state, preview = Pos(3, 4))
    }

    /** A mid-game 15x15 board, red to move. */
    private val midGame: GameState = GameState.start(
        15, 15, dotsPerPlayer = 33,
        dots = mapOf(
            Pos(6, 6) to Player.RED, Pos(8, 7) to Player.RED, Pos(4, 2) to Player.RED, Pos(7, 9) to Player.RED,
            Pos(7, 6) to Player.BLUE, Pos(6, 8) to Player.BLUE, Pos(2, 1) to Player.BLUE, Pos(9, 8) to Player.BLUE,
        ),
    )

    /** Placing by drag, the finger down at (7, 7): the preview's row and column across the board. */
    @Test
    fun guides() = capture("board_15x15_guides", midGame, preview = Pos(7, 7), guides = true)

    @Test
    fun threatHighlight() {
        // Blue's dot in the middle is one red dot from capture (closing point below it); the two
        // red dots at the bottom are two blue dots away.
        val state = GameState.fromAscii(
            """
            .........
            ....R....
            ...RBR...
            .........
            .........
            .....B...
            ....BRB..
            .....RB..
            .........
            """,
            dotsPerPlayer = 20,
        )
        val dangers = Player.entries.flatMap { Hints.dangers(state, it) }
        capture("board_threat_highlight", state, dangers = dangers)
    }

    /**
     * One position in each theme, light and dark: a capture (wash, captured dot, last move), a
     * threat with its closing point, links, and Blue's preview. The theme changes only data;
     * the drawing is the same code.
     */
    private val themed: GameState = GameState.fromAscii(
        """
        .........
        ....R....
        ...RBR...
        .........
        .........
        .....B...
        ....BRB..
        .....RB..
        .........
        """,
        dotsPerPlayer = 20,
    ).play(Pos(4, 3)).state

    private fun captureThemed(name: String, theme: GameTheme, dark: Boolean) {
        val dangers = Player.entries.flatMap { Hints.dangers(themed, it) }
        compose.setContent {
            Box(Modifier.size(360.dp)) {
                BoardView(
                    themed, BoardStyle.of(theme, dark), "board", Modifier.fillMaxSize(),
                    lastMove = Pos(4, 3), preview = Pos(1, 7), showLinks = true, dangers = dangers,
                )
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    /**
     * Every piece and mark of pieces.md on one board: a red capture and a blue capture (each
     * captured piece with its cut), Red's last move (the pinhole), a blue piece one move from
     * capture with its closing point, and Blue's preview with drag's guides, one arm running
     * through the red area.
     */
    private val allPieces: GameState = GameState.fromAscii(
        """
        .........
        ....R....
        ...RBR...
        .........
        ......B..
        .....BRB.
        .R.......
        RBR......
        .........
        """,
        dotsPerPlayer = 20,
    ).play(Pos(4, 3)).state.play(Pos(6, 6)).state.play(Pos(7, 1)).state

    private fun capturePieces(name: String, dark: Boolean, highContrast: Boolean) {
        val dangers = Player.entries.flatMap { Hints.dangers(allPieces, it) }
        compose.setContent {
            Box(Modifier.size(360.dp)) {
                BoardView(
                    allPieces, BoardStyle.of(case.theme, dark, highContrast), "board", Modifier.fillMaxSize(),
                    lastMove = Pos(7, 1), preview = Pos(4, 6), dangers = dangers, guides = true,
                )
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/${case.name(name)}.png")
    }

    @Test
    fun pieces() = capturePieces("board_pieces", dark = false, highContrast = false)

    @Test
    fun piecesDark() = capturePieces("board_pieces_dark", dark = true, highContrast = false)

    @Test
    fun piecesHighContrast() = capturePieces("board_pieces_contrast", dark = false, highContrast = true)

    @Test
    fun piecesHighContrastDark() = capturePieces("board_pieces_contrast_dark", dark = true, highContrast = true)

    /**
     * A level's special points (A-15): holes at the corners give the board an odd outline, a hole
     * inside it is a gap, a blocked point carries the wall's mark, and two bonus points carry the
     * pearl's. Blue's dot on one pearl has just been captured, for 2 points; the other is empty.
     */
    private fun captureSpecialPoints(name: String, dark: Boolean) {
        val start = GameState.fromAscii(
            """
            --.......--
            -.........-
            ...........
            ....R...+..
            ...R+R.....
            ...........
            ...B..--...
            ...BR.-....
            .....#.....
            -.....B...-
            --.......--
            """,
            dotsPerPlayer = 20,
            first = Player.BLUE,
        )
        val captured = start.play(Pos(4, 4)).state.play(Pos(4, 5))
        assertEquals(listOf(2), captured.captures.map { it.points })
        compose.setContent {
            Box(Modifier.size(360.dp)) {
                BoardView(captured.state, BoardStyle.of(case.theme, dark), "board", Modifier.fillMaxSize(), lastMove = Pos(4, 5))
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/${case.name(name)}.png")
    }

    @Test
    fun specialPoints() = captureSpecialPoints("board_special_points", dark = false)

    @Test
    fun specialPointsDark() = captureSpecialPoints("board_special_points_dark", dark = true)

    /** The POC's plain look, once. */
    @Test
    fun plainTheme() {
        assumeTrue(case.isNotebook)
        captureThemed("theme_board_plain", GameTheme.PLAIN, dark = false)
    }

    @Test
    fun plainThemeDark() {
        assumeTrue(case.isNotebook)
        captureThemed("theme_board_plain_dark", GameTheme.PLAIN, dark = true)
    }

    @Test
    fun theme() = captureThemed("theme_board_${case.key}", case.theme, dark = false)

    @Test
    fun themeDark() = captureThemed("theme_board_${case.key}_dark", case.theme, dark = true)
}
