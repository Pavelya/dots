package dots.campaign

import dots.engine.Booster

/** [count] of [booster], for the player to keep: one of a chest's two choices. */
data class Reward(val booster: Booster, val count: Int) {
    init {
        require(count >= 1) { "a reward holds 1 or more boosters" }
    }
}

/**
 * The chest on the path after [after], every 10th level of the campaign (decision 0027). Passing
 * that level makes it ready; the player opens it once and keeps one of its two [choices].
 */
data class Chest(val after: Level, val choices: List<Reward>) {
    init {
        require(choices.size == 2) { "a chest holds two choices" }
        require(choices[0].booster != choices[1].booster) { "a chest's two choices are different boosters" }
    }

    companion object {
        /** A chest after every this many levels of the campaign, counted from its first. */
        const val EVERY = 10

        /**
         * Each chest's two choices, in campaign order. Each booster in them is one the levels after
         * that chest allow, so a reward can be used; Undo is never in a chest, as a level allows one
         * (decision 0023).
         */
        internal val CHOICES = listOf(
            Booster.WALL to Booster.BOMB, // after 1-10
            Booster.WALL to Booster.DOUBLE_DOT, // after 2-5
            Booster.WALL to Booster.BOMB, // after 2-15
            Booster.BOMB to Booster.DOUBLE_DOT, // after 3-5
            Booster.DOUBLE_DOT to Booster.HINT, // after 3-15
            Booster.HINT to Booster.BOMB, // after 4-5
            Booster.WALL to Booster.HINT, // after 4-15
        )

        /** How many of a booster a chest gives. */
        const val COUNT = 2

        /** The chests of [levels], the campaign in order: one after each [EVERY]th level, but not after the last. */
        internal fun of(levels: List<Level>): List<Chest> =
            levels.dropLast(1).filterIndexed { i, _ -> (i + 1) % EVERY == 0 }.mapIndexed { i, level ->
                val (first, second) = CHOICES.getOrNull(i)
                    ?: throw IllegalStateException("the chest after level ${level.code} has no choices (Chest.CHOICES)")
                Chest(level, listOf(Reward(first, COUNT), Reward(second, COUNT)))
            }
    }
}
