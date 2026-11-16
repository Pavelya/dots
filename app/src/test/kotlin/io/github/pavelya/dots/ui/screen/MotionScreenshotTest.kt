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
import dots.engine.MoveResult
import dots.engine.Player
import dots.engine.Pos
import dots.engine.areas
import io.github.pavelya.dots.data.LevelEnd
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.game.BoardMoment
import io.github.pavelya.dots.game.CapturedArea
import io.github.pavelya.dots.game.Ending
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.game.LastCapture
import io.github.pavelya.dots.game.OpponentLook
import io.github.pavelya.dots.game.ThreatMark
import io.github.pavelya.dots.ui.board.BoardGeometry
import io.github.pavelya.dots.ui.board.BoardInput
import io.github.pavelya.dots.ui.motion.EndOfGame
import io.github.pavelya.dots.ui.motion.rememberTimeline
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Key frames of the motion storyboard (docs/design/motion.md, D-08), drawn by stepping the
 * animation clock: each frame is named after its time, so it can be shown in the PR beside the
 * strip in docs/design/motion/ with the same time. The boards are the canvas's game of Level 7
 * ([CanvasGame]), as on the strips. Reference images in app/src/test/screenshots; they are
 * recorded in CI (CLAUDE.md, "Recording screenshots").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class MotionScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val human = Player.RED

    /**
     * Draws [content] as the phone would, with the clock stopped, then starts it: the moments that
     * [content] holds start on the next frame, which is their 0 ms.
     */
    private fun show(content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            DotsTheme {
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
            compose.onRoot().captureRoboImage("src/test/screenshots/motion_${name}_$time.png")
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
        return CampaignParts(parts.title, parts.mission, parts.boosters, missionBefore = before)
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
        show { Board(landing(threat, move, Pos(10, 9), Player.RED, edgeMarks = null), links = true) }
        frames("placing", 40, 80, 120, 200)
    }

    /**
     * The computer's move (strip-computer-move) and the threat it makes (strip-threat, 200 ms
     * later): Blue's (7, 7) lands with two ripples and its edge marks; the threat rings draw in,
     * pulse three times and are still at 5250 ms.
     */
    @Test
    fun computersMoveAndThreat() {
        show { Board(landing(beforeBlue, CanvasGame.threat, Pos(7, 7), Player.BLUE, edgeMarks = Pos(7, 7))) }
        frames("turn", 100, 325, 450, 900, 1250, 5250)
    }

    /** A capture of two dots (strip-capture): Red closes its ring at (4, 9). */
    @Test
    fun capture() {
        val before = ui(MoveResult(CanvasGame.beforeSmallCapture, emptyList()), lastMove = Pos(11, 2)).copy(edgeMarks = Pos(11, 2))
        show { Board(landing(before, CanvasGame.smallCapture, Pos(4, 9), Player.RED, edgeMarks = null)) }
        frames("capture", 300, 600, 750, 950, 1250)
    }

    /** The big capture of six dots (strip-big-capture): Red closes the big ring at (5, 7). */
    @Test
    fun bigCapture() {
        show { Board(landing(threat, CanvasGame.bigCapture, Pos(5, 7), Player.RED, edgeMarks = null)) }
        frames("big_capture", 400, 700, 900, 1100, 1400)
    }

    /**
     * The end of Level 7 after the big capture (strip-end), then the result note and its stars
     * (strip-stars): the tools leave, the three areas light up in the order they were captured,
     * the note slides up at 1100 ms and settles at 1400 ms, and the stars follow.
     */
    @Test
    fun endOfGameAndStars() {
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
        frames("end", 250, 450, 750, 1250, 1250 + 150, 1100 + 300 + 150, 1100 + 300 + 400, 1100 + 300 + 800, 1100 + 300 + 1100)
    }

    /** Dots running out (strip-running-out): the count at 6, 5, 4, 3, 2 and 1 dots left. */
    @Test
    fun dotsRunningOut() {
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
        val point = Pos(10, 7)
        val opening = GameUiState(state = CanvasGame.opening.state, vsComputer = true, preview = point)
        show { GameBoard(opening, human, CanvasGame.parts(opening.state), input = BoardInput(PlacingMethod.DRAG, {}, {}, {}), onUndo = {}, onPause = {}) }
        compose.onNodeWithTag(BOARD_TAG).performTouchInput {
            down(BoardGeometry(15, 15, width.toFloat(), height.toFloat()).center(point))
        }
        compose.mainClock.advanceTimeByFrame()
        frames("drag", 50, 100)
    }
}
