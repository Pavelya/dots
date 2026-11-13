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

    /**
     * Uses one of the player's boosters (campaign only). A booster does not end the turn: the
     * player still places their dot afterwards. At most one booster per turn. See [Booster].
     */
    sealed interface UseBooster : Action {
        val booster: Booster
    }

    /** Blocks the empty point [pos]: nobody may place a dot there for the rest of the game. */
    data class Wall(val pos: Pos) : UseBooster {
        override val booster get() = Booster.WALL
    }

    /**
     * Shows the player the computer's best move for them, [pos]. The board does not change; the
     * point is kept so that a record shows what was suggested. `dots.ai.Hints.hint` finds it.
     */
    data class Hint(val pos: Pos) : UseBooster {
        override val booster get() = Booster.HINT
    }

    /**
     * Removes the opponent's dots on [pos] and the four points next to it (up, down, left,
     * right), except on points out of play. See [GameState.bombHits].
     */
    data class Bomb(val pos: Pos) : UseBooster {
        override val booster get() = Booster.BOMB
    }

    /** The player places two dots this turn instead of one. */
    data object DoubleDot : UseBooster {
        override val booster get() = Booster.DOUBLE_DOT
    }

    /**
     * Takes back the player's last turn: their dot (both dots after a double dot), any booster used
     * in that turn, which they get back, and the opponent's reply. The player then plays that turn
     * again. See [GameState.useBooster].
     */
    data object Undo : UseBooster {
        override val booster get() = Booster.UNDO
    }

    companion object {
        /**
         * The word a game record uses for [action]: `e5`, `pass`, `stop`, `done`, or a booster's
         * name with its point, `wall:e5`, `hint:e5`, `bomb:e5`, `double-dot` and `undo`.
         */
        fun word(action: Action): String = when (action) {
            is Place -> Notation.name(action.pos)
            Pass -> "pass"
            Stop -> "stop"
            Finish -> "done"
            is Wall -> "${Booster.WALL.text}:${Notation.name(action.pos)}"
            is Hint -> "${Booster.HINT.text}:${Notation.name(action.pos)}"
            is Bomb -> "${Booster.BOMB.text}:${Notation.name(action.pos)}"
            DoubleDot -> Booster.DOUBLE_DOT.text
            Undo -> Booster.UNDO.text
        }

        /** Reads a word written by [word]. */
        fun parse(word: String): Action {
            when (word) {
                "pass" -> return Pass
                "stop" -> return Stop
                "done" -> return Finish
                Booster.DOUBLE_DOT.text -> return DoubleDot
                Booster.UNDO.text -> return Undo
            }
            val parts = word.split(':', limit = 2)
            fun point(text: String) = Notation.parse(text) ?: throw IllegalArgumentException("Bad move '$word'")
            if (parts.size == 1) return Place(point(word))
            val pos = point(parts[1])
            return when (parts[0]) {
                Booster.WALL.text -> Wall(pos)
                Booster.HINT.text -> Hint(pos)
                Booster.BOMB.text -> Bomb(pos)
                else -> throw IllegalArgumentException("Bad move '$word'")
            }
        }
    }
}

/**
 * The boosters of the solo campaign (decision 0006), with the word level files and game records
 * use for each. The computer never uses them.
 */
enum class Booster(val text: String) {
    /** Blocks one empty point: [Action.Wall]. */
    WALL("wall"),

    /** Shows the computer's best move for the player: [Action.Hint]. */
    HINT("hint"),

    /** Removes the opponent's dots in a plus of 5 points: [Action.Bomb]. */
    BOMB("bomb"),

    /** Two dots in one turn: [Action.DoubleDot]. */
    DOUBLE_DOT("double-dot"),

    /** Takes back the player's last turn: [Action.Undo] (decision 0023). */
    UNDO("undo"),
}
