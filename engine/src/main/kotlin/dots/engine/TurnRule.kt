package dots.engine

/**
 * A rule that gives a player two dots on some of their turns: the campaign's boss rules. A player
 * has at most one. Both dots come out of the player's dots left, as with [Booster.DOUBLE_DOT].
 * [GameState.doubleMoveIn] counts down to the next such turn.
 */
sealed interface TurnRule {
    /** True if the player's [turn]th turn (counting from 1) is a turn of two dots. */
    fun isDouble(turn: Int): Boolean

    /** Two dots on every [every]th turn of the player's own: turns [every], 2 x [every], ... */
    data class DoubleMove(val every: Int) : TurnRule {
        init {
            require(every >= 2) { "a double move comes at most every 2nd turn" }
        }

        override fun isDouble(turn: Int) = turn % every == 0
    }

    /**
     * Two dots on every turn, one in each half of the board: the left half (the first width / 2
     * columns) and the right half. Either half may come first. The board's width must be even.
     */
    data object TwoHalves : TurnRule {
        override fun isDouble(turn: Int) = true
    }
}
