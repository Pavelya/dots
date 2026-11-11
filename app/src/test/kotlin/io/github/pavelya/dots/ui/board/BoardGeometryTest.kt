package io.github.pavelya.dots.ui.board

import androidx.compose.ui.geometry.Offset
import dots.engine.Pos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardGeometryTest {
    private val boards = listOf(12, 15, 20)
    private val screens = listOf(1080f to 1080f, 1080f to 1500f, 720f to 600f)

    @Test
    fun `every point's centre, and anywhere within half a cell of it, means that point`() {
        for (n in boards) for ((w, h) in screens) {
            val g = BoardGeometry(n, n, w, h)
            val near = g.spacing * 0.49f
            for (x in 0 until n) for (y in 0 until n) {
                val c = g.center(x, y)
                for (d in listOf(Offset.Zero, Offset(near, 0f), Offset(-near, 0f), Offset(0f, near), Offset(0f, -near), Offset(near, -near))) {
                    assertEquals("$n on ${w}x$h at ($x,$y) moved by $d", Pos(x, y), g.pointAt(c + d))
                }
            }
        }
    }

    @Test
    fun `touches more than half a cell off the board mean no point`() {
        for (n in boards) for ((w, h) in screens) {
            val g = BoardGeometry(n, n, w, h)
            val far = g.spacing * 0.51f
            assertNull(g.pointAt(g.center(0, 0) - Offset(far, 0f)))
            assertNull(g.pointAt(g.center(0, 0) - Offset(0f, far)))
            assertNull(g.pointAt(g.center(n - 1, n - 1) + Offset(far, 0f)))
            assertNull(g.pointAt(g.center(n - 1, n - 1) + Offset(0f, far)))
        }
    }

    @Test
    fun `the grid is centred and uses the whole width of a tall screen`() {
        val g = BoardGeometry(15, 15, 1080f, 1500f)

        assertEquals(72f, g.spacing, 0.001f)
        assertEquals(Offset(36f, 246f), g.center(0, 0))
        assertEquals(Offset(1044f, 1254f), g.center(14, 14))
    }

    @Test
    fun `zoomed in, the point zoomed around stays put, neighbours move apart, and touches still find their point`() {
        val plain = BoardGeometry(15, 15, 720f, 720f)
        val zoomed = plain.zoomed(2f, Pos(3, 12))

        assertEquals(plain.center(3, 12).x, zoomed.center(3, 12).x, 0.01f)
        assertEquals(plain.center(3, 12).y, zoomed.center(3, 12).y, 0.01f)
        assertEquals(plain.spacing * 2, zoomed.spacing, 0.001f)
        assertEquals(plain.center(3, 12).x + zoomed.spacing, zoomed.center(4, 11).x, 0.01f)
        assertEquals(plain.center(3, 12).y - zoomed.spacing, zoomed.center(4, 11).y, 0.01f)
        for (x in 0 until 15) for (y in 0 until 15) {
            val c = zoomed.center(x, y)
            assertEquals(Pos(x, y), zoomed.pointAt(c + Offset(zoomed.spacing * 0.49f, -zoomed.spacing * 0.49f)))
        }
    }
}
