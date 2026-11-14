package dots.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The captured areas the app draws: each region once, with its ring. */
class AreaTest {

    private fun p(name: String) = Notation.parse(name)!!

    private fun points(vararg names: String) = names.map(::p)

    @Test
    fun `a board with no capture has no areas`() {
        assertTrue(GameState.start(9, 9).areas().isEmpty())
    }

    @Test
    fun `an area is the region inside the ring, with the ring the capture drew`() {
        val result = GameState.fromAscii(
            """
            ......
            .RRRR.
            .RB..R
            .R...R
            .RRR..
            ......
            """,
        ).play(p("e5"))
        val capture = result.captures.single()

        val area = result.state.areas().single()
        assertEquals(Player.RED, area.by)
        assertEquals(capture.region.toSet(), area.region.toSet())
        assertEquals(area.region.sortedWith(compareBy({ it.y }, { it.x })), area.region)
        // The same dots in the same order; only the starting dot differs.
        assertEquals(capture.ring.toSet(), area.ring.toSet())
        val start = capture.ring.indexOf(area.ring.first())
        assertEquals(area.ring, capture.ring.drop(start) + capture.ring.take(start))
    }

    @Test
    fun `two captures make two areas, one for each side`() {
        // Red is one dot short of a diamond round b2's blue dot; blue is one short round e5's red dot.
        val state = GameState.fromAscii(
            """
            .R......
            RBR.....
            ........
            ....B...
            ...BRB..
            ........
            ........
            """,
        )
        val afterRed = state.play(p("b3")).state
        val afterBlue = afterRed.play(p("e6")).state

        val areas = afterBlue.areas()
        assertEquals(listOf(Player.RED, Player.BLUE), areas.map { it.by })
        assertEquals(listOf(points("b2"), points("e5")), areas.map { it.region })
        assertEquals(points("b1", "a2", "c2", "b3").toSet(), areas[0].ring.toSet())
        assertEquals(points("e4", "d5", "f5", "e6").toSet(), areas[1].ring.toSet())
    }

    @Test
    fun `a bigger ring takes in the area it surrounds`() {
        // A red diamond (one dot short) round a blue dot, all inside a blue ring (one dot short).
        val start = GameState.start(
            width = 9,
            height = 9,
            dots = mapOf(
                Pos(4, 3) to Player.RED, Pos(3, 4) to Player.RED, Pos(5, 4) to Player.RED,
                Pos(4, 4) to Player.BLUE,
                Pos(4, 1) to Player.BLUE, Pos(5, 2) to Player.BLUE, Pos(6, 3) to Player.BLUE,
                Pos(7, 4) to Player.BLUE, Pos(6, 5) to Player.BLUE, Pos(5, 6) to Player.BLUE,
                Pos(4, 7) to Player.BLUE, Pos(3, 6) to Player.BLUE, Pos(2, 5) to Player.BLUE,
                Pos(1, 4) to Player.BLUE, Pos(2, 3) to Player.BLUE,
            ),
        )
        val afterRed = start.play(Pos(4, 5)).state
        assertEquals(listOf(Player.RED), afterRed.areas().map { it.by })

        val afterBlue = afterRed.play(Pos(3, 2)).state
        val area = afterBlue.areas().single()
        assertEquals(Player.BLUE, area.by)
        assertTrue(Pos(4, 4) in area.region && Pos(4, 3) in area.region)
        assertTrue(area.ring.all { afterBlue.ownerAt(it) == Player.BLUE })
    }
}
