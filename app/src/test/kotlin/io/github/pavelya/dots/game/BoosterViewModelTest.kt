package io.github.pavelya.dots.game

import dots.ai.Ai
import dots.campaign.Campaign
import dots.campaign.Level
import dots.campaign.LevelMap
import dots.campaign.LevelRecord
import dots.campaign.Mission
import dots.campaign.Progress
import dots.campaign.Strength
import dots.engine.Action
import dots.engine.Booster
import dots.engine.Game
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import dots.log.GameLogEntry
import dots.campaign.CampaignMap
import dots.campaign.LevelResult
import io.github.pavelya.dots.data.CampaignJournal
import io.github.pavelya.dots.data.GameJournal
import io.github.pavelya.dots.data.LevelEnd
import io.github.pavelya.dots.data.UsedBoosters
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Keeps the last entry the game hands over. */
private class LastEntry : GameJournal {
    var entry: GameLogEntry? = null
    override fun start(entry: GameLogEntry) { this.entry = entry }
    override fun save(entry: GameLogEntry) { this.entry = entry }
    override fun finish(entry: GameLogEntry) { this.entry = entry }
}

/** Plays the first free point, in the top row, far from where the tests aim. */
private class TopRow : Ai {
    override fun choose(state: GameState): Action = Action.Place(state.legalMoves().first())
}

/** The boosters in a level (A-05): the preview, choosing a point by tap or drag, Confirm and Cancel. */
@OptIn(ExperimentalCoroutinesApi::class)
class BoosterViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    init {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** A 7 x 7 level with one of each booster; the computer has dots at (3, 3) and (5, 5). */
    private val level = Level(
        1, 7, "Every Booster", Strength.BEGINNER, 6,
        LevelMap(listOf(".......", ".......", ".......", "...B...", ".......", ".....B.", ".......")),
        Mission.Win,
        boosters = Booster.entries.associateWith { 1 },
    )

    /** The point the Hint shows in these tests, unless the real one is asked for. */
    private val hintPoint = Pos(6, 6)

    private fun game(
        journal: GameJournal? = null,
        hintFor: (GameState, Level?) -> Action.Hint? = { _, _ -> Action.Hint(hintPoint) },
        level: Level = this.level,
    ) = GameViewModel(
        start = levelGame(level, threatHighlight = false, now = 0),
        nextGame = { levelGame(level, threatHighlight = false, now = 0) },
        computerFor = { TopRow() },
        journal = journal,
        computerDispatcher = dispatcher,
        replyDelayMillis = 0,
        clock = { 0L },
        hintFor = hintFor,
    )

    private fun GameViewModel.place(pos: Pos) {
        onTap(pos)
        onTap(pos)
    }

    private val GameViewModel.state get() = ui.value.state

    private fun GameViewModel.left(booster: Booster) = state.boostersLeft(Player.RED, booster)

    /** The first of [points] still free to play. */
    private fun GameViewModel.free(vararg points: Pos): Pos = points.first { state.isLegal(it) }

    @Test
    fun `a Wall's point is chosen by a tap, and nothing is spent until Confirm`() {
        val vm = game()
        assertTrue(Booster.WALL in vm.ui.value.usable)

        vm.chooseBooster(Booster.WALL)
        val aim = vm.ui.value.aim!!
        assertEquals(Booster.WALL, aim.booster)
        assertTrue(Pos(2, 2) in aim.targets)
        assertFalse("not on a dot", Pos(3, 3) in aim.targets)
        assertFalse(aim.ready)
        assertTrue("no other booster while the preview is open", vm.ui.value.usable.isEmpty())

        vm.confirmBooster() // no point yet: nothing happens
        assertEquals(1, vm.left(Booster.WALL))

        vm.onTap(Pos(2, 2))
        assertEquals(Pos(2, 2), vm.ui.value.aim?.point)
        vm.onTap(Pos(3, 3)) // a dot: keeps the point
        vm.onTap(null) // off the board: keeps the point
        assertEquals(Pos(2, 2), vm.ui.value.aim?.point)
        assertNull("no dot is placed while aiming", vm.state.ownerAt(Pos(2, 2)))
        assertEquals(1, vm.left(Booster.WALL))

        vm.confirmBooster()
        assertNull(vm.ui.value.aim)
        assertTrue(vm.state.isBlocked(Pos(2, 2)))
        assertEquals(0, vm.left(Booster.WALL))
        assertEquals("the player still places their dot", Player.RED, vm.state.toMove)
        assertTrue("one booster per turn", vm.ui.value.usable.isEmpty())
    }

