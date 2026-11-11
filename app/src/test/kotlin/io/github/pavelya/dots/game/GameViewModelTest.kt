package io.github.pavelya.dots.game

import androidx.lifecycle.ViewModelStore
import dots.ai.Ai
import dots.engine.Action
import dots.engine.GameSettings
import dots.engine.GameState
import dots.engine.Opening
import dots.engine.Player
import dots.engine.Pos
import dots.log.GameLogEntry
import io.github.pavelya.dots.data.GameJournal
import io.github.pavelya.dots.data.PlacingMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher

/** Keeps every entry the game hands over, last one last. */
private class RecordingJournal : GameJournal {
    val entries = mutableListOf<GameLogEntry>()
    override fun start(entry: GameLogEntry) { entries += entry }
    override fun save(entry: GameLogEntry) { entries += entry }
    override fun finish(entry: GameLogEntry) { entries += entry }
}

/** Plays the first free point; predictable, so tests can follow the game. */
private class FirstFreePoint : Ai {
    override fun choose(state: GameState): Action = Action.Place(state.legalMoves().first())
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private val small = GameSettings(width = 5, height = 5, dotsPerPlayer = 2, opening = Opening.EMPTY)

    init {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun twoPlayers() = gameViewModel(newSettings = { GameSettings.POC.copy(seed = 1) })

    private fun vsComputer(settings: GameSettings = small, ai: () -> Ai = { FirstFreePoint() }) =
        gameViewModel(newSettings = { settings }, newComputer = ai, computerDispatcher = dispatcher)

    private fun GameViewModel.place(pos: Pos) {
        onTap(pos)
        onTap(pos)
    }

    @Test
    fun `a dot is placed only on the second tap, then it is the other player's turn`() {
        val vm = twoPlayers()
        val corner = Pos(0, 0)

        vm.onTap(corner)
        assertEquals(corner, vm.ui.value.preview)
        assertNull(vm.ui.value.state.ownerAt(corner))

        vm.onTap(corner)
        val ui = vm.ui.value
        assertEquals(Player.RED, ui.state.ownerAt(corner))
        assertEquals(corner, ui.lastMove)
        assertNull(ui.preview)
        assertEquals(Player.BLUE, ui.state.toMove)
        assertEquals(32, ui.state.dotsLeft(Player.RED))
    }

    @Test
    fun `a finger moving over the board moves the preview, and lifting it places the dot`() {
        val vm = twoPlayers()
        vm.place(Pos(0, 0))
        vm.aim(Pos(2, 2))
        assertEquals(Pos(2, 2), vm.ui.value.preview)
        vm.aim(Pos(0, 0)) // taken: no preview there
        assertNull(vm.ui.value.preview)
        vm.aim(Pos(3, 2))
        assertEquals(Pos(3, 2), vm.ui.value.preview)

        vm.release(Pos(3, 2))

        assertEquals(Player.BLUE, vm.ui.value.state.ownerAt(Pos(3, 2)))
        assertNull(vm.ui.value.preview)
        assertEquals(Player.RED, vm.ui.value.state.toMove)
    }

    @Test
    fun `lifting the finger off the board or on a taken point places nothing`() {
        val vm = twoPlayers()
        vm.aim(Pos(2, 2))
        vm.release(null)
        assertNull(vm.ui.value.preview)
        assertEquals(0, vm.ui.value.state.moveCount)

        vm.release(Pos(2, 2))
        vm.release(Pos(2, 2))
        assertEquals(1, vm.ui.value.state.moveCount)
    }

    @Test
    fun `the log records the placing method, every touch, and every dot placed by touch`() = runTest(dispatcher) {
        val journal = RecordingJournal()
        val vm = gameViewModel(
            newSettings = { small },
            newComputer = { FirstFreePoint() },
            computerDispatcher = dispatcher,
            journal = journal,
            placing = { PlacingMethod.MAGNIFIER },
        )
        vm.release(null) // a touch lifted off the board
        vm.release(Pos(4, 4))
        vm.release(Pos(3, 3)) // the computer is thinking: not a touch on the player's turn
        advanceUntilIdle()
        vm.onTap(Pos(3, 4))
        vm.onTap(Pos(3, 4))
        advanceUntilIdle()

        val logged = journal.entries.last()
        assertEquals("magnifier", logged.placing)
        assertEquals(4, logged.touches)
        assertEquals(2, logged.placed)
    }

    @Test
    fun `tapping an occupied point clears the preview and changes nothing`() {
        val vm = twoPlayers()
        vm.place(Pos(0, 0))
        vm.onTap(Pos(14, 14))

        vm.onTap(Pos(0, 0))

        assertNull(vm.ui.value.preview)
        assertEquals(Player.BLUE, vm.ui.value.state.toMove)
    }

    @Test
    fun `the computer replies, and taps are ignored while it thinks`() = runTest(dispatcher) {
        val vm = vsComputer()
        vm.place(Pos(4, 4))
        assertTrue(vm.ui.value.thinking)

        vm.place(Pos(3, 3))
        assertNull(vm.ui.value.state.ownerAt(Pos(3, 3)))

        advanceUntilIdle()
        val ui = vm.ui.value
        assertFalse(ui.thinking)
        assertEquals(2, ui.state.moveCount)
        assertEquals(Player.BLUE, ui.state.ownerAt(Pos(0, 0)))
        assertEquals(Pos(0, 0), ui.lastMove)
        assertEquals(Player.RED, ui.state.toMove)
        assertTrue(ui.canUndo)
    }

    @Test
    fun `undo takes back the human's move and the computer's reply`() = runTest(dispatcher) {
        val vm = vsComputer()
        vm.place(Pos(4, 4))
        advanceUntilIdle()

        vm.undo()

        val ui = vm.ui.value
        assertEquals(0, ui.state.moveCount)
        assertNull(ui.state.ownerAt(Pos(4, 4)))
        assertFalse(ui.canUndo)
        assertEquals(1, vm.undoCount)
    }

    @Test
    fun `undo while the computer thinks cancels its reply`() = runTest(dispatcher) {
        val vm = vsComputer()
        vm.place(Pos(4, 4))
        assertTrue(vm.ui.value.thinking)

        vm.undo()
        advanceUntilIdle()

        assertEquals(0, vm.ui.value.state.moveCount)
        assertFalse(vm.ui.value.thinking)
    }

    @Test
    fun `a finished game offers no undo, and play again starts a fresh one`() = runTest(dispatcher) {
        val vm = vsComputer()
        vm.place(Pos(4, 4))
        advanceUntilIdle()
        vm.place(Pos(4, 3))
        advanceUntilIdle()

        assertTrue(vm.ui.value.state.isOver)
        assertFalse(vm.ui.value.canUndo)

        vm.playAgain()
        assertEquals(0, vm.ui.value.state.moveCount)
        assertEquals(Player.RED, vm.ui.value.state.toMove)
        assertEquals(0, vm.undoCount)
    }

    @Test
    fun `leaving the screen cancels the computer's thinking`() = runTest(dispatcher) {
        var cancelSeen = false
        val watcher = object : Ai {
            override fun choose(state: GameState) = Action.Place(state.legalMoves().first())
            override fun choose(state: GameState, isCancelled: () -> Boolean): Action {
                cancelSeen = isCancelled()
                return choose(state)
            }
        }
        val vm = vsComputer(ai = { watcher })
        val store = ViewModelStore().apply { put("game", vm) }
        vm.place(Pos(4, 4))

        store.clear() // what Android does when the screen goes away
        advanceUntilIdle()

        assertEquals("the reply was never applied", 1, vm.ui.value.state.moveCount)
        assertFalse("the computer never started thinking after the screen closed", cancelSeen)
    }

    @Test
    fun `the computer thinks on its own thread, so a tap returns at once`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val release = CountDownLatch(1)
        val slow = object : Ai {
            override fun choose(state: GameState): Action {
                release.await(5, TimeUnit.SECONDS)
                return Action.Place(state.legalMoves().first())
            }
        }
        val background = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val vm = gameViewModel(newSettings = { small }, newComputer = { slow }, computerDispatcher = background, replyDelayMillis = 0)

        val started = System.nanoTime()
        vm.place(Pos(4, 4))
        val tookMillis = (System.nanoTime() - started) / 1_000_000

        assertTrue("the tap took $tookMillis ms", tookMillis < 1_000)
        assertTrue(vm.ui.value.thinking)
        release.countDown()
        val deadline = System.currentTimeMillis() + 5_000
        while (vm.ui.value.thinking && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertFalse(vm.ui.value.thinking)
        assertEquals(2, vm.ui.value.state.moveCount)
        background.close()
    }

    @Test
    fun `the real computer at each difficulty plays a whole game in the test format`() = runTest(dispatcher) {
        for (difficulty in io.github.pavelya.dots.data.Difficulty.entries) {
            val vm = gameViewModel(
                newSettings = { GameSettings.POC.copy(seed = 5) },
                newComputer = { difficulty.computer(kotlin.random.Random(6)) },
                computerDispatcher = dispatcher,
            )
            var turns = 0
            while (!vm.ui.value.state.isOver && turns++ < 100) {
                vm.place(vm.ui.value.state.legalMoves().first())
                advanceUntilIdle()
            }
            val end = vm.ui.value.state
            assertTrue("$difficulty did not finish", end.isOver)
            assertEquals("$difficulty: both sides place all 33 dots", 66, end.moveCount)
        }
    }

    @Test
    fun `threats are marked only when the setting is on`() {
        val settings = GameSettings(width = 5, height = 5, dotsPerPlayer = 10, opening = Opening.EMPTY)
        fun play(showThreats: Boolean): GameViewModel {
            val vm = gameViewModel(newSettings = { settings }, showThreats = showThreats)
            // Red builds three sides of a diamond around blue's dot at the centre.
            for (move in listOf(Pos(2, 1), Pos(2, 2), Pos(1, 2), Pos(4, 4), Pos(3, 2))) vm.place(move)
            return vm
        }

        val on = play(showThreats = true).ui.value.dangers
        assertEquals(listOf(Pos(2, 2)), on.first().group)
        assertEquals(listOf(Pos(2, 3)), on.first().closingPoints)
        assertTrue(play(showThreats = false).ui.value.dangers.isEmpty())
    }
}
