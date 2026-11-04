package io.github.pavelya.dots.game

import dots.engine.Pos
import org.junit.Assert.assertEquals
import org.junit.Test

class PlacementTest {
    private val a = Pos(3, 4)
    private val b = Pos(5, 5)
    private val playable: (Pos) -> Boolean = { it != Pos(0, 0) }

    @Test
    fun `first tap previews, second tap on the same point places`() {
        val first = Placement.onTap(null, a, playable)
        assertEquals(Placement.Result(preview = a, placed = null), first)
        assertEquals(Placement.Result(preview = null, placed = a), Placement.onTap(first.preview, a, playable))
    }

    @Test
    fun `tapping another point moves the preview instead of placing`() {
        assertEquals(Placement.Result(preview = b, placed = null), Placement.onTap(a, b, playable))
    }

    @Test
    fun `tapping an unplayable point or off the board clears the preview`() {
        assertEquals(Placement.Result(null, null), Placement.onTap(a, Pos(0, 0), playable))
        assertEquals(Placement.Result(null, null), Placement.onTap(a, null, playable))
    }
}
