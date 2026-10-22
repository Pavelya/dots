package dots.ai

import dots.engine.GameState
import dots.engine.Player

/**
 * A group of enemy dots that can be captured, and how soon.
 *
 * @param anchor the index (y * width + x) of one dot of the group
 * @param size how many dots the group has
 * @param cost how many more dots the attacker must place to wall the group in; 1 means one move
 *   from capture
 * @param cells indices of the empty cells of one cheapest way to do it
 */
internal class Threat(val anchor: Int, val size: Int, val cost: Int, val cells: IntArray)

/**
 * Finds, for every group of a player's dots that is not grounded, the fewest dots the opponent
 * needs to place to capture it: a minimum vertex cut between the group and the border.
 *
 * It is the same quantity as the collecting planner's cut ([Cleanup.cheapestEnclosure]), but built
 * for speed inside the search: the grid is the graph (nothing is allocated per call), flows are
 * found by a handful of augmenting paths, and it gives up once a group needs more than `maxCost`
 * dots. Each cell is a node pair (in = 2i, out = 2i + 1) joined by an edge of capacity 1 if it can
 * be built on, unbounded if it is occupied or out of play, and absent if it is already a wall.
 *
 * Not thread-safe: the scratch arrays are reused between calls.
 */
internal class ThreatFinder {
    private var width = 0
    private var height = 0
    private var cells = 0

    private var kind = ByteArray(0)
    private var flowThrough = IntArray(0)
    private var flowOnLink = IntArray(0)
    private var visitedAt = IntArray(0)
    private var cameFrom = IntArray(0)
    private var queue = IntArray(0)
    private var groupSeen = IntArray(0)
    private var groupCells = IntArray(0)
    private var touched = IntArray(0)
    private var touchedCount = 0
    private var stamp = 0
    private var groupStamp = 0

    /** Threats by [attacker] against the groups of [victim] that cost at most [maxCost] dots. */
    fun find(state: GameState, victim: Player, maxCost: Int): List<Threat> {
        if (maxCost <= 0) return emptyList()
        prepare(state, victim)

        val result = ArrayList<Threat>()
        groupStamp++
        for (start in 0 until cells) {
            if (kind[start] != VICTIM || groupSeen[start] == groupStamp) continue
            val size = collectGroup(start)
            if (size == 0) continue // touches the border: grounded
            val threat = cheapestWall(start, size, maxCost)
            if (threat != null) result += threat
        }
        return result
    }

    private fun prepare(state: GameState, victim: Player) {
        if (state.width != width || state.height != height) {
            width = state.width
            height = state.height
            cells = width * height
            kind = ByteArray(cells)
            flowThrough = IntArray(cells)
            flowOnLink = IntArray(4 * cells)
            visitedAt = IntArray(2 * cells)
            cameFrom = IntArray(2 * cells)
            queue = IntArray(2 * cells)
            groupSeen = IntArray(cells)
            groupCells = IntArray(cells)
            touched = IntArray(8 * cells)
            stamp = 0
            groupStamp = 0
        }
        val attacker = victim.opponent
        for (i in 0 until cells) {
            val x = i % width
            val y = i / width
            val owner = state.ownerAt(x, y)
            kind[i] = when {
                owner == attacker && !state.isCaptured(x, y) -> WALL
                owner == victim && !state.isCaptured(x, y) -> VICTIM
                owner == null && !state.isDead(x, y) -> BUILDABLE
                else -> PASSABLE
            }
        }
    }

