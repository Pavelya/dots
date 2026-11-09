package dots.campaign

import dots.engine.GameState
import dots.engine.Player

/**
 * Lower bounds the [Solver] uses to stop searching lines that cannot work: how many more dots a
 * side needs at least to capture a dot, or to make one of its own safe. Not thread-safe: the
 * scratch arrays are reused between calls.
 */
internal class WallCost {
    private var width = 0
    private var height = 0
    private var cells = 0
    private var kind = ByteArray(0)
    private var flowThrough = IntArray(0)
    private var flowOnLink = IntArray(0)
    private var cameFrom = IntArray(0)
    private var seenAt = IntArray(0)
    private var queue = IntArray(0)
    private var stamp = 0

    /**
     * The fewest dots [attacker] must still place to capture the dot at [target] (an index,
     * y * width + x), or [limit] + 1 if that is more than [limit]. [Int.MAX_VALUE] if it can never
     * be captured.
     *
     * A capture needs the attacker's dots to wall the target off from the border: orthogonal
     * steps from the target must not reach the border without crossing one. So the answer is at
     * least the smallest set of empty points that, added to the attacker's live dots, cuts every
     * such path: a minimum vertex cut, found with augmenting paths. Points that hold a dot of the
     * target's side, or that are out of play, can be passed through but not built on.
     *
     * When the answer is at most [limit] and [cut] is given, the points of one smallest such set
     * are marked in it.
     */
    fun toCapture(state: GameState, attacker: Player, target: Int, limit: Int, cut: BooleanArray? = null): Int {
        prepare(state)
        for (i in 0 until cells) {
            val x = i % width
            val y = i / width
            val owner = state.ownerAt(x, y)
            kind[i] = when {
                owner == attacker && !state.isCaptured(x, y) -> WALL
                owner == null && !state.isDead(x, y) -> BUILD
                else -> OPEN
            }
            flowThrough[i] = 0
        }
        flowOnLink.fill(0)
        if (onBorder(target) || openPathToBorder(target)) return Int.MAX_VALUE
        var flow = 0
        while (flow <= limit && augment(target)) flow++
        if (cut != null && flow <= limit) {
            // The last search found no path: the points it could enter but not cross make a minimum cut.
            for (i in 0 until cells) {
                if (kind[i] == BUILD && seenAt[2 * i] == stamp && seenAt[2 * i + 1] != stamp) cut[i] = true
            }
        }
        return flow
    }

    /**
     * The fewest dots [player] must still place to join the dot at [dot] to the border by a chain
     * of its own dots stepping only up, down, left or right (what `Grounding.safe` asks), or
     * [Int.MAX_VALUE] if no such chain can be built.
     */
    fun toSave(state: GameState, player: Player, dot: Int): Int {
        prepare(state)
        val cost = chainCosts(state, player, listOf(dot))
        var best = Int.MAX_VALUE
        for (i in 0 until cells) if (onBorder(i) && cost[i] < best) best = cost[i]
        return best
    }

    /** Marks in [path] the empty points on the cheapest chains that [toSave] counts. */
    fun savePaths(state: GameState, player: Player, dot: Int, path: BooleanArray) {
        prepare(state)
        val fromDot = chainCosts(state, player, listOf(dot))
        val fromBorder = chainCosts(state, player, (0 until cells).filter { onBorder(it) })
        var best = Int.MAX_VALUE
        for (i in 0 until cells) if (onBorder(i) && fromDot[i] < best) best = fromDot[i]
        if (best == Int.MAX_VALUE) return
        for (i in 0 until cells) {
            if (fromDot[i] == Int.MAX_VALUE || fromBorder[i] == Int.MAX_VALUE) continue
            val empty = state.ownerAt(i % width, i / width) == null
            // Both costs count the point itself once.
            if (empty && fromDot[i] + fromBorder[i] - 1 == best) path[i] = true
        }
    }

    /**
     * For each point, the fewest empty points to fill for a straight chain of [player]'s dots from
     * one of [starts] to it, the point itself included; [Int.MAX_VALUE] where no chain can reach.
     */
    private fun chainCosts(state: GameState, player: Player, starts: List<Int>): IntArray {
        val cost = IntArray(cells) { Int.MAX_VALUE }
        val deque = ArrayDeque<Int>()
        fun stepCost(i: Int): Int {
            val x = i % width
            val y = i / width
            val owner = state.ownerAt(x, y)
            return when {
                owner == player && !state.isCaptured(x, y) -> 0
                owner == null && !state.isDead(x, y) -> 1
                else -> -1
            }
        }
        for (start in starts) {
            val c = stepCost(start)
            if (c < 0) continue
            cost[start] = c
            if (c == 0) deque.addFirst(start) else deque.addLast(start)
        }
        while (deque.isNotEmpty()) {
            val i = deque.removeFirst()
            val x = i % width
            val y = i / width
            for (d in 0 until 4) {
                val nx = x + DX[d]
                val ny = y + DY[d]
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
                val j = ny * width + nx
                val step = stepCost(j)
                if (step < 0) continue
                if (cost[i] + step < cost[j]) {
                    cost[j] = cost[i] + step
                    if (step == 0) deque.addFirst(j) else deque.addLast(j)
                }
            }
        }
        return cost
    }

