package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.ai.Hints
import dots.campaign.Level
import dots.campaign.LevelMap
import dots.campaign.Mission
import dots.campaign.StarMeasure
import dots.campaign.Stars
import dots.campaign.Strength
import dots.engine.Booster
import dots.engine.GameSettings
import dots.engine.GameState
import dots.engine.MoveResult
import dots.engine.Player
import dots.engine.Pos
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.game.BoosterAim
import io.github.pavelya.dots.game.Bubble
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.game.LastCapture
import io.github.pavelya.dots.game.Mood
import io.github.pavelya.dots.game.OpponentLook
import io.github.pavelya.dots.game.gameViewModel
import io.github.pavelya.dots.ui.board.BoardInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The game board in the moments of the canvas game frames (docs/design/canvas: `game-start`, with
 * the level opening, `game-threat` and `game-big-capture`, with the mission note), in free play,
 * between two people, and in Pause. Each is
 * shown next to its frame in the PR. Reference images in app/src/test/screenshots; they are
 * recorded in CI (CLAUDE.md, "Recording screenshots").
 *
 * The boards are the canvas's own game of Level 7 (draw.py, `POSITIONS`), played by the rules,
 * with the canvas's boosters in the tool bar (draw.py, `BOOSTERS`). The booster previews (A-05) are
 * drawn on the threat position: Undo's as the canvas frame `game-undo`, Wall's and Bomb's as the
 * wireframes' "Booster aimed" in the same note.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class GameScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val human = Player.RED

    /** Draws [content] as the phone would: 24 dp of status bar at the top, 12 dp of gesture bar at the bottom. */
    private fun show(content: @Composable () -> Unit) {
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface { Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp)) { content() } }
            }
        }
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")

    @Test
    fun levelOpens() {
        show {
            BoxWithConstraints {
                Board(ui(CanvasGame.opening, lastMove = null), campaign = CanvasGame.parts(CanvasGame.opening.state, note = false))
                LevelOpening(CanvasGame.level, Modifier.align(Alignment.TopCenter).padding(top = maxHeight * 0.26f, start = 36.dp, end = 36.dp))
            }
        }
        capture("game_start")
    }

    @Test
    fun threat() {
        val threat = CanvasGame.threat
        assertEquals(OpponentLook(Mood.SMUG, Bubble.YOUR_MOVE), ui(threat).opponent)
        assertEquals(listOf(2, 2, 13), listOf(threat.state.score(Player.RED), threat.state.score(Player.BLUE), threat.state.dotsLeft(Player.RED)))
        show { Board(ui(threat)) }
        capture("game_threat")
    }

    @Test
    fun bigCapture() {
        val big = CanvasGame.bigCapture
        assertEquals(OpponentLook(Mood.UPSET, Bubble.OUCH), ui(big).opponent)
        assertEquals(8, big.state.score(Player.RED))
        show { Board(ui(big)) }
        capture("game_big_capture")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun threatOnASmallPhone() {
        show { Board(ui(CanvasGame.threat)) }
        capture("game_threat_small_phone")
    }

    /** Undo's preview (canvas frame `game-undo`): the turn it takes back pale in dashed rings, its threat marks gone. */
    @Test
    fun undoPreview() {
        show { Preview(undoUi()) }
        capture("game_undo")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun undoPreviewOnASmallPhone() {
        show { Preview(undoUi()) }
        capture("game_undo_small_phone")
    }

    /** The Bomb aimed at Red's dot in danger: the plus of 5 points, and the three blue dots it would remove. */
    @Test
    fun bombAimed() {
        val state = CanvasGame.threat.state
        val center = Pos(9, 8)
        val hits = state.bombHits(center)
        assertEquals(3, hits.size)
        val plus = listOf(center, Pos(9, 7), Pos(8, 8), Pos(10, 8), Pos(9, 9))
        val targets = state.legalMoves().filter { state.bombHits(it).isNotEmpty() }
        val aim = BoosterAim(Booster.BOMB, point = center, targets = targets, plus = plus, gone = hits)
        show { Preview(ui(CanvasGame.threat).copy(aim = aim)) }
        capture("game_bomb_aimed")
    }

    /** The Wall before a point is chosen: every empty point it may block is marked, and the button says how to choose one. */
    @Test
    fun wallBeforeAPoint() {
        val state = CanvasGame.threat.state
        show { Preview(ui(CanvasGame.threat).copy(aim = BoosterAim(Booster.WALL, targets = state.legalMoves()))) }
        capture("game_wall_choosing")
    }

    /** After a Hint and a Double dot (the engine allows one booster a turn; the frame shows both marks): the circled point and the "×2" chip. */
    @Test
    fun hintAndDoubleDot() {
        show { Board(ui(CanvasGame.threat).copy(hint = Pos(10, 9), doubleDots = 2)) }
        capture("game_hint_double_dot")
    }

    /** The Boosters screen (canvas frame `boosters`) with the canvas's counts. */
    @Test
    fun boostersScreen() {
        show { BoostersNote(mapOf(Booster.WALL to 2, Booster.HINT to 1, Booster.DOUBLE_DOT to 1, Booster.UNDO to 1), onClose = {}) }
        capture("boosters")
    }

    @Test
    fun freePlay() {
        val start = GameSettings.POC.copy(seed = 1).newGame()
        val first = start.play(start.near(7, 7)).state
        val reply = first.near(8, 8)
        val ui = ui(MoveResult(first.play(reply).state, emptyList()), lastMove = reply).copy(canUndo = true)
        show { Board(ui, campaign = null) }
        capture("game_free_play")
    }

    /** Free play, open-ended (decision 0025): Undo and Stop in the tool bar, no dots left. */
    @Test
    fun freePlayOpenEnded() {
        val start = GameSettings.POC.copy(seed = 1, dotsPerPlayer = null).newGame()
        val first = start.play(start.near(7, 7)).state
        val reply = first.near(8, 8)
        val ui = ui(MoveResult(first.play(reply).state, emptyList()), lastMove = reply)
            .copy(canUndo = true, openEnded = true, canStop = true)
        show { Board(ui, campaign = null) }
        capture("game_free_play_open_ended")
    }

    /** Two players, open-ended: Red has stopped, and Blue collects, then taps Done. */
    @Test
    fun twoPlayersCollecting() {
        val start = GameSettings.POC.copy(seed = 1, dotsPerPlayer = null).newGame()
        val first = start.play(start.near(7, 7)).state
        val second = first.play(first.near(8, 8)).state
        val stopped = second.stop()
        val ui = GameUiState(state = stopped, lastMove = first.near(8, 8), openEnded = true, collecting = true)
        show { Board(ui, campaign = null) }
        capture("game_two_players_collecting")
    }

    @Test
    fun twoPlayersTurnBanner() {
        val game = gameViewModel(newSettings = { GameSettings.POC.copy(seed = 1) })
        val move = game.ui.value.state.near(7, 7)
        compose.mainClock.autoAdvance = false
        show { GameScreen(game, onToMap = {}, onRules = {}) }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnUiThread { repeat(2) { game.onTap(move) } } // choose, then place
        compose.mainClock.advanceTimeBy(100) // the banner shows a frame after the turn changes
        capture("game_two_players_banner")
    }

    @Test
    fun pause() {
        val start = GameSettings.POC.copy(seed = 1).newGame()
        show {
            Box {
                Board(ui(MoveResult(start, emptyList()), lastMove = null), campaign = null)
                PauseNote(sound = true, onSound = {}, onResume = {}, onRestart = {}, onToMap = {}, onRules = {})
            }
        }
        capture("game_pause")
    }

    @Composable
    private fun Board(ui: GameUiState, campaign: CampaignParts? = CanvasGame.parts(ui.state)) {
        GameBoard(ui, human, campaign, input = BoardInput.NONE, onUndo = {}, onPause = {})
    }

    /** The board with [ui]'s booster preview open over the player's line and the tool bar, as GameScreen lays it. */
    @Composable
    private fun Preview(ui: GameUiState) {
        Box(Modifier.fillMaxSize()) {
            Board(ui)
            BoosterNote(
                aim = ui.aim!!,
                method = PlacingMethod.TWO_TAPS,
                style = rememberGameStyle(),
                onCancel = {},
                onConfirm = {},
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
            )
        }
    }

    /** Undo's preview on the threat position, as the canvas draws it: Red's (9, 8) and Blue's answer (7, 7) go. */
    private fun undoUi(): GameUiState {
        val gone = listOf(Pos(9, 8), Pos(7, 7))
        val threat = ui(CanvasGame.threat)
        return threat.copy(
            aim = BoosterAim(Booster.UNDO, gone = gone),
            dangers = threat.dangers.filter { danger -> danger.group.none { it in gone } },
            opponent = OpponentLook(Mood.SMUG, Bubble.TAKE_IT_BACK),
        )
    }

    /** What the screen shows against the computer after [move], with the threat highlight on, as the view model makes it. */
    private fun ui(move: MoveResult, lastMove: Pos? = move.state.lastPlaced(move.state.toMove.opponent)): GameUiState {
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
        )
    }
}

