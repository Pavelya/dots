package dots.engine

/**
 * Groups of a player's live dots, joined orthogonally or diagonally. A group that touches the
 * border is "grounded": it can never be enclosed, so it can never be captured.
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

    private fun isLive(state: GameState, player: Player, x: Int, y: Int) =
        state.ownerAt(x, y) == player && !state.isCaptured(x, y)

    private fun onBorder(state: GameState, pos: Pos) =
        pos.x == 0 || pos.y == 0 || pos.x == state.width - 1 || pos.y == state.height - 1
}
