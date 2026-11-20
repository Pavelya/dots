package io.github.pavelya.dots.analytics

import dots.ai.Ai
import dots.campaign.Campaign
import dots.campaign.Level
import dots.campaign.LevelMap
import dots.campaign.LevelResult
import dots.campaign.Mission
import dots.campaign.MissionStatus
import dots.campaign.Reason
import dots.campaign.Solver
import dots.campaign.Strength
import dots.campaign.Outcome as MissionOutcome
import dots.daily.Daily
import dots.daily.DailyAttempt
import dots.daily.DailyProgress
import dots.daily.DailyPuzzle
import dots.daily.DailyRecord
import dots.daily.Day
import dots.engine.Action
import dots.engine.Booster
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import dots.log.GameLogEntry
import dots.log.Outcome
import io.github.pavelya.dots.data.DailyEnd
import io.github.pavelya.dots.data.DailyJournal
import io.github.pavelya.dots.data.GameJournal
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.dailyGame
import io.github.pavelya.dots.game.freeGame
import io.github.pavelya.dots.game.levelGame
import io.github.pavelya.dots.data.AppSettings
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
import org.junit.Assert.assertTrue
import org.junit.Test

/** Keeps every event, as Google Analytics would get them. */
private class Recorded : Analytics {
    val events = mutableListOf<AnalyticsEvent>()
    override fun log(event: AnalyticsEvent) {
        events += event
    }

    fun named(name: String) = events.filter { it.name == name }
}

/** Forgets every entry. */
private object NoJournal : GameJournal {
    override fun start(entry: GameLogEntry) {}
    override fun save(entry: GameLogEntry) {}
    override fun finish(entry: GameLogEntry) {}
}

/** Keeps the daily progress in memory. */
private class Days(private val today: Day) : DailyJournal {
    var progress = DailyProgress.NONE
    override suspend fun finished(puzzle: DailyPuzzle, startedAt: Long, actions: List<Action>, passed: Boolean): DailyEnd {
        val attempt = DailyAttempt(puzzle.day, startedAt, passed, actions)
        progress = progress.finished(attempt)
        return DailyEnd.of(puzzle, attempt, progress, today)
    }
}

/** Plays the first free point, in the top row. */
private class TopRow : Ai {
    override fun choose(state: GameState): Action = Action.Place(state.legalMoves().first())
}

/** The computer's actions of [line] (every second one, from the second), one after the other. */
private class Replies(line: List<Action>) : Ai {
    private val replies = line.filterIndexed { i, _ -> i % 2 == 1 }.iterator()
    override fun choose(state: GameState): Action = replies.next()
}