    @Test
    fun `under drag the point follows the finger, and lifting it off the targets keeps the one chosen`() {
        val vm = game()
        vm.chooseBooster(Booster.WALL)

        vm.aim(Pos(1, 1))
        assertEquals(Pos(1, 1), vm.ui.value.aim?.point)
        vm.release(null) // lifted off the board before any point was chosen
        assertNull(vm.ui.value.aim?.point)

        vm.aim(Pos(2, 2))
        vm.release(Pos(2, 2))
        assertEquals(Pos(2, 2), vm.ui.value.aim?.point)
        vm.aim(Pos(4, 4))
        assertEquals(Pos(4, 4), vm.ui.value.aim?.point)
        vm.release(null)
        assertEquals(Pos(2, 2), vm.ui.value.aim?.point)
        vm.aim(Pos(3, 3))
        vm.release(Pos(3, 3)) // a dot: not a target
        assertEquals(Pos(2, 2), vm.ui.value.aim?.point)

        vm.confirmBooster()
        assertTrue(vm.state.isBlocked(Pos(2, 2)))
        assertFalse(vm.state.isBlocked(Pos(4, 4)))
    }

    @Test
    fun `Cancel closes the preview and spends nothing`() {
        val vm = game()
        vm.chooseBooster(Booster.BOMB)
        vm.onTap(Pos(3, 3))
        val aim = vm.ui.value.aim!!
        assertEquals(listOf(Pos(3, 3)), aim.gone)
        assertEquals(setOf(Pos(3, 3), Pos(3, 2), Pos(2, 3), Pos(4, 3), Pos(3, 4)), aim.plus.toSet())

        vm.cancelBooster()
        assertNull(vm.ui.value.aim)
        assertEquals(1, vm.left(Booster.BOMB))
        assertEquals(Player.BLUE, vm.state.ownerAt(Pos(3, 3)))
        assertTrue(Booster.BOMB in vm.ui.value.usable)
    }

    @Test
    fun `a Bomb removes the computer's dots in its plus`() {
        val vm = game()
        vm.chooseBooster(Booster.BOMB)
        assertFalse("a bomb needs a dot to hit", Pos(0, 0) in vm.ui.value.aim!!.targets)
        vm.onTap(Pos(3, 4))
        assertEquals(listOf(Pos(3, 3)), vm.ui.value.aim?.gone)

        vm.confirmBooster()
        assertNull(vm.state.ownerAt(Pos(3, 3)))
        assertEquals(Player.BLUE, vm.state.ownerAt(Pos(5, 5)))
        assertEquals(0, vm.left(Booster.BOMB))
    }

    @Test
    fun `a booster with none left cannot be chosen`() = runTest(dispatcher) {
        val vm = game()
        vm.chooseBooster(Booster.WALL)
        vm.onTap(Pos(2, 2))
        vm.confirmBooster()
        vm.place(Pos(4, 4))
        advanceUntilIdle()

        assertEquals(Player.RED, vm.state.toMove)
        assertFalse(Booster.WALL in vm.ui.value.usable)
        assertTrue(Booster.BOMB in vm.ui.value.usable)
        vm.chooseBooster(Booster.WALL)
        assertNull(vm.ui.value.aim)
    }

    @Test
    fun `boosters wait while the computer thinks`() = runTest(dispatcher) {
        val vm = game()
        vm.place(Pos(4, 4))
        assertTrue(vm.ui.value.thinking)
        assertTrue(vm.ui.value.usable.isEmpty())
        vm.chooseBooster(Booster.HINT)
        assertNull(vm.ui.value.aim)
        advanceUntilIdle()
        assertTrue(Booster.HINT in vm.ui.value.usable)
    }

    @Test
    fun `a Double dot gives two dots this turn, and the chip counts them`() = runTest(dispatcher) {
        val vm = game()
        vm.chooseBooster(Booster.DOUBLE_DOT)
        assertTrue("no point to choose", vm.ui.value.aim!!.ready)
        vm.confirmBooster()
        assertEquals(2, vm.ui.value.doubleDots)

        vm.place(Pos(2, 2))
        assertEquals(Player.RED, vm.state.toMove)
        assertEquals(1, vm.ui.value.doubleDots)

        vm.place(Pos(2, 4))
        assertEquals(Player.BLUE, vm.state.toMove)
        assertNull(vm.ui.value.doubleDots)
        advanceUntilIdle()
        assertNull(vm.ui.value.doubleDots)
    }

