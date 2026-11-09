package dots.ai

import dots.engine.Action
import dots.engine.GameState

/** A computer opponent. */
interface Ai {
    /** What [GameState.toMove] should do now. Only call this while the game is not over. */
    fun choose(state: GameState): Action

    /**
     * As [choose], but may give up early once [isCancelled] returns true; the action returned
     * then is legal but should be thrown away. By default it simply ignores [isCancelled].
     */
    fun choose(state: GameState, isCancelled: () -> Boolean): Action = choose(state)
}

/**
 * How hard a [SearchAi] plays.
 *
 * @param depth plies searched ahead: 1 only grabs captures, 2 also avoids giving one away,
 *   3 can set up a double threat, 4 also sees the answer to it.
 * @param breadth moves kept per ply after ordering; the root keeps twice as many. A small breadth
 *   is a player who weighs only the few most obvious moves and answers.
 * @param mistakeRate chance per turn of playing a random nearby move instead of the best one. Only
 *   the benchmarks' stand-in players use it: the [STEPS] make a person's mistakes instead.
 * @param variety so that a position does not always get the same answer, each candidate move gets
 *   a random bonus of 0 to this many points before the best is picked. A move this far below the
 *   best is never played, a move just below it often is. For scale, a captured dot is worth 10,000
 *   and touching one more dot 2 or 3; 0 picks only between moves with exactly the best score.
 * @param reach only cells within this many steps of an existing dot are considered.
 * @param focus a person looks where the game just happened. With a focus, the computer considers
 *   only points at most this many steps (diagonals count as one step) from the last dot each side
 *   placed, at every ply it searches. Chances and dangers elsewhere on the board go unseen, for
 *   itself and for its opponent. 0 looks at the whole board.
 * @param attackReach the computer looks for enemy groups it could capture with at most this many
 *   more dots and tries those cells first; 0 turns it off.
 * @param dangerReach the same for its own groups the opponent could capture: it considers
 *   blocking those cells first.
 * @param attackWeights how strongly the computer wants to play a cell that walls in an enemy
 *   group, by how many more dots that group needs: the first number is for a group one dot from
 *   capture, the second for two dots, and so on; the last number also covers every greater
 *   distance. The bonus is multiplied by what capturing the group would score (its size, one more
 *   for each dot on a bonus point; at most 3) and added to the move's score, so it decides between
 *   moves as well as which moves are searched. For scale, a captured dot is worth 10,000.
 * @param dangerWeights the same for a cell that would stop the opponent walling in one of the
 *   computer's own groups. Higher than [attackWeights] means it defends before it attacks.
 * @param timeLimitMillis if set, the computer answers within about this many milliseconds,
 *   searching as deep as time allows up to [depth]; null searches to [depth] however long it takes.
 */
data class AiConfig(
    val depth: Int,
    val breadth: Int,
    val mistakeRate: Double = 0.0,
    val variety: Int = 0,
    val reach: Int = 2,
    val focus: Int = 0,
    val attackReach: Int = 0,
    val dangerReach: Int = 0,
    val timeLimitMillis: Long? = null,
    val attackWeights: List<Int> = BALANCED,
    val dangerWeights: List<Int> = BALANCED,
) {
    init {
        require(depth >= 1) { "depth must be at least 1" }
        require(breadth >= 1) { "breadth must be at least 1" }
        require(mistakeRate in 0.0..1.0) { "mistakeRate must be a probability" }
        require(variety >= 0) { "variety must not be negative" }
        require(reach >= 1) { "reach must be at least 1" }
        require(focus >= 0) { "focus must not be negative" }
        require(attackReach >= 0 && dangerReach >= 0) { "threat reach must not be negative" }
        require(timeLimitMillis == null || timeLimitMillis > 0) { "timeLimitMillis must be positive" }
        require(attackWeights.isNotEmpty() && dangerWeights.isNotEmpty()) { "weights must not be empty" }
        require((attackWeights + dangerWeights).all { it >= 0 }) { "weights must not be negative" }
    }

    companion object {
        /** Threat weights by distance from capture (1, 2, 3, 4 or more dots), used for attack and defence alike. */
        val BALANCED = listOf(3000, 600, 120, 30)

        /** Half of [BALANCED]: for danger weights, a player who would rather attack than defend. */
        val ATTACK_FIRST = listOf(1500, 300, 60, 15)

        /*
         * The strength steps. Below HARD each one plays like a weaker person, not like a strong
         * player who sometimes plays at random: it looks only near the last dots (focus 2), sees a
         * group in danger only one dot from capture, and would rather attack than defend. What
         * sets them apart is how far ahead they think and how many moves they weigh. README,
         * "Strength steps", has the benchmark that shows each beats the one before it.
         */

        /** Step 1. Thinks one move ahead and sees no threats: it plays next to dots and closes a ring it can close now. */
        val BEGINNER = AiConfig(depth = 1, breadth = 12, variety = 10, focus = 2)

        /** Step 2. Still one move ahead, but builds toward rings two dots from closing and sees its own groups one dot from capture. */
        val EASY = AiConfig(
            depth = 1, breadth = 12, variety = 10, focus = 2,
            attackReach = 2, dangerReach = 1, dangerWeights = ATTACK_FIRST,
        )

        /** Step 3. Checks the answer to its move, but weighs only its 4 most obvious moves and 2 answers to each. */
        val STEADY = AiConfig(
            depth = 2, breadth = 2, variety = 10, focus = 2,
            attackReach = 2, dangerReach = 1, dangerWeights = ATTACK_FIRST,
        )

        /** Step 4. As [STEADY], weighing 20 moves and 10 answers to each. */
        val NORMAL = AiConfig(
            depth = 2, breadth = 10, variety = 10, focus = 2,
            attackReach = 2, dangerReach = 1, dangerWeights = ATTACK_FIRST,
        )

        /**
         * Step 5, the strongest. Looks at the whole board, and one dot less far for danger than for
         * chances to attack: looking as far made it passive, and it failed to capture players who
         * only defend (README, "Attack and defence").
         */
        val HARD = AiConfig(depth = 3, breadth = 8, variety = 5, attackReach = 3, dangerReach = 2)
        /**
         * The [dots.engine.Booster.HINT]: [HARD] without its random bonus, so it shows the move it
         * rates best, within 1.5 seconds on a phone.
         */
        val HINT = HARD.copy(variety = 0, timeLimitMillis = 1500)

        /** Looks one move further than [HARD], at about three times the cost. Benchmarks show no gain yet. */
        val EXPERT = AiConfig(depth = 4, breadth = 8, variety = 5, attackReach = 3, dangerReach = 3)

        /** The strength steps, weakest first. [EXPERT] is not one: it is no stronger than [HARD]. */
        val STEPS = listOf(BEGINNER, EASY, STEADY, NORMAL, HARD)
    }
}
