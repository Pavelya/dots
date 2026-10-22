package dots.ai

import dots.engine.GameState
import dots.engine.Grounding
import dots.engine.Openings
import dots.engine.Player
import dots.engine.Pos
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThreatFinderTest {

    private val finder = ThreatFinder()

    private fun cells(state: GameState, vararg cells: Pos) = cells.map { it.y * state.width + it.x }.toSet()

    @Test
    fun `a diamond missing one dot is one move from capture`() {
        val state = GameState.fromAscii(
            """
            .....
            ..R..
            .RBR.
            .....
            .....
            """,
        )

        val threats = finder.find(state, Player.BLUE, 3)

        assertEquals(1, threats.size)
        assertEquals(1, threats[0].cost)
        assertEquals(1, threats[0].size)
        assertEquals(cells(state, Pos(2, 3)), threats[0].cells.toSet())
    }

    @Test
    fun `a diamond missing two dots costs two and names both cells`() {
        val state = GameState.fromAscii(
            """
            .....
            ..R..
            .RB..
            .....
            .....
            """,
        )

        val threat = finder.find(state, Player.BLUE, 3).single()

        assertEquals(2, threat.cost)
        assertEquals(cells(state, Pos(3, 2), Pos(2, 3)), threat.cells.toSet())
    }

    @Test
    fun `threats dearer than the limit are left out`() {
        val state = GameState.fromAscii(
            """
            .....
            ..R..
            .RB..
            .....
            .....
            """,
        )

        assertTrue(finder.find(state, Player.BLUE, 1).isEmpty())
        assertTrue(finder.find(state, Player.BLUE, 0).isEmpty())
    }

    @Test
    fun `grounded groups and groups already inside a closed chain are not threats`() {
        val grounded = GameState.fromAscii(
            """
            B....
            .R...
            .....
            .....
            ....R
            """,
        )
        assertTrue(finder.find(grounded, Player.BLUE, 5).isEmpty(), "the blue dot is on the border")

        val ring = GameState.fromAscii(
            """
            ......
            .RRRR.
            .R...R
            .R...R
            .RRR..
            ......
            """,
        ).play(Pos(4, 4)).state.play(Pos(2, 2)).state
        assertTrue(finder.find(ring, Player.BLUE, 5).isEmpty(), "a dot inside a finished chain needs a dividing wall instead")
    }

    @Test
    fun `agrees with the slow planner on positions from real games`() {
        val sloppy = AiConfig(depth = 1, breadth = 12, mistakeRate = 0.6)
        var compared = 0
        var realThreats = 0
        for (seed in 1..24) {
            val random = Random(seed)
            val ai = SearchAi(sloppy, random)
            val opening = if (seed % 2 == 0) Openings.cross(9, 9) else Openings.randomCrosses(11, 11, 2, random)
            val size = if (seed % 2 == 0) 9 else 11
            var state = GameState.start(size, size, dots = opening)
            repeat(40) {
                if (state.isOver) return@repeat
                state = state.apply(ai.choose(state)).state
                for (victim in Player.entries) {
                    val (all, threats) = compare(state, victim)
                    compared += all
                    realThreats += threats
                }
            }
        }
        assertTrue(compared > 500, "only $compared comparisons")
        assertTrue(realThreats > 100, "only $realThreats of them were real threats, so the check proves little")
    }

    /** Returns how many groups were compared and how many of them are threats within the limit. */
    private fun compare(state: GameState, victim: Player): Pair<Int, Int> {
        val attacker = victim.opponent
        val limit = 6
        val threats = finder.find(state, victim, limit)
        var count = 0
        var real = 0
        for (group in Grounding.ungrounded(state, victim)) {
            val reference = Cleanup.cheapestEnclosure(state, group, attacker)
            val anchors = group.map { it.y * state.width + it.x }.toSet()
            val threat = threats.firstOrNull { it.anchor in anchors }
            val expected = reference?.size?.takeIf { it in 1..limit }
            assertEquals(expected, threat?.cost, "group ${group.first()} of $victim in\n${state.toAscii()}")
            if (threat != null) {
                assertEquals(threat.cost, threat.cells.size)
                assertEquals(group.size, threat.size)
            }
            count++
            if (expected != null) real++
        }
        return count to real
    }

    @Test
    fun `a move that creates two separate threats wins a dot`() {
        // Red's dot between the two blue dots shortens both captures to one move each; blue can
        // only stop one of them.
        val fork = GameState.fromAscii(
            """
            .......
            ..R.R..
            .RB.BR.
            .......
            .......
            .......
            """,
        )
        val red = SearchAi(AiConfig.HARD, Random(1))
        val blue = SearchAi(AiConfig.HARD, Random(2))

        val afterFork = fork.play(red.chooseMove(fork)!!).state
        val afterDefence = afterFork.play(blue.chooseMove(afterFork)!!).state
        val result = afterDefence.play(red.chooseMove(afterDefence)!!)

        assertEquals(1, result.state.score(Player.RED))
    }
}
