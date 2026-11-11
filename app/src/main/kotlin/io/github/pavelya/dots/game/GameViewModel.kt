package io.github.pavelya.dots.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dots.ai.Ai
import dots.ai.Danger
import dots.ai.Hints
import dots.campaign.CampaignMap
import dots.campaign.Level
import dots.campaign.LevelRecord
import dots.campaign.MissionStatus
import dots.campaign.Tutorial
import dots.campaign.Outcome as MissionOutcome
import dots.engine.Action
import dots.engine.Booster
import dots.engine.Game
import dots.engine.GameState
import dots.engine.MoveResult
import dots.engine.Player
import dots.engine.Pos
import dots.log.GameLogEntry
import dots.log.Outcome
import io.github.pavelya.dots.data.CampaignJournal
import io.github.pavelya.dots.data.GameJournal
import io.github.pavelya.dots.data.LevelEnd
import io.github.pavelya.dots.data.PlacingMethod
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Everything the game screen draws, as one immutable value. */
data class GameUiState(
    val state: GameState,
    val preview: Pos? = null,
    val lastMove: Pos? = null,
    /** True while the computer is choosing its move; taps are ignored meanwhile. */
    val thinking: Boolean = false,
    val canUndo: Boolean = false,
    val vsComputer: Boolean = false,
    /** Groups close to capture, for both players; empty unless the threat highlight is on. */
    val dangers: List<Danger> = emptyList(),
    /** The computer's face and speech bubble; null between two people. */
    val opponent: OpponentLook? = null,
    /** True once the game is over: no move is left or, in a campaign level, its mission is decided. */
    val isOver: Boolean = state.isOver,
    /** The campaign level being played; null in free play and between two people. */
    val level: Level? = null,
    /** Where the level's mission stands; null outside the campaign. */
    val mission: MissionStatus? = null,
    /** A finished level's end, once its stars are saved; Result opens then. Null until then, and outside the campaign. */
    val levelEnd: LevelEnd? = null,
    /** True before the game's first action: a level then opens by showing its mission. */
    val fresh: Boolean = false,
    /** Counts the games played on this screen, so that each new one opens afresh. */
    val round: Int = 0,
    /**
     * The tutorial's guided step (A-06): what to do now and the point to tap. Null outside the
     * tutorial's levels, once the player skipped it, and at the end. On the computer's turn the
     * last step stays, without a point.
     */
    val guide: Tutorial.Step? = null,
    /**
     * True for the whole of a tutorial level whose steps are not skipped: the board is laid out for
     * the step note, whether or not a step shows at the moment.
     */
    val guided: Boolean = false,
    /** True for a practice game, such as a replay of the tutorial: its end changes no campaign progress. */
    val practice: Boolean = false,
) {
    /** True when Result can open: the game is over and, in a level, its end is saved. */
    val showsResult: Boolean get() = isOver && (level == null || levelEnd != null)
}

/**
 * Holds one game and turns screen events into game actions. The screen sends events ([onTap],
 * [aim], [release], [undo], [playAgain]) and draws [ui]; all rules are the engine's.
 *
 * A game is described by a [GameLogEntry]: a new one, or the saved one when the app was closed
 * mid-game (its moves are replayed). Every change is reported to the [journal], which saves it
 * and logs the game once it is over. A campaign level's game ends as soon as its mission is
 * decided; its stars and failures then go to the [campaign], and Result opens once they are saved.
 *
 * Against the computer the human is red and moves first. The computer thinks on
 * [computerDispatcher], never on the main thread, and its thinking is cancelled by undo, a new
 * game, or leaving the screen. It answers after at least [replyDelayMillis], so the player sees
 * their own dot land before the reply.
 *
 * @param start the game to play: new, or resumed
 * @param nextGame the game "play again" starts (a fresh seed every time; the same level again)
 * @param computerFor the computer opponent of a game, or null for two players on one phone
 * @param clock the time now in milliseconds, for the log
 * @param placing how dots are placed now, for the log: the game log records the method with the
 *   touches and dots it took, so that `cli placing-report` can compare methods (A-13)
 */