/** The free point nearest to ([x], [y]). */
private fun GameState.near(x: Int, y: Int): Pos = legalMoves().minBy { (it.x - x) * (it.x - x) + (it.y - y) * (it.y - y) }

/**
 * The canvas's game of Level 7 (docs/design/canvas/draw.py, `POSITIONS`): red is the player, blue
 * the computer, six dots ahead. Played by the rules, Blue's last dot at (7, 7) also captures Red's
 * (8, 7), which the canvas leaves on the board; Red's (9, 8) is then the dot in danger.
 */
private object CanvasGame {
    private fun points(vararg xy: Int) = xy.toList().chunked(2).map { Pos(it[0], it[1]) }

    private val openingRed = points(6, 7, 8, 7)
    private val openingBlue = points(7, 6, 7, 8, 4, 4, 10, 4, 4, 10, 10, 10, 2, 7, 12, 7)

    /** The canvas's Level 7, Siege: capture 8 dots, against a computer six dots ahead. */
    val level: Level by lazy {
        val rows = List(15) { y ->
            String(CharArray(15) { x -> if (Pos(x, y) in openingRed) 'R' else if (Pos(x, y) in openingBlue) 'B' else '.' })
        }
        Level(1, 7, "Siege", Strength.HARD, 30, LevelMap(rows), Mission.Capture(8, 30), stars = Stars(StarMeasure.MOVES, 8, 14))
    }