/** The events the game reports (Q-06, decision 0033): which, when, with what, and never twice. */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    init {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val empty = LevelMap(List(7) { "......." })

    /** One dot each and a Wall: a draw, so the level fails. */
    private val drawn = Level(1, 7, "One Dot Each", Strength.BEGINNER, 1, empty, Mission.Win, boosters = mapOf(Booster.WALL to 1))

    /** Survive the computer's one dot: passed as soon as it is placed, or cannot capture. */
    private val survived = Level(1, 8, "One Dot Survives", Strength.BEGINNER, 1, empty, Mission.Survive(1))

    private fun levelVm(
        level: Level,
        analytics: Analytics,
        start: GameLogEntry = levelGame(level, threatHighlight = false, now = 0),
        journal: GameJournal = NoJournal,
    ) = GameViewModel(
        start = start,
        nextGame = { levelGame(level, threatHighlight = false, now = 0) },
        computerFor = { TopRow() },
        journal = journal,
        computerDispatcher = dispatcher,
        replyDelayMillis = 0,
        clock = { 0L },
        analytics = analytics,
    )

    private fun GameViewModel.place(pos: Pos) {
        onTap(pos)
        onTap(pos)
    }

    @Test
    fun `every event fits Google Analytics' rules and holds nothing but game facts`() {
        val level = Campaign.levels[9]
        val puzzle = Daily.puzzle(Day(2027, 1, 5))!!
        val events = listOf(
            Events.levelStart(level),
            Events.levelEnd(level, LevelResult(MissionStatus(MissionOutcome.PASSED, Reason.GAME_OVER, 4, 0, null), stars = 3, starScore = 4)),
            Events.boosterUsed(level, Booster.DOUBLE_DOT),
            Events.dailyPuzzleDone(puzzle, solved = true),
        )
        assertEquals(listOf("level_start", "level_end", "booster_used", "daily_puzzle_done"), events.map { it.name })
        val allowed = setOf("level_name", "level_number", "world", "success", "stars", "booster", "puzzle_number")
        events.forEach { assertTrue("${it.name}: ${it.params.keys}", allowed.containsAll(it.params.keys)) }
        assertEquals(mapOf("level_name" to level.code, "level_number" to 10L, "world" to level.world.toLong()), events[0].params)
        assertEquals(1L, events[1].params["success"])
        assertEquals(3L, events[1].params["stars"])
        assertEquals("double-dot", events[2].params["booster"])
        assertEquals(mapOf("puzzle_number" to puzzle.number.toLong(), "success" to 1L), events[3].params)
    }

    @Test
    fun `level numbers count from the first level across worlds`() {
        assertEquals(1L, Events.levelStart(Campaign.levels.first()).params["level_number"])
        val last = Campaign.levels.last()
        assertEquals(Campaign.levels.size.toLong(), Events.levelStart(last).params["level_number"])
    }

    @Test
    fun `a new game of a level reports its start, a practice game or a free game does not`() {
        val recorded = Recorded()
        val journal = ReportingJournal(NoJournal) { recorded }
        journal.start(levelGame(Campaign.levels.first(), threatHighlight = false, now = 0))
        journal.start(levelGame(Campaign.levels.first(), threatHighlight = false, now = 0, practice = true))
        journal.start(freeGame(AppSettings(), strength = null, now = 0))
        assertEquals(listOf(Events.levelStart(Campaign.levels.first())), recorded.events)
    }

    @Test
    fun `Play again reports a new start through the journal`() = runTest(dispatcher) {
        val recorded = Recorded()
        val vm = levelVm(drawn, recorded, journal = ReportingJournal(NoJournal) { recorded })
        vm.playAgain()
        assertEquals(listOf(Events.levelStart(drawn)), recorded.named("level_start"))
    }

    @Test
    fun `a failed level reports its end with success 0 and no stars`() = runTest(dispatcher) {
        val recorded = Recorded()
        val vm = levelVm(drawn, recorded)
        vm.place(Pos(3, 3))
        advanceUntilIdle()
        assertTrue(vm.ui.value.isOver)
        val end = recorded.named("level_end").single()
        assertEquals(0L, end.params["success"])
        assertEquals(0L, end.params["stars"])
        assertEquals("1-7", end.params["level_name"])
    }

    @Test
    fun `a passed level reports its end with success 1 and its stars`() = runTest(dispatcher) {
        val recorded = Recorded()
        val vm = levelVm(survived, recorded)
        vm.place(Pos(3, 3))
        advanceUntilIdle()
        assertTrue(vm.ui.value.isOver)
        val end = recorded.named("level_end").single()
        assertEquals(1L, end.params["success"])
        assertEquals(1L, end.params["stars"])
    }

    @Test
    fun `each booster used is reported with its level, and only once`() = runTest(dispatcher) {
        val recorded = Recorded()
        val vm = levelVm(drawn, recorded)
        vm.chooseBooster(Booster.WALL)
        vm.onTap(Pos(2, 2))
        vm.confirmBooster()
        advanceUntilIdle()
        val used = recorded.named("booster_used").single()
        assertEquals("wall", used.params["booster"])
        assertEquals("1-7", used.params["level_name"])
    }

    @Test
    fun `a practice game reports nothing`() = runTest(dispatcher) {
        val recorded = Recorded()
        val vm = levelVm(drawn, recorded, start = levelGame(drawn, threatHighlight = false, now = 0, practice = true))
        vm.chooseBooster(Booster.WALL)
        vm.onTap(Pos(2, 2))
        vm.confirmBooster()
        advanceUntilIdle()
        vm.place(vm.ui.value.state.legalMoves().last())
        advanceUntilIdle()
        assertTrue(vm.ui.value.isOver)
        assertTrue(recorded.events.toString(), recorded.events.isEmpty())
    }

    private val day = Day(2027, 1, 5)
    private val puzzle = Daily.puzzle(day)!!
    private val line: List<Action> = Solver().solve(puzzle.level).line.map { it.action }

    private fun dailyVm(analytics: Analytics, start: GameLogEntry = dailyGame(puzzle, threatHighlight = false, now = 0)) = GameViewModel(
        start = start,
        nextGame = { dailyGame(puzzle, threatHighlight = false, now = 0) },
        computerFor = { Replies(line) },
        computerDispatcher = dispatcher,
        replyDelayMillis = 0,
        clock = { 0L },
        daily = Days(day),
        analytics = analytics,
    )

    @Test
    fun `a solved daily puzzle is reported once, and View result reports nothing`() = runTest(dispatcher) {
        val recorded = Recorded()
        val vm = dailyVm(recorded)
        for (action in line.filterIndexed { i, _ -> i % 2 == 0 }) {
            vm.place((action as Action.Place).pos)
            advanceUntilIdle()
        }
        advanceUntilIdle()
        assertTrue(vm.ui.value.isOver)
        assertEquals(listOf(Events.dailyPuzzleDone(puzzle, solved = true)), recorded.events)

        val again = Recorded()
        val record = DailyRecord(puzzle, line)
        val end = record.replay()
        val solved = GameLogEntry.start(record, puzzle.strength.text, threatHighlight = false, now = 0)
            .copy(record = record, outcome = Outcome.FINISHED, red = end.score(Player.RED), blue = end.score(Player.BLUE))
        val view = dailyVm(again, start = solved)
        advanceUntilIdle()
        assertTrue(view.ui.value.showsResult)
        assertTrue(again.events.toString(), again.events.isEmpty())
    }
}