    @Test
    fun `a Double dot can be used with one dot left, and its second dot is extra`() = runTest(dispatcher) {
        val vm = game()
        // Five of the player's 6 dots along the bottom row, each answered along the top.
        for (x in 0 until 5) {
            vm.place(Pos(x, 6))
            advanceUntilIdle()
        }
        assertEquals(1, vm.state.dotsLeft(Player.RED))
        assertTrue(Booster.DOUBLE_DOT in vm.ui.value.usable)

        vm.chooseBooster(Booster.DOUBLE_DOT)
        vm.confirmBooster()
        vm.place(Pos(5, 6))
        assertEquals(Player.RED, vm.state.toMove)
        assertEquals(0, vm.state.dotsLeft(Player.RED))
        assertEquals(1, vm.ui.value.doubleDots)
        vm.place(Pos(6, 6))
        assertEquals(7, vm.state.dotsPlaced(Player.RED))
        advanceUntilIdle()
        assertEquals("the computer answers with its last dot", 6, vm.state.dotsPlaced(Player.BLUE))
        assertTrue(vm.state.isOver)
    }

    @Test
    fun `a Hint circles the best move until the player's dot lands`() = runTest(dispatcher) {
        val vm = game()
        vm.chooseBooster(Booster.HINT)
        vm.confirmBooster()
        assertTrue("the hint is looked for off the main thread", vm.ui.value.thinking)
        advanceUntilIdle()

        assertFalse(vm.ui.value.thinking)
        assertEquals(hintPoint, vm.ui.value.hint)
        assertEquals(0, vm.left(Booster.HINT))

        vm.place(hintPoint)
        assertNull(vm.ui.value.hint)
    }

    @Test
    fun `Undo waits for a turn, shows the turn it takes back, and takes it back`() = runTest(dispatcher) {
        val vm = game()
        assertFalse("nothing to take back yet", Booster.UNDO in vm.ui.value.usable)

        vm.place(Pos(4, 4))
        advanceUntilIdle()
        val reply = vm.state.lastPlaced(Player.BLUE)!!
        assertTrue(Booster.UNDO in vm.ui.value.usable)

        vm.chooseBooster(Booster.UNDO)
        assertEquals(setOf(Pos(4, 4), reply), vm.ui.value.aim?.gone?.toSet())
        assertEquals(OpponentLook(Mood.SMUG, Bubble.TAKE_IT_BACK), vm.ui.value.opponent)

        vm.confirmBooster()
        assertNull(vm.ui.value.aim)
        assertNull(vm.state.ownerAt(Pos(4, 4)))
        assertNull(vm.state.ownerAt(reply))
        assertNull(vm.ui.value.lastMove)
        assertEquals(0, vm.left(Booster.UNDO))
        assertEquals(1, vm.undoCount)
        assertEquals(Player.RED, vm.state.toMove)
    }

    @Test
    fun `each booster comes with its effect to play, until the next dot lands`() = runTest(dispatcher) {
        val vm = game()
        vm.chooseBooster(Booster.BOMB)
        vm.onTap(Pos(3, 4))
        vm.confirmBooster()
        val bomb = vm.ui.value.booster!!
        assertEquals(Booster.BOMB, bomb.booster)
        assertEquals(Player.RED, bomb.by)
        assertEquals(Pos(3, 4), bomb.point)
        assertEquals(listOf(Pos(3, 3)), bomb.removed)
        assertNull("a booster is no dot", vm.ui.value.moment)

        vm.place(Pos(4, 4))
        assertNull(vm.ui.value.booster)
        advanceUntilIdle()

        vm.chooseBooster(Booster.WALL)
        vm.onTap(Pos(2, 2))
        vm.confirmBooster()
        val wall = vm.ui.value.booster!!
        assertEquals(Booster.WALL, wall.booster)
        assertEquals(Pos(2, 2), wall.point)
        assertTrue("each moment plays once", wall.id > bomb.id)
    }

