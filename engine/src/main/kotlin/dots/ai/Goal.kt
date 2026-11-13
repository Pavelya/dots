package dots.ai

import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos

/**
 * What a computer plays for when captures are not the whole story: the mission of a level, seen
 * from [player]'s side. [SearchAi] uses it in four ways:
 *
 * - **The end.** A position where [reached] is decided ends the search there, as a win or a loss
 *   worth more than any number of captures. A quicker win is preferred, and a later loss.
 * - **What a capture counts.** The dots [player] captures count [gains] percent of a capture in
 *   the plain game, the dots it loses [losses] percent. This holds for captures made and for groups
 *   close to capture alike.
 * - **A marked dot.** The group holding [marked], whoever owns it, counts as [MARKED_WORTH] dots,
 *   whether it is under attack or in danger.
 * - **Moves left.** A group that would take more dots to wall in than its attacker may still
 *   place for the goal ([movesLeft]) is no threat at all.
 *
 * The search's limits apply to the goal as to everything else: a computer that thinks one move
 * ahead sees a goal decided only on its own move, and one that looks only near the last dots does
 * not see a marked dot in danger elsewhere.
 */
interface Goal {
    /** Whose goal it is: the side the computer plays. */
    val player: Player

    /** True once [player] has reached the goal in [state], false once it has lost it, null while it is open. */
    fun reached(state: GameState): Boolean?

    /** What a dot [player] captures counts, in percent of a dot in the plain game. */
    val gains: Int get() = 100

    /** What a dot [player] loses counts, in percent of a dot in the plain game. */
    val losses: Int get() = 100

    /** A dot whose group matters more than its size, or null. */
    val marked: Pos? get() = null

    /**
     * How many more dots [attacker] may place for a capture to count toward the goal, or null
     * when there is no limit.
     */
    fun movesLeft(state: GameState, attacker: Player): Int? = null

    companion object {
        /** What the group holding the [marked] dot counts, in dots. */
        const val MARKED_WORTH = 10
    }
}
