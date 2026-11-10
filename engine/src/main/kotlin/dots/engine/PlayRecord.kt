package dots.engine

/**
 * A game as where it starts plus the actions taken, which replays to exactly the same position.
 * Two kinds exist: a free game ([GameRecord]) and a campaign level (`dots.campaign.LevelRecord`).
 * Each is written as one line of text; `dots.log.GameLogEntry` reads either kind back.
 */
interface PlayRecord {
    val actions: List<Action>

    /** The position before the first action. */
    fun start(): GameState

    /** The position after every action. */
    fun replay(): GameState = actions.fold(start()) { state, action -> state.apply(action).state }

    /** Whether the game is over in [state]: when no move is left or, in a level, when its mission is decided. */
    fun isOver(state: GameState): Boolean = state.isOver

    /** The same game from the same start, with [actions] taken instead. */
    fun withActions(actions: List<Action>): PlayRecord

    /** The record as one line of text. */
    fun toText(): String
}
