package dots.engine

/**
 * A game in progress with its history: what an app screen holds. Each action is applied to the
 * immutable [GameState], and every state is kept, so undo is exact and the [record] can be
 * saved or logged at any time. Not thread-safe; one owner (for example a ViewModel) uses it.
 *
 * The game starts where its record starts: a free game's settings, or a campaign level.
 */
class Game private constructor(private val origin: PlayRecord, private val states: MutableList<GameState>) {
    private val actions = ArrayList<Action>()

    constructor(settings: GameSettings) : this(GameRecord(settings, emptyList()))

    /** A new game from where [start] starts; its actions are not played. */
    constructor(start: PlayRecord) : this(start.withActions(emptyList()), mutableListOf(start.start()))

    val state: GameState get() = states.last()

    val record: PlayRecord get() = origin.withActions(actions.toList())

    /** True when the game is over: no move is left or, in a level, its mission is decided. */
    val isOver: Boolean get() = origin.isOver(state)

    /**
     * True if the player to move may use [booster] now. Unlike [GameState.canUse], it knows when a
     * level's mission is decided: no booster then, and no [Action.Undo] either.
     */
    fun canUse(booster: Booster): Boolean = !isOver && state.canUse(booster)

    fun apply(action: Action): MoveResult {
        require(action != Action.Undo || !isOver) { "No undo once the game is over" }
        val result = state.apply(action)
        states += result.state
        actions += action
        return result
    }

    /**
     * True if [player] has an action to take back with [undo], free play's unlimited undo. A
     * campaign level uses the Undo booster instead ([Action.Undo]), which stays in the record.
     */
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
        fun from(record: PlayRecord): Game {
            val game = Game(record)
            for (action in record.actions) game.apply(action)
            return game
        }
    }
}
