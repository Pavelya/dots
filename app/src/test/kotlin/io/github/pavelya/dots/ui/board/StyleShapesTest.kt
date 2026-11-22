package io.github.pavelya.dots.ui.board

import androidx.compose.ui.geometry.Offset
import io.github.pavelya.dots.data.PieceStyleId
import io.github.pavelya.dots.data.RingEffectId
import io.github.pavelya.dots.ui.theme.GameTheme
import io.github.pavelya.dots.ui.theme.PieceShape
import io.github.pavelya.dots.ui.theme.RingEffect
import io.github.pavelya.dots.ui.theme.dressed
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The piece styles' shapes measure what docs/design/cosmetics.md says (its "Piece styles" tables,
 * as `python3 docs/design/pieces/cosmetics.py measure` prints them), and a piece style or ring
 * effect changes only what it should.
 */
class StyleShapesTest {
    /** Width, height, top, bottom, area and reach from the point, in dot sizes, from cosmetics.py measure. */
    private val measured = mapOf(
        PieceShape.HEART to listOf(1.20, 1.09, -0.42, 0.67, 0.80, 0.67),
        PieceShape.CLUB to listOf(1.06, 1.15, -0.57, 0.58, 0.77, 0.62),
        PieceShape.FLAME to listOf(0.96, 1.20, -0.68, 0.52, 0.80, 0.68),
        PieceShape.SNOWFLAKE to listOf(1.10, 1.24, -0.62, 0.62, 0.74, 0.62),
    )

    /** Whether [p] is inside the closed run of [points] (even-odd crossing). */
    private fun inside(points: List<Offset>, p: Offset): Boolean {
        var crossings = false
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            if ((a.y > p.y) != (b.y > p.y) && p.x < a.x + (p.y - a.y) * (b.x - a.x) / (b.y - a.y)) crossings = !crossings
        }
        return crossings
    }

    @Test
    fun `each shape has the size, area and reach of its page`() {
        for ((shape, values) in measured) {
            val (width, height, top, bottom) = values
            val area = values[4]
            val reach = values[5]
            val parts = StyleShapes.parts(shape)!!
            val box = StyleShapes.bounds(shape)!!
            assertEquals("$shape width", width, box.width.toDouble(), 0.006)
            assertEquals("$shape height", height, box.height.toDouble(), 0.006)
            assertEquals("$shape top", top, box.top.toDouble(), 0.006)
            assertEquals("$shape bottom", bottom, box.bottom.toDouble(), 0.006)
            assertEquals("$shape reach", reach, parts.flatten().maxOf { hypot(it.x, it.y) }.toDouble(), 0.006)
            // Overlapping parts are one area of ink: count the points of a fine grid inside any part.
            val step = 0.005f
            var count = 0
            var y = box.top
            while (y < box.bottom) {
                var x = box.left
                while (x < box.right) {
                    if (parts.any { inside(it, Offset(x, y)) }) count++
                    x += step
                }
                y += step
            }
            assertEquals("$shape area", area, count * step.toDouble() * step, 0.01)
        }
    }

    @Test
    fun `the theme's own pieces have no piece style shape`() {
        for (shape in listOf(PieceShape.DOT, PieceShape.DIAMOND, PieceShape.STAR, PieceShape.COMET, PieceShape.TRIANGLE)) {
            assertEquals(null, StyleShapes.parts(shape))
        }
    }

    @Test
    fun `a piece style changes only the pieces, a ring effect only the rings, and Standard nothing`() {
        for (theme in GameTheme.ALL) for (board in listOf(theme.board, theme.highContrastBoard)) {
            assertSame(board, board.dressed(PieceStyleId.STANDARD, RingEffectId.STANDARD))
            val cards = board.dressed(PieceStyleId.HEARTS_AND_CLUBS, RingEffectId.STANDARD)
            assertEquals(board.copy(red = cards.red, blue = cards.blue), cards)
            assertEquals(listOf(PieceShape.HEART, PieceShape.CLUB), listOf(cards.red.shape, cards.blue.shape))
            val fire = board.dressed(PieceStyleId.FLAME_AND_FROST, RingEffectId.STANDARD)
            assertEquals(listOf(PieceShape.FLAME, PieceShape.SNOWFLAKE), listOf(fire.red.shape, fire.blue.shape))
            val lasso = board.dressed(PieceStyleId.STANDARD, RingEffectId.LASSO)
            assertEquals(board.copy(ringEffect = RingEffect.LASSO), lasso)
            assertEquals(board.copy(ringEffect = RingEffect.STAMP), board.dressed(PieceStyleId.STANDARD, RingEffectId.RUBBER_STAMP))
        }
    }
}
