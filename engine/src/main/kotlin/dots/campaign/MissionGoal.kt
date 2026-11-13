package dots.campaign

import dots.ai.Goal
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import kotlin.math.min

/**
 * The computer's goal in [level]: that the player fails the mission. It is reached when the
 * mission fails and lost when it passes ([Level.status]). A ring that needs more dots than its
 * attacker has left is no threat. Besides that, each mission changes what captures count for the
 * computer:
 *
 * - **Capture N in M moves.** The dots it loses count double, the dots it captures half, and a
 *   ring the player cannot close within the moves left is no danger.
 * - **Survive M moves.** The other way round: its captures count double, its losses half, and a
 *   ring it cannot close within its moves left is no chance.
 * - **Rescue** and **target.** The group holding the marked dot counts as [Goal.MARKED_WORTH] dots
 *   (to capture in "rescue", to guard in "target"); every other dot counts half, captured or lost.
 * - **Win**, **win by N** and **win with fewer dots.** Captures count as in the plain game, whose
 *   aim is the mission's: the end of the mission is what the goal adds, such as a margin that is
 *   out of the player's reach.
 */
internal class MissionGoal(private val level: Level) : Goal {
    private val mission = level.mission

    override val player: Player get() = level.computer

    override fun reached(state: GameState): Boolean? = when (level.status(state).outcome) {
        Outcome.PLAYING -> null
        Outcome.PASSED -> false
        Outcome.FAILED -> true
    }

    override val gains: Int = when (mission) {
        is Mission.Survive -> MORE
        is Mission.Capture, is Mission.Rescue, is Mission.Target -> LESS
        else -> SAME
    }

    override val losses: Int = when (mission) {
        is Mission.Capture -> MORE
        is Mission.Survive, is Mission.Rescue, is Mission.Target -> LESS
        else -> SAME
    }

    override val marked: Pos? = when (mission) {
        is Mission.Rescue -> mission.dot
        is Mission.Target -> mission.dot
        else -> null
    }

    /** The dots [attacker] has left, or fewer in "capture" for the player and in "survive" for the computer. */
    override fun movesLeft(state: GameState, attacker: Player): Int? {
        val limit = when {
            mission is Mission.Capture && attacker == level.human -> mission.moves - state.dotsPlaced(attacker)
            mission is Mission.Survive && attacker == level.computer -> mission.moves - state.dotsPlaced(attacker)
            else -> null
        }
        val dots = state.dotsLeft(attacker)
        return if (limit == null || dots == null) limit ?: dots else min(limit, dots)
    }

    private companion object {
        const val SAME = 100
        const val MORE = 200
        const val LESS = 50
    }
}
