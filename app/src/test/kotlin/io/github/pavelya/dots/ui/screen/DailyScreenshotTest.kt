package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.ai.Ai
import dots.campaign.Campaign
import dots.campaign.Mission
import dots.campaign.Solver
import dots.daily.Daily
import dots.daily.DailyAttempt
import dots.daily.DailyProgress
import dots.daily.DailyPuzzle
import dots.daily.Day
import dots.daily.Streak
import dots.engine.Action
import dots.engine.GameState
import dots.engine.Pos
import io.github.pavelya.dots.data.DailyEnd
import io.github.pavelya.dots.data.DailyJournal
import io.github.pavelya.dots.data.SampleProgress
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.dailyGame
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.ThemeCase
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The daily puzzle (A-07): the map's note with the streak (canvas frame `map`), Today's puzzle over
 * the map (the wireframes' Today's puzzle: play, resume, solved), the daily board, and the daily
 * Result (the wireframes' Daily result), shown next to their frames in the PR. Reference images in
 * app/src/test/screenshots; they are recorded in CI (CLAUDE.md, "Recording screenshots").
 *
 * The daily board and its Result are the game screen, so they run in each theme ([ThemeCase]).
 * The map and the notes over it stay Notebook's (decision 0028).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class DailyScreenshotTest(private val case: ThemeCase) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes(): List<Array<Any>> = ThemeCase.parameters()
    }

    @get:Rule
    val compose = createComposeRule()

    private val hour = 3_600_000L
    private val minute = 60_000L

    /** Tuesday 5 January 2027's puzzle. */
    private val puzzle: DailyPuzzle = Daily.puzzle(Day(2027, 1, 5))!!

    /**
     * Solved from Tuesday 29 December to Monday 4 January but for Saturday 2 January, which last
     * week's freeze covered: a streak of 6 days on Tuesday 5 January, with this week's freeze left.
     */
    private val solvedBefore: Set<Day> = (0..6).map { Day(2026, 12, 29) + it }.filter { it != Day(2027, 1, 2) }.toSet()
    private val streak: Streak = Streak.of(solvedBefore, puzzle.day)

    private val actions = MapActions(onLevel = {}, onDaily = {}, onPlay = {}, onRules = {}, onStats = {}, onShop = {}, onSettings = {})

    /** Draws [content] as the phone would: 24 dp of status bar at the top, 12 dp of gesture bar at the bottom. */
    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            DotsTheme(case.theme, dark = dark) {
                Surface { Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp)) { content() } }
            }
        }
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("src/test/screenshots/${case.name(name)}.png")

    private fun waitFor(text: String, substring: Boolean = false) =
        compose.waitUntil(timeoutMillis = 10_000) { compose.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty() }

    /** Today's puzzle over the dimmed map, as the dialog draws it. */
    private fun note(action: DailyAction, dark: Boolean = false, shown: DailyPuzzle? = puzzle, today: Day = puzzle.day) {
        assumeTrue("Today's puzzle is a note over the map", case.isNotebook)
        val map = Campaign.map(SampleProgress.midWorld)
        show(dark) {
            Box {
                MapScreen(map, actions, streak = streak.days)
                DailyPuzzleNote(
                    puzzle = shown,
                    today = today,
                    action = action,
                    streak = streak,
                    toNextPuzzle = { 14 * hour + 22 * minute },
                    onAction = {},
                    onClose = {},
                )
            }
        }
    }

    @Test
    fun `the streak of the screenshots`() {
        assertEquals(6, streak.days)
        assertEquals(true, streak.freezeLeft)
    }

    @Test
    fun `the map's note shows the streak`() {
        assumeTrue("the map is not the game screen", case.isNotebook)
        show { MapScreen(Campaign.map(SampleProgress.midWorld), actions, streak = 6) }
        compose.onNodeWithText("Streak 6").assertIsDisplayed()
        capture("map_daily_streak")
    }

    @Test
    fun `Today's puzzle, to play`() {
        note(DailyAction.PLAY)
        compose.onNodeWithText("Play").assertIsDisplayed()
        compose.onNodeWithContentDescription("14:22 to the next puzzle").assertIsDisplayed()
        compose.onNodeWithContentDescription("6 days streak · 1 freeze left").assertIsDisplayed()
        capture("daily_puzzle")
    }

    @Test
    fun `Today's puzzle, dark`() {
        note(DailyAction.PLAY, dark = true)
        capture("daily_puzzle_dark")
    }

    @Test
    fun `Today's puzzle, to resume`() {
        note(DailyAction.RESUME)
        compose.onNodeWithText("Resume").assertIsDisplayed()
        capture("daily_puzzle_resume")
    }

    @Test
    fun `Today's puzzle, solved`() {
        note(DailyAction.VIEW_RESULT)
        compose.onNodeWithText("View result").assertIsDisplayed()
        capture("daily_puzzle_solved")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun `Today's puzzle on a small phone`() {
        note(DailyAction.PLAY)
        capture("daily_puzzle_small_phone")
    }

    @Test
    fun `before the first puzzle`() {
        note(DailyAction.PLAY, shown = null, today = Day(2026, 11, 15))
        waitFor("The first puzzle comes on", substring = true)
        capture("daily_puzzle_none_yet")
    }

    /** The computer's actions of [line], one after the other. */
    private class Replies(line: List<Action>) : Ai {
        private val replies = line.filterIndexed { i, _ -> i % 2 == 1 }.iterator()
        override fun choose(state: GameState): Action = replies.next()
    }

    /** Plays the first free point. */
    private val firstFreePoint = object : Ai {
        override fun choose(state: GameState): Action = Action.Place(state.legalMoves().first())
    }

    /** Keeps the attempts in memory, on [puzzle]'s day, after the [streak] so far. */
    private val days = object : DailyJournal {
        var progress = DailyProgress(solvedBefore)

        override suspend fun finished(puzzle: DailyPuzzle, startedAt: Long, actions: List<Action>, passed: Boolean): DailyEnd {
            val attempt = DailyAttempt(puzzle.day, startedAt, passed, actions)
            progress = progress.finished(attempt)
            return DailyEnd.of(puzzle, attempt, progress, puzzle.day)
        }
    }

    private fun game(of: DailyPuzzle, computer: Ai) = GameViewModel(
        start = dailyGame(of, threatHighlight = false, now = 0),
        nextGame = { dailyGame(of, threatHighlight = false, now = 1) },
        computerFor = { computer },
        computerDispatcher = Dispatchers.Unconfined,
        replyDelayMillis = 0,
        daily = days,
    )

    /** Plays [point] (choose, then place), and waits for the computer's reply if the game goes on. */
    private fun play(vm: GameViewModel, point: Pos) {
        compose.runOnUiThread { repeat(2) { vm.onTap(point) } }
        compose.waitUntil(timeoutMillis = 5_000) { !vm.ui.value.thinking }
    }

    private val line: List<Action> = Solver().solve(puzzle.level).line.map { it.action }

    private fun solved(name: String, dark: Boolean = false) {
        val vm = game(puzzle, Replies(line))
        var shared: String? = null
        show(dark) { GameScreen(vm, onToMap = {}, onRules = {}, onShare = { shared = it }, toNextPuzzle = { 14 * hour + 22 * minute }) }
        for (move in line.filterIndexed { i, _ -> i % 2 == 0 }) play(vm, (move as Action.Place).pos)
        waitFor("Solved!")
        capture(name)
        compose.onNodeWithText("Share").performClick()
        assertEquals(vm.ui.value.dailyEnd?.share, shared)
    }

    @Test
    fun `the daily board has the mission and no tool bar`() {
        val vm = game(puzzle, Replies(line))
        // The opening shows the puzzle's number and mission big for a moment: the clock waits.
        compose.mainClock.autoAdvance = false
        show { GameScreen(vm, onToMap = {}, onRules = {}) }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("Same puzzle for everyone today.").assertIsDisplayed()
        capture("daily_board_opening")
        compose.mainClock.autoAdvance = true
        // The first move ends it; the mission sits in the corner note, and there is no tool bar.
        play(vm, (line.first() as Action.Place).pos)
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasContentDescription("Mission", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        capture("daily_board")
    }

    @Test
    fun `daily result, solved`() = solved("daily_result")

    @Test
    fun `daily result, solved, dark`() = solved("daily_result_dark", dark = true)

    @Test
    fun `daily result, failed`() {
        // A capture puzzle in which the player only takes the first free point: nothing is captured.
        val capture = Daily.year(2027).first { it.mission is Mission.Capture }
        val vm = game(capture, firstFreePoint)
        show { GameScreen(vm, onToMap = {}, onRules = {}, toNextPuzzle = { 14 * hour + 22 * minute }) }
        while (!vm.ui.value.isOver) play(vm, vm.ui.value.state.legalMoves().first())
        compose.waitUntil(timeoutMillis = 10_000) { vm.ui.value.showsResult }
        compose.onNodeWithText("Retry").assertIsDisplayed()
        capture("daily_result_failed")
    }
}
