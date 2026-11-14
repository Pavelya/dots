package dots.engine

/**
 * A captured area as it lies on the board now, for drawing: the [region] [by] has enclosed (every
 * point inside the ring, empty or not, top to bottom and left to right) and the [ring] of [by]'s
 * dots around it.
 */
class Area(val by: Player, val region: List<Pos>) {
    /**
     * [by]'s dots around the [region], each next to the one before it, clockwise as seen on screen,
     * starting at the ring dot nearest the region's first point. The ring is traced as
     * [Capture.ring] traces it.
     */
    val ring: List<Pos> by lazy { Ring.around(region, region.first()) }
}

/**
 * The captured areas on the board, ordered by their first point. Each is one connected piece of
 * points one side has enclosed. When a bigger ring took in an earlier area, the earlier area is
 * part of the bigger one, whoever closed it.
 */
fun GameState.areas(): List<Area> {
    val seen = BooleanArray(width * height)
    val areas = ArrayList<Area>()
    for (y in 0 until height) for (x in 0 until width) {
        val by = enclosedBy(x, y) ?: continue
        if (seen[y * width + x]) continue
        val region = ArrayList<Pos>()
        val queue = ArrayDeque<Pos>()
        queue += Pos(x, y)
        seen[y * width + x] = true
        while (queue.isNotEmpty()) {
            val p = queue.removeFirst()
            region += p
            for ((dx, dy) in NEIGHBOURS) {
                val nx = p.x + dx
                val ny = p.y + dy
                if (!inBounds(nx, ny) || seen[ny * width + nx] || enclosedBy(nx, ny) != by) continue
                seen[ny * width + nx] = true
                queue += Pos(nx, ny)
            }
        }
        areas += Area(by, region.sortedWith(compareBy({ it.y }, { it.x })))
    }
    return areas
}

private val NEIGHBOURS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