    /**
     * Gathers the group containing [start] (dots linked orthogonally or diagonally) into
     * [groupCells] and returns its size, or 0 if it touches the border.
     */
    private fun collectGroup(start: Int): Int {
        var size = 0
        var head = 0
        groupCells[size++] = start
        groupSeen[start] = groupStamp
        var grounded = false
        while (head < size) {
            val i = groupCells[head++]
            val x = i % width
            val y = i / width
            if (x == 0 || y == 0 || x == width - 1 || y == height - 1) grounded = true
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
                    val j = ny * width + nx
                    if (kind[j] == VICTIM && groupSeen[j] != groupStamp) {
                        groupSeen[j] = groupStamp
                        groupCells[size++] = j
                    }
                }
            }
        }
        return if (grounded) 0 else size
    }

    private fun cheapestWall(anchor: Int, size: Int, maxCost: Int): Threat? {
        if (canEscapeUnblocked(size)) return null

        var flow = 0
        var exhausted = false // no augmenting path left: the flow is maximal, so it equals the cut
        while (flow <= maxCost) {
            val end = findPath(size)
            if (end < 0) {
                exhausted = true
                break
            }
            augment(end)
            flow++
        }

        // A flow of zero means the group is already walled in; more than maxCost is too far off.
        var threat: Threat? = null
        if (exhausted && flow > 0) {
            val cut = ArrayList<Int>()
            for (i in 0 until cells) {
                if (kind[i] == BUILDABLE && visitedAt[2 * i] == stamp && visitedAt[2 * i + 1] != stamp) cut += i
            }
            threat = Threat(anchor, size, flow, cut.toIntArray())
        }
        resetFlows()
        return threat
    }

    /** True if the group can reach the border through cells nobody can build on. */
    private fun canEscapeUnblocked(size: Int): Boolean {
        stamp++
        var head = 0
        var tail = 0
        for (k in 0 until size) {
            queue[tail++] = groupCells[k]
            visitedAt[2 * groupCells[k]] = stamp
        }
        while (head < tail) {
            val i = queue[head++]
            val x = i % width
            val y = i / width
            if (x == 0 || y == 0 || x == width - 1 || y == height - 1) return true
            for (d in 0 until 4) {
                val j = neighbour(i, x, y, d)
                if (j < 0 || kind[j] == WALL || kind[j] == BUILDABLE || visitedAt[2 * j] == stamp) continue
                visitedAt[2 * j] = stamp
                queue[tail++] = j
            }
        }
        return false
    }

    /**
     * Breadth-first search for an augmenting path from the group to the border in the residual
     * graph. Returns the node it ended on (an out-node on the border), or -1 if there is none, in
     * which case [visitedAt] marks everything still reachable from the group.
     */
    private fun findPath(size: Int): Int {
        stamp++
        var head = 0
        var tail = 0
        for (k in 0 until size) {
            val node = 2 * groupCells[k]
            visitedAt[node] = stamp
            cameFrom[node] = -1
            queue[tail++] = node
        }
        while (head < tail) {
            val u = queue[head++]
            val i = u shr 1
            val x = i % width
            val y = i / width
            if (u and 1 == 0) {
                // in-node: through the cell itself, or back along a link that carries flow into it
                if (kind[i] != BUILDABLE || flowThrough[i] == 0) tail = visit(u, 2 * i + 1, tail)
                for (d in 0 until 4) {
                    val j = neighbour(i, x, y, d)
                    if (j >= 0 && kind[j] != WALL && flowOnLink[4 * j + (d xor 1)] > 0) tail = visit(u, 2 * j + 1, tail)
                }
            } else {
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) return u
                for (d in 0 until 4) {
                    val j = neighbour(i, x, y, d)
                    if (j >= 0 && kind[j] != WALL) tail = visit(u, 2 * j, tail)
                }
                if (flowThrough[i] > 0) tail = visit(u, 2 * i, tail)
            }
        }
        return -1
    }

    private fun visit(from: Int, to: Int, tail: Int): Int {
        if (visitedAt[to] == stamp) return tail
        visitedAt[to] = stamp
        cameFrom[to] = from
        queue[tail] = to
        return tail + 1
    }

    /** Pushes one unit of flow along the path that ends at [end]. */
    private fun augment(end: Int) {
        var v = end
        while (cameFrom[v] != -1) {
            val u = cameFrom[v]
            val iu = u shr 1
            val iv = v shr 1
            val uIn = u and 1 == 0
            val vIn = v and 1 == 0
            when {
                iu == iv -> if (uIn) flowThrough[iu]++ else flowThrough[iu]--
                !uIn && vIn -> flowOnLink[4 * iu + direction(iu, iv)]++
                else -> flowOnLink[4 * iv + direction(iv, iu)]--
            }
            touch(iu)
            touch(iv)
            v = u
        }
    }

    private fun touch(cell: Int) {
        if (touchedCount < touched.size) touched[touchedCount++] = cell
    }

    private fun resetFlows() {
        if (touchedCount >= touched.size) {
            flowThrough.fill(0)
            flowOnLink.fill(0)
        } else {
            for (k in 0 until touchedCount) {
                val i = touched[k]
                flowThrough[i] = 0
                for (d in 0 until 4) flowOnLink[4 * i + d] = 0
            }
        }
        touchedCount = 0
    }

    /** The cell next to [i] in direction [d] (0 left, 1 right, 2 up, 3 down), or -1 off the board. */
    private fun neighbour(i: Int, x: Int, y: Int, d: Int): Int = when (d) {
        0 -> if (x > 0) i - 1 else -1
        1 -> if (x < width - 1) i + 1 else -1
        2 -> if (y > 0) i - width else -1
        else -> if (y < height - 1) i + width else -1
    }

    private fun direction(from: Int, to: Int): Int = when (to - from) {
        -1 -> 0
        1 -> 1
        -width -> 2
        else -> 3
    }

    private companion object {
        const val WALL: Byte = 0
        const val BUILDABLE: Byte = 1
        const val PASSABLE: Byte = 2
        const val VICTIM: Byte = 3
    }
}
