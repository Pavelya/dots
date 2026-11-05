package dots.ai

import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos

/**
 * A group of dots close to being captured.
 *
 * @param group the group's dots
 * @param dotsNeeded how many more dots the opponent must place to wall it in
 * @param closingPoints the empty points of one cheapest way to do it (other ways may exist)
 */
data class Danger(val group: List<Pos>, val dotsNeeded: Int, val closingPoints: List<Pos>)

/** What the screen can show about threats on the board, for both players. */
object Hints {
    /**
     * Groups of [player] that the opponent could capture with at most [maxDots] more dots, nearest
     * first. Threats the opponent has too few dots left to carry out are left out.
     */
    fun dangers(state: GameState, player: Player, maxDots: Int = 2): List<Danger> {
        if (state.isOver) return emptyList()
        val dotsLeft = state.dotsLeft(player.opponent) ?: Int.MAX_VALUE
        val width = state.width
        fun pos(i: Int) = Pos(i % width, i / width)
        return ThreatFinder().find(state, player, maxDots)
            .filter { it.cost <= dotsLeft }
            .sortedBy { it.cost }
            .map { threat -> Danger(threat.group.map(::pos), threat.cost, threat.cells.map(::pos)) }
    }
}
