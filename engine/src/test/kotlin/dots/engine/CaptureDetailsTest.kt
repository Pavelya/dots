package dots.engine

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What a [MoveResult] tells the app so it can animate captures and boosters. */
class CaptureDetailsTest {

    private fun p(name: String) = Notation.parse(name)!!

    private fun points(vararg names: String) = names.map(::p)

    private fun board(map: String, holesAreBorder: Boolean = true): GameState =
        GameState.fromAscii(map, holesAreBorder = holesAreBorder)

    // The ring

    @Test
    fun `a diamond's ring starts at the closing dot and runs clockwise`() {
        val result = board(
            """
            .....
            ..R..
            .RBR.
            .....
            .....
            """,
        ).play(p("c4"))

        val capture = result.captures.single()
        assertEquals(p("c4"), capture.closing)
        assertEquals(points("c4", "b3", "c2", "d3"), capture.ring)
        assertSurrounds(result.state, capture)
    }

    @Test
    fun `a ring cuts the corners of a square with diagonal links`() {
        val result = board(
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
        assertEquals(points("e5", "d5", "c5", "b4", "b3", "c2", "d2", "e2", "f3", "f4"), capture.ring)
        assertEquals(listOf(p("c3")), capture.dots)
        assertEquals(1, capture.points)
        assertSurrounds(result.state, capture)
    }

    @Test
    fun `a line of dots sticking into the area is not run along and back`() {
        // d3 and d4 stick down into the area from the top of the ring.
        val result = board(
            """
            .......
            .RRRRR.
            .R.R.R.
            .RBRBR.
            .R...R.
            .RR.RR.
            .......
            """,
        ).play(p("d6"))

        val capture = result.captures.single()
        assertEquals(points("d6", "c6", "b5", "b4", "b3", "c2", "d3", "e2", "f3", "f4", "f5", "e6"), capture.ring)
        assertEquals(capture.ring.size, capture.ring.toSet().size)
        assertEquals(setOf(p("c4"), p("e4")), capture.dots.toSet())
        assertSurrounds(result.state, capture)
    }

    @Test
    fun `one dot closing two rings gives each its own ring from that dot`() {
        val result = board(
            """
            .......
            ..R.R..
            .RB.BR.
            ..R.R..
            .......
            """,
        ).play(p("d3"))

        assertEquals(2, result.captures.size)
        assertEquals(points("d3", "c4", "b3", "c2"), result.captures[0].ring)
        assertEquals(points("d3", "e2", "f3", "e4"), result.captures[1].ring)
        assertEquals(2, result.state.score(Player.RED))
        for (capture in result.captures) assertSurrounds(result.state, capture)
    }

    @Test
    fun `a ring goes round an open hole inside the area`() {
        val result = board(
            """
            ......
            .RRR..
            .R-B..
            .RRR..
            ......
            """,
            holesAreBorder = false,
        ).play(p("e3"))

        val capture = result.captures.single()
        assertEquals(points("e3", "d4", "c4", "b3", "c2", "d2"), capture.ring)
        assertEquals(setOf(p("c3"), p("d3")), capture.region.toSet())
        assertSurrounds(result.state, capture)
    }

    @Test
    fun `a loose loop of the capturer's dots stays inside the ring, with its pocket`() {
        // The diamond e4, d5, f5, e6 walls off e5; it does not touch the outer ring.
        val result = board(
            """
            .........
            .RRRRRRR.
            .R.....R.
            .R..R..R.
            .R.R.R.R.
            .R..R..R.
            .RB....R.
            .RRR.RRR.
            .........
            """,
        ).play(p("e8"))

        val capture = result.captures.single()
        assertTrue(points("e4", "d5", "f5", "e6", "e5").none { it in capture.ring })
        assertTrue(p("e5") !in capture.region)
        assertTrue(assertSurrounds(result.state, capture), "e5 should be a pocket inside the ring")
        assertTrue(result.state.isLegal(p("e5")))
    }

    @Test
    fun `a ring touches itself where it goes round a loop joined to it`() {
        // The arm c5, d5 joins the diamond e4, d5, f5, e6 to the outer ring at b5.
        val result = board(
            """
            .........
            .RRRRRRR.
            .R.....R.
            .R..R..R.
            .RRR.R.R.
            .R..R..R.
            .RB....R.
            .RRR.RRR.
            .........
            """,
        ).play(p("e8"))

        val capture = result.captures.single()
        assertEquals(2, capture.ring.count { it == p("c5") })
        assertEquals(2, capture.ring.count { it == p("d5") })
        assertTrue(p("e5") !in capture.region)
        assertSurrounds(result.state, capture)
        assertTrue(result.state.isLegal(p("e5")))
    }

    @Test
    fun `a capture on a bonus point says what it adds to the score`() {
        val result = board(
            """
            .....
            ..R..
            .R+R.
            .....
            .....
            """,
        ).place("a1", "c3").play(p("c4"))

        val capture = result.captures.single()
        assertEquals(listOf(p("c3")), capture.dots)
        assertEquals(2, capture.points)
        assertEquals(2, result.state.score(Player.RED))
    }

    /**
     * Random games on open boards and on boards with open holes: every capture's ring is closed,
     * follows only the capturer's dots, starts at the closing dot and surrounds exactly the area.
     */
    @Test
    fun `every ring in random games is closed, follows the capturer's dots and surrounds exactly the area`() {
        var captures = 0
        for (seed in 0 until 1000) {
            val random = Random(seed)
            val size = 7 + seed % 5
            var state = if (seed % 3 == 0) {
                val mid = size / 2
                GameState.start(size, size, holes = setOf(Pos(mid, mid), Pos(mid, mid + 1)), holesAreBorder = false)
            } else {
                GameState.start(size, size)
            }
            while (!state.isOver) {
                val moves = state.legalMoves()
                if (moves.isEmpty()) break
                val result = state.play(moves[random.nextInt(moves.size)])
                for (capture in result.captures) {
                    captures++
                    assertSurrounds(result.state, capture)
                }
                state = result.state
            }
        }
        assertTrue(captures > 2000, "only $captures captures; the test needs more games")
    }

    /**
     * Checks that [capture]'s ring is closed (each dot next to the one before, across or
     * diagonally, and the last next to the first), uses only the capturer's dots, starts at the
     * closing dot and surrounds exactly the area: the points strictly inside it that are not the
     * capturer's dots are the region, plus any pockets the capturer's dots wall off inside it.
     * Returns true if there was such a pocket.
     */
    private fun assertSurrounds(state: GameState, capture: Capture): Boolean {
        val ring = capture.ring
        val by = capture.by
        fun wall(pos: Pos) = state.ownerAt(pos) == by && !state.isCaptured(pos)

        assertTrue(ring.size >= 4, "ring $ring is too short")
        assertEquals(capture.closing, ring.first(), "ring $ring does not start at the closing dot")
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            assertTrue(a != b && maxOf(abs(a.x - b.x), abs(a.y - b.y)) == 1, "ring $ring: $a and $b are not neighbours")
            assertTrue(wall(a), "ring $ring: $a is not a live dot of $by")
        }

        // The cells that should be inside: the region and its holes. A hole is a part of the rest of
        // the board (joined across and diagonally, as gaps between the region's cells are) that does
        // not reach the edge.
        val region = capture.region.toSet()
        val outer = HashSet<Pos>()
        val stack = ArrayDeque<Pos>()
        for (y in 0 until state.height) for (x in 0 until state.width) {
            val pos = Pos(x, y)
            if ((x == 0 || y == 0 || x == state.width - 1 || y == state.height - 1) && pos !in region && outer.add(pos)) stack += pos
        }
        while (stack.isNotEmpty()) {
            val pos = stack.removeLast()
            for (dx in -1..1) for (dy in -1..1) {
                val next = Pos(pos.x + dx, pos.y + dy)
                if (state.inBounds(next.x, next.y) && next !in region && outer.add(next)) stack += next
            }
        }

        var pocket = false
        for (y in 0 until state.height) for (x in 0 until state.width) {
            val pos = Pos(x, y)
            if (wall(pos)) continue
            val expected = pos !in outer
            assertEquals(expected, inside(ring, pos), "ring $ring around region $region: wrong side for $pos")
            if (expected && pos !in region) pocket = true
        }
        return pocket
    }

