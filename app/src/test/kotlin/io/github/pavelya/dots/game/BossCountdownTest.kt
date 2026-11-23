package io.github.pavelya.dots.game

import dots.campaign.Campaign
import dots.campaign.Level
import dots.engine.Action
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import dots.ai.Ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The boss's counter above the board (A-28): what it shows, checked against the turns on which the
 * engine gives the boss two dots, and the boss's two dots numbered on the board.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BossCountdownTest {
    private val boss = Player.BLUE

    /** What the counter showed through one game of a level. */
    private class Game(
        /** At the start of each of the boss's turns, by turn (the first is the boss's turn 1). */
        val bossTurns: List<Int?>,
        /** On each of the player's turns: the number of boss turns before it, and what it showed. */
        val playerTurns: List<Pair<Int, Int?>>,
        /** The boss's turns on which the engine gave it two dots. */
        val doubleTurns: List<Int>,
    )

    /** The first of [moves] that closes no ring. */
    private fun GameState.quiet(moves: List<Pos>): Pos = moves.first { play(it).captures.isEmpty() }

    /**
     * Plays [level] to the end of its dots: the player from the bottom row up, the boss from the
     * top row down, each skipping points that would close a ring, so that nothing is captured. The
     * boss's second dot of a turn of two must still show 0.
     */
    private fun play(level: Level): Game {
        var state = level.newGame()
        val bossTurns = mutableListOf<Int?>()
        val playerTurns = mutableListOf<Pair<Int, Int?>>()
        val doubleTurns = mutableListOf<Int>()
        var turnDots = 0
        var placed = 0
        while (!state.isOver) {
            if (state.toMove == boss) {
                if (placed == turnDots) {
                    turnDots = state.dotsThisTurn
                    placed = 0
                    bossTurns += bossCountdown(state, boss)
                    if (turnDots == 2) doubleTurns += bossTurns.size
                } else {
                    assertEquals("the second dot of turn ${bossTurns.size}", 0, bossCountdown(state, boss))
                }
                placed++
                state = state.play(state.quiet(state.legalMoves())).state
            } else {
                turnDots = 0
                placed = 0
                playerTurns += bossTurns.size to bossCountdown(state, boss)
                state = state.play(state.quiet(state.legalMoves().reversed())).state
            }
        }
        assertEquals("nothing captured", 0, state.score(Player.RED) + state.score(Player.BLUE))
        return Game(bossTurns, playerTurns, doubleTurns)
    }

    /**
     * Checks that the counter always shows the boss's turns until its next turn of two, as the
     * engine plays them: 0 on those turns and on no other, and nothing once no turn of two is to come.
     */
    private fun checkAgainstTheEngine(game: Game) {
        fun next(after: Int): Int? = game.doubleTurns.firstOrNull { it > after }
        game.bossTurns.forEachIndexed { i, shown ->
            val turn = i + 1
            val expected = if (turn in game.doubleTurns) 0 else next(turn)?.let { it - turn }
            assertEquals("at the start of the boss's turn $turn", expected, shown)
        }
        for ((done, shown) in game.playerTurns) {
            assertEquals("on the player's turn after $done boss turns", next(done)?.let { it - done }, shown)
        }
    }

    @Test
    fun `on 1-15 it counts down from the first move and reaches now on the boss's turns 5, 10 and 15`() {
        val game = play(Campaign.level(1, 15))
        assertEquals(listOf(5, 10, 15), game.doubleTurns)
        assertEquals(listOf(5, 10, 15), game.bossTurns.indices.filter { game.bossTurns[it] == 0 }.map { it + 1 })
        assertEquals("from the first move", 0 to 5, game.playerTurns.first())
        assertEquals(List(3) { listOf(5, 4, 3, 2, 1) }.flatten(), game.playerTurns.take(15).map { it.second })
        assertTrue("never now on the player's turn", game.playerTurns.none { it.second == 0 })
        checkAgainstTheEngine(game)
    }

    @Test
    fun `2-20 and 3-20 have their own schedule, every 4 and every 3 turns`() {
        for ((level, every) in listOf(Campaign.level(2, 20) to 4, Campaign.level(3, 20) to 3)) {
            val game = play(level)
            // Each turn takes one from the budget (decision 0039), so every double move up to the
            // boss's last turn comes: 5 on 2-20 (22 turns), 8 on 3-20 (26 turns).
            assertEquals(level.code, level.dotsPerPlayer, game.bossTurns.size)
            assertEquals(level.code, List(level.dotsPerPlayer / every) { (it + 1) * every }, game.doubleTurns)
            assertEquals("${level.code}: from the first move", 0 to every, game.playerTurns.first())
            checkAgainstTheEngine(game)
        }
    }

    @Test
    fun `no other level has a counter`() {
        val withCounter = Campaign.levels.filter { bossCountdown(it.newGame(), it.computer) != null }.map { it.code }
        assertEquals(listOf("1-15", "2-20", "3-20"), withCounter)
    }

    // The boss's two dots, numbered on the board.

    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    init {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Plays the first free point, in the top row. */
    private class TopRow : Ai {
        override fun choose(state: GameState): Action = Action.Place(state.legalMoves().first())
    }

    private fun GameViewModel.placeAtTheBottom() {
        val pos: Pos = ui.value.state.legalMoves().last()
        onTap(pos)
        onTap(pos)
    }

    @Test
    fun `the boss's two dots are numbered from its turn of two until the player's next dot`() = runTest(dispatcher) {
        val level = Campaign.level(1, 15)
        val vm = GameViewModel(
            start = levelGame(level, threatHighlight = false, now = 0),
            nextGame = { levelGame(level, threatHighlight = false, now = 0) },
            computerFor = { TopRow() },
            computerDispatcher = dispatcher,
            replyDelayMillis = 0,
            clock = { 0L },
        )
        repeat(4) {
            vm.placeAtTheBottom()
            advanceUntilIdle()
            assertEquals("after the boss's turn ${it + 1}", emptyList<Pos>(), vm.ui.value.bossDots)
        }
        vm.placeAtTheBottom()
        advanceUntilIdle()
        val state = vm.ui.value.state
        val numbered = vm.ui.value.bossDots
        assertEquals(2, numbered.size)
        assertTrue(numbered.all { state.ownerAt(it) == boss })
        assertNotEquals(numbered[0], numbered[1])
        assertEquals("2 is its last dot", state.lastPlaced(boss), numbered[1])
        assertEquals("the counter starts again", 5, bossCountdown(state, boss))

        vm.placeAtTheBottom()
        assertEquals(emptyList<Pos>(), vm.ui.value.bossDots)
        advanceUntilIdle()
        assertEquals("a turn of one dot is not numbered", emptyList<Pos>(), vm.ui.value.bossDots)
    }
}
