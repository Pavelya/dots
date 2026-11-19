package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.ai.Hints
import dots.campaign.Campaign
import dots.campaign.CampaignMap
import dots.campaign.Level
import dots.campaign.Progress
import dots.engine.Action
import dots.engine.Booster
import dots.engine.MoveResult
import dots.engine.Player
import dots.engine.Pos
import dots.engine.areas
import io.github.pavelya.dots.data.LevelEnd
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.game.BoardMoment
import io.github.pavelya.dots.game.BoosterMoment
import io.github.pavelya.dots.game.Bubble
import io.github.pavelya.dots.game.CapturedArea
import io.github.pavelya.dots.game.Ending
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.game.LastCapture
import io.github.pavelya.dots.game.Mood
import io.github.pavelya.dots.game.OpponentLook
import io.github.pavelya.dots.game.ThreatMark
import io.github.pavelya.dots.game.UndoneTurn
import io.github.pavelya.dots.ui.board.BoardGeometry
import io.github.pavelya.dots.ui.board.BoardInput
import io.github.pavelya.dots.ui.motion.EndOfGame
import io.github.pavelya.dots.ui.motion.rememberTimeline
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.Palette
import io.github.pavelya.dots.ui.theme.ThemeCase
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Key frames of the motion storyboard (docs/design/motion.md, D-08), drawn by stepping the
 * animation clock: each frame is named after its time, so it can be shown in the PR beside the
 * strip in docs/design/motion/ with the same time. The boards are the canvas's game of Level 7
 * ([CanvasGame]), as on the strips. Reference images in app/src/test/screenshots; they are
 * recorded in CI (CLAUDE.md, "Recording screenshots").
 *
 * The moments Star chart and Neon arcade change (motion.md, "Star chart" and "Neon arcade") run in
 * those themes too ([ThemeCase]), at the times of each theme's strip; their frames are named
 * `motion_<moment>_<theme>_<time>`. Every other moment plays as Notebook's in every theme.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class MotionScreenshotTest(private val case: ThemeCase) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes(): List<Array<Any>> = ThemeCase.parameters()
    }

    @get:Rule
    val compose = createComposeRule()

    /**
     * The times at which this theme's frames of a moment are taken: Notebook's, or a theme's own
     * strip's. A moment a theme leaves as Notebook's (no times) is not run in it.
     */
    private fun times(notebook: LongArray, starChart: LongArray? = null, neonArcade: LongArray? = null): LongArray {
        val times = when (case) {
            ThemeCase.STAR_CHART -> starChart
            ThemeCase.NEON_ARCADE -> neonArcade
            else -> notebook
        }
        assumeTrue("the moment plays as Notebook's in ${case.key}", times != null)
        return times!!
    }

    /** Runs only in Notebook: a moment the other themes play as Notebook's, or a screen they don't change. */
    private fun notebookOnly() = times(longArrayOf())

    private val human = Player.RED

    /**
     * Draws [content] as the phone would, with the clock stopped, then starts it: the moments that
     * [content] holds start on the next frame, which is their 0 ms.
     */
    private fun show(content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            DotsTheme(case.theme) {
                Surface { Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp)) { content() } }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
    }

    /** Captures a frame at each of [times] ms, named `motion_[name]_<time>`. */
    private fun frames(name: String, vararg times: Long) {
        var now = 0L
        for (time in times) {
            compose.mainClock.advanceTimeBy(time - now)
            now = time
            val theme = if (case.isNotebook) "" else "_${case.key}"
            compose.onRoot().captureRoboImage("src/test/screenshots/motion_${name}${theme}_$time.png")
        }
    }

    /** What the screen shows against the computer after [move], with the threat highlight on, as the view model makes it. */
    private fun ui(move: MoveResult, lastMove: Pos?): GameUiState {
        val state = move.state
        val dangers = Player.entries.flatMap { Hints.dangers(state, it) }
        val capture = move.captures.takeIf { it.isNotEmpty() }?.let { LastCapture(it.first().by, it.sumOf { c -> c.points }) }
        val inDanger = dangers.any { it.dotsNeeded <= 1 && state.ownerAt(it.group.first()) == human }
        return GameUiState(
            state = state,
            lastMove = lastMove,
            vsComputer = true,
            dangers = dangers,
            opponent = OpponentLook.of(state, human, capture, inDanger),
            mission = CanvasGame.level.status(state),
        )
    }

    /** The screen after [move], with the moment of [dot] played by [by]; the screen showed [before] until then. */
    private fun landing(before: GameUiState, move: MoveResult, dot: Pos, by: Player, edgeMarks: Pos?): GameUiState {
        val after = ui(move, lastMove = dot)
        val known = before.dangers.map(ThreatMark::of).toSet()
        val moment = BoardMoment(
            id = 1,
            dot = dot,
            by = by,
            announced = by != human,
            captures = move.captures,
            pinholeBefore = before.lastMove,
            marksGone = before.edgeMarks?.takeIf { it != edgeMarks },
            freshThreats = after.dangers.map(ThreatMark::of).filter { it !in known }.toSet(),
            missionBefore = before.mission,
            opponentBefore = before.opponent,
        )
        return after.copy(moment = moment, edgeMarks = edgeMarks)
    }

    /** Level 7's parts as [ui] shows them, with the mission note as it was before the moment. */
    private fun parts(ui: GameUiState): CampaignParts {
        val parts = CanvasGame.parts(ui.state)
        val before = ui.moment?.missionBefore?.let { missionNote(CanvasGame.level, it) }
        return CampaignParts(parts.title, parts.mission, parts.boosters, missionBefore = before, world = parts.world)
    }

    @Composable
    private fun Board(ui: GameUiState, links: Boolean = false, input: BoardInput = BoardInput.NONE) {
        GameBoard(ui, human, parts(ui), input = input, onUndo = {}, onPause = {}, links = links)
    }

    /** Before the threat: Blue to move, Red's last dot (4, 9). */
    private val beforeBlue: GameUiState get() = ui(CanvasGame.smallCapture, lastMove = Pos(4, 9))

    /** The threat position, with the edge marks of Blue's (7, 7) on. */
    private val threat: GameUiState get() = ui(CanvasGame.threat, lastMove = Pos(7, 7)).copy(edgeMarks = Pos(7, 7))

    /** Placing a dot (strip-placing): Red's (10, 9) grows, settles, and its link grows to (9, 8). */
    @Test
    fun placing() {
        val move = CanvasGame.threat.state.play(Pos(10, 9))
        // Neon arcade's strip (strip-placing-neon-arcade): the disc lights up as it settles.
        val at = times(longArrayOf(40, 80, 120, 200), neonArcade = longArrayOf(80, 120, 160, 200))
        show { Board(landing(threat, move, Pos(10, 9), Player.RED, edgeMarks = null), links = true) }
        frames("placing", *at)
    }

    /**
     * The computer's move (strip-computer-move) and the threat it makes (strip-threat, 200 ms
     * later): Blue's (7, 7) lands with two ripples and its edge marks; the threat rings draw in,
     * pulse three times and are still at 5250 ms.
     */
    @Test
    fun computersMoveAndThreat() {
        val at = times(longArrayOf(100, 325, 450, 900, 1250, 5250), neonArcade = longArrayOf(100, 160, 325, 900))
        show { Board(landing(beforeBlue, CanvasGame.threat, Pos(7, 7), Player.BLUE, edgeMarks = Pos(7, 7))) }
        frames("turn", *at)
    }

    /** A capture of two dots (strip-capture): Red closes its ring at (4, 9). */
    @Test
    fun capture() {
        val before = ui(MoveResult(CanvasGame.beforeSmallCapture, emptyList()), lastMove = Pos(11, 2)).copy(edgeMarks = Pos(11, 2))
        val at = times(
            longArrayOf(300, 600, 750, 950, 1250),
            starChart = longArrayOf(200, 300, 400, 550, 650, 1250),
            neonArcade = longArrayOf(360, 560, 600, 640, 720, 1250),
        )
        show { Board(landing(before, CanvasGame.smallCapture, Pos(4, 9), Player.RED, edgeMarks = null)) }
        frames("capture", *at)
    }

    /** The big capture of six dots (strip-big-capture): Red closes the big ring at (5, 7). */
    @Test
    fun bigCapture() {
        val at = times(
            longArrayOf(400, 700, 900, 1100, 1400),
            starChart = longArrayOf(250, 400, 550, 650, 750, 1400),
            neonArcade = longArrayOf(400, 660, 700, 740, 820, 1400),
        )
        show { Board(landing(threat, CanvasGame.bigCapture, Pos(5, 7), Player.RED, edgeMarks = null)) }
        frames("big_capture", *at)
    }

    /**
     * The end of Level 7 after the big capture (strip-end), then the result note and its stars
     * (strip-stars): the tools leave, the three areas light up in the order they were captured,
     * the note slides up at 1100 ms and settles at 1400 ms, and the stars follow.
     */
    @Test
    fun endOfGameAndStars() {
        // In the other themes the areas light up in their own way: the tubes thicken with their glow and core.
        val notebook = longArrayOf(250, 450, 750, 1250, 1250 + 150, 1100 + 300 + 150, 1100 + 300 + 400, 1100 + 300 + 800, 1100 + 300 + 1100)
        val ends = times(notebook, starChart = longArrayOf(450, 750, 1250, 2500), neonArcade = longArrayOf(450, 750, 1250, 2500))
        val state = CanvasGame.bigCapture.state
        val level = CanvasGame.level
        val areas = state.areas()
        // Blue's ring was closed first, then Red's small one, then the big one.
        val order = listOf(
            areas.first { it.by == Player.BLUE },
            areas.first { it.by == Player.RED && Pos(4, 10) in it.region },
            areas.first { it.by == Player.RED && Pos(4, 4) in it.region },
        ).map { CapturedArea(it.region.first(), it.by) }
        val playing = ui(CanvasGame.bigCapture, lastMove = Pos(5, 7))
        val over = playing.copy(
            isOver = true,
            captureOrder = order,
            opponentInPlay = playing.opponent,
            opponent = OpponentLook.of(state, human, null, false, Ending.WON),
            level = level,
            levelEnd = LevelEnd(level.result(state), CampaignMap.Opened.NOTHING, failed = 0, next = null),
        )
        val times = EndOfGame(order.size)
        show {
            val ending = rememberTimeline(1, times.length)
            val ended by remember { derivedStateOf { !ending.running } }
            Box {
                GameBoard(over, human, CanvasGame.parts(state), input = BoardInput.NONE, onUndo = {}, onPause = {}, ending = ending)
                if (ended) ResultNote(over, Names(true, human), onNext = {}, onAgain = {}, onAgainWithHint = {}, onMap = {})
            }
        }
        frames("end", *ends)
    }

    /** Dots running out (strip-running-out): the count at 6, 5, 4, 3, 2 and 1 dots left. */
    @Test
    fun dotsRunningOut() {
        notebookOnly()
        compose.setContent {
            DotsTheme {
                Surface {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (left in 6 downTo 1) DotsLeft(left, Player.RED, Palette.current)
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/motion_running_out.png")
    }

    /**
     * The level unlock after Level 7, passed for the first time with two stars (strip-unlock): the
     * stars fly in, the stop fills, the path inks in, Level 8 is circled, and its card comes up.
     */
    @Test
    fun levelUnlock() {
        notebookOnly()
        val level7 = Campaign.level(1, 7)
        val level8 = Campaign.level(1, 8)
        val before = Campaign.levels.takeWhile { it != level7 }
        val map = Campaign.map(Progress(before.associate { it.code to 3 } + (level7.code to 2)))
        val opened = mutableListOf<Level>()
        val actions = MapActions(onLevel = { opened += it }, onDaily = {}, onPlay = {}, onRules = {}, onStats = {}, onShop = {}, onSettings = {})
        show { MapScreen(map, actions, unlock = Unlock(level7, starsBefore = 0, stars = 2, next = level8, worldOpens = false)) }
        frames("unlock", 300, 650, 1000, 1400)
        compose.mainClock.advanceTimeBy(500)
        assertEquals(listOf(level8), opened)
    }

    /** Drag's guides (strip-drag): drawn out from the ring in 100 ms as the finger goes down. */
    @Test
    fun dragGuides() {
        notebookOnly()
        val point = Pos(10, 7)
        val opening = GameUiState(state = CanvasGame.opening.state, vsComputer = true, preview = point)
        show { GameBoard(opening, human, CanvasGame.parts(opening.state), input = BoardInput(PlacingMethod.DRAG, {}, {}, {}), onUndo = {}, onPause = {}) }
        compose.onNodeWithTag(BOARD_TAG).performTouchInput {
            down(BoardGeometry(15, 15, width.toFloat(), height.toFloat()).center(point))
        }
        compose.mainClock.advanceTimeByFrame()
        frames("drag", 50, 100)
    }

    /** The threat position with one booster of each kind, as the booster strip starts: Blue's (7, 7) last, its edge marks on. */
    private val boosted: GameUiState get() = ui(CanvasGame.boosted, lastMove = Pos(7, 7)).copy(edgeMarks = Pos(7, 7))

    /**
     * The screen just after Red uses [action] in the [boosted] position, as the view model makes it,
     * with the booster's effect to play.
     */
    private fun used(action: Action.UseBooster, givenBack: Pair<Booster, Pos>? = null): GameUiState {
        val before = CanvasGame.boosted.state
        val result = before.apply(action)
        val state = result.state
        val undone = if (action == Action.Undo) {
            val gone = before.boardPoints().filter { before.ownerAt(it) != null && state.ownerAt(it) == null }
            UndoneTurn(
                before = before,
                answer = gone.filter { before.ownerAt(it) == Player.BLUE },
                own = gone.filter { before.ownerAt(it) == Player.RED },
                givenBack = givenBack?.first,
                givenBackAt = givenBack?.second,
                opponentBefore = OpponentLook(Mood.SMUG, Bubble.TAKE_IT_BACK),
            )
        } else {
            null
        }
        val moment = BoosterMoment(1, action.booster, Player.RED, result.booster?.pos, result.booster?.removed.orEmpty(), undone)
        val lastMove = if (undone != null) state.lastPlaced(Player.BLUE) else Pos(7, 7)
        return ui(result, lastMove).copy(
            booster = moment,
            edgeMarks = if (undone != null) null else Pos(7, 7),
            hint = (action as? Action.Hint)?.pos,
            doubleDots = if (action == Action.DoubleDot) 2 else null,
        )
    }

    /** Level 7's parts with the tool bar as [ui]'s position counts the boosters. */
    private fun boosterParts(ui: GameUiState): CampaignParts {
        val tools = Booster.entries.map { val left = ui.state.boostersLeft(Player.RED, it); BoosterTool(it, left, usable = left > 0) }
        return CampaignParts(
            title = "Level 7",
            mission = missionNote(CanvasGame.level, CanvasGame.level.status(ui.state)),
            boosters = { BoosterBar(tools, Palette.current) {} },
            world = CanvasGame.level.world,
        )
    }

    @Composable
    private fun BoostedBoard(ui: GameUiState) {
        GameBoard(ui, human, boosterParts(ui), input = BoardInput.NONE, onUndo = {}, onPause = {})
    }

    /** Wall (strip-boosters, first row): the square drawn round (9, 9) by 200 ms, hatched by 400 ms. */
    @Test
    fun wall() {
        notebookOnly()
        show { BoostedBoard(used(Action.Wall(Pos(9, 9)))) }
        frames("wall", 0, 100, 200, 300, 400)
    }

    /** Hint (second row): (9, 9) circled twice, 250 ms per loop. */
    @Test
    fun hint() {
        notebookOnly()
        show { BoostedBoard(used(Action.Hint(Pos(9, 9)))) }
        frames("hint", 0, 125, 250, 375, 500)
    }

    /** Hint, then the player's dot lands on (9, 9): the circles shrink away in 150 ms. */
    @Test
    fun hintThenDot() {
        notebookOnly()
        val hinted = used(Action.Hint(Pos(9, 9))).copy(booster = null)
        val move = hinted.state.play(Pos(9, 9))
        val landed = landing(hinted, move, Pos(9, 9), Player.RED, edgeMarks = null)
        show { BoostedBoard(landed.copy(moment = landed.moment?.copy(hintBefore = Pos(9, 9)))) }
        frames("hint_dot", 0, 50)
    }

    /** Bomb (third row): it drops on (8, 7), and at 150 ms the four blue dots round it swell, shrink away, and the specks fly. */
    @Test
    fun bomb() {
        // Neon arcade: the hits' glow swells and shrinks with them.
        val at = times(longArrayOf(0, 150, 220, 300, 450), neonArcade = longArrayOf(150, 220, 300))
        show { BoostedBoard(used(Action.Bomb(Pos(8, 7)))) }
        frames("bomb", *at)
    }

    /** Double dot (fourth row, 0 to 300 ms): the "×2" chip grows in beside "You". */
    @Test
    fun doubleDot() {
        notebookOnly()
        show { BoostedBoard(used(Action.DoubleDot)) }
        frames("double_dot", 0, 150, 300)
    }

    /** Double dot, the first dot (the row's 700 ms): (9, 9) sparks, and the chip turns to ×1 and swells. */
    @Test
    fun doubleDotFirst() {
        notebookOnly()
        val chosen = used(Action.DoubleDot).copy(booster = null)
        val move = chosen.state.play(Pos(9, 9))
        val landed = landing(chosen, move, Pos(9, 9), Player.RED, edgeMarks = null)
        show { BoostedBoard(landed.copy(moment = landed.moment?.copy(doubleDots = 2), doubleDots = 1)) }
        frames("double_dot_first", 100)
    }

    /** Double dot, the second dot (the row's 1200 and 1350 ms): (10, 9) sparks, the chip shrinks away, and the turn passes. */
    @Test
    fun doubleDotSecond() {
        notebookOnly()
        val chosen = used(Action.DoubleDot).copy(booster = null)
        val first = chosen.state.play(Pos(9, 9))
        val between = ui(first, lastMove = Pos(9, 9)).copy(doubleDots = 1)
        val move = first.state.play(Pos(10, 9))
        val landed = landing(between, move, Pos(10, 9), Player.RED, edgeMarks = null)
        show { BoostedBoard(landed.copy(moment = landed.moment?.copy(doubleDots = 1))) }
        frames("double_dot_second", 0, 150)
    }

    /**
     * Undo (fifth row): Blue's answer (7, 7) rubbed out from 0 ms, Red's (9, 8) from 200 ms, the
     * strokes gone at 600 ms, with 14 dots left and the pinhole on Blue's (12, 12).
     */
    @Test
    fun undo() {
        notebookOnly()
        show { BoostedBoard(used(Action.Undo)) }
        frames("undo", 0, 125, 300, 450, 600)
    }

    /**
     * Undo of a turn in which Red had dropped a bomb on (8, 7), then placed (9, 9), and Blue
     * answered (13, 13): the four blue dots the bomb took grow back in from 200 to 450 ms, with
     * Red's dot rubbed out, and the bomb flies back to its button.
     */
    @Test
    fun undoGivesBackABomb() {
        notebookOnly()
        val after = CanvasGame.boosted.state
        val before = after.apply(Action.Bomb(Pos(8, 7))).state.play(Pos(9, 9)).state.play(Pos(13, 13)).state
        val undone = UndoneTurn(
            before = before,
            answer = listOf(Pos(13, 13)),
            own = listOf(Pos(9, 9)),
            givenBack = Booster.BOMB,
            givenBackAt = Pos(8, 7),
            opponentBefore = OpponentLook(Mood.SMUG, Bubble.TAKE_IT_BACK),
        )
        val shown = ui(CanvasGame.boosted, lastMove = Pos(7, 7)).copy(booster = BoosterMoment(1, Booster.UNDO, Player.RED, undone = undone))
        show { BoostedBoard(shown) }
        frames("undo_bomb", 200, 300, 450)
    }

    /**
     * Undo of a turn in which Red had put up a wall at (9, 9): the wall stays until 200 ms, flies
     * back to its button by 400 ms, and the button's count goes up and swells.
     */
    @Test
    fun undoGivesBackAWall() {
        notebookOnly()
        show { BoostedBoard(used(Action.Undo, givenBack = Booster.WALL to Pos(9, 9))) }
        frames("undo_wall", 100, 300, 500)
    }
}

/** Every point of the board, row by row. */
private fun dots.engine.GameState.boardPoints(): List<Pos> = (0 until height).flatMap { y -> (0 until width).map { x -> Pos(x, y) } }
