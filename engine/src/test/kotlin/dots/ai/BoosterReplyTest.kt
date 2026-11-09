package dots.ai

import dots.campaign.Campaign
import dots.campaign.Strength
import dots.engine.Action
import dots.engine.Booster
import dots.engine.GameState
import dots.engine.Notation
import dots.engine.Player
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** After each booster, the computer at every strength still answers with a legal move. */
class BoosterReplyTest {

    /** Level 1-3 with one of each booster, a few moves in. */
    private val start: GameState = run {
        val level = Campaign.level(1, 3).copy(boosters = Booster.entries.associateWith { 1 })
        listOf("d6", "g5", "c4", "e7").fold(level.newGame()) { state, name -> state.play(Notation.parse(name)!!).state }
    }

    private fun p(name: String) = Notation.parse(name)!!

    private fun assertComputerAnswers(state: GameState) {
        assertEquals(Player.BLUE, state.toMove, state.toAscii())
        for (strength in Strength.entries) {
            val action = SearchAi(strength.config, Random(1)).choose(state)
            assertTrue(action is Action.Place && state.isLegal(action), "$strength chose $action in\n${state.toAscii()}")
        }
    }

    @Test
    fun `after a wall`() {
        val state = start.apply(Action.Wall(p("f7"))).state.play(p("e8")).state
        assertComputerAnswers(state)
        for (strength in Strength.entries) {
            assertTrue(SearchAi(strength.config, Random(2)).choose(state) != Action.Place(p("f7")))
        }
    }

    @Test
    fun `after a hint`() {
        val hint = Hints.hint(start)
        assertNotNull(hint)
        val hinted = start.apply(hint).state
        assertNull(Hints.hint(hinted), "one booster per turn")
        assertComputerAnswers(hinted.play(hint.pos).state)
    }

    @Test
    fun `after a bomb`() {
        val bombed = start.apply(Action.Bomb(p("f5"))).state
        assertEquals(listOf(null, null, Player.RED), listOf("f5", "g5", "e5").map { bombed.ownerAt(p(it)) })
        assertComputerAnswers(bombed.play(p("f5")).state)
    }

    @Test
    fun `after a double dot`() {
        val state = start.apply(Action.DoubleDot).state.play(p("g6")).state.play(p("f4")).state
        assertComputerAnswers(state)
    }

    @Test
    fun `a hint is the move that captures when there is one`() {
        val state = GameState.start(
            5, 5,
            dots = mapOf(p("c2") to Player.RED, p("b3") to Player.RED, p("d3") to Player.RED, p("c3") to Player.BLUE),
            boosters = mapOf(Player.RED to mapOf(Booster.HINT to 1)),
        )
        assertEquals(Action.Hint(p("c4")), Hints.hint(state, SearchAi(AiConfig.HARD.copy(variety = 0))))
        assertNull(Hints.hint(state.apply(Action.Hint(p("c4"))).state.play(p("a1")).state), "blue has no hint")
    }
}
