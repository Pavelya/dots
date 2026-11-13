package dots.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoosterTest {

    private val one = Booster.entries.associateWith { 1 }

    /** A start from a map (`.` empty, `R`/`B` dots, rows from the top); red has [boosters]. */
    private fun board(map: String, boosters: Map<Booster, Int> = one, dots: Int? = null, first: Player = Player.RED): GameState {
        val rows = map.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val start = HashMap<Pos, Player>()
        for ((y, row) in rows.withIndex()) {
            for ((x, ch) in row.withIndex()) {
                if (ch == 'R') start[Pos(x, y)] = Player.RED
                if (ch == 'B') start[Pos(x, y)] = Player.BLUE
            }
        }
        return GameState.start(rows[0].length, rows.size, dots, first, start, boosters = mapOf(Player.RED to boosters))
    }

    private fun p(name: String) = Notation.parse(name)!!

    private fun GameState.place(vararg names: String): GameState = names.fold(this) { state, name -> state.play(p(name)).state }

    private fun GameState.use(action: Action): GameState = apply(action).state

    // Wall

    @Test
    fun `a wall blocks an empty point for both players and the turn goes on`() {
        val start = board(
            """
            .......
            .......
            ...B...
            ..BRB..
            .......
            .......
            .......
            """,
        )
        val walled = start.use(Action.Wall(p("d5")))

        assertTrue(walled.isBlocked(p("d5")))
        assertTrue(walled.isDead(p("d5")))
        assertEquals('#', walled.toAscii().lines()[4][3])
        assertEquals(0, walled.boostersLeft(Player.RED, Booster.WALL))
        assertEquals(Player.RED, walled.toMove)
        assertEquals(start.legalMoves().size - 1, walled.legalMoves().size)
        assertFalse(walled.isLegal(p("d5")))

        val blueToMove = walled.place("a1")
        assertEquals(Player.BLUE, blueToMove.toMove)
        assertFalse(blueToMove.isLegal(p("d5")))
        assertFailsWith<IllegalArgumentException> { blueToMove.play(p("d5")) }
    }

    @Test
    fun `a wall on the closing point stops the ring there, but a wider ring still passes over it`() {
        val walled = board(
            """
            .......
            .......
            ...B...
            ..BRB..
            .......
            .......
            .......
            """,
        ).use(Action.Wall(p("d5"))).place("a1")

        // Blue goes round the wall instead: c5, e5, d6 close a ring around d4 and the wall.
        val result = walled.place("c5", "a2", "e5", "a3").play(p("d6"))

        assertEquals(listOf(p("d4")), result.captures.single().dots)
        assertTrue(p("d5") in result.captures.single().region)
        assertEquals(1, result.state.score(Player.BLUE))
        assertTrue(result.state.isBlocked(p("d5")))
    }

    @Test
    fun `a wall needs an empty point in play`() {
        val start = board(
            """
            .....
            ..R..
            .RBR.
            .....
            .....
            """,
        )
        val captured = start.place("c4", "e5")

        assertFalse(start.isLegal(Action.Wall(p("c2"))))
        assertFalse(start.isLegal(Action.Wall(p("c3"))))
        assertFalse(captured.isLegal(Action.Wall(p("c3"))))
        assertFalse(start.isLegal(Action.Wall(Pos(5, 0))))
        assertTrue(start.isLegal(Action.Wall(p("a1"))))
        assertFailsWith<IllegalArgumentException> { start.useBooster(Action.Wall(p("c2"))) }
    }

    // Hint

    @Test
    fun `a hint changes nothing on the board and must name a legal move`() {
        val start = board(
            """
            .....
            ..R..
            .RBR.
            .....
            .....
            """,
        )
        val hinted = start.use(Action.Hint(p("c4")))

        assertEquals(start.toAscii(), hinted.toAscii())
        assertEquals(Player.RED, hinted.toMove)
        assertEquals(0, hinted.boostersLeft(Player.RED, Booster.HINT))
        assertFalse(start.isLegal(Action.Hint(p("c3"))))
        assertFalse(start.isLegal(Action.Hint(Pos(9, 9))))
    }

    // Bomb

    @Test
    fun `a bomb removes the opponent's dots in a plus of five points and nothing else`() {
        val start = board(
            """
            .......
            ...B...
            ..RBBB.
            ...B...
            ..B....
            .......
            """,
            dots = 10,
        )
        assertEquals(setOf(p("d3"), p("e3"), p("d2"), p("d4")), start.bombHits(p("d3")).toSet())

        val bombed = start.use(Action.Bomb(p("d3")))

        assertEquals(
            """
            .......
            .......
            ..R..B.
            .......
            ..B....
            .......
            """.trimIndent(),
            bombed.toAscii(),
        )
        for (name in listOf("d3", "e3", "d2", "d4")) assertTrue(bombed.isLegal(p(name)), name)
        assertEquals(Player.RED, bombed.toMove)
        assertEquals(0, bombed.score(Player.RED))
        assertEquals(10, bombed.dotsLeft(Player.BLUE))
        assertEquals(0, bombed.dotsPlaced(Player.BLUE))
        assertEquals(start.legalMoves().size + 4, bombed.legalMoves().size)
    }

    @Test
    fun `a bomb at the border hits only the points on the board`() {
        val start = board(
            """
            BB...
            BB...
            .....
            ....B
            ...BB
            """,
        )
        assertEquals(setOf(p("a1"), p("b1"), p("a2")), start.bombHits(p("a1")).toSet())
        assertEquals(setOf(p("e4"), p("e5"), p("d5")), start.bombHits(p("e5")).toSet())
        assertEquals(setOf(p("e4")), start.bombHits(p("e3")).toSet())
        assertTrue(start.bombHits(Pos(-1, 0)).isEmpty())
        assertFalse(start.isLegal(Action.Bomb(Pos(-1, 0))))

        val bombed = start.use(Action.Bomb(p("a1")))
        for (name in listOf("a1", "b1", "a2")) assertEquals(null, bombed.ownerAt(p(name)), name)
        assertEquals(Player.BLUE, bombed.ownerAt(p("b2")))
    }

    @Test
    fun `a bomb never reaches into a captured area and needs a dot to hit`() {
        val captured = board(
            """
            .......
            ..R....
            .RBR...
            .......
            .......
            """,
        ).place("c4", "e3")
        assertEquals(1, captured.score(Player.RED))

        assertTrue(captured.bombHits(p("c3")).isEmpty())
        assertFalse(captured.isLegal(Action.Bomb(p("c3"))))
        assertFalse(captured.isLegal(Action.Bomb(p("a5"))))
        assertEquals(listOf(p("e3")), captured.bombHits(p("d3")))

        val bombed = captured.use(Action.Bomb(p("d3")))
        assertTrue(bombed.isCaptured(p("c3")))
        assertEquals(Player.BLUE, bombed.ownerAt(p("c3")))
        assertEquals(1, bombed.score(Player.RED))
        assertEquals(null, bombed.ownerAt(p("e3")))
    }

    @Test
    fun `a bombed ring cannot close where its dot was`() {
        // Blue is one dot (c4) from a diamond round red's c3; the bomb takes away d3.
        val start = board(
            """
            .....
            ..B..
            .BRB.
            .....
            .....
            """,
        )
        val bombed = start.use(Action.Bomb(p("d3"))).place("a1")
        val result = bombed.play(p("c4"))

        assertTrue(result.captures.isEmpty())
        assertEquals(0, result.state.score(Player.BLUE))
    }

    // Double dot

    @Test
    fun `a double dot places two dots in one turn`() {
        val start = board(
            """
            .......
            .......
            ...R...
            ...B...
            .......
            """,
            dots = 10,
        )
        val doubled = start.use(Action.DoubleDot)
        assertEquals(2, doubled.dotsThisTurn)
        assertEquals(Player.RED, doubled.toMove)

        val first = doubled.place("a1")
        assertEquals(Player.RED, first.toMove)
        assertEquals(1, first.dotsThisTurn)
        assertFalse(first.isLegal(Action.Pass))
        assertFalse(first.isLegal(Action.Stop))
        assertFailsWith<IllegalStateException> { first.pass() }
        for (booster in Booster.entries) assertFalse(first.canUse(booster), "$booster")

        val second = first.place("a2")
        assertEquals(Player.BLUE, second.toMove)
        assertEquals(8, second.dotsLeft(Player.RED))
        assertEquals(2, second.dotsPlaced(Player.RED))
        assertFalse(second.boosterUsed)
    }

    @Test
    fun `a double dot can close two rings, one with each dot`() {
        val start = board(
            """
            .........
            ..R...R..
            .RBR.RBR.
            .........
            .........
            """,
        )
        val first = start.use(Action.DoubleDot).play(p("c4"))
        assertEquals(listOf(p("c3")), first.captures.single().dots)
        assertEquals(Player.RED, first.state.toMove)

        val second = first.state.play(p("g4"))
        assertEquals(listOf(p("g3")), second.captures.single().dots)
        assertEquals(2, second.state.score(Player.RED))
        assertEquals(Player.BLUE, second.state.toMove)
    }

    @Test
    fun `a double dot closes a ring the computer could have blocked`() {
        // Red needs d2 and d4 round blue's d3; one dot a turn, blue would take the other point.
        val start = board(
            """
            .......
            .......
            ..RBR..
            .......
            .......
            """,
        )
        val first = start.use(Action.DoubleDot).place("d2")
        assertEquals(0, first.score(Player.RED))
        assertEquals(Player.RED, first.toMove)

        val second = first.play(p("d4"))
        assertEquals(listOf(p("d3")), second.captures.single().dots)
        assertEquals(Player.BLUE, second.state.toMove)
    }

    @Test
    fun `a double dot needs two dots left and ends early on a full board`() {
        assertFalse(board(".....\n..R..\n..B..", dots = 1).canUse(Booster.DOUBLE_DOT))
        assertTrue(board(".....\n..R..\n..B..", dots = 2).canUse(Booster.DOUBLE_DOT))
        assertTrue(board(".....\n..R..\n..B..").canUse(Booster.DOUBLE_DOT))

        val lastPoint = board(
            """
            RRR
            R.B
            BBB
            """,
        ).use(Action.DoubleDot).place("b2")
        assertTrue(lastPoint.isOver)
        assertEquals(1, lastPoint.dotsThisTurn)
    }

    // Undo

    private val cross = """
        .......
        .......
        ...B...
        ..BRB..
        .......
        .......
        .......
    """

    /** True if [a] and [b] are the same position, with the same counts, the same player to move and the same boosters. */
    private fun assertSame(a: GameState, b: GameState, undosUsed: Int = 0) {
        assertEquals(a.toAscii(), b.toAscii())
        assertEquals(a.toMove, b.toMove)
        assertEquals(a.moveCount, b.moveCount)
        for (player in Player.entries) {
            assertEquals(a.score(player), b.score(player), "$player")
            assertEquals(a.dotsLeft(player), b.dotsLeft(player), "$player")
            assertEquals(a.dotsPlaced(player), b.dotsPlaced(player), "$player")
            assertEquals(a.lastPlaced(player), b.lastPlaced(player), "$player")
            for (booster in Booster.entries) {
                val used = if (booster == Booster.UNDO && player == Player.RED) undosUsed else 0
                assertEquals(a.boostersLeft(player, booster) - used, b.boostersLeft(player, booster), "$player $booster")
            }
        }
    }

    @Test
    fun `an undo takes back the player's dot and the reply, and is not the turn's one booster`() {
        val start = board(cross, dots = 10)
        val replied = start.place("a1", "g7")
        assertTrue(replied.canUse(Booster.UNDO))

        val back = replied.use(Action.Undo)

        assertSame(start, back, undosUsed = 1)
        assertEquals(Player.RED, back.toMove)
        assertFalse(back.boosterUsed)
        assertTrue(back.canUse(Booster.WALL))
        assertTrue(back.isLegal(Action.Wall(p("a1"))))
        assertFalse(back.canUse(Booster.UNDO), "none left")
    }

    @Test
    fun `an undo takes back both dots of a double dot and gives the double dot back`() {
        val turn = board(cross, dots = 10).place("a1", "g7")
        val doubled = turn.use(Action.DoubleDot).place("a2", "a3", "g6")
        assertEquals(Player.RED, doubled.toMove)
        assertEquals(0, doubled.boostersLeft(Player.RED, Booster.DOUBLE_DOT))

        val back = doubled.use(Action.Undo)

        assertSame(turn, back, undosUsed = 1)
        assertEquals(1, back.boostersLeft(Player.RED, Booster.DOUBLE_DOT))
        assertTrue(back.canUse(Booster.DOUBLE_DOT))
    }

    @Test
    fun `an undo gives back a wall or a bomb used in the turn it takes back`() {
        val turn = board(cross).place("a1", "g7")

        val walled = turn.use(Action.Wall(p("b1"))).place("a2", "g6")
        assertTrue(walled.isBlocked(p("b1")))
        val unwalled = walled.use(Action.Undo)
        assertSame(turn, unwalled, undosUsed = 1)
        assertFalse(unwalled.isBlocked(p("b1")))
        assertTrue(unwalled.isLegal(p("b1")))

        val bombed = turn.use(Action.Bomb(p("d3"))).place("a2", "g6")
        assertEquals(null, bombed.ownerAt(p("d3")))
        val unbombed = bombed.use(Action.Undo)
        assertSame(turn, unbombed, undosUsed = 1)
        assertEquals(Player.BLUE, unbombed.ownerAt(p("d3")))
        assertEquals(1, unbombed.boostersLeft(Player.RED, Booster.BOMB))
    }

    @Test
    fun `an undo takes back a capture`() {
        val turn = board(
            """
            .......
            .......
            ..RBR..
            .......
            .......
            """,
        ).place("d2", "a5")
        val captured = turn.place("d4", "g5")
        assertEquals(1, captured.score(Player.RED))

        val back = captured.use(Action.Undo)

        assertSame(turn, back, undosUsed = 1)
        assertFalse(back.isCaptured(p("d3")))
        assertFalse(back.isDead(p("d3")))
    }

    @Test
    fun `undos in a row go back a turn each, until none are left`() {
        val start = board(cross, boosters = mapOf(Booster.UNDO to 3))
        val first = start.place("a1", "g7")
        val second = first.place("a2", "g6")

        val once = second.use(Action.Undo)
        assertSame(first, once, undosUsed = 1)
        val twice = once.use(Action.Undo)
        assertSame(start, twice, undosUsed = 2)
        assertFalse(twice.canUse(Booster.UNDO), "no turn left to take back")

        val again = twice.place("b1", "f7").use(Action.Undo)
        assertSame(start, again, undosUsed = 3)
        val spent = again.place("b1", "f7")
        assertEquals(0, spent.boostersLeft(Player.RED, Booster.UNDO))
        assertFalse(spent.canUse(Booster.UNDO))
        assertFalse(spent.isLegal(Action.Undo))
        assertFailsWith<IllegalArgumentException> { spent.useBooster(Action.Undo) }
    }

    @Test
    fun `no undo before the player's first move, during a turn, or after the game ends`() {
        val start = board(cross, dots = 2)
        assertFalse(start.canUse(Booster.UNDO))
        assertFalse(board(cross, first = Player.BLUE).place("g7").canUse(Booster.UNDO), "red's first turn")

        val turn = start.place("a1", "g7")
        assertTrue(turn.canUse(Booster.UNDO))
        assertFalse(turn.use(Action.Hint(p("a2"))).canUse(Booster.UNDO), "after a booster this turn")
        val doubled = board(cross, dots = 10).place("a1", "g7").use(Action.DoubleDot).place("a2")
        assertFalse(doubled.canUse(Booster.UNDO), "between the two dots of a double dot")

        val over = turn.place("a2", "g6")
        assertTrue(over.isOver)
        assertFalse(over.canUse(Booster.UNDO))
    }

    @Test
    fun `an undo goes back over a reply of two dots and a turn the computer passed`() {
        val start = GameState.start(
            7, 7, dots = mapOf(p("d4") to Player.RED, p("d3") to Player.BLUE),
            boosters = mapOf(Player.RED to mapOf(Booster.UNDO to 2)),
            turnRules = mapOf(Player.BLUE to TurnRule.DoubleMove(2)),
        )
        val first = start.place("a1", "g7")
        val second = first.place("a2", "g6", "g5") // blue's second turn has two dots
        val third = second.place("a3").pass()
        assertEquals(Player.RED, third.toMove)
        assertEquals(1, third.doubleMoveIn(Player.BLUE))

        val once = third.use(Action.Undo)
        assertSame(second, once, undosUsed = 1)
        assertEquals(second.doubleMoveIn(Player.BLUE), once.doubleMoveIn(Player.BLUE))
        val twice = once.use(Action.Undo)
        assertSame(first, twice, undosUsed = 2)
        val replayed = twice.place("a2")
        assertEquals(Player.BLUE, replayed.toMove)
        assertEquals(2, replayed.dotsThisTurn, "blue's double turn comes again")
    }

    @Test
    fun `no undo in a game where nobody has one`() {
        val start = board(cross, boosters = mapOf(Booster.WALL to 1))
        assertFalse(start.place("a1", "g7").canUse(Booster.UNDO))
    }

    // Counts and turns

    @Test
    fun `one booster per turn, counted down, and only for the player who has them`() {
        val start = board(
            """
            .......
            .......
            ...R...
            ...B...
            .......
            """,
            boosters = mapOf(Booster.WALL to 2, Booster.HINT to 1),
        )
        assertFalse(start.canUse(Booster.BOMB))
        assertFalse(start.canUse(Booster.DOUBLE_DOT))

        val walled = start.use(Action.Wall(p("a1")))
        assertTrue(walled.boosterUsed)
        for (booster in Booster.entries) assertFalse(walled.canUse(booster), "$booster")
        assertFailsWith<IllegalArgumentException> { walled.useBooster(Action.Hint(p("b1"))) }

        val blue = walled.place("b1")
        for (booster in Booster.entries) assertFalse(blue.canUse(booster), "blue has no $booster")

        val nextTurn = blue.place("c1")
        assertTrue(nextTurn.canUse(Booster.WALL))
        val last = nextTurn.use(Action.Wall(p("a2"))).place("e1", "f1")
        assertEquals(0, last.boostersLeft(Player.RED, Booster.WALL))
        assertFalse(last.canUse(Booster.WALL))
        assertTrue(last.canUse(Booster.HINT))
    }

    @Test
    fun `a pass ends the turn and a booster used before it`() {
        val start = board(".....\n..R..\n..B..", boosters = mapOf(Booster.WALL to 2))
        val passed = start.use(Action.Wall(p("a1"))).pass().pass()

        assertTrue(passed.isOver)
        val again = start.use(Action.Wall(p("a1"))).pass()
        assertFalse(again.boosterUsed)
        assertEquals(Player.BLUE, again.toMove)
    }

    @Test
    fun `no boosters after a stop`() {
        val collecting = board(".....\n..R..\n..B..", first = Player.BLUE).stop()

        assertEquals(Player.RED, collecting.toMove)
        assertTrue(collecting.isCleanup)
        for (booster in Booster.entries) assertFalse(collecting.canUse(booster), "$booster")
    }

    @Test
    fun `booster actions have words for game records`() {
        val actions = listOf(
            Action.Wall(p("e5")), Action.Hint(p("ab12")), Action.Bomb(p("a1")), Action.DoubleDot, Action.Undo,
            Action.Place(p("c3")), Action.Pass, Action.Stop, Action.Finish,
        )
        val words = actions.map(Action::word)

        assertEquals(listOf("wall:e5", "hint:ab12", "bomb:a1", "double-dot", "undo", "c3", "pass", "stop", "done"), words)
        assertEquals(actions, words.map(Action::parse))
        for (bad in listOf("wall:", "wall:zz", "nuke:e5", "wall", "double-dot:e5", "undo:e5", "e5:wall")) {
            assertFailsWith<IllegalArgumentException>(bad) { Action.parse(bad) }
        }
    }
}