    @Test
    fun `dots after a Double dot count down for their sparks, and a dot after a Hint takes its circles away`() = runTest(dispatcher) {
        val vm = game()
        vm.chooseBooster(Booster.DOUBLE_DOT)
        vm.confirmBooster()
        assertEquals(Booster.DOUBLE_DOT, vm.ui.value.booster?.booster)
        vm.place(Pos(2, 2))
        assertEquals(2, vm.ui.value.moment?.doubleDots)
        vm.place(Pos(2, 4))
        assertEquals(1, vm.ui.value.moment?.doubleDots)
        advanceUntilIdle()
        assertNull("the computer's dot does not spark", vm.ui.value.moment?.doubleDots)

        vm.chooseBooster(Booster.HINT)
        vm.confirmBooster()
        advanceUntilIdle()
        assertEquals(Booster.HINT, vm.ui.value.booster?.booster)
        assertEquals(hintPoint, vm.ui.value.booster?.point)
        vm.place(Pos(1, 5))
        assertEquals(hintPoint, vm.ui.value.moment?.hintBefore)
    }

    @Test
    fun `Undo's effect knows the turn it takes back and the booster it gives back`() = runTest(dispatcher) {
        val vm = game()
        vm.chooseBooster(Booster.WALL)
        vm.onTap(Pos(2, 2))
        vm.confirmBooster()
        vm.place(Pos(4, 4))
        advanceUntilIdle()
        val reply = vm.state.lastPlaced(Player.BLUE)!!

        vm.chooseBooster(Booster.UNDO)
        vm.confirmBooster()
        val undo = vm.ui.value.booster!!
        assertEquals(Booster.UNDO, undo.booster)
        val undone = undo.undone!!
        assertEquals(listOf(reply), undone.answer)
        assertEquals(listOf(Pos(4, 4)), undone.own)
        assertEquals(Booster.WALL, undone.givenBack)
        assertEquals(Pos(2, 2), undone.givenBackAt)
        assertEquals(OpponentLook(Mood.SMUG, Bubble.TAKE_IT_BACK), undone.opponentBefore)
        assertEquals(Player.RED, undone.before.ownerAt(Pos(4, 4)))
        assertEquals(1, vm.left(Booster.WALL))
    }

    @Test
    fun `a level played with all five boosters replays from its log line, as cli log-check replays it`() = runTest(dispatcher) {
        // A record names its level by world and number, so the game is a real one: Rocky Shore
        // allows one of each booster.
        val rockyShore = Campaign.level(4, 6)
        assertEquals(Booster.entries.toSet(), rockyShore.boosters.keys)
        val journal = LastEntry()
        val vm = game(journal, hintFor = { state, played -> played!!.hint(state) }, level = rockyShore) // the real Hint
        fun turn(play: () -> Unit) {
            play()
            advanceUntilIdle()
            assertEquals(Player.RED, vm.state.toMove)
        }

        turn { vm.place(Pos(6, 6)) }
        turn { vm.chooseBooster(Booster.UNDO); vm.confirmBooster() }
        turn { vm.chooseBooster(Booster.WALL); vm.onTap(Pos(7, 7)); vm.confirmBooster(); vm.place(Pos(6, 6)) }
        turn {
            vm.chooseBooster(Booster.HINT)
            vm.confirmBooster()
            advanceUntilIdle()
            val hint = vm.ui.value.hint
            assertNotNull(hint)
            vm.place(hint!!)
        }
        turn {
            vm.chooseBooster(Booster.BOMB)
            vm.onTap(vm.ui.value.aim!!.targets.first())
            vm.confirmBooster()
            vm.place(vm.free(Pos(6, 8), Pos(8, 6), Pos(5, 6), Pos(6, 5)))
        }
        turn {
            vm.chooseBooster(Booster.DOUBLE_DOT)
            vm.confirmBooster()
            vm.place(vm.free(Pos(8, 8), Pos(5, 8), Pos(8, 5)))
            vm.place(vm.free(Pos(5, 5), Pos(7, 5), Pos(5, 7)))
        }

        for (booster in Booster.entries) assertEquals("$booster used", 0, vm.left(booster))
        val entry = journal.entry!!
        val words = (entry.record as LevelRecord).toText()
        for (word in listOf("undo", "wall:", "hint:", "bomb:", "double-dot")) assertTrue("$word in $words", word in words)

        // The same check `cli log-check` runs on a shared log, then the position itself.
        val logged = GameLogEntry.parseLog(entry.toJson()).single()
        assertNull(logged.check())
        val replayed = Game.from(logged.record).state
        val now = vm.state
        for (y in 0 until now.height) for (x in 0 until now.width) {
            assertEquals("owner at ($x, $y)", now.ownerAt(x, y), replayed.ownerAt(x, y))
            assertEquals("blocked at ($x, $y)", now.isBlocked(x, y), replayed.isBlocked(x, y))
        }
        assertEquals(now.score(Player.RED), replayed.score(Player.RED))
    }

