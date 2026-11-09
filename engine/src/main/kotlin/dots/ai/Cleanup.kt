package dots.ai

import dots.engine.Action
import dots.engine.GameState
import dots.engine.Grounding
import dots.engine.Player
import dots.engine.Pos

/**
 * Plays the collecting phase after the opponent has declared their dots grounded. The opponent no
 * longer moves, so any group that is not grounded can be captured by walling it in.
 *
 * Walling a group in is a minimum vertex cut: the fewest empty cells that, once built on, leave
 * no orthogonal path from the group to the border. That is exactly how the engine decides a
 * region is enclosed, so completing the cut captures the group. Each turn this places one dot of
 * the cheapest cut among all exposed groups.
 *
 * Known gap: a group that already sits inside the collector's own closed chain (cut of size zero)
 * needs a dividing wall instead, which this planner does not attempt.
 */
internal object Cleanup {
    private const val INF = 1 shl 20

    fun next(state: GameState): Action {
        val cleaner = state.toMove
        var best: List<Pos>? = null
        for (group in Grounding.ungrounded(state, cleaner.opponent)) {
            val cut = cheapestEnclosure(state, group, cleaner) ?: continue
            if (cut.isNotEmpty() && (best == null || cut.size < best.size)) best = cut
        }
        return best?.let { Action.Place(it.first()) } ?: Action.Finish
    }

    /**
     * The cells to build on to enclose [group], or null if it cannot be enclosed. Slow but simple;
     * [ThreatFinder] is the fast version used inside the search and is tested against this one.
     */
    internal fun cheapestEnclosure(state: GameState, group: List<Pos>, cleaner: Player): List<Pos>? {
        val width = state.width
        val height = state.height
        val n = width * height
        val wall = BooleanArray(n) { i ->
            val x = i % width
            val y = i / width
            state.ownerAt(x, y) == cleaner && !state.isCaptured(x, y)
        }
        fun buildable(i: Int) = state.ownerAt(i % width, i / width) == null && !state.isDead(i % width, i / width)

        // Every cell is split into in/out nodes joined by an edge whose capacity is the price of
        // walling the cell: 1 for an empty playable cell, unbounded for anything else.
        val source = 2 * n
        val sink = 2 * n + 1
        val graph = FlowGraph(2 * n + 2, 2 * (6 * n + group.size))
        for (i in 0 until n) {
            if (wall[i]) continue
            val x = i % width
            val y = i / width
            graph.add(2 * i, 2 * i + 1, if (buildable(i)) 1 else INF)
            for ((dx, dy) in ORTHOGONAL) {
                val nx = x + dx
                val ny = y + dy
                if (!state.inBounds(nx, ny) || wall[ny * width + nx]) continue
                graph.add(2 * i + 1, 2 * (ny * width + nx), INF)
            }
            if (state.isBorder(x, y)) graph.add(2 * i + 1, sink, INF)
        }
        for (pos in group) graph.add(source, 2 * (pos.y * width + pos.x), INF)

        val flow = graph.maxFlow(source, sink)
        if (flow >= INF) return null

        val reachable = graph.reachableFromSource()
        val cut = ArrayList<Pos>()
        for (i in 0 until n) {
            if (!wall[i] && buildable(i) && reachable[2 * i] && !reachable[2 * i + 1]) {
                cut += Pos(i % width, i / width)
            }
        }
        return cut
    }

    private class FlowGraph(private val nodes: Int, maxEdges: Int) {
        private val head = IntArray(nodes) { -1 }
        private val next = IntArray(maxEdges)
        private val to = IntArray(maxEdges)
        private val cap = IntArray(maxEdges)
        private var count = 0
        private var visited = BooleanArray(nodes)

        fun add(from: Int, target: Int, capacity: Int) {
            to[count] = target; cap[count] = capacity; next[count] = head[from]; head[from] = count++
            to[count] = from; cap[count] = 0; next[count] = head[target]; head[target] = count++
        }

        /** Shortest-augmenting-path max flow; stops early once the flow is unbounded. */
        fun maxFlow(source: Int, sink: Int): Int {
            var flow = 0
            val via = IntArray(nodes)
            val queue = IntArray(nodes)
            while (flow < INF) {
                visited = BooleanArray(nodes)
                var tail = 0
                var headIndex = 0
                queue[tail++] = source
                visited[source] = true
                while (headIndex < tail && !visited[sink]) {
                    val u = queue[headIndex++]
                    var e = head[u]
                    while (e != -1) {
                        val v = to[e]
                        if (cap[e] > 0 && !visited[v]) {
                            visited[v] = true
                            via[v] = e
                            queue[tail++] = v
                        }
                        e = next[e]
                    }
                }
                if (!visited[sink]) return flow

                var bottleneck = Int.MAX_VALUE
                var v = sink
                while (v != source) {
                    bottleneck = minOf(bottleneck, cap[via[v]])
                    v = to[via[v] xor 1]
                }
                v = sink
                while (v != source) {
                    cap[via[v]] -= bottleneck
                    cap[via[v] xor 1] += bottleneck
                    v = to[via[v] xor 1]
                }
                flow += bottleneck
            }
            return flow
        }

        /** Nodes still reachable from the source once the flow is maximal (valid after [maxFlow]). */
        fun reachableFromSource(): BooleanArray = visited
    }

    private val ORTHOGONAL = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
}