    /** True if [pos], not a point of [ring], is inside it: a ray to the right crosses it an odd number of times. */
    private fun inside(ring: List<Pos>, pos: Pos): Boolean {
        var crossings = 0
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            if ((a.y > pos.y) != (b.y > pos.y)) {
                // Links are one step long, so b.y - a.y is 1 or -1 here.
                val x = a.x + (pos.y - a.y) * (b.x - a.x) * (b.y - a.y)
                if (x > pos.x) crossings++
            }
        }
        return crossings % 2 == 1
    }

    // Boosters

    private val one = Booster.entries.associateWith { 1 }

    private fun withBoosters(map: String): GameState {
        val rows = map.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val dots = HashMap<Pos, Player>()
        for ((y, row) in rows.withIndex()) for ((x, ch) in row.withIndex()) {
            if (ch == 'R') dots[Pos(x, y)] = Player.RED
            if (ch == 'B') dots[Pos(x, y)] = Player.BLUE
        }
        return GameState.start(rows[0].length, rows.size, dots = dots, boosters = mapOf(Player.RED to one))
    }

    private fun GameState.place(vararg names: String): GameState = names.fold(this) { state, name -> state.play(p(name)).state }

    private val blueCross = """
        .......
        ...B...
        ..BBB..
        ...B...
        .R.....
    """

    @Test
    fun `a bomb says where it fell and which dots it removed, and changes no score`() {
        val start = withBoosters(blueCross)
        val result = start.apply(Action.Bomb(p("d3")))

        val effect = result.booster!!
        assertEquals(Player.RED, effect.by)
        assertEquals(Booster.BOMB, effect.booster)
        assertEquals(p("d3"), effect.pos)
        assertEquals(setOf(p("d3"), p("c3"), p("e3"), p("d2"), p("d4")), effect.removed.toSet())
        assertTrue(effect.removed.all { result.state.ownerAt(it) == null })
        assertTrue(result.captures.isEmpty())
        assertEquals(0, result.state.score(Player.RED))
        assertEquals(0, result.state.score(Player.BLUE))
    }

    @Test
    fun `a wall and a hint say their point, a double dot has none`() {
        val start = withBoosters(blueCross)

        val wall = start.apply(Action.Wall(p("a1"))).booster!!
        assertEquals(Booster.WALL, wall.booster)
        assertEquals(p("a1"), wall.pos)
        assertTrue(wall.removed.isEmpty())

        val hint = start.apply(Action.Hint(p("g5"))).booster!!
        assertEquals(Booster.HINT, hint.booster)
        assertEquals(p("g5"), hint.pos)
        assertTrue(hint.removed.isEmpty())

        val double = start.apply(Action.DoubleDot).booster!!
        assertEquals(Booster.DOUBLE_DOT, double.booster)
        assertNull(double.pos)
        assertTrue(double.removed.isEmpty())
    }

    @Test
    fun `placing a dot or passing has no booster effect`() {
        val start = withBoosters(blueCross)
        assertNull(start.apply(Action.Place(p("a1"))).booster)
        assertNull(start.apply(Action.Pass).booster)
        assertNull(start.place("a1").apply(Action.Pass).booster)
    }
}
