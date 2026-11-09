package dots.ai

import dots.engine.Action
import dots.engine.Booster
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

/** What the screen can show about threats on the board, for both players, and the hint booster. */
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

    /**
     * The [Booster.HINT] for the player to move: the computer's best move for them, played as
     * [ai] sees it. Null if they cannot use a hint now or have no move. Apply the result to use
     * up the hint; the record keeps the point.
     */
    fun hint(state: GameState, ai: SearchAi = SearchAi(AiConfig.HINT)): Action.Hint? {
        if (!state.canUse(Booster.HINT)) return null
        return ai.chooseMove(state)?.let { Action.Hint(it) }?.takeIf { state.isLegal(it) }
    }
}
