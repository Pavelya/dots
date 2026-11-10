package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.ai.Hints
import dots.engine.GameSettings
import dots.engine.GameState
import dots.engine.MoveResult
import dots.engine.Player
import dots.engine.Pos
import io.github.pavelya.dots.game.Bubble
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.game.LastCapture
import io.github.pavelya.dots.game.Mood
import io.github.pavelya.dots.game.OpponentLook
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The game board in the moments of the canvas game frames (docs/design/canvas: `game-start`,
 * `game-threat`, `game-big-capture`), in free play, between two people, and in Pause. Each is
 * shown next to its frame in the PR. Reference images in app/src/test/screenshots; they are
 * recorded in CI (CLAUDE.md, "Recording screenshots").
 *
 * The boards are the canvas's own game of Level 7 (draw.py, `POSITIONS`), played by the rules.
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
        show { Board(ui(CanvasGame.opening, lastMove = null)) }
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

    @Test
    fun freePlay() {
        val start = GameSettings.POC.copy(seed = 1).newGame()
        val first = start.play(start.near(7, 7)).state
        val reply = first.near(8, 8)
        val ui = ui(MoveResult(first.play(reply).state, emptyList()), lastMove = reply).copy(canUndo = true)
        show { Board(ui, campaign = null) }
        capture("game_free_play")
    }

    @Test
    fun twoPlayersTurnBanner() {
        val start = GameSettings.POC.copy(seed = 1).newGame()
        var ui by mutableStateOf(GameUiState(start))
        compose.mainClock.autoAdvance = false
        show { Board(ui, campaign = null) }
        compose.mainClock.advanceTimeByFrame()
        val move = start.near(7, 7)
        compose.runOnUiThread { ui = GameUiState(start.play(move).state, lastMove = move) }
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
    private fun Board(ui: GameUiState, campaign: CampaignParts? = level7) {
        GameBoard(ui, human, campaign, onTap = {}, onUndo = {}, onPause = {})
    }

    /** Level 7's name and a stand-in for its mission note, whose content A-03 builds. The boosters are A-05's. */
    private val level7 = CampaignParts(
        title = "Level 7",
        mission = {
            Column {
                Text("Mission", style = MaterialTheme.typography.labelSmall)
                Text("Capture 8 dots", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        },
        boosters = {},
    )

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
