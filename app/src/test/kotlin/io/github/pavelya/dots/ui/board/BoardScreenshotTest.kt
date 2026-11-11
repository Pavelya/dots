package io.github.pavelya.dots.ui.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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

    private val style = BoardStyle.plain(lightColorScheme(), dark = false)

    private fun capture(
        name: String,
        state: GameState,
        lastMove: Pos? = null,
        preview: Pos? = null,
        dangers: List<Danger> = emptyList(),
        touch: BoardTouch = BoardTouch(),
    ) {
        compose.setContent {
            Box(Modifier.size(360.dp)) {
                BoardView(state, style, "board", Modifier.fillMaxSize(), lastMove, preview, dangers = dangers, touch = touch)
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

    /** A mid-game 15x15 board for the placing methods' marks, red to move. */
    private val midGame: GameState = GameState.start(
        15, 15, dotsPerPlayer = 33,
        dots = mapOf(
            Pos(6, 6) to Player.RED, Pos(8, 7) to Player.RED, Pos(4, 2) to Player.RED, Pos(7, 9) to Player.RED,
            Pos(7, 6) to Player.BLUE, Pos(6, 8) to Player.BLUE, Pos(2, 1) to Player.BLUE, Pos(9, 8) to Player.BLUE,
        ),
    )

    /** The magnifier method: a finger down at (7, 7), the magnifier above it. */
    @Test
    fun magnifier() {
        val finger = Pos(7, 7)
        // 360 dp across 15 points: 24 dp each, at 2 px per dp; the first point is half a cell in.
        val px = { i: Int -> (12f + 24f * i) * 2 }
        capture("board_15x15_magnifier", midGame, preview = finger, touch = BoardTouch(finger = Offset(px(7), px(7))))
    }

    /** The magnifier near the top edge moves beside the finger. */
    @Test
    fun magnifierNearTheTop() {
        val px = { i: Int -> (12f + 24f * i) * 2 }
        capture("board_15x15_magnifier_top", midGame, preview = Pos(3, 1), touch = BoardTouch(finger = Offset(px(3), px(1))))
    }

    /** The zoom method after its first tap: twice as large around the preview. */
    @Test
    fun zoomedIn() = capture("board_15x15_zoomed", midGame, preview = Pos(7, 7), touch = BoardTouch(zoom = 2f, zoomAround = Pos(7, 7)))

    /** The drag method, or a long press that has finished: the preview's row and column across the board. */
    @Test
    fun guides() = capture("board_15x15_guides", midGame, preview = Pos(7, 7), touch = BoardTouch(guides = 1f))

    /** A long press halfway: the row and column half way to the edges. */
    @Test
    fun guidesHalfway() = capture("board_15x15_guides_half", midGame, preview = Pos(7, 7), touch = BoardTouch(guides = 0.5f))

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
}
