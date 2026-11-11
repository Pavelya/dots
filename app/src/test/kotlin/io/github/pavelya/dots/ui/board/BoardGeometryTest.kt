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
}
