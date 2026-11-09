package dots.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Holes, blocked points and bonus points: how they change placing, capturing and grounding. */
class BoardShapeTest {

    /** A start from a map: `.` empty, `R`/`B` dots, `-` a hole, `#` a blocked point, `+` a bonus point. */
    private fun board(map: String, holesAreBorder: Boolean = true, first: Player = Player.RED): GameState =
        GameState.fromAscii(map, first = first, holesAreBorder = holesAreBorder)

    private fun p(name: String) = Notation.parse(name)!!

    /** Plays [names] in turn, red and blue alternating from whoever is to move. */
    private fun GameState.place(vararg names: String): GameState = names.fold(this) { state, name -> state.play(p(name)).state }

    // Holes

    @Test
    fun `nobody may place a dot on a hole or a blocked point, or wall them`() {
        val start = GameState.start(
            7, 5, dots = mapOf(p("d3") to Player.BLUE, p("d4") to Player.RED),
            boosters = mapOf(Player.RED to mapOf(Booster.WALL to 1)),
            holes = setOf(p("d2")), blocked = setOf(p("c3")), bonus = setOf(p("e3")),
        )

        for (point in listOf("d2", "c3")) {
            assertTrue(start.isDead(p(point)), point)
            assertFalse(start.isLegal(p(point)), point)
            assertFalse(start.isLegal(Action.Wall(p(point))), point)
            assertFalse(start.place("a1").isLegal(p(point)), "blue on $point")
        }
        assertTrue(start.isHole(p("d2")) && !start.isBlocked(p("d2")))
        assertTrue(start.isBlocked(p("c3")) && !start.isHole(p("c3")))
        assertTrue(start.isBonus(p("e3")) && start.isLegal(p("e3")))
        assertEquals(7 * 5 - 2 - 2, start.legalMoves().size)
        assertEquals(".......\n...-...\n..#B+..\n...R...\n.......", start.toAscii())
    }

    @Test
    fun `a dot next to a hole that counts as border can never be captured`() {
        val map = """
            .......
            ...R...
            ..RBR..
            ...-...
            .......
            .......
            .......
            """
        // Red closes the same ring round the blue dot and the hole below it on both boards.
        val border = board(map, holesAreBorder = true).place("c4", "a1", "e4", "g1", "d5")
        val open = board(map, holesAreBorder = false).place("c4", "a1", "e4", "g1", "d5")

        assertTrue(border.isBorder(p("d3")))
        assertTrue(p("d3") in Grounding.safe(border, Player.BLUE))
        assertEquals(0, border.score(Player.RED))
        assertFalse(border.isCaptured(p("d3")))

        assertFalse(open.isBorder(p("d3")))
        assertEquals(1, open.score(Player.RED))
        assertTrue(open.isCaptured(p("d3")))
        assertEquals(Player.RED, open.enclosedBy(p("d4")))
    }

    @Test
    fun `a dot only diagonally next to a hole can still be captured`() {
        val state = board(
            """
            .......
            .......
            ..-....
            ...B...
            .......
            .......
            .......
            """,
        )
        // d3 and c4 are next to the hole, so on the border; red may still place there.
        val captured = state.place("d3", "a1", "c4", "g1", "e4", "a7", "d5")

        assertTrue(state.isBorder(p("d3")) && state.isBorder(p("c4")))
        assertFalse(state.isBorder(p("d4")))
        assertTrue(captured.isCaptured(p("d4")))
        assertEquals(1, captured.score(Player.RED))
    }

    @Test
    fun `a hole that reaches the edge of the map is outside, even when holes are open`() {
        val map = """
            --.....
            -......
            .......
            ...-...
            .......
            """
        for (holesAreBorder in listOf(true, false)) {
            val start = board(map.replaceFirst("-......", "-B....."), holesAreBorder)
            // b2 sits in the notch of the outline: no ring can go round it.
            val walled = start.place("c1", "g5", "c2", "g4", "b3", "g3", "a3")

            assertTrue(start.isBorder(p("b2")), "holes are border: $holesAreBorder")
            assertTrue(p("b2") in Grounding.safe(walled, Player.BLUE))
            assertFalse(walled.isCaptured(p("b2")))
            assertEquals(holesAreBorder, start.isBorder(p("d3")), "next to the hole inside the board")
        }
    }

    // Blocked points

    @Test
    fun `a ring passes over a blocked point and captures what it encloses`() {
        val state = board(
            """
            .......
            ...R...
            ..R#R..
            ..RBR..
            .......
            """,
        )

        assertFalse(state.isBorder(p("d4")))
        assertFalse(p("d4") in Grounding.safe(state, Player.BLUE))
        val result = state.play(p("d5"))
        assertEquals(1, result.state.score(Player.RED))
        assertEquals(setOf(p("d3"), p("d4")), result.captures.single().region.toSet())
        assertTrue(result.state.isBlocked(p("d3")))
    }

    @Test
    fun `a dot joined to the border through a blocked point is safe`() {
        val state = board(
            """
            ...#...
            ...B...
            .......
            .......
            .......
            """,
        )
        val walled = state.place("c1", "g5", "c2", "g4", "d3", "g3", "e2", "a5", "e1")

        assertTrue(p("d2") in Grounding.safe(state, Player.BLUE))
        assertFalse(state.isBorder(p("d2")))
        assertFalse(walled.isCaptured(p("d2")))
        assertEquals(0, walled.score(Player.RED))
    }

    // Bonus points

    @Test
    fun `a dot captured on a bonus point is worth 2`() {
        val state = board(
            """
            .......
            ...R...
            ..R+R..
            .......
            .......
            """,
            first = Player.BLUE,
        )
        val result = state.place("d3").play(p("d4"))

        assertEquals(2, state.worth(p("d3")))
        assertEquals(1, state.worth(p("d4")))
        assertEquals(listOf(p("d3")), result.captures.single().dots)
        assertEquals(2, result.captures.single().points)
        assertEquals(2, result.state.score(Player.RED))
    }

    @Test
    fun `an empty bonus point inside a capture adds nothing`() {
        val result = board(
            """
            .......
            ...R...
            ..R+R..
            ..RBR..
            .......
            """,
        ).play(p("d5"))

        assertEquals(1, result.captures.single().points)
        assertEquals(1, result.state.score(Player.RED))
        assertTrue(result.state.isDead(p("d3")))
        assertEquals(",", result.state.toAscii().lines()[2].substring(3, 4))
    }

    @Test
    fun `a starting dot may not sit on a special point, and a point is only one kind`() {
        for (where in listOf("holes", "blocked", "bonus")) {
            val points = setOf(p("b2"))
            assertFailsWith<IllegalArgumentException>(where) {
                GameState.start(
                    5, 5, dots = mapOf(p("b2") to Player.RED),
                    holes = if (where == "holes") points else emptySet(),
                    blocked = if (where == "blocked") points else emptySet(),
                    bonus = if (where == "bonus") points else emptySet(),
                )
            }
        }
        assertFailsWith<IllegalArgumentException> { GameState.start(5, 5, holes = setOf(p("b2")), bonus = setOf(p("b2"))) }
        assertFailsWith<IllegalArgumentException> { GameState.start(5, 5, blocked = setOf(p("f9"))) }
    }
}
