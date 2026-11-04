package io.github.pavelya.dots.game

import androidx.lifecycle.ViewModel
import dots.engine.Action
import dots.engine.Game
import dots.engine.GameSettings
import dots.engine.GameState
import dots.engine.Pos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Everything the game screen draws, as one immutable value. */
data class GameUiState(
    val state: GameState,
    val preview: Pos? = null,
    val lastMove: Pos? = null,
)

/**
 * Holds one game and turns screen events into game actions. The screen only sends events
 * ([onTap]) and draws [ui]; all rules are the engine's.
 */
class GameViewModel(settings: GameSettings) : ViewModel() {
    private val game = Game(settings)
    private val _ui = MutableStateFlow(GameUiState(game.state))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    /** A tap on board point [point], or off the board when null. */
    fun onTap(point: Pos?) {
        val current = _ui.value
        val result = Placement.onTap(current.preview, point) { game.state.isLegal(it) }
        val placed = result.placed
        _ui.value = if (placed != null) {
            game.apply(Action.Place(placed))
            GameUiState(game.state, preview = null, lastMove = placed)
        } else {
            current.copy(preview = result.preview)
        }
    }
}
