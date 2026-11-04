package dots.engine

/**
 * A game in progress with its history: what an app screen holds. Each action is applied to the
 * immutable [GameState], and every state is kept, so undo is exact and the [record] can be
 * saved or logged at any time. Not thread-safe; one owner (for example a ViewModel) uses it.
 */
class Game private constructor(val settings: GameSettings, private val states: MutableList<GameState>) {
    private val actions = ArrayList<Action>()

    constructor(settings: GameSettings) : this(settings, mutableListOf(settings.newGame()))

    val state: GameState get() = states.last()

    val record: GameRecord get() = GameRecord(settings, actions.toList())

    fun apply(action: Action): MoveResult {
        val result = state.apply(action)
        states += result.state
        actions += action
        return result
    }

    /** True if [player] has an action to take back. */
    fun canUndo(player: Player): Boolean = lastActionBy(player) != null

    /**
     * Takes back [player]'s last action and everything played after it (against the computer:
     * its reply too), so it is [player]'s turn again. Returns false if there is nothing to undo.
     */
    fun undo(player: Player): Boolean {
        val index = lastActionBy(player) ?: return false
        while (actions.size > index) {
            actions.removeAt(actions.size - 1)
            states.removeAt(states.size - 1)
        }
        return true
    }

    private fun lastActionBy(player: Player): Int? = actions.indices.lastOrNull { states[it].toMove == player }

    companion object {
        /** Rebuilds a game, history included, from its record. */
        fun from(record: GameRecord): Game {
            val game = Game(record.settings)
            for (action in record.actions) game.apply(action)
            return game
        }
    }
}
