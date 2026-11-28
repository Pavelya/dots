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

    // A bomb that breaks a ring (decision 0042)

    /**
     * Blue's ring round red's d3 and d4 (a bonus point, worth 2), with c3 and e3 empty inside:
     * red has placed d4 and blue has just closed the ring with d5. Red to move, with one of each
     * booster.
     */
    private fun ringRoundBonus(): GameState {
        val start = GameState.start(
            7, 7,
            dots = mapOf(
                p("c2") to Player.BLUE, p("d2") to Player.BLUE, p("e2") to Player.BLUE,
                p("b3") to Player.BLUE, p("f3") to Player.BLUE, p("c4") to Player.BLUE, p("e4") to Player.BLUE,
                p("d3") to Player.RED,
            ),
            boosters = mapOf(Player.RED to one),
            bonus = setOf(p("d4")),
        )
        return start.place("d4", "d5")
    }

    @Test
    fun `a bomb on a dot of a ring breaks the capture`() {
        // The game that found it: the computer's ring round two of the player's dots.
        val captured = board(
            """
            .......
            ...B...
            ..BRB..
            ..BRB..
            .......
            """,
            first = Player.BLUE,
        ).place("d5")
        assertEquals(2, captured.score(Player.BLUE))
        assertTrue(captured.isCaptured(p("d3")))

        val result = captured.apply(Action.Bomb(p("f3")))
        val bombed = result.state

        assertEquals(listOf(p("e3")), result.booster!!.removed)
        assertEquals(0, bombed.score(Player.BLUE))
        for (name in listOf("d3", "d4")) {
            assertFalse(bombed.isCaptured(p(name)), name)
            assertFalse(bombed.isDead(p(name)), name)
            assertEquals(Player.RED, bombed.ownerAt(p(name)), name)
            assertEquals(null, bombed.enclosedBy(p(name)), name)
        }
        assertTrue(bombed.areas().isEmpty())
        assertEquals(listOf(listOf(p("d3"), p("d4"))), result.booster!!.broken.map { it.region })
        assertEquals(Player.BLUE, result.booster!!.broken.single().by)
    }

    @Test
    fun `a broken capture frees its dots and points and takes off what it scored, bonus points included`() {
        val captured = ringRoundBonus()
        assertEquals(3, captured.score(Player.BLUE))
        assertFalse(captured.isLegal(p("c3")))
        val area = listOf(p("c3"), p("d3"), p("e3"), p("d4"))
        assertEquals(listOf(area), captured.bombBreaks(p("f4")).map { it.region })

        // The plus round f4 hits e4 and f3, both next to e3 inside the ring.
        val bombed = captured.use(Action.Bomb(p("f4")))

        assertEquals(0, bombed.score(Player.BLUE))
        assertEquals(0, bombed.score(Player.RED))
        assertFalse(bombed.isCaptured(p("d3")))
        assertFalse(bombed.isCaptured(p("d4")))
        for (name in listOf("c3", "e3")) {
            assertFalse(bombed.isDead(p(name)), name)
            assertTrue(bombed.isLegal(p(name)), name)
        }
        // Red still places their dot this turn, and may place it inside the old ring.
        val next = bombed.play(p("c3"))
        assertTrue(next.captures.isEmpty())
        assertEquals(Player.BLUE, next.state.toMove)
    }

    @Test
    fun `a dot that touches the area only at a corner breaks nothing`() {
        val captured = board(
            """
            .......
            .BBB...
            .BRB...
            .B.B...
            .......
            """,
            first = Player.BLUE,
        ).place("c4")
        assertEquals(1, captured.score(Player.BLUE))

        // The plus round a2 hits only b2, the ring's corner: c3 has blue on all four sides still.
        assertEquals(listOf(p("b2")), captured.bombHits(p("a2")))
        assertTrue(captured.bombBreaks(p("a2")).isEmpty())
        val result = captured.apply(Action.Bomb(p("a2")))

        assertTrue(result.booster!!.broken.isEmpty())
        assertEquals(null, result.state.ownerAt(p("b2")))
        assertEquals(1, result.state.score(Player.BLUE))
        assertTrue(result.state.isCaptured(p("c3")))
        assertTrue(result.state.isDead(p("c3")))
    }

    @Test
    fun `a capture made earlier inside the broken area stays`() {
        // Red closes a diamond round blue's e5 with e6; blue then closes a bigger diamond round
        // red's with e8, taking red's four ring dots.
        val captured = board(
            """
            .........
            ....B....
            ...B.B...
            ..B.R.B..
            .B.RBR.B.
            ..B...B..
            ...B.B...
            .........
            .........
            """,
        ).place("e6", "e8")
        assertEquals(1, captured.score(Player.RED))
        assertEquals(4, captured.score(Player.BLUE))
        assertEquals(Player.BLUE, captured.enclosedBy(p("e5")))
        val inside = listOf("e3", "d4", "e4", "f4", "c5", "d5", "e5", "f5", "g5", "d6", "e6", "f6", "e7").map { p(it) }
        assertEquals(listOf(inside), captured.bombBreaks(p("e9")).map { it.region })

        // The plus round e9 hits only e8, next to e7 inside blue's ring.
        val bombed = captured.use(Action.Bomb(p("e9")))

        assertEquals(0, bombed.score(Player.BLUE))
        assertEquals(1, bombed.score(Player.RED))
        for (name in listOf("e4", "d5", "f5", "e6")) {
            assertFalse(bombed.isCaptured(p(name)), name)
            assertFalse(bombed.isDead(p(name)), name)
        }
        for (name in listOf("e3", "c5", "e7")) assertTrue(bombed.isLegal(p(name)), name)
        assertTrue(bombed.isCaptured(p("e5")))
        assertTrue(bombed.isDead(p("e5")))
        assertEquals(Player.RED, bombed.enclosedBy(p("e5")))
        assertEquals(listOf(listOf(p("e5"))), bombed.areas().map { it.region })
        assertEquals(Player.RED, bombed.areas().single().by)
    }

    @Test
    fun `an undo after a bomb that broke a ring gives the capture back`() {
        val captured = ringRoundBonus()
        val bombed = captured.use(Action.Bomb(p("f4"))).place("a1", "g7")
        assertEquals(0, bombed.score(Player.BLUE))

        val back = bombed.use(Action.Undo)

        assertEquals(3, back.score(Player.BLUE))
        assertTrue(back.isCaptured(p("d3")))
        assertTrue(back.isCaptured(p("d4")))
        assertTrue(back.isDead(p("c3")))
        assertEquals(Player.BLUE, back.ownerAt(p("e4")))
        assertEquals(1, back.boostersLeft(Player.RED, Booster.BOMB))
        assertEquals(captured.toAscii(), back.toAscii())
    }

    @Test
    fun `the captor can close a broken ring again`() {
        val captured = ringRoundBonus()
        // The plus round d6 hits only d5, the dot that closed the ring.
        val bombed = captured.use(Action.Bomb(p("d6"))).place("a1")
        assertEquals(0, bombed.score(Player.BLUE))

        val result = bombed.play(p("d5"))

        assertEquals(3, result.state.score(Player.BLUE))
        assertEquals(listOf(p("d3"), p("d4")), result.captures.single().dots.sortedBy { it.y })
        assertTrue(result.state.isDead(p("c3")))
        assertEquals(1, result.state.areas().size)
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
        // The second dot is extra: the turn takes one from the budget, like any other.
        assertEquals(9, first.dotsLeft(Player.RED))
        assertEquals(9, second.dotsLeft(Player.RED))
        assertEquals(2, second.dotsPlaced(Player.RED))
        assertEquals(1, second.movesMade(Player.RED))
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
    fun `a double dot needs one dot left and ends early on a full board`() {
        assertFalse(board(".....\n..R..\n..B..", dots = 0).canUse(Booster.DOUBLE_DOT))
        assertTrue(board(".....\n..R..\n..B..", dots = 1).canUse(Booster.DOUBLE_DOT))
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

    @Test
    fun `with one dot left, a double dot places two, and the second is the player's last`() {
        val start = board(".......\n.......\n...R...\n...B...\n.......", dots = 1)
        assertTrue(start.canUse(Booster.DOUBLE_DOT))

        val first = start.use(Action.DoubleDot).place("a1")
        assertEquals(0, first.dotsLeft(Player.RED))
        assertEquals(1, first.dotsToPlace(Player.RED))
        assertEquals(Player.RED, first.toMove)
        assertTrue(first.isLegal(p("a2")))
        assertFalse(first.isOver)

        val second = first.place("a2")
        assertEquals(2, second.dotsPlaced(Player.RED))
        assertEquals(0, second.dotsToPlace(Player.RED))
        assertEquals(Player.BLUE, second.toMove, "blue still answers with its last dot")
        assertTrue(second.place("g1").isOver)
    }

    @Test
    fun `with a double dot used once, both sides place their last dot on the same turn`() {
        // 6 dots each, red first; red's turn 3 has two dots. Red places 7, blue 6, turn by turn.
        var state = board(".........\n.........\n....R....\n....B....\n.........\n.........", dots = 6)
        val turns = ArrayList<Pair<Player, Int>>()
        while (!state.isOver) {
            val mover = state.toMove
            if (mover == Player.RED && state.dotsLeft(Player.RED) == 4) state = state.use(Action.DoubleDot)
            var dots = 0
            while (state.toMove == mover && !state.isOver) {
                state = state.play(state.legalMoves().first()).state
                dots++
            }
            turns += mover to dots
        }
        val red = turns.filter { it.first == Player.RED }.map { it.second }
        val blue = turns.filter { it.first == Player.BLUE }.map { it.second }
        assertEquals(listOf(1, 1, 2, 1, 1, 1), red)
        assertEquals(listOf(1, 1, 1, 1, 1, 1), blue)
        assertEquals(Player.BLUE, turns.last().first, "blue answers red's last dot")
        assertEquals(7, state.dotsPlaced(Player.RED))
        assertEquals(6, state.movesMade(Player.RED))
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