class GameViewModel(
    start: GameLogEntry,
    private val nextGame: () -> GameLogEntry,
    private val computerFor: (GameLogEntry) -> Ai?,
    private val journal: GameJournal? = null,
    private val campaign: CampaignJournal? = null,
    private val computerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val replyDelayMillis: Long = 400,
    private val clock: () -> Long = System::currentTimeMillis,
    private val placing: () -> PlacingMethod = { PlacingMethod.TWO_TAPS },
) : ViewModel() {
    val human: Player = Player.RED

    private var entry = start
    private var game = Game.from(start.record)
    private var computer: Ai? = computerFor(start)
    private var thinking: Job? = null
    private var levelEnd: LevelEnd? = null
    private var round = 0

    /** When the game last changed (or was opened), to add up the time spent playing. */
    private var lastActive = clock()

    /** True once the player skipped the tutorial's guided steps on this screen. */
    private var skipped = false

    /** The guided step shown last, kept without its point while the computer moves. */
    private var shownGuide: Tutorial.Step? = null

    /** How often the human took a move back in this game. */
    val undoCount: Int get() = entry.undos

    /** The campaign level being played, or null. */
    private val level: Level? get() = (entry.record as? LevelRecord)?.level

    private val _ui = MutableStateFlow(snapshot(lastMove = lastPlaced()))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    init {
        // A resumed level may have been closed between its last move and the saving of its stars.
        if (game.isOver) levelOver()
        letComputerMove() // a resumed game may have been closed while the computer was to move
    }

    /**
     * A tap on board point [point], or off the board when null, placing by two taps ([Placement]):
     * the first shows the preview, a second on the same point places the dot.
     */
    fun onTap(point: Pos?) {
        if (!humanToMove()) return
        touched()
        val result = Placement.onTap(_ui.value.preview, point) { game.state.isLegal(it) }
        val placed = result.placed
        if (placed == null) {
            showPreview(result.preview)
        } else {
            put(placed)
        }
    }

    /**
     * A finger is down over [point], or off the board when null: the preview follows it, on a
     * playable point only. Placing methods that place on release send this while the finger moves;
     * it is not a touch of its own.
     */
    fun aim(point: Pos?) {
        if (!humanToMove()) return
        val preview = point?.takeIf { game.state.isLegal(it) }
        if (preview != _ui.value.preview) showPreview(preview)
    }

    /**
     * A touch that ends asking for a dot at [point]: it is placed if the point is playable. Null,
     * or an unplayable point, places nothing and clears the preview (the finger was lifted off the
     * board, or too early).
     */
    fun release(point: Pos?) {
        if (!humanToMove()) return
        touched()
        if (point == null || !game.state.isLegal(point)) {
            showPreview(null)
        } else {
            put(point)
        }
    }

    private fun showPreview(preview: Pos?) {
        _ui.value = _ui.value.copy(preview = preview, guide = guideFor(game.state, preview))
    }

    /** True when a touch on the board may place a dot: not while the computer thinks or after the end. */
    private fun humanToMove(): Boolean =
        !_ui.value.thinking && !game.isOver && (computer == null || game.state.toMove == human)

    /** Counts a touch on the board for the log; it is saved with the next move. */
    private fun touched() {
        entry = entry.copy(touches = entry.touches + 1)
    }

    private fun put(point: Pos) {
        val move = game.apply(Action.Place(point))
        entry = entry.copy(placed = entry.placed + 1)
        changed()
        _ui.value = snapshot(lastMove = point, captured = move)
        letComputerMove()
    }

    /** Takes back the human's last move and the computer's reply. */
    fun undo() {
        if (computer == null || game.isOver) return
        thinking?.cancel()
        if (game.undo(human)) {
            entry = entry.copy(undos = entry.undos + 1)
            changed()
        }
        _ui.value = snapshot(lastMove = lastPlaced())
    }

    /**
     * Starts a new game: free play with fresh settings (a new seed, so new crosses), or the same
     * level again, as practice if this game is. An unfinished game is logged as abandoned.
     */
    fun playAgain() {
        val level = level
        begin(if (entry.practice && level != null) levelGame(level, entry.threatHighlight, clock(), practice = true) else nextGame())
    }

    /** In a practice game of the tutorial, plays its next level, also as practice; else does nothing. */
    fun practiceNext() {
        val next = level?.takeIf { entry.practice }?.let(Tutorial::next) ?: return
        begin(levelGame(next, entry.threatHighlight, clock(), practice = true))
    }

    /** Hides the tutorial's guided steps on this screen, also in the levels played after this one. */
    fun skipGuides() {
        skipped = true
        shownGuide = null
        _ui.value = _ui.value.copy(guide = null, guided = false)
    }

    /**
     * Plays the same level again with [booster] given on top of the level's own: the help Result
     * offers after the third failure in a row. Outside the campaign it is [playAgain].
     */
    fun retryWith(booster: Booster) {
        val level = level ?: return playAgain()
        begin(levelGame(level, entry.threatHighlight, clock(), gift = mapOf(booster to 1), practice = entry.practice))
    }

    private fun begin(next: GameLogEntry) {
        thinking?.cancel()
        entry = next
        game = Game.from(entry.record)
        computer = computerFor(entry)
        levelEnd = null
        shownGuide = null
        round++
        lastActive = clock()
        journal?.start(entry)
        _ui.value = snapshot(lastMove = null)
    }

    private fun letComputerMove() {
        val ai = computer ?: return
        val position = game.state
        if (game.isOver || position.toMove == human) return
        _ui.value = _ui.value.copy(thinking = true)
        thinking = viewModelScope.launch {
            delay(replyDelayMillis)
            val action = withContext(computerDispatcher) {
                val context = coroutineContext
                ai.choose(position) { !context.isActive }
            }
            ensureActive()
            if (game.state !== position) return@launch // the position changed meanwhile (undo)
            val result = game.apply(action)
            changed()
            _ui.value = snapshot(lastMove = (action as? Action.Place)?.pos ?: lastPlaced(), captured = result)
            letComputerMove() // the engine skips the human's turn if they have no dots left
        }
    }

    /** Brings [entry] up to date with the game and hands it to the journal. */
    private fun changed() {
        val now = clock()
        val state = game.state
        val over = game.isOver
        entry = entry.copy(
            updatedAt = now,
            playMillis = entry.playMillis + (now - lastActive).coerceIn(0, GameLogEntry.IDLE_CAP_MILLIS),
            outcome = if (over) Outcome.FINISHED else Outcome.PLAYING,
            red = state.score(Player.RED),
            blue = state.score(Player.BLUE),
            record = game.record,
            placing = placing().key,
        )
        lastActive = now
        if (over) {
            journal?.finish(entry)
            levelOver()
        } else {
            journal?.save(entry)
        }
    }

    /**
     * Saves a finished level's stars and failures, then lets Result open. A practice game saves
     * nothing; in the tutorial, Next then plays its next level.
     */
    private fun levelOver() {
        val level = level ?: return
        val result = level.result(game.state)
        val thisRound = round
        val practice = entry.practice
        viewModelScope.launch {
            val end = when {
                practice -> LevelEnd(result, CampaignMap.Opened.NOTHING, failed = 0, next = Tutorial.next(level))
                else -> campaign?.finished(level, result) ?: LevelEnd(result, CampaignMap.Opened.NOTHING, failed = 0, next = null)
            }
            if (round != thisRound) return@launch // another game has started meanwhile
            levelEnd = end
            _ui.value = _ui.value.copy(levelEnd = end)
        }
    }

    /** The guided step for [state] with a dot shown at [preview]; see [GameUiState.guide]. */
    private fun guideFor(state: GameState, preview: Pos?): Tutorial.Step? {
        val level = level
        if (level == null || skipped || game.isOver || Tutorial.lesson(level) == null) {
            shownGuide = null
            return null
        }
        shownGuide = Tutorial.step(level, state, preview) ?: shownGuide?.copy(point = null)
        return shownGuide
    }

    private fun lastPlaced(): Pos? = game.record.actions.lastOrNull { it is Action.Place }?.let { (it as Action.Place).pos }

    /** What the screen draws now; [captured] is the move just played, if any, for the computer's face. */
    private fun snapshot(lastMove: Pos?, captured: MoveResult? = null): GameUiState {
        val state = game.state
        val level = level
        val mission = level?.status(state)
        val over = game.isOver
        val dangers = if (entry.threatHighlight && !over) Player.entries.flatMap { Hints.dangers(state, it) } else emptyList()
        val opponent = computer?.let {
            val lastCapture = captured?.captures?.takeIf { it.isNotEmpty() }?.let { captures ->
                LastCapture(captures.first().by, captures.sumOf { c -> c.points })
            }
            val inDanger = if (entry.threatHighlight) {
                dangers.any { it.dotsNeeded <= 1 && state.ownerAt(it.group.first()) == human }
            } else {
                !over && Hints.dangers(state, human, maxDots = 1).isNotEmpty()
            }
            val ending = when {
                !over -> null
                mission == null -> OpponentLook.endingOf(state, human)
                mission.outcome == MissionOutcome.PASSED -> Ending.WON
                else -> Ending.LOST
            }
            OpponentLook.of(state, human, lastCapture, inDanger, ending)
        }
        return GameUiState(
            state = state,
            lastMove = lastMove,
            canUndo = computer != null && level == null && !over && game.canUndo(human),
            vsComputer = computer != null,
            dangers = dangers,
            opponent = opponent,
            isOver = over,
            level = level,
            mission = mission,
            levelEnd = levelEnd,
            fresh = game.record.actions.isEmpty(),
            round = round,
            guide = guideFor(state, preview = null),
            guided = level != null && Tutorial.lesson(level) != null && !skipped,
            practice = entry.practice,
        )
    }
}
