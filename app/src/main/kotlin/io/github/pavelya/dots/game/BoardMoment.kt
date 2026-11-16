package io.github.pavelya.dots.game

import dots.ai.Danger
import dots.campaign.MissionStatus
import dots.engine.Capture
import dots.engine.Player
import dots.engine.Pos

/**
 * A dot that has just landed, with everything the screen needs to play its moment
 * (docs/design/motion.md): placing, the computer's move, a capture and a new threat. [id] grows
 * with every dot, so the screen plays each one once.
 *
 * - [dot] was placed by [by]. When it is [announced] (the computer's dot, or any dot between two
 *   people), two ripples spread from it and edge marks show its row and column.
 * - [captures] are the areas it closed, played at the same time.
 * - Until the new dot settles the pinhole stays on [pinholeBefore]; the edge marks of an earlier
 *   dot, at [marksGone], shrink back.
 * - [freshThreats] are the threats that are new with this dot: only they draw in and pulse.
 * - Until the capture's points land, the screen shows the mission as it was ([missionBefore]),
 *   and until the face changes, the opponent's old look ([opponentBefore]).
 */
data class BoardMoment(
    val id: Int,
    val dot: Pos,
    val by: Player,
    val announced: Boolean,
    val captures: List<Capture> = emptyList(),
    val pinholeBefore: Pos? = null,
    val marksGone: Pos? = null,
    val freshThreats: Set<ThreatMark> = emptySet(),
    val missionBefore: MissionStatus? = null,
    val opponentBefore: OpponentLook? = null,
) {
    /** The dots captured at once. */
    val captured: Int get() = captures.sumOf { it.dots.size }

    /** The points the capture adds to [by]'s score once they land. */
    val points: Int get() = captures.sumOf { it.points }
}

/**
 * A threat as the screen marks it: the [group] close to capture and how many dots it needs. A group
 * that comes one dot closer is a new threat.
 */
data class ThreatMark(val group: Set<Pos>, val dotsNeeded: Int) {
    companion object {
        fun of(danger: Danger): ThreatMark = ThreatMark(danger.group.toSet(), danger.dotsNeeded)
    }
}

/** A captured area at the end of a game: its first point (the board's areas are found by it) and its captor. */
data class CapturedArea(val first: Pos, val by: Player)
