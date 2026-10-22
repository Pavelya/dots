package dots.ai

import dots.engine.Action
import dots.engine.GameState
import dots.engine.Grounding
import dots.engine.Player
import dots.engine.Pos
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Alpha-beta search over captured dots, with a small "pressure" bonus for leaning on enemy dots.
 *
 * Every ply only looks at cells near existing dots, ranks them by what the move wins right away
 * and by a cheap neighbourhood heuristic, and searches the best few. That keeps deep levels fast
 * enough for a phone while still finding threats a few moves out.
 */
class SearchAi(
    private val config: AiConfig = AiConfig.NORMAL,
    private val random: Random = Random.Default,
) : Ai {

    private class Node(val pos: Pos, val state: GameState, val key: Int, val local: Int)

    override fun choose(state: GameState): Action {
        if (state.isOver) return Action.Pass
        if (state.isCleanup) return Cleanup.next(state)
        if (shouldStop(state)) return Action.Stop
        return chooseMove(state)?.let { Action.Place(it) } ?: Action.Pass
    }

    /**
     * Declares the dots grounded once the game is already won: even if the opponent captured every
     * dot of mine that is still exposed, they would not catch up. Not used with a dot budget,
     * which ends the game by itself.
     */
    private fun shouldStop(state: GameState): Boolean {
        val me = state.toMove
        if (state.dotsLeft(me) != null) return false
        val exposed = Grounding.ungrounded(state, me).sumOf { it.size }
        return state.score(me) > state.score(me.opponent) + exposed
    }

    /** The best dot to place, ignoring stop/pass; null if nothing can be placed. */
    fun chooseMove(state: GameState): Pos? {
        if (state.isOver) return null
        val me = state.toMove
        val candidates = candidates(state)
        if (candidates.isEmpty()) return null
        if (config.mistakeRate > 0 && random.nextDouble() < config.mistakeRate) {
            return candidates[random.nextInt(candidates.size)]
        }

        var bestValue = Int.MIN_VALUE
        val best = ArrayList<Pos>()
        for (node in expand(state, me, config.breadth * 2)) {
            val value = search(node.state, me, config.depth - 1, -INF, INF) + node.local
            when {
                value > bestValue -> {
                    bestValue = value
                    best.clear()
                    best += node.pos
                }
                value == bestValue -> best += node.pos
            }
        }
        return best[random.nextInt(best.size)]
    }

    private fun search(state: GameState, me: Player, depth: Int, alpha: Int, beta: Int): Int {
        if (depth == 0 || state.isOver) return evaluate(state, me)
        val nodes = expand(state, me, config.breadth)
        if (nodes.isEmpty()) return evaluate(state, me)

        val maximizing = state.toMove == me
        var low = alpha
        var high = beta
        var best = if (maximizing) -INF else INF
        for (node in nodes) {
            val value = search(node.state, me, depth - 1, low, high)
            if (maximizing) {
                best = max(best, value)
                low = max(low, best)
            } else {
                best = min(best, value)
                high = min(high, best)
            }
            if (low >= high) break
        }
        return best
    }

    /** The mover's best [limit] moves, most promising first, each already played out. */
    private fun expand(state: GameState, me: Player, limit: Int): List<Node> {
        val mover = state.toMove
        val sign = if (mover == me) 1 else -1
        return candidates(state)
            .map { pos ->
                val child = state.play(pos).state
                val local = neighbourhood(state, pos, mover)
                Node(pos, child, sign * evaluate(child, me) + local, local)
            }
            .sortedByDescending { it.key }
            .take(limit)
    }

    private fun evaluate(state: GameState, me: Player): Int =
        (state.score(me) - state.score(me.opponent)) * SCORE_WEIGHT +
            pressure(state, me) - pressure(state, me.opponent)

    /**
     * How hard [attacker] leans on the enemy dots that could still be captured. Without this the
     * search sees nothing between "no capture yet" positions and plays aimlessly: a ring is
     * built around a dot long before anything is won.
     */
    private fun pressure(state: GameState, attacker: Player): Int {
        var total = 0
        for (y in 1 until state.height - 1) {
            for (x in 1 until state.width - 1) { // dots on the border can never be enclosed
                if (state.ownerAt(x, y) != attacker.opponent || state.isCaptured(x, y)) continue
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        val nx = x + dx
                        val ny = y + dy
                        if ((dx == 0 && dy == 0) || !state.inBounds(nx, ny)) continue
                        if (state.ownerAt(nx, ny) != attacker || state.isCaptured(nx, ny)) continue
                        total += if (max(abs(dx), abs(dy)) == 1) NEAR_WEIGHT else FAR_WEIGHT
                    }
                }
            }
        }
        return total
    }

    /** Prefers moves that touch dots: they build chains and pressure the enemy. */
    private fun neighbourhood(state: GameState, pos: Pos, mover: Player): Int {
        var value = 0
        for (dy in -1..1) {
            for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val x = pos.x + dx
                val y = pos.y + dy
                if (!state.inBounds(x, y) || state.isCaptured(x, y)) continue
                val owner = state.ownerAt(x, y) ?: continue
                value += if (owner == mover) OWN_NEIGHBOUR else ENEMY_NEIGHBOUR
            }
        }
        return value
    }

    private fun candidates(state: GameState): List<Pos> {
        val width = state.width
        val height = state.height
        val reach = config.reach
        val near = BooleanArray(width * height)
        var anyDot = false
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (state.ownerAt(x, y) == null || state.isCaptured(x, y)) continue
                anyDot = true
                for (ny in max(0, y - reach)..min(height - 1, y + reach)) {
                    for (nx in max(0, x - reach)..min(width - 1, x + reach)) near[ny * width + nx] = true
                }
            }
        }
        if (!anyDot) {
            val cx = width / 2
            val cy = height / 2
            return listOf(Pos(cx, cy)).filter { state.isLegal(it) }.ifEmpty { state.legalMoves() }
        }

        val result = ArrayList<Pos>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (near[y * width + x] && state.isLegal(x, y)) result += Pos(x, y)
            }
        }
        return result.ifEmpty { state.legalMoves() }
    }

    private companion object {
        const val INF = 1_000_000

        /** A captured dot outweighs any amount of pressure. */
        const val SCORE_WEIGHT = 10_000
        const val NEAR_WEIGHT = 3
        const val FAR_WEIGHT = 1
        const val OWN_NEIGHBOUR = 3
        const val ENEMY_NEIGHBOUR = 2
    }
}
