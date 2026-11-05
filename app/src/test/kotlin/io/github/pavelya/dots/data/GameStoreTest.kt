package io.github.pavelya.dots.data

import dots.ai.Ai
import dots.engine.Action
import dots.engine.GameSettings
import dots.engine.GameState
import dots.engine.Opening
import dots.engine.Player
import dots.engine.Pos
import dots.log.GameLogEntry
import dots.log.Outcome
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.gameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.Executor

/** Plays the first free point; predictable, so tests can follow the game. */
private class FirstFreePoint : Ai {
    override fun choose(state: GameState): Action = Action.Place(state.legalMoves().first())
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val sameThread = Executor { it.run() }
    private val settings = GameSettings(width = 7, height = 7, dotsPerPlayer = 4, opening = Opening.CROSS)

    init {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** A store on [dir], as the app opens it after a (re)start. */
    private fun openStore(dir: File = folder.root) = GameStore({ dir }, "test-1", sameThread)

    private fun newGame() = GameLogEntry.start(settings, "test", threatHighlight = false, now = 0)

    private fun play(store: GameStore, start: GameLogEntry) =
        GameViewModel(start, ::newGame, computerFor = { FirstFreePoint() }, journal = store, computerDispatcher = dispatcher)

    /** A new game against the computer, started the way the home screen starts one. */
    private fun vsComputer(store: GameStore) = newGame().let { store.start(it); play(store, it) }

    /** Opens the saved game the way the app's game screen does. */
    private fun resume(store: GameStore) = play(store, checkNotNull(store.current.value) { "no saved game" })

    private fun GameViewModel.place(pos: Pos) {
        onTap(pos)
        onTap(pos)
    }

    @Test
    fun `a game closed mid-way comes back in the same position`() = runTest(dispatcher) {
        val store = openStore()
        val before = vsComputer(store)
        before.place(Pos(0, 6))
        advanceUntilIdle()
        before.place(Pos(6, 6))
        advanceUntilIdle()
        before.place(Pos(6, 0))
        advanceUntilIdle()
        before.undo()

        // Android kills the app: everything in memory is gone, only the files are left.
        val after = resume(openStore())
        advanceUntilIdle()

        val was = before.ui.value
        val now = after.ui.value
        assertEquals(was.state.toAscii(), now.state.toAscii())
        assertEquals(was.state.toMove, now.state.toMove)
        assertEquals(was.state.dotsLeft(Player.RED), now.state.dotsLeft(Player.RED))
        assertEquals(was.lastMove, now.lastMove)
        assertEquals(was.canUndo, now.canUndo)
        assertEquals(1, after.undoCount)
        assertTrue(now.vsComputer)
    }

    @Test
    fun `a game closed while the computer was to move gets its reply after resuming`() = runTest(dispatcher) {
        val store = openStore()
        val before = vsComputer(store)
        before.place(Pos(0, 6))
        assertTrue(before.ui.value.thinking) // closed before the computer answered

        val after = resume(openStore())
        advanceUntilIdle()

        assertEquals(2, after.ui.value.state.moveCount)
        assertEquals(Player.RED, after.ui.value.state.toMove)
    }

    @Test
    fun `a finished game goes to the log, consistent with its record, and is no longer resumable`() = runTest(dispatcher) {
        val store = openStore()
        val vm = vsComputer(store)
        while (!vm.ui.value.state.isOver) {
            vm.place(vm.ui.value.state.legalMoves().last())
            advanceUntilIdle()
        }

        assertNull(store.current.value)
        assertEquals(1, store.loggedGames.value)
        val reopened = openStore()
        assertNull(reopened.current.value)
        assertEquals(1, reopened.loggedGames.value)

        val logged = GameLogEntry.parseLog(reopened.logFile.readText()).single()
        assertEquals(Outcome.FINISHED, logged.outcome)
        assertEquals("test-1", logged.appVersion)
        assertEquals(8, logged.record.actions.size)
        assertNull(logged.check())
    }

    @Test
    fun `starting another game logs the unfinished one as abandoned, unless it had no moves`() = runTest(dispatcher) {
        val store = openStore()
        val vm = vsComputer(store)
        vm.playAgain() // no moves yet: nothing to log
        assertEquals(0, store.loggedGames.value)

        vm.place(Pos(0, 6))
        advanceUntilIdle()
        vm.playAgain()

        val logged = GameLogEntry.parseLog(store.logFile.readText()).single()
        assertEquals(Outcome.ABANDONED, logged.outcome)
        assertEquals(2, logged.record.actions.size)
        assertNull(logged.check())
        assertEquals(0, checkNotNull(store.current.value).record.actions.size)
    }

    @Test
    fun `time away from the game is not counted as playing time`() {
        var now = 1_000_000L
        val store = openStore()
        val vm = gameViewModel(newSettings = { settings }, journal = store, clock = { now })
        now += 10_000
        vm.place(Pos(0, 6))
        now += 3_600_000 // an hour away
        vm.place(Pos(6, 6))

        val saved = checkNotNull(store.current.value)
        assertEquals(10_000 + GameLogEntry.IDLE_CAP_MILLIS, saved.playMillis)
        assertEquals(now, saved.updatedAt)
    }

    @Test
    fun `a damaged saved game is ignored instead of crashing the app`() {
        File(folder.root, "current-game.json").writeText("{\"app\":\"x\", broken")
        val store = openStore()
        assertNull(store.current.value)
        assertTrue(store.loaded.value)
        assertFalse(File(folder.root, "game-log.txt").exists())
    }
}
