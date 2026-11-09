package dots.engine

/**
 * Groups of a player's live dots, joined orthogonally or diagonally. A group that touches the
 * border ([GameState.isBorder]) is "grounded": it can never be enclosed, so it can never be
 * captured.
 */
object Grounding {
    fun groups(state: GameState, player: Player): List<List<Pos>> {
        val width = state.width
        val height = state.height
        val seen = BooleanArray(width * height)
        val groups = ArrayList<List<Pos>>()
        for (start in 0 until width * height) {
            if (seen[start] || !isLive(state, player, start % width, start / width)) continue
            val group = ArrayList<Pos>()
            val stack = ArrayDeque<Int>()
            stack.addLast(start)
            seen[start] = true
            while (stack.isNotEmpty()) {
                val i = stack.removeLast()
                val x = i % width
                val y = i / width
                group += Pos(x, y)
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (!state.inBounds(nx, ny) || seen[ny * width + nx]) continue
                        if (isLive(state, player, nx, ny)) {
                            seen[ny * width + nx] = true
                            stack.addLast(ny * width + nx)
                        }
                    }
                }
            }
            groups += group
        }
        return groups
    }

    /** The groups that can still be captured: those not touching the border. */
    fun ungrounded(state: GameState, player: Player): List<List<Pos>> =
        groups(state, player).filter { group -> group.none { onBorder(state, it) } }

    /**
     * [player]'s live dots that can certainly never be captured: those joined to the border by a
     * chain that steps only up, down, left or right, through the player's live dots and points
     * out of play (holes, blocked points, the inside of earlier captures). An enemy ring cannot
     * cross such a chain: nobody can ever place a dot on its points. A ring can cross a diagonal
     * step, so a group that is only joined to the border diagonally may not be safe.
     */
    fun safe(state: GameState, player: Player): Set<Pos> {
        val width = state.width
        val height = state.height
        val reached = HashSet<Pos>()
        val stack = ArrayDeque<Pos>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pos = Pos(x, y)
                if (state.isBorder(x, y) && isLink(state, player, x, y) && reached.add(pos)) stack.addLast(pos)
            }
        }
        while (stack.isNotEmpty()) {
            val (x, y) = stack.removeLast()
            for ((dx, dy) in STRAIGHT) {
                val next = Pos(x + dx, y + dy)
                if (state.inBounds(next.x, next.y) && isLink(state, player, next.x, next.y) && reached.add(next)) {
                    stack.addLast(next)
                }
            }
        }
        return reached.filterTo(HashSet()) { isLive(state, player, it.x, it.y) }
    }

    /** How many of [player]'s dots are live (not captured). */
    fun liveCount(state: GameState, player: Player): Int {
        var count = 0
        for (y in 0 until state.height) {
            for (x in 0 until state.width) if (isLive(state, player, x, y)) count++
        }
        return count
    }

    private val STRAIGHT = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    private fun isLive(state: GameState, player: Player, x: Int, y: Int) =
        state.ownerAt(x, y) == player && !state.isCaptured(x, y)

    /** A point a safe chain may run through: one of [player]'s live dots, or a point out of play. */
    private fun isLink(state: GameState, player: Player, x: Int, y: Int) =
        isLive(state, player, x, y) || state.isDead(x, y)

    private fun onBorder(state: GameState, pos: Pos) = state.isBorder(pos.x, pos.y)
}