    /** Keeps what a game reports of the boosters it used. */
    private class Spending : CampaignJournal {
        val reports = mutableListOf<Pair<Long, UsedBoosters>>()
        override suspend fun finished(level: Level, result: LevelResult) = LevelEnd(result, CampaignMap.Opened.NOTHING, 0, null)
        override fun spent(game: Long, level: Level, used: UsedBoosters) { reports += game to used }
    }

    /** Saves what games spend into [progress], as the progress store does: each game's change once. */
    private class Saved(var progress: Progress) : CampaignJournal {
        private var last: Pair<Long, UsedBoosters>? = null

        override suspend fun finished(level: Level, result: LevelResult) = LevelEnd(result, CampaignMap.Opened.NOTHING, 0, null)

        override fun spent(game: Long, level: Level, used: UsedBoosters) {
            val before = last?.takeIf { it.first == game }?.second ?: UsedBoosters.NONE
            fun change(now: Map<Booster, Int>, was: Map<Booster, Int>) = (now.keys + was.keys).associateWith { (now[it] ?: 0) - (was[it] ?: 0) }
            progress = progress.spend(change(used.owned, before.owned)).spendOn(level, change(used.own, before.own))
            last = game to used
        }
    }

    @Test
    fun `the player's own boosters come on top of the level's, and the game reports each one it uses or gets back`() = runTest(dispatcher) {
        val spending = Spending()
        val owned = Progress(boosters = mapOf(Booster.WALL to 1))
        val start = levelGame(level, threatHighlight = false, now = 7, progress = owned)
        val vm = GameViewModel(
            start = start,
            nextGame = { start },
            computerFor = { TopRow() },
            campaign = spending,
            computerDispatcher = dispatcher,
            replyDelayMillis = 0,
            clock = { 0L },
            progress = { owned },
        )
        assertEquals(2, vm.left(Booster.WALL))
        assertEquals("a new game reports nothing used yet", listOf(7L to UsedBoosters.NONE), spending.reports)

        vm.chooseBooster(Booster.WALL)
        vm.onTap(Pos(2, 2))
        vm.confirmBooster()
        vm.place(Pos(4, 4))
        advanceUntilIdle()
        assertEquals("the player's own wall is used first", 7L to UsedBoosters(owned = mapOf(Booster.WALL to 1)), spending.reports.last())
        val reported = spending.reports.size

        vm.chooseBooster(Booster.UNDO)
        vm.confirmBooster()
        assertEquals(
            "the undo gives the wall back and is used itself",
            7L to UsedBoosters(own = mapOf(Booster.UNDO to 1)),
            spending.reports.last(),
        )
        assertEquals(reported + 1, spending.reports.size)

        // Retry with the free Hint takes the player's own boosters again.
        vm.retryWith(Booster.HINT)
        assertEquals(2, vm.left(Booster.WALL))
        assertEquals(2, vm.left(Booster.HINT))
    }

    @Test
    fun `a level's own booster used is gone on every later attempt, and an undo gives it back`() = runTest(dispatcher) {
        val saved = Saved(Progress.NONE)
        var now = 100L
        val vm = GameViewModel(
            start = levelGame(level, threatHighlight = false, now = now, progress = saved.progress),
            nextGame = { levelGame(level, threatHighlight = false, now = ++now, progress = saved.progress) },
            computerFor = { TopRow() },
            campaign = saved,
            computerDispatcher = dispatcher,
            replyDelayMillis = 0,
            clock = { ++now },
            progress = { saved.progress },
        )
        vm.chooseBooster(Booster.WALL)
        vm.onTap(Pos(2, 2))
        vm.confirmBooster()
        vm.place(Pos(4, 4))
        advanceUntilIdle()
        assertEquals(mapOf(Booster.WALL to 1), saved.progress.spent(level))

        vm.chooseBooster(Booster.UNDO)
        vm.confirmBooster()
        assertEquals("the undo gives the wall back", 1, vm.left(Booster.WALL))
        assertEquals("and stays used", mapOf(Booster.UNDO to 1), saved.progress.spent(level))

        vm.chooseBooster(Booster.WALL)
        vm.onTap(Pos(2, 2))
        vm.confirmBooster()
        vm.place(Pos(4, 4))
        advanceUntilIdle()
        assertEquals(mapOf(Booster.WALL to 1, Booster.UNDO to 1), saved.progress.spent(level))

        // Retry and Restart start the next game from the saved progress: no Wall, no Undo.
        vm.playAgain()
        assertEquals(0, vm.left(Booster.WALL))
        assertEquals(0, vm.left(Booster.UNDO))
        assertEquals("an unused booster stays", 1, vm.left(Booster.BOMB))
        assertFalse("the tool bar's Wall cannot be chosen", Booster.WALL in vm.ui.value.usable)
        assertTrue(Booster.BOMB in vm.ui.value.usable)

        // The free Hint is for one attempt; the attempt after it has the level's Hint only.
        vm.retryWith(Booster.HINT)
        assertEquals(2, vm.left(Booster.HINT))
        assertEquals(0, vm.left(Booster.WALL))
        vm.playAgain()
        assertEquals(1, vm.left(Booster.HINT))

        // A game started again from the map takes the same counts.
        val fromMap = Game.from(levelGame(level, threatHighlight = false, now = ++now, progress = saved.progress).record).state
        assertEquals(0, fromMap.boostersLeft(Player.RED, Booster.WALL))
        assertEquals(1, fromMap.boostersLeft(Player.RED, Booster.BOMB))
    }

