package io.github.pavelya.dots.game

import dots.ai.Ai
import dots.engine.Action
import dots.engine.GameSettings
import dots.engine.GameState
import dots.engine.Opening
import dots.engine.Player
import dots.engine.Pos
import dots.log.GameLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What each dot that lands hands the screen to animate (docs/design/motion.md), and when the computer answers. */
@OptIn(ExperimentalCoroutinesApi::class)
class BoardMomentTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    init {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val firstFree = object : Ai {
        override fun choose(state: GameState): Action = Action.Place(state.legalMoves().first())
    }

    private fun GameViewModel.place(x: Int, y: Int) {
        onTap(Pos(x, y))
        onTap(Pos(x, y))
    }

    private fun vsComputer(holdFor: (dots.engine.MoveResult) -> Long = { 0L }, replyDelayMillis: Long = 0): GameViewModel {
        val settings = GameSettings(width = 5, height = 5, dotsPerPlayer = 3, opening = Opening.EMPTY)
        fun next() = GameLogEntry.start(settings, "test", false, 0L)
        return GameViewModel(
            start = next(),
            nextGame = ::next,
            computerFor = { firstFree },
            computerDispatcher = dispatcher,
            replyDelayMillis = replyDelayMillis,
            clock = { 0L },
            holdFor = holdFor,
        )
    }

    private fun twoPlayers(settings: GameSettings) = gameViewModel(newSettings = { settings })

    @Test
    fun `the player's dot lands quietly, the computer's is announced with edge marks, which go with the player's next dot`() = runTest(dispatcher) {
        val vm = vsComputer()
        vm.place(4, 4)
        val mine = requireNotNull(vm.ui.value.moment)
        assertEquals(Pos(4, 4), mine.dot)
        assertEquals(Player.RED, mine.by)
        assertFalse(mine.announced)
        assertNull(vm.ui.value.edgeMarks)

        advanceUntilIdle()
        val reply = requireNotNull(vm.ui.value.moment)
        assertEquals(Pos(0, 0), reply.dot)
        assertEquals(Player.BLUE, reply.by)
        assertTrue(reply.announced)
        assertEquals(Pos(4, 4), reply.pinholeBefore)
        assertTrue(reply.id > mine.id)
        assertEquals(Pos(0, 0), vm.ui.value.edgeMarks)

        vm.place(4, 3)
        val next = requireNotNull(vm.ui.value.moment)
        assertEquals(Pos(0, 0), next.marksGone)
        assertNull(vm.ui.value.edgeMarks)
    }

    @Test
    fun `a preview keeps the moment, so it plays once, and an undo leaves nothing to play`() = runTest(dispatcher) {
        val vm = vsComputer()
        vm.place(4, 4)
        advanceUntilIdle()
        val reply = vm.ui.value.moment
        vm.onTap(Pos(4, 3))
        assertEquals(reply, vm.ui.value.moment)
        vm.undo()
        assertNull(vm.ui.value.moment)
        assertNull(vm.ui.value.edgeMarks)
    }

    @Test
    fun `the computer answers no sooner than the player's move has played out`() = runTest(dispatcher) {
        val vm = vsComputer(holdFor = { 1_000L }, replyDelayMillis = 500)
        vm.place(4, 4)
        runCurrent()
        advanceTimeBy(999)
        assertNull(vm.ui.value.state.ownerAt(Pos(0, 0)))
        advanceTimeBy(2)
        runCurrent()
        assertEquals(Player.BLUE, vm.ui.value.state.ownerAt(Pos(0, 0)))
    }

    @Test
    fun `the computer waits at least 500 ms by default`() = runTest(dispatcher) {
        val settings = GameSettings(width = 5, height = 5, dotsPerPlayer = 3, opening = Opening.EMPTY)
        fun next() = GameLogEntry.start(settings, "test", false, 0L)
        val vm = GameViewModel(start = next(), nextGame = ::next, computerFor = { firstFree }, computerDispatcher = dispatcher)
        vm.place(4, 4)
        runCurrent()
        advanceTimeBy(499)
        assertNull(vm.ui.value.state.ownerAt(Pos(0, 0)))
        advanceTimeBy(2)
        runCurrent()
        assertEquals(Player.BLUE, vm.ui.value.state.ownerAt(Pos(0, 0)))
    }

    @Test
    fun `a capture comes with its moment, and between two people every dot is announced`() {
        val vm = twoPlayers(GameSettings(width = 5, height = 5, dotsPerPlayer = 6, opening = Opening.EMPTY))
        // Red rings Blue's (2, 2).
        vm.place(2, 1); vm.place(2, 2); vm.place(1, 2); vm.place(0, 0); vm.place(3, 2)
        assertTrue(requireNotNull(vm.ui.value.moment).announced)
        vm.place(4, 0)
        vm.place(2, 3)
        val moment = requireNotNull(vm.ui.value.moment)
        assertEquals(Pos(2, 3), moment.dot)
        assertEquals(1, moment.captured)
        assertEquals(1, moment.points)
        assertEquals(listOf(Pos(2, 2)), moment.captures.single().dots)
        assertEquals(1, vm.ui.value.state.score(Player.RED))
        assertEquals(Pos(2, 3), vm.ui.value.edgeMarks)
        assertEquals(Pos(4, 0), moment.marksGone)
    }

    @Test
    fun `a new threat is marked fresh, and only the dot that made it pulses it`() {
        val settings = GameSettings(width = 5, height = 5, dotsPerPlayer = 6, opening = Opening.EMPTY)
        fun next() = GameLogEntry.start(settings, GameLogEntry.HUMAN, true, 0L)
        val vm = GameViewModel(start = next(), nextGame = ::next, computerFor = { null }, clock = { 0L })
        vm.place(2, 1); vm.place(2, 2); vm.place(1, 2); vm.place(0, 0)
        // Red's third dot leaves Blue's (2, 2) one dot from capture.
        vm.place(3, 2)
        val fresh = requireNotNull(vm.ui.value.moment).freshThreats
        assertTrue(ThreatMark(setOf(Pos(2, 2)), 1) in fresh)
        vm.place(4, 0)
        assertTrue(requireNotNull(vm.ui.value.moment).freshThreats.none { it.group == setOf(Pos(2, 2)) })
    }

    @Test
    fun `at the end, the areas come in the order they were captured, not the order on the board`() {
        val vm = twoPlayers(GameSettings(width = 7, height = 7, dotsPerPlayer = 8, opening = Opening.EMPTY))
        // Red first rings Blue's (5, 5), then Blue's (1, 1), which lies higher on the board.
        val red = listOf(Pos(5, 4), Pos(4, 5), Pos(6, 5), Pos(5, 6), Pos(1, 0), Pos(0, 1), Pos(2, 1), Pos(1, 2))
        val blue = listOf(Pos(5, 5), Pos(1, 1), Pos(3, 3), Pos(3, 4), Pos(0, 6), Pos(6, 0), Pos(3, 6), Pos(6, 3))
        for (i in red.indices) {
            vm.place(red[i].x, red[i].y)
            if (!vm.ui.value.isOver) vm.place(blue[i].x, blue[i].y)
        }
        assertTrue(vm.ui.value.isOver)
        assertEquals(listOf(CapturedArea(Pos(5, 5), Player.RED), CapturedArea(Pos(1, 1), Player.RED)), vm.ui.value.captureOrder)
    }
}
