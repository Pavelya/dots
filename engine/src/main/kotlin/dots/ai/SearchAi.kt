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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Alpha-beta search over captured dots. Tiers that look at threats score the positions where the
 * search stops by how close every group is to capture ([evaluateLeaf]); EASY uses a cheap
 * "pressure" count of dots placed around enemy dots instead.
 *
 * Every ply considers only a few promising moves. Which ones comes from threats ([ThreatFinder]):
 * for every enemy group the computer could capture with a few more dots, the cells that wall it in
 * are tried first, and so are the cells that would stop the opponent's own captures. The rest are
 * the cells next to the most dots. That is what lets the search build toward a capture several
 * moves out instead of waiting for one to appear. Not thread-safe.
 */
class SearchAi(
    private val config: AiConfig = AiConfig.NORMAL,
    private val random: Random = Random.Default,
) : Ai {

    private class Node(val pos: Pos, val state: GameState, val key: Int, val local: Int)

    private val finder = ThreatFinder()

    /** Set while a search runs: true once it must give up (time is up or it was cancelled). */
    private var mustStop: () -> Boolean = { false }

    /** Thrown inside the search to abandon it; never leaves this class. */
    private class Abandoned : RuntimeException(null, null, false, false)

    override fun choose(state: GameState): Action = choose(state) { false }

    /**
     * As [choose], but gives up as soon as [isCancelled] returns true (for example when the
     * player leaves the game screen). A cancelled call still returns a legal action, which the
     * caller is expected to throw away.
     */
    fun choose(state: GameState, isCancelled: () -> Boolean): Action {
        if (state.isOver) return Action.Pass
        if (state.isCleanup) return Cleanup.next(state)
        if (shouldStop(state)) return Action.Stop
        return chooseMove(state, isCancelled)?.let { Action.Place(it) } ?: Action.Pass
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

    /**
     * The best dot to place, ignoring stop/pass; null if nothing can be placed.
     *
     * Without [AiConfig.timeLimitMillis] it searches to the full depth. With it, it searches one
     * ply deeper at a time (iterative deepening) and returns the best move of the deepest search
     * that finished in time; one ply always finishes, so there is always an answer.
     */
    fun chooseMove(state: GameState, isCancelled: () -> Boolean = { false }): Pos? {
        if (state.isOver) return null
        val candidates = candidates(state)
        if (candidates.isEmpty()) return null
        if (config.mistakeRate > 0 && random.nextDouble() < config.mistakeRate) {
            return candidates[random.nextInt(candidates.size)]
        }

        val limit = config.timeLimitMillis
        if (limit == null) {
            mustStop = isCancelled
            return try {
                bestMove(state, config.depth)
            } catch (_: Abandoned) {
                candidates.first()
            } finally {
                mustStop = { false }
            }
        }

        val deadline = TimeSource.Monotonic.markNow() + limit.milliseconds
        var best = bestMove(state, 1)
        mustStop = { isCancelled() || deadline.hasPassedNow() }
        try {
            for (depth in 2..config.depth) {
                if (mustStop()) break
                best = bestMove(state, depth)
            }
        } catch (_: Abandoned) {
            // keep the best move of the last search that finished
        } finally {
            mustStop = { false }
        }
        return best
    }

    private fun bestMove(state: GameState, depth: Int): Pos {
        val me = state.toMove
        var bestValue = Int.MIN_VALUE
        val best = ArrayList<Pos>()
        for (node in expand(state, me, config.breadth * 2)) {
            val value = search(node.state, me, depth - 1, -INF, INF) + node.local
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
        if (mustStop()) throw Abandoned()
        if (depth == 0 || state.isOver) return evaluateLeaf(state, me)
        val nodes = expand(state, me, config.breadth)
        if (nodes.isEmpty()) return evaluateLeaf(state, me)

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
        val bonus = threatBonus(state, mover)
        return movesToTry(state, bonus)
            .map { pos ->
                val child = state.play(pos).state
                val local = neighbourhood(state, pos, mover) + (bonus?.get(pos.y * state.width + pos.x) ?: 0)
                Node(pos, child, sign * evaluate(child, me) + local, local)
            }
            .sortedByDescending { it.key }
            .take(limit)
    }

    /**
     * A bonus per cell for walling in an enemy group that is close to capture, or for getting in
     * the way of the opponent doing the same; null when the tier does not look at threats.
     */
    private fun threatBonus(state: GameState, mover: Player): IntArray? {
        if (config.attackReach == 0 && config.dangerReach == 0) return null
        val bonus = IntArray(state.width * state.height)
        for (threat in finder.find(state, mover.opponent, config.attackReach)) {
            for (cell in threat.cells) bonus[cell] += weight(ATTACK, threat)
        }
        for (threat in finder.find(state, mover, config.dangerReach)) {
            for (cell in threat.cells) bonus[cell] += weight(DANGER, threat)
        }
        return bonus
    }

    private fun weight(table: IntArray, threat: Threat): Int =
        table[min(threat.cost, table.size - 1)] * min(threat.size, MAX_GROUP_WEIGHT)

    /** Without threat guidance every cell near a dot; with it the threat cells plus the busiest few. */
    private fun movesToTry(state: GameState, bonus: IntArray?): List<Pos> {
        val near = candidates(state)
        if (bonus == null) return near
        val mover = state.toMove
        val chosen = LinkedHashSet<Pos>()
        near.sortedByDescending { neighbourhood(state, it, mover) }.take(config.breadth + EXTRA_NEIGHBOURS).forEach { chosen += it }
        for (i in bonus.indices) {
            if (bonus[i] <= 0) continue
            val pos = Pos(i % state.width, i / state.width)
            if (state.isLegal(pos)) chosen += pos
        }
        return chosen.toList()
    }

    /**
     * The value of a position where the search stops.
     *
     * Besides captured dots, every group of either side is scored by how close it is to being
     * walled in. A group one dot from capture is nearly lost if its enemy is to move, and a group
     * two or three dots away is still a liability: without this a search cannot tell a safe
     * position from one where a group is slowly being strangled, and searching deeper makes that
     * blindness worse, not better. Tiers that do not look at threats use the cheaper
     * [evaluate].
     */
    private fun evaluateLeaf(state: GameState, me: Player): Int {
        if (config.attackReach == 0) return evaluate(state, me)
        var value = (state.score(me) - state.score(me.opponent)) * SCORE_WEIGHT
        if (state.isOver) return value
        val meToMove = state.toMove == me
        for (threat in finder.find(state, me.opponent, LEAF_REACH)) value += threatValue(threat, meToMove)
        for (threat in finder.find(state, me, LEAF_REACH)) value -= threatValue(threat, !meToMove)
        return value
    }

    /** What a group that costs the attacker [Threat.cost] more dots is worth to the attacker. */
    private fun threatValue(threat: Threat, attackerToMove: Boolean): Int {
        val table = if (attackerToMove) TEMPO else NO_TEMPO
        return table[min(threat.cost, table.size - 1)] * threat.size
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

        const val LEAF_REACH = 3

        /** Value per dot of a group that is [cost] dots from capture, when its attacker is to move... */
        val TEMPO = intArrayOf(0, 9_000, 2_500, 800, 200)

        /** ...and when the other side is to move and may yet defend. */
        val NO_TEMPO = intArrayOf(0, 3_500, 1_500, 500, 150)
        const val NEAR_WEIGHT = 3
        const val FAR_WEIGHT = 1
        const val OWN_NEIGHBOUR = 3
        const val ENEMY_NEIGHBOUR = 2
        const val EXTRA_NEIGHBOURS = 6
        const val MAX_GROUP_WEIGHT = 3

        /** Bonus by cost (dots still needed) for a cell that walls in an enemy group. */
        val ATTACK = intArrayOf(0, 3000, 600, 120, 30)

        /** The same for a cell that would stop the opponent walling in one of our groups. */
        val DANGER = intArrayOf(0, 2500, 400, 80, 20)
    }
}
