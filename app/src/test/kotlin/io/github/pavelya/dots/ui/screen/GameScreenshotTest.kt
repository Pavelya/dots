package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.ai.Hints
import dots.campaign.Campaign
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
import dots.log.GameLogEntry
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.game.BoosterAim
import io.github.pavelya.dots.game.FreeGame
import io.github.pavelya.dots.game.Bubble
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.game.LastCapture
import io.github.pavelya.dots.game.Mood
import io.github.pavelya.dots.game.OpponentLook
import io.github.pavelya.dots.game.gameViewModel
import io.github.pavelya.dots.ui.board.BoardGeometry
import io.github.pavelya.dots.ui.board.BoardInput
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.GameTheme
import io.github.pavelya.dots.ui.theme.Palette
import io.github.pavelya.dots.ui.theme.ThemeCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The game board in the moments of the canvas game frames (docs/design/canvas: `game-start`, with
 * the level opening, `game-threat` and `game-big-capture`, with the mission note), in free play,
 * between two people (with an open-ended game's Stop and Done, the frames of D-16), and in Pause. Each is
 * shown next to its frame in the PR. Reference images in app/src/test/screenshots; they are
 * recorded in CI (CLAUDE.md, "Recording screenshots").
 *
 * The boards are the canvas's own game of Level 7 (draw.py, `POSITIONS`), played by the rules,
 * with the canvas's boosters in the tool bar (draw.py, `BOOSTERS`). The booster previews (A-05) are
 * drawn on the threat position: Undo's as the canvas frame `game-undo`, Wall's and Bomb's as the
 * wireframes' "Booster aimed" in the same note, and both aimed by drag as the canvas frames
 * `game-wall-drag` and `game-bomb-drag` (D-18).
 *
 * Every test runs in each theme ([ThemeCase]): Notebook, and Star chart and Neon arcade, whose
 * images are shown next to their own canvas frames (`game-threat-star-chart` and so on). The
 * Boosters screen is not the game screen, so it stays Notebook's (decision 0028).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class GameScreenshotTest(private val case: ThemeCase) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes(): List<Array<Any>> = ThemeCase.parameters()
    }

    @get:Rule
    val compose = createComposeRule()

    private val human = Player.RED

    /** Draws [content] as the phone would: 24 dp of status bar at the top, 12 dp of gesture bar at the bottom. */
    private fun show(theme: GameTheme = case.theme, dark: Boolean = false, highContrast: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            DotsTheme(theme, dark, highContrast) {
                Surface { Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp)) { content() } }
            }
        }
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("src/test/screenshots/${case.name(name)}.png")

    private fun levelOpens(name: String, dark: Boolean) {
        show(dark = dark) {
            BoxWithConstraints {
                Board(ui(CanvasGame.opening, lastMove = null), campaign = CanvasGame.parts(CanvasGame.opening.state, note = false))
                LevelOpening(CanvasGame.level, Modifier.align(Alignment.TopCenter).padding(top = maxHeight * 0.26f, start = 36.dp, end = 36.dp))
            }
        }
        capture(name)
    }

    @Test
    fun levelOpens() = levelOpens("game_start", dark = false)

    @Test
    fun levelOpensDark() = levelOpens("game_start_dark", dark = true)

    @Test
    fun threat() {
        val threat = CanvasGame.threat
        assertEquals(OpponentLook(Mood.SMUG, Bubble.YOUR_MOVE), ui(threat).opponent)
        assertEquals(listOf(2, 2, 13), listOf(threat.state.score(Player.RED), threat.state.score(Player.BLUE), threat.state.dotsLeft(Player.RED)))
        show { Board(ui(threat)) }
        capture("game_threat")
    }

    @Test
    fun threatDark() {
        show(dark = true) { Board(ui(CanvasGame.threat)) }
        capture("game_threat_dark")
    }

    /** The threat with the links between dots turned on in Settings (A-20); the frames above show the default, off. */
    @Test
    fun threatWithLinks() {
        show { Board(ui(CanvasGame.threat), links = true) }
        capture("game_threat_links")
    }

    @Test
    fun threatWithLinksDark() {
        show(dark = true) { Board(ui(CanvasGame.threat), links = true) }
        capture("game_threat_links_dark")
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
    fun bigCaptureDark() {
        show(dark = true) { Board(ui(CanvasGame.bigCapture)) }
        capture("game_big_capture_dark")
    }

    /**
     * A board in [world], with its board touches beside the opponent (canvas frames `game-world-2`
     * to `-4`): the threat, as the frames show it, or the big capture, the most crowded board.
     */
    private fun inWorld(world: Int, big: Boolean, dark: Boolean) {
        val move = if (big) CanvasGame.bigCapture else CanvasGame.threat
        show(dark = dark) { Board(ui(move), campaign = CanvasGame.parts(move.state, world = world)) }
        capture("game_world_${world}_${if (big) "big_capture" else "threat"}${if (dark) "_dark" else ""}")
    }

    @Test
    fun threatInWorld2() = inWorld(2, big = false, dark = false)

    @Test
    fun threatInWorld2Dark() = inWorld(2, big = false, dark = true)

    @Test
    fun bigCaptureInWorld2() = inWorld(2, big = true, dark = false)

    @Test
    fun bigCaptureInWorld2Dark() = inWorld(2, big = true, dark = true)

    @Test
    fun threatInWorld3() = inWorld(3, big = false, dark = false)

    @Test
    fun threatInWorld3Dark() = inWorld(3, big = false, dark = true)

    @Test
    fun bigCaptureInWorld3() = inWorld(3, big = true, dark = false)

    @Test
    fun bigCaptureInWorld3Dark() = inWorld(3, big = true, dark = true)

    @Test
    fun threatInWorld4() = inWorld(4, big = false, dark = false)

    @Test
    fun threatInWorld4Dark() = inWorld(4, big = false, dark = true)

    @Test
    fun bigCaptureInWorld4() = inWorld(4, big = true, dark = false)

    @Test
    fun bigCaptureInWorld4Dark() = inWorld(4, big = true, dark = true)

    /**
     * World 1's boss, the Ink Bottle (1-15), as it is about to place the two dots of its special
     * turn: its attacking face above the board (docs/design/illustrations.md, "The World 1 boss").
     * Both sides have placed their dots along the top row, so nothing is captured.
     */
    private fun bossTurn(dark: Boolean) {
        val level = Campaign.level(1, 15)
        var state = level.newGame()
        while (!OpponentLook.bossTurn(state, human.opponent)) state = state.play(state.legalMoves().first()).state
        val ui = GameUiState(
            state = state,
            lastMove = state.lastPlaced(human),
            vsComputer = true,
            opponent = OpponentLook.of(state, human, null, humanInDanger = false),
            strength = level.strength,
            level = level,
        )
        assertEquals(OpponentLook(Mood.ATTACKING), ui.opponent)
        val parts = CampaignParts(
            title = "Level 15",
            mission = missionNote(level, level.status(state)),
            boosters = { BoosterBar(listOf(BoosterTool(Booster.UNDO, 1, usable = true)), Palette.current) {} },
            world = level.world,
        )
        show(dark = dark) { Board(ui, campaign = parts) }
        capture("game_boss_turn${if (dark) "_dark" else ""}")
    }

    /**
     * Reef (4-18) before the first move (A-15): open holes, two of them a gap inside the board,
     * blocked points with the wall's mark and pearls on the bonus points. The dark one in Notebook.
     */
    private fun reef(dark: Boolean) {
        if (dark) assumeTrue(case.isNotebook)
        val level = Campaign.level(4, 18)
        val state = level.newGame()
        val ui = GameUiState(
            state = state,
            lastMove = null,
            vsComputer = true,
            opponent = OpponentLook.of(state, human, null, humanInDanger = false),
            strength = level.strength,
            level = level,
        )
        val parts = CampaignParts(
            title = "Level 18",
            mission = missionNote(level, level.status(state)),
            boosters = { BoosterBar(listOf(BoosterTool(Booster.WALL, 1, usable = true), BoosterTool(Booster.UNDO, 1, usable = true)), Palette.current) {} },
            world = level.world,
        )
        show(dark = dark) { Board(ui, campaign = parts) }
        capture("game_reef${if (dark) "_dark" else ""}")
    }

    @Test
    fun reef() = reef(dark = false)

    @Test
    fun reefDark() = reef(dark = true)

    @Test
    fun bossTurn() = bossTurn(dark = false)

    @Test
    fun bossTurnDark() = bossTurn(dark = true)

    /** The big capture with high-contrast pieces turned on in Settings (pieces.md, "High contrast"). */
    @Test
    fun bigCaptureHighContrast() {
        show(highContrast = true) { Board(ui(CanvasGame.bigCapture)) }
        capture("game_big_capture_contrast")
    }

    @Test
    fun bigCaptureHighContrastDark() {
        show(dark = true, highContrast = true) { Board(ui(CanvasGame.bigCapture)) }
        capture("game_big_capture_contrast_dark")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun threatOnASmallPhone() {
        show { Board(ui(CanvasGame.threat)) }
        capture("game_threat_small_phone")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun threatOnASmallPhoneDark() {
        show(dark = true) { Board(ui(CanvasGame.threat)) }
        capture("game_threat_small_phone_dark")
    }

    /** Undo's preview (canvas frame `game-undo`): the turn it takes back pale in dashed rings, its threat marks gone. */
    @Test
    fun undoPreview() {
        show { Preview(undoUi()) }
        capture("game_undo")
    }

    @Test
    fun undoPreviewDark() {
        show(dark = true) { Preview(undoUi()) }
        capture("game_undo_dark")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun undoPreviewOnASmallPhone() {
        show { Preview(undoUi()) }
        capture("game_undo_small_phone")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun undoPreviewOnASmallPhoneDark() {
        show(dark = true) { Preview(undoUi()) }
        capture("game_undo_small_phone_dark")
    }

    /**
     * Placing the first dot by drag (canvas frame `game-drag`): the finger is down on (10, 7), the
     * preview sits there, and dashed guides run from its ring to the board's edges.
     */
    private fun drag(name: String, dark: Boolean, highContrast: Boolean = false) {
        val point = Pos(10, 7)
        val opening = ui(CanvasGame.opening, lastMove = null).copy(preview = point)
        show(dark = dark, highContrast = highContrast) { Board(opening, input = BoardInput(PlacingMethod.DRAG, {}, {}, {})) }
        compose.onNodeWithTag(BOARD_TAG).performTouchInput {
            down(BoardGeometry(15, 15, width.toFloat(), height.toFloat()).center(point))
        }
        capture(name)
    }

    @Test
    fun placingByDrag() = drag("game_drag", dark = false)

    @Test
    fun placingByDragDark() = drag("game_drag_dark", dark = true)

    /** Drag with high-contrast pieces: the preview outlined in ink, the guides wider, on a strip of paper. */
    @Test
    fun placingByDragHighContrast() = drag("game_drag_contrast", dark = false, highContrast = true)

    @Test
    fun placingByDragHighContrastDark() = drag("game_drag_contrast_dark", dark = true, highContrast = true)

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun placingByDragOnASmallPhone() = drag("game_drag_small_phone", dark = false)

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun placingByDragOnASmallPhoneDark() = drag("game_drag_small_phone_dark", dark = true)

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

    /**
     * Aiming a booster by drag (canvas frames `game-wall-drag`, `game-bomb-drag`): the finger is
     * down on [point], a target, so the booster's mark sits there and the guides run from it to the
     * board's edges in Pencil. The app draws no thumb.
     */
    private fun aimedByDrag(name: String, booster: Booster, point: Pos, dark: Boolean) {
        val state = CanvasGame.threat.state
        // As the view model finds them: every empty point for a wall, every point whose plus holds a blue dot for a bomb.
        val points = (0 until state.height).flatMap { y -> (0 until state.width).map { x -> Pos(x, y) } }
        val targets = if (booster == Booster.WALL) state.legalMoves() else points.filter { state.bombHits(it).isNotEmpty() }
        assertTrue("$point is a target", point in targets)
        val aim = when (booster) {
            Booster.WALL -> BoosterAim(Booster.WALL, point = point, targets = targets)
            else -> {
                val plus = listOf(point, Pos(point.x, point.y - 1), Pos(point.x - 1, point.y), Pos(point.x + 1, point.y), Pos(point.x, point.y + 1))
                BoosterAim(Booster.BOMB, point = point, targets = targets, plus = plus, gone = state.bombHits(point))
            }
        }
        show(dark = dark) { Preview(ui(CanvasGame.threat).copy(aim = aim), PlacingMethod.DRAG) }
        compose.onNodeWithTag(BOARD_TAG).performTouchInput {
            down(BoardGeometry(15, 15, width.toFloat(), height.toFloat()).center(point))
        }
        capture(name)
    }

    /** The Wall on the point that would close Blue's ring round Red's pair, as on the canvas. */
    private val wallPoint = Pos(9, 9)

    /** The Bomb on Red's dot in danger, as [bombAimed]: the app's engine has (8, 7), the canvas's point, already captured by Blue. */
    private val bombPoint = Pos(9, 8)

    @Test
    fun wallAimedByDrag() = aimedByDrag("game_wall_drag", Booster.WALL, wallPoint, dark = false)

    @Test
    fun wallAimedByDragDark() = aimedByDrag("game_wall_drag_dark", Booster.WALL, wallPoint, dark = true)

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun wallAimedByDragOnASmallPhone() = aimedByDrag("game_wall_drag_small_phone", Booster.WALL, wallPoint, dark = false)

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun wallAimedByDragOnASmallPhoneDark() = aimedByDrag("game_wall_drag_small_phone_dark", Booster.WALL, wallPoint, dark = true)

    @Test
    fun bombAimedByDrag() = aimedByDrag("game_bomb_drag", Booster.BOMB, bombPoint, dark = false)

    @Test
    fun bombAimedByDragDark() = aimedByDrag("game_bomb_drag_dark", Booster.BOMB, bombPoint, dark = true)

    /** After a Hint and a Double dot (the engine allows one booster a turn; the frame shows both marks): the circled point and the "×2" chip. */
    @Test
    fun hintAndDoubleDot() {
        show { Board(ui(CanvasGame.threat).copy(hint = Pos(10, 9), doubleDots = 2)) }
        capture("game_hint_double_dot")
    }

    /** The Boosters screen (canvas frame `boosters`) with the canvas's counts. */
    private fun boostersScreen(name: String, dark: Boolean) {
        assumeTrue("the Boosters screen is not the game screen", case.isNotebook)
        show(dark = dark) { BoostersNote(mapOf(Booster.WALL to 2, Booster.HINT to 1, Booster.DOUBLE_DOT to 1, Booster.UNDO to 1), onClose = {}) }
        capture(name)
    }

    @Test
    fun boostersScreen() = boostersScreen("boosters", dark = false)

    @Test
    fun boostersScreenDark() = boostersScreen("boosters_dark", dark = true)

    @Test
    fun freePlay() {
        val start = GameSettings.POC.copy(seed = 1).newGame()
        val first = start.play(start.near(7, 7)).state
        val reply = first.near(8, 8)
        val ui = ui(MoveResult(first.play(reply).state, emptyList()), lastMove = reply).copy(canUndo = true)
        show { Board(ui, campaign = null) }
        capture("game_free_play")
    }

    /**
     * The canvas's open-ended game (D-16, draw.py `FREE`) as the view model shows it, on the game
     * screen: [entry] played by the rules, Stop's note open when [asking]. The light frames run in
     * every theme; the dark ones in Notebook, whose frames the canvas draws.
     */
    private fun freeGame(name: String, entry: GameLogEntry, dark: Boolean, asking: Boolean = false) {
        if (dark) assumeTrue(case.isNotebook)
        val game = FreeGame.viewModel(entry)
        if (asking) game.askStop()
        show(dark = dark) { GameScreen(game, onToMap = {}, onRules = {}) }
        capture(name)
    }

    /** Canvas frame `game-free-open`: Stop at the left, Undo at the right, "no limit" for the dots left. */
    @Test
    fun freeOpen() = freeGame("game_free_open", FreeGame.open, dark = false)

    @Test
    fun freeOpenDark() = freeGame("game_free_open_dark", FreeGame.open, dark = true)

    /** Canvas frame `game-free-stop`: Stop's note, Red's three open dots circled. */
    @Test
    fun freeStop() = freeGame("game_free_stop", FreeGame.open, dark = false, asking = true)

    @Test
    fun freeStopDark() = freeGame("game_free_stop_dark", FreeGame.open, dark = true, asking = true)

    /** Canvas frame `game-computer-stops`: the computer stopped, smug; its two open dots circled; the player collects. */
    @Test
    fun computerStops() = freeGame("game_computer_stops", FreeGame.computerStopped, dark = false)

    @Test
    fun computerStopsDark() = freeGame("game_computer_stops_dark", FreeGame.computerStopped, dark = true)

    /** Canvas frame `game-two-done`: two players, Red stopped, Blue collects. */
    @Test
    fun twoDone() = freeGame("game_two_done", FreeGame.redStopped, dark = false)

    @Test
    fun twoDoneDark() = freeGame("game_two_done_dark", FreeGame.redStopped, dark = true)

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
    private fun Board(ui: GameUiState, campaign: CampaignParts? = CanvasGame.parts(ui.state), input: BoardInput = BoardInput.NONE, links: Boolean = false) {
        GameBoard(ui, human, campaign, input = input, onUndo = {}, onPause = {}, links = links)
    }

    /** The board with [ui]'s booster preview open over the player's line and the tool bar, as GameScreen lays it. */
    @Composable
    private fun Preview(ui: GameUiState, method: PlacingMethod = PlacingMethod.TWO_TAPS) {
        Box(Modifier.fillMaxSize()) {
            Board(ui, input = BoardInput(method, {}, {}, {}))
            BoosterNote(
                aim = ui.aim!!,
                method = method,
                style = Palette.current,
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

    private fun ui(move: MoveResult, lastMove: Pos? = move.state.lastPlaced(move.state.toMove.opponent)): GameUiState =
        CanvasGame.ui(move, human, lastMove)
}

/** The free point nearest to ([x], [y]). */
private fun GameState.near(x: Int, y: Int): Pos = legalMoves().minBy { (it.x - x) * (it.x - x) + (it.y - y) * (it.y - y) }

/**
 * The canvas's game of Level 7 (docs/design/canvas/draw.py, `POSITIONS`): red is the player, blue
 * the computer, six dots ahead. Played by the rules, Blue's last dot at (7, 7) also captures Red's
 * (8, 7), which the canvas leaves on the board; Red's (9, 8) is then the dot in danger.
 */
internal object CanvasGame {
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
    fun parts(state: GameState, note: Boolean = true, world: Int = level.world): CampaignParts {
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
            boosters = { BoosterBar(tools, Palette.current) {} },
            world = world,
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

    /** Blue has closed its ring at (11, 2); Red is to move, and closes its own at (4, 9) in [smallCapture]. */
    val beforeSmallCapture: GameState
        get() {
            val dots = (red - Pos(4, 9)).associateWith { Player.RED } + (blue - Pos(11, 2) - Pos(7, 7)).associateWith { Player.BLUE }
            val start = GameState.start(15, 15, dotsPerPlayer = 14, first = Player.BLUE, dots = dots)
            return start.play(Pos(11, 2)).state
        }

    /** Red closes its ring at (4, 9): two blue dots (the motion page's "Capture"). */
    val smallCapture: MoveResult get() = beforeSmallCapture.play(Pos(4, 9))

    /** Each side has closed a ring, and Blue has just played (7, 7). Red to move, 13 dots left. */
    val threat: MoveResult get() = smallCapture.state.play(Pos(7, 7))

    /** Red closes the big ring at (5, 7): six blue dots at once. */
    val bigCapture: MoveResult get() = threat.state.play(Pos(5, 7))

    /** What the screen shows against the computer after [move], with the threat highlight on, as the view model makes it. */
    fun ui(move: MoveResult, human: Player = Player.RED, lastMove: Pos? = move.state.lastPlaced(move.state.toMove.opponent)): GameUiState {
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
            strength = level.strength,
        )
    }

    /**
     * The [threat] position reached with one booster of each kind for Red, Red's last turn as on
     * the booster strip (docs/design/motion/strip-boosters.png): Red played (9, 8) and Blue
     * answered (7, 7). Red to move, 13 dots left.
     */
    val boosted: MoveResult
        get() {
            val dots = (red - Pos(4, 9) - Pos(9, 8)).associateWith { Player.RED } +
                (blue - Pos(11, 2) - Pos(12, 12) - Pos(7, 7)).associateWith { Player.BLUE }
            val boosters = mapOf(Player.RED to Booster.entries.associateWith { 1 })
            val start = GameState.start(15, 15, dotsPerPlayer = 15, first = Player.BLUE, dots = dots, boosters = boosters)
            return start.play(Pos(11, 2)).state.play(Pos(4, 9)).state.play(Pos(12, 12)).state.play(Pos(9, 8)).state.play(Pos(7, 7))
        }
}