    /** Places the player's dots on the last free points until the level is over and its end saved. */
    private fun kotlinx.coroutines.test.TestScope.playOut(vm: GameViewModel) {
        while (!vm.ui.value.isOver) {
            vm.place(vm.state.legalMoves().last())
            advanceUntilIdle()
        }
        advanceUntilIdle()
    }

    @Test
    fun `an ad watched on a failed level's Result gives its booster to the next attempt only`() = runTest(dispatcher) {
        val ad = level.adBooster!!
        val last = LastEntry()
        val vm = game(journal = last)
        vm.adWatched()
        assertNull("not while the game goes on", vm.ui.value.adForRetry)

        playOut(vm)
        assertFalse(vm.ui.value.levelEnd!!.result.isPassed)
        vm.adWatched()
        vm.adWatched()
        assertEquals("one booster, however often it is called", ad, vm.ui.value.adForRetry)

        vm.playAgain()
        assertEquals(ad, vm.ui.value.adInPlay)
        assertNull(vm.ui.value.adForRetry)
        assertEquals("the level's own and the ad's", 2, vm.left(ad))
        assertEquals(ad, (last.entry?.record as LevelRecord).ad)

        // The attempt after it has the level's own only.
        playOut(vm)
        vm.playAgain()
        assertNull(vm.ui.value.adInPlay)
        assertEquals(1, vm.left(ad))

        // With the free Hint too, after the third failure.
        playOut(vm)
        vm.adWatched()
        vm.retryWith(Booster.HINT)
        assertEquals(ad, vm.ui.value.adInPlay)
        assertEquals(2, vm.left(ad))
        assertEquals(2, vm.left(Booster.HINT))
    }

    @Test
    fun `no booster from an ad in a practice game`() = runTest(dispatcher) {
        val practice = levelGame(level, threatHighlight = false, now = 0, practice = true)
        val vm = GameViewModel(
            start = practice,
            nextGame = { practice },
            computerFor = { TopRow() },
            computerDispatcher = dispatcher,
            replyDelayMillis = 0,
            clock = { 0L },
        )
        playOut(vm)
        vm.adWatched()
        assertNull(vm.ui.value.adForRetry)
    }

    @Test
    fun `a practice game neither spends nor misses the level's boosters`() = runTest(dispatcher) {
        val saved = Saved(Progress.NONE.spendOn(level, mapOf(Booster.WALL to 1)))
        val practice = levelGame(level, threatHighlight = false, now = 0, progress = saved.progress, practice = true)
        val vm = GameViewModel(
            start = practice,
            nextGame = { practice },
            computerFor = { TopRow() },
            campaign = saved,
            computerDispatcher = dispatcher,
            replyDelayMillis = 0,
            clock = { 0L },
            progress = { saved.progress },
        )
        assertEquals(1, vm.left(Booster.WALL))
        vm.chooseBooster(Booster.BOMB)
        vm.onTap(Pos(3, 4))
        vm.confirmBooster()
        advanceUntilIdle()
        assertEquals(0, vm.left(Booster.BOMB))
        assertEquals(mapOf(Booster.WALL to 1), saved.progress.spent(level))
    }
}
