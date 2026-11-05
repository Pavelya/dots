package io.github.pavelya.dots.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dots.ai.Ai
import dots.engine.Action
import dots.engine.Game
import dots.engine.GameSettings
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
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
)

/**
 * Holds one game and turns screen events into game actions. The screen sends events ([onTap],
 * [undo], [playAgain]) and draws [ui]; all rules are the engine's.
 *
 * Against the computer the human is red and moves first. The computer thinks on
 * [computerDispatcher], never on the main thread, and its thinking is cancelled by undo, a new
 * game, or leaving the screen. It answers after at least [replyDelayMillis], so the player sees
 * their own dot land before the reply.
 *
 * @param newSettings settings for each new game (a fresh seed every time)
 * @param newComputer the opponent for each new game, or null for two players on one phone
 */
class GameViewModel(
    private val newSettings: () -> GameSettings,
    private val newComputer: (() -> Ai)? = null,
    private val computerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val replyDelayMillis: Long = 400,
) : ViewModel() {
    val human: Player = Player.RED

    private var game = Game(newSettings())
    private var computer: Ai? = newComputer?.invoke()
    private var thinking: Job? = null

    /** How often the human took a move back in this game (for the test log, P-08). */
    var undoCount: Int = 0
        private set

    private val _ui = MutableStateFlow(snapshot(lastMove = null))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

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
        game.apply(Action.Place(placed))
        _ui.value = snapshot(lastMove = placed)
        letComputerMove()
    }

    /** Takes back the human's last move and the computer's reply. */
    fun undo() {
        if (computer == null) return
        thinking?.cancel()
        if (game.undo(human)) undoCount++
        _ui.value = snapshot(lastMove = lastPlaced())
    }

    /** Starts a new game with fresh settings (a new seed, so new crosses). */
    fun playAgain() {
        thinking?.cancel()
        game = Game(newSettings())
        computer = newComputer?.invoke()
        undoCount = 0
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
            game.apply(action)
            _ui.value = snapshot(lastMove = (action as? Action.Place)?.pos ?: lastPlaced())
            letComputerMove() // the engine skips the human's turn if they have no dots left
        }
    }

    private fun lastPlaced(): Pos? = game.record.actions.lastOrNull { it is Action.Place }?.let { (it as Action.Place).pos }

    private fun snapshot(lastMove: Pos?) = GameUiState(
        state = game.state,
        lastMove = lastMove,
        canUndo = computer != null && !game.state.isOver && game.canUndo(human),
        vsComputer = computer != null,
    )
}