    /**
     * Level 7's name, its mission note as it stands in [state] (while the level opens there is no
     * [note]), and the canvas's boosters: Wall 2, Hint 1, Bomb 0, Double dot 1, Undo 1, which
     * waits until the player has placed a dot.
     */
    fun parts(state: GameState, note: Boolean = true): CampaignParts {
        val status = level.status(state)
        val waiting = state.dotsPlaced(Player.RED) == 0
        val tools = listOf(
            BoosterTool(Booster.WALL, 2, usable = true),
            BoosterTool(Booster.HINT, 1, usable = true),
            BoosterTool(Booster.BOMB, 0, usable = false),
            BoosterTool(Booster.DOUBLE_DOT, 1, usable = true),
            BoosterTool(Booster.UNDO, 1, usable = !waiting, waiting = waiting),
        )
        return CampaignParts(
            title = "Level 7",
            mission = if (note) missionNote(level, status) else null,
            boosters = { BoosterBar(tools, rememberGameStyle()) {} },
        )
    }

    /** The level opens: Red's two dots and Blue's eight, 30 dots each to place. */
    val opening: MoveResult
        get() {
            val dots = openingRed.associateWith { Player.RED } + openingBlue.associateWith { Player.BLUE }
            return MoveResult(GameState.start(15, 15, dotsPerPlayer = 30, dots = dots), emptyList())
        }

    private val redRing = points(4, 9, 5, 10, 5, 11, 4, 12, 3, 11, 3, 10)
    private val blueRing = points(11, 2, 12, 3, 11, 4, 10, 3)
    private val bigRing = points(4, 3, 5, 3, 6, 4, 6, 5, 6, 6, 5, 7, 4, 7, 3, 6, 3, 5, 3, 4)
    private val blueAttack = points(8, 6, 9, 7, 10, 8, 8, 8, 7, 7)
    private val cluster = points(5, 4, 4, 5, 5, 5, 4, 6, 5, 6)
    private val red = openingRed + redRing + points(11, 3, 9, 8) + (bigRing - Pos(5, 7))
    private val blue = openingBlue + blueRing + blueAttack + cluster + points(4, 11, 11, 11, 12, 12)

    /** Each side has closed a ring, and Blue has just played (7, 7). Red to move, 13 dots left. */
    val threat: MoveResult
        get() {
            val closingBlue = Pos(11, 2)
            val closingRed = Pos(4, 9)
            val last = Pos(7, 7)
            val dots = (red - closingRed).associateWith { Player.RED } + (blue - closingBlue - last).associateWith { Player.BLUE }
            val start = GameState.start(15, 15, dotsPerPlayer = 14, first = Player.BLUE, dots = dots)
            return start.play(closingBlue).state.play(closingRed).state.play(last)
        }

    /** Red closes the big ring at (5, 7): six blue dots at once. */
    val bigCapture: MoveResult get() = threat.state.play(Pos(5, 7))
}
