package dots.engine

/** Everything a player can do on their turn. */
sealed interface Action {
    data class Place(val pos: Pos) : Action

    data object Pass : Action

    /**
     * Ends normal play by declaring "all the dots I want to keep are grounded". The opponent then
     * gets as many moves as they like to capture whatever is left (the competitive grounding rule).
     */
    data object Stop : Action

    /** Ends the game once the opponent has finished collecting after a [Stop]. */
    data object Finish : Action
}
