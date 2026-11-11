package io.github.pavelya.dots.ui.board

import androidx.compose.ui.geometry.Offset
import dots.engine.Pos
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Where the board's points sit inside a drawing area of [width] x [height] pixels, and which point a
 * touch means. Every point gets a square cell; the grid is centred in the area.
 *
 * A [zoom] above 1 enlarges the board around [focus], a spot in the area that stays where it is,
 * as when the board is zoomed in on a tap; the area then shows only part of the board.
 */
class BoardGeometry(
    val columns: Int,
    val rows: Int,
    val width: Float,
    val height: Float,
    val zoom: Float = 1f,
    private val focus: Offset = Offset.Zero,
) {
    /** The cell size before zooming. */
    private val unit: Float = min(width / columns, height / rows)
    private val left = (width - unit * (columns - 1)) / 2
    private val top = (height - unit * (rows - 1)) / 2

    /** The distance between two neighbouring points, as drawn. */
    val spacing: Float = unit * zoom

    fun center(x: Int, y: Int): Offset =
        Offset(focus.x + (left + x * unit - focus.x) * zoom, focus.y + (top + y * unit - focus.y) * zoom)

    fun center(pos: Pos): Offset = center(pos.x, pos.y)

    /** The point nearest to a touch at [at], or null if the touch is more than half a cell off the board. */
    fun pointAt(at: Offset): Pos? {
        val x = (((at.x - focus.x) / zoom + focus.x - left) / unit).roundToInt()
        val y = (((at.y - focus.y) / zoom + focus.y - top) / unit).roundToInt()
        return if (x in 0 until columns && y in 0 until rows) Pos(x, y) else null
    }

    /** This board, not zoomed itself, enlarged [by] times around [point], which stays where it is. */
    fun zoomed(by: Float, point: Pos): BoardGeometry = BoardGeometry(columns, rows, width, height, by, center(point))
}
