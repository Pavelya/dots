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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Reference images in app/src/test/screenshots; re-record with `./gradlew :app:recordRoborazziDebug`. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h400dp-xhdpi")
class BoardScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val style = BoardStyle.of(GameTheme.NOTEBOOK, dark = false)

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
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
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

    @Test
    fun plainTheme() = captureThemed("theme_board_plain", GameTheme.PLAIN, dark = false)

    @Test
    fun plainThemeDark() = captureThemed("theme_board_plain_dark", GameTheme.PLAIN, dark = true)

    @Test
    fun notebookTheme() = captureThemed("theme_board_notebook", GameTheme.NOTEBOOK, dark = false)

    @Test
    fun notebookThemeDark() = captureThemed("theme_board_notebook_dark", GameTheme.NOTEBOOK, dark = true)
}
