package io.github.pavelya.dots.ui.board

import androidx.compose.ui.geometry.Offset
import dots.engine.Pos
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Where the board's points sit inside a drawing area of [width] x [height] pixels, and which point a
 * touch means. Every point gets a square cell of [spacing]; the grid is centred in the area.
 */
class BoardGeometry(val columns: Int, val rows: Int, val width: Float, val height: Float) {
    val spacing: Float = min(width / columns, height / rows)
    private val left = (width - spacing * (columns - 1)) / 2
    private val top = (height - spacing * (rows - 1)) / 2

    fun center(x: Int, y: Int): Offset = Offset(left + x * spacing, top + y * spacing)

    fun center(pos: Pos): Offset = center(pos.x, pos.y)

    /** The point nearest to a touch at [at], or null if the touch is more than half a cell off the board. */
    fun pointAt(at: Offset): Pos? {
        val x = ((at.x - left) / spacing).roundToInt()
        val y = ((at.y - top) / spacing).roundToInt()
        return if (x in 0 until columns && y in 0 until rows) Pos(x, y) else null
    }
}
