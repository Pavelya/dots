package io.github.pavelya.dots.game

import dots.ai.Danger
import dots.campaign.MissionStatus
import dots.engine.Booster
import dots.engine.Capture
import dots.engine.GameState
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
 * - A Hint's circles round [hintBefore] shrink away as the dot lands. After a Double dot,
 *   [doubleDots] is how many dots the turn still had before this one (2, then 1): the dot sparks,
 *   and the chip counts down or leaves (motion.md, "Boosters").
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
    val hintBefore: Pos? = null,
    val doubleDots: Int? = null,
) {
    /** The dots captured at once. */
    val captured: Int get() = captures.sumOf { it.dots.size }

    /** The points the capture adds to [by]'s score once they land. */
    val points: Int get() = captures.sumOf { it.points }
}

/**
 * A booster the player has just used, with everything the screen needs to play its effect
 * (docs/design/motion.md, "Boosters"). [id] differs from every other moment's, so the screen plays
 * each once.
 *
 * - [point] is the wall, the hinted point or the bomb's centre; null for Double dot and Undo.
 * - [removed] are the opponent's dots a bomb took off the board: they swell and shrink away.
 * - [undone] is the turn an Undo took back, rubbed out newest first.
 */
data class BoosterMoment(
    val id: Int,
    val booster: Booster,
    val by: Player,
    val point: Pos? = null,
    val removed: List<Pos> = emptyList(),
    val undone: UndoneTurn? = null,
)

/**
 * The turn an Undo took back: the board [before] it, the pieces of the opponent's [answer] and the
 * player's [own] dots, and the booster the player used in that turn ([givenBack], used at
 * [givenBackAt], or at their dot when it had no point), which flies back to its button. Until the
 * turn is gone the opponent keeps the look of Undo's preview ([opponentBefore]).
 */
class UndoneTurn(
    val before: GameState,
    val answer: List<Pos>,
    val own: List<Pos>,
    val givenBack: Booster? = null,
    val givenBackAt: Pos? = null,
    val opponentBefore: OpponentLook? = null,
)

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
