package io.github.pavelya.dots.ui.screen

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.toPath
import io.github.pavelya.dots.ui.theme.Palette

/**
 * Each world's board touches (decision 0028; docs/design/illustrations.md, "Board touches"): three
 * or four light pencil doodles in the empty paper beside the opponent's photo, left of it and right
 * of it under the speech bubble. Never on or next to the board, the tool bar, the mission note or
 * the bubble. Line doodles in the theme's Red pale and Blue pale, 1.6 dp, no fill, so they follow the
 * chosen theme and never change the pieces, rings, fills or sounds.
 *
 * The paths are the canvas's (docs/design/canvas/draw.py, `TOUCHES`), in dp on its 360 dp frame,
 * where the photo's row starts [ROW_TOP] dp down. The left ones keep to the screen's left edge
 * and the right ones to its right edge, so on a narrow phone they stay clear of the photo.
 */
object BoardTouches {
    /** One ink's doodles on one side of the screen, as path data on the canvas's frame. */
    class Touch(val path: String, val red: Boolean, val right: Boolean) {
        internal val drawn: Path by lazy { addPathNodes(path).toPath() }
    }

    /** Where the photo's row starts on the canvas's frame: the photo is at 100 dp, 8 dp into the row. */
    const val ROW_TOP = 92f

    /** The canvas's width, from whose right edge the right touches are measured. */
    const val FRAME_WIDTH = 360f

    private val TOUCHES: Map<Int, List<Touch>> = mapOf(
        // A wave and a star; a spiral and a paper plane.
        1 to listOf(
            Touch("M12 132 q8 -10 16 0 q8 10 16 0 M26 186 l4 9 9 1 -7 6 2 9 -8 -5 -8 5 2 -9 -7 -6 9 -1z", red = false, right = false),
            Touch("M326 236 a10 10 0 1 1 -3 -7 M296 196 l26 -8 -8 26 -5 -11z M296 196 l13 7", red = false, right = true),
        ),
        // A flower and a leaf; a butterfly and a tulip.
        2 to listOf(
            Touch(
                "M31 128 a3 3 0 1 0 -6 0 a3 3 0 1 0 6 0 M36.71 132.15 a3 3 0 1 0 -6 0 a3 3 0 1 0 6 0 " +
                    "M34.53 138.85 a3 3 0 1 0 -6 0 a3 3 0 1 0 6 0 M27.47 138.85 a3 3 0 1 0 -6 0 a3 3 0 1 0 6 0 " +
                    "M25.29 132.15 a3 3 0 1 0 -6 0 a3 3 0 1 0 6 0 M26 134 a2 2 0 1 0 4 0 a2 2 0 1 0 -4 0",
                red = true, right = false,
            ),
            Touch("M28 142 v16 M28 152 q6 -8 12 -6 q-4 8 -12 6 M20 214 q2 -18 20 -20 q0 18 -20 20 M20 214 l12 -12", red = false, right = false),
            Touch("M318 252 v-14 M312 238 q-1 -9 2 -10 l3 4 l1 -5 l1 5 l3 -4 q3 1 2 10 z", red = false, right = true),
            Touch(
                "M312 204 v8 M312 207 c-5 -9 -14 -6 -11 1 c2 4 7 2 11 -1 M312 207 c5 -9 14 -6 11 1 c-2 4 -7 2 -11 -1 " +
                    "M312 209 c-4 2 -7 7 -3 7 c2 0 3 -4 3 -7 M312 209 c4 2 7 7 3 7 c-2 0 -3 -4 -3 -7",
                red = true, right = true,
            ),
        ),
        // A crescent moon and a star; a comet and a twinkle.
        3 to listOf(
            Touch(
                "M34 122 a16 16 0 1 0 0 32 a22 22 0 0 1 0 -32 z M30 191 L32.38 196.72 L38.56 197.22 L33.85 201.25 " +
                    "L35.29 207.28 L30 204.05 L24.71 207.28 L26.15 201.25 L21.44 197.22 L27.62 196.72 Z",
                red = false, right = false,
            ),
            Touch("M318 228 v18 M309 237 h18", red = false, right = true),
            Touch("M318 190 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 M318 193 l-20 12 M320 196 l-14 14 M317 190 l-22 4", red = true, right = true),
        ),
        // A fish and a shell; a boat and a wave.
        4 to listOf(
            Touch("M14 138 q13 -11 26 0 q-13 11 -26 0 z M40 138 l7 -5 v10 z", red = false, right = false),
            Touch("M18 212 q12 -26 24 0 z M30 212 v-13 M24 212 l3 -12 M36 212 l-3 -12", red = true, right = false),
            Touch(
                "M296 212 h28 l-5 7 h-18 z M310 212 V190 M310 193 l11 15 h-11 M300 240 q4 -5 8 0 q4 5 8 0 q4 -5 8 0",
                red = false, right = true,
            ),
        ),
    )

    /** The touches of [world]; none for a world without them. */
    fun of(world: Int): List<Touch> = TOUCHES[world].orEmpty()
}

/**
 * Draws the board touches of [world] behind the row that holds the opponent's photo, which spans
 * the screen's width. Nothing for no world (free play, two players, the daily puzzle).
 */
internal fun Modifier.boardTouches(world: Int?, style: Palette): Modifier {
    val touches = world?.let(BoardTouches::of).orEmpty()
    if (touches.isEmpty()) return this
    return drawBehind {
        val dp = density
        for (touch in touches) {
            val left = if (touch.right) size.width / dp - BoardTouches.FRAME_WIDTH else 0f
            translate(left * dp, -BoardTouches.ROW_TOP * dp) {
                scale(dp, dp, pivot = Offset.Zero) {
                    drawPath(
                        touch.drawn,
                        if (touch.red) style.redPale else style.bluePale,
                        style = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}