    private fun prepare(state: GameState) {
        if (state.width == width && state.height == height) return
        width = state.width
        height = state.height
        cells = width * height
        kind = ByteArray(cells)
        flowThrough = IntArray(cells)
        flowOnLink = IntArray(4 * cells)
        cameFrom = IntArray(2 * cells)
        seenAt = IntArray(2 * cells)
        queue = IntArray(2 * cells)
        stamp = 0
    }

    private fun onBorder(i: Int): Boolean {
        val x = i % width
        val y = i / width
        return x == 0 || y == 0 || x == width - 1 || y == height - 1
    }

    /** True if the border can be reached from [target] through points nobody can build on: no cut exists. */
    private fun openPathToBorder(target: Int): Boolean {
        stamp++
        var head = 0
        var tail = 0
        queue[tail++] = target
        seenAt[target] = stamp
        while (head < tail) {
            val i = queue[head++]
            if (onBorder(i)) return true
            val x = i % width
            val y = i / width
            for (d in 0 until 4) {
                val nx = x + DX[d]
                val ny = y + DY[d]
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
                val j = ny * width + nx
                if (kind[j] == OPEN && seenAt[j] != stamp) {
                    seenAt[j] = stamp
                    queue[tail++] = j
                }
            }
        }
        return false
    }

    /**
     * Finds one more unit of flow from [target] to the border and pushes it, or returns false.
     * Each point is two nodes, in (2i) and out (2i + 1), joined by an edge of capacity 1 for a
     * point that can be built on and unbounded for one that cannot; a wall has no edge. Links run
     * from out(i) to in(j) between orthogonal neighbours, unbounded.
     */
    private fun augment(target: Int): Boolean {
        stamp++
        var head = 0
        var tail = 0
        val start = 2 * target
        queue[tail++] = start
        seenAt[start] = stamp
        cameFrom[start] = -1
        var end = -1
        while (head < tail) {
            val node = queue[head++]
            val i = node shr 1
            if ((node and 1) == 0) {
                // in(i): through the point, or back along a link that carries flow into it.
                val through = kind[i] == OPEN || (kind[i] == BUILD && flowThrough[i] == 0)
                if (through && visit(node + 1, node)) queue[tail++] = node + 1
                val x = i % width
                val y = i / width
                for (d in 0 until 4) {
                    val nx = x - DX[d]
                    val ny = y - DY[d]
                    if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
                    val from = ny * width + nx
                    if (flowOnLink[4 * from + d] > 0 && visit(2 * from + 1, node)) queue[tail++] = 2 * from + 1
                }
            } else {
                // out(i) of a border point leads out of the board: a path is found.
                if (onBorder(i)) {
                    end = node
                    break
                }
                // Otherwise on to a neighbour, or back through the point if flow passes through it.
                if (flowThrough[i] > 0 && visit(node - 1, node)) queue[tail++] = node - 1
                val x = i % width
                val y = i / width
                for (d in 0 until 4) {
                    val nx = x + DX[d]
                    val ny = y + DY[d]
                    if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
                    val j = ny * width + nx
                    if (kind[j] != WALL && visit(2 * j, node)) queue[tail++] = 2 * j
                }
            }
        }
        if (end < 0) return false

        var node = end
        while (cameFrom[node] >= 0) {
            val prev = cameFrom[node]
            val a = prev shr 1
            val b = node shr 1
            when {
                a == b && (prev and 1) == 0 -> flowThrough[a]++ // in -> out
                a == b -> flowThrough[a]-- // out -> in, undoing flow
                (prev and 1) == 1 -> flowOnLink[4 * a + direction(a, b)]++ // out(a) -> in(b)
                else -> flowOnLink[4 * b + direction(b, a)]-- // in(a) -> out(b), undoing flow on b -> a
            }
            node = prev
        }
        return true
    }

    private fun visit(node: Int, from: Int): Boolean {
        if (seenAt[node] == stamp) return false
        seenAt[node] = stamp
        cameFrom[node] = from
        return true
    }

    private fun direction(from: Int, to: Int): Int {
        for (d in 0 until 4) {
            if (from % width + DX[d] == to % width && from / width + DY[d] == to / width) return d
        }
        error("points $from and $to are not neighbours")
    }

    private companion object {
        const val WALL: Byte = 0
        const val BUILD: Byte = 1
        const val OPEN: Byte = 2
        val DX = intArrayOf(-1, 1, 0, 0)
        val DY = intArrayOf(0, 0, -1, 1)
    }
}
