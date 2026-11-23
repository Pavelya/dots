package dots.campaign

import dots.engine.Booster

/** [count] of [booster], for the player to keep: one of a chest's choices. */
data class Reward(val booster: Booster, val count: Int) {
    init {
        require(count >= 1) { "a reward holds 1 or more boosters" }
    }
}

/**
 * The chest on the path after [after], every 10th level of the campaign (decision 0027). Passing
 * that level makes it ready; the player opens it once. A chest of two [choices] lets the player
 * keep one; a chest of one gives it, with no pick (decision 0038).
 */
data class Chest(val after: Level, val choices: List<Reward>) {
    init {
        require(choices.size in 1..2) { "a chest holds one or two choices" }
        require(choices.map { it.booster }.distinct().size == choices.size) { "a chest's two choices are different boosters" }
    }

    /** True when the chest holds one reward, which the player keeps without a pick. */
    val isSingle: Boolean get() = choices.size == 1

    companion object {
        /** A chest after every this many levels of the campaign, counted from its first. */
        const val EVERY = 10

        /**
         * Each chest's choices, in campaign order. Each is a kind that a level between that chest
         * and the next one allows (to the end of the campaign for the last chest), so a reward can
         * be used soon (decision 0038). Where those levels allow one kind, the chest holds just
         * that one. Undo is never in a chest, as a level allows one (decision 0023).
         */
        internal val CHOICES = listOf(
            listOf(Booster.WALL), // after 1-10: 1-11 to 2-5 allow only Wall
            listOf(Booster.WALL), // after 2-5: 2-6 to 2-15 allow only Wall
            listOf(Booster.WALL, Booster.BOMB), // after 2-15
            listOf(Booster.BOMB, Booster.DOUBLE_DOT), // after 3-5
            listOf(Booster.DOUBLE_DOT, Booster.HINT), // after 3-15
            listOf(Booster.HINT, Booster.BOMB), // after 4-5
            listOf(Booster.WALL, Booster.HINT), // after 4-15
        )

        /** How many of a booster a chest gives. */
        const val COUNT = 2

        /** The chests of [levels], the campaign in order: one after each [EVERY]th level, but not after the last. */
        internal fun of(levels: List<Level>): List<Chest> =
            levels.dropLast(1).filterIndexed { i, _ -> (i + 1) % EVERY == 0 }.mapIndexed { i, level ->
                val kinds = CHOICES.getOrNull(i)
                    ?: throw IllegalStateException("the chest after level ${level.code} has no choices (Chest.CHOICES)")
                Chest(level, kinds.map { Reward(it, COUNT) })
            }
    }
}
