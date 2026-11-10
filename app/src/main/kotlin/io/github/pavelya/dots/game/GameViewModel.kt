package io.github.pavelya.dots.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dots.ai.Ai
import dots.ai.Danger
import dots.ai.Hints
import dots.engine.Action
import dots.engine.Game
import dots.engine.GameState
import dots.engine.MoveResult
import dots.engine.Player
import dots.engine.Pos
import dots.log.GameLogEntry
import dots.log.Outcome
import io.github.pavelya.dots.data.GameJournal
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
)

/**
 * Holds one game and turns screen events into game actions. The screen sends events ([onTap],
 * [undo], [playAgain]) and draws [ui]; all rules are the engine's.
 *
 * A game is described by a [GameLogEntry]: a new one, or the saved one when the app was closed
 * mid-game (its moves are replayed). Every change is reported to the [journal], which saves it
 * and logs the game once it is over.
 *
 * Against the computer the human is red and moves first. The computer thinks on
 * [computerDispatcher], never on the main thread, and its thinking is cancelled by undo, a new
 * game, or leaving the screen. It answers after at least [replyDelayMillis], so the player sees
 * their own dot land before the reply.
 *
 * @param start the game to play: new, or resumed
 * @param nextGame the game "play again" starts (a fresh seed every time)
 * @param computerFor the computer opponent of a game, or null for two players on one phone
 * @param clock the time now in milliseconds, for the log
 */
class GameViewModel(
    start: GameLogEntry,
    private val nextGame: () -> GameLogEntry,
    private val computerFor: (GameLogEntry) -> Ai?,
    private val journal: GameJournal? = null,
    private val computerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val replyDelayMillis: Long = 400,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    val human: Player = Player.RED

    private var entry = start
    private var game = Game.from(start.record)
    private var computer: Ai? = computerFor(start)
    private var thinking: Job? = null

    /** When the game last changed (or was opened), to add up the time spent playing. */
    private var lastActive = clock()

    /** How often the human took a move back in this game. */
    val undoCount: Int get() = entry.undos

    private val _ui = MutableStateFlow(snapshot(lastMove = lastPlaced()))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    init {
        letComputerMove() // a resumed game may have been closed while the computer was to move
    }

    /** A tap on board point [point], or off the board when null. */
    fun onTap(point: Pos?) {
        val current = _ui.value
        val state = game.state
        if (current.thinking || state.isOver) return
        if (computer != null && state.toMove != human) return
        val result = Placement.onTap(current.preview, point) { state.isLegal(it) }
        val placed = result.placed
        if (placed == null) {
            _ui.value = current.copy(preview = result.preview)
            return
        }
        val result = game.apply(Action.Place(placed))
        changed()
        _ui.value = snapshot(lastMove = placed, captured = result)
        letComputerMove()
    }

    /** Takes back the human's last move and the computer's reply. */
    fun undo() {
        if (computer == null) return
        thinking?.cancel()
        if (game.undo(human)) {
            entry = entry.copy(undos = entry.undos + 1)
            changed()
        }
        _ui.value = snapshot(lastMove = lastPlaced())
    }

    /** Starts a new game with fresh settings (a new seed, so new crosses). An unfinished game is logged as abandoned. */
    fun playAgain() {
        thinking?.cancel()
        entry = nextGame()
        game = Game.from(entry.record)
        computer = computerFor(entry)
        lastActive = clock()
        journal?.start(entry)
        _ui.value = snapshot(lastMove = null)
    }

    private fun letComputerMove() {
        val ai = computer ?: return
        val position = game.state
        if (position.isOver || position.toMove == human) return
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
        entry = entry.copy(
            updatedAt = now,
            playMillis = entry.playMillis + (now - lastActive).coerceIn(0, GameLogEntry.IDLE_CAP_MILLIS),
            outcome = if (state.isOver) Outcome.FINISHED else Outcome.PLAYING,
            red = state.score(Player.RED),
            blue = state.score(Player.BLUE),
            record = game.record,
        )
        lastActive = now
        if (state.isOver) journal?.finish(entry) else journal?.save(entry)
    }

    private fun lastPlaced(): Pos? = game.record.actions.lastOrNull { it is Action.Place }?.let { (it as Action.Place).pos }

    /** What the screen draws now; [captured] is the move just played, if any, for the computer's face. */
    private fun snapshot(lastMove: Pos?, captured: MoveResult? = null): GameUiState {
        val state = game.state
        val dangers = if (entry.threatHighlight) Player.entries.flatMap { Hints.dangers(state, it) } else emptyList()
        val opponent = computer?.let {
            val lastCapture = captured?.captures?.takeIf { it.isNotEmpty() }?.let { captures ->
                LastCapture(captures.first().by, captures.sumOf { c -> c.points })
            }
            val inDanger = if (entry.threatHighlight) {
                dangers.any { it.dotsNeeded <= 1 && state.ownerAt(it.group.first()) == human }
            } else {
                Hints.dangers(state, human, maxDots = 1).isNotEmpty()
            }
            OpponentLook.of(state, human, lastCapture, inDanger)
        }
        return GameUiState(
            state = state,
            lastMove = lastMove,
            canUndo = computer != null && !state.isOver && game.canUndo(human),
            vsComputer = computer != null,
            dangers = dangers,
            opponent = opponent,
        )
    }
}
