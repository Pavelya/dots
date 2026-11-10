package dots.engine

import kotlin.math.abs

/**
 * Enemy dots won by [by] when a ring closed: everything the app needs to animate the capture.
 *
 * [dots] are the newly captured dots, [region] is every cell inside the ring (empty cells and
 * dots alike); the region is out of play from now on. [closing] is the dot whose placing closed
 * the ring. [points] is what the capture adds to [by]'s score: one per dot, two for a dot on a
 * bonus point.
 */
class Capture(val by: Player, val dots: List<Pos>, val region: List<Pos>, val closing: Pos, val points: Int = dots.size) {
    /**
     * [by]'s dots around the [region], in drawing order: each is next to the one before it
     * (across or diagonally), and the last joins back to the first. The ring starts at [closing]
     * (or at the ring's dot nearest to it, if [closing] is not on the ring) and runs clockwise as
     * seen on screen.
     *
     * It is the tightest ring: it follows the edge of the region and cuts corners with diagonal
     * links. Where a line of [by]'s dots sticks into the region, the ring may dip in to its first
     * dot but never runs along the line and back. A dot appears twice only where the ring touches
     * itself, around a pocket that [by]'s dots cut off from the region.
     *
     * Worked out when first read, so the computer's search, which never reads it, pays nothing.
     */
    val ring: List<Pos> by lazy { Ring.around(region, closing) }
}

/**
 * What a booster did, for the app to animate. [pos] is the point it was used on: the wall, the
 * hinted point or the bomb's centre; null for a double dot. [removed] are the opponent's dots a
 * bomb took off the board. Boosters never change the score.
 */
class BoosterEffect(val by: Player, val booster: Booster, val pos: Pos?, val removed: List<Pos> = emptyList())

/**
 * The position after a move plus every capture that move triggered, in the order they were
 * resolved, and the [booster] effect when the action was a booster.
 */
class MoveResult(val state: GameState, val captures: List<Capture>, val booster: BoosterEffect? = null)

/** Traces the ring around a captured region. */
internal object Ring {
    /** Directions in clockwise order on screen (y grows down): east, south, west, north. */
    private val DX = intArrayOf(1, 0, -1, 0)
    private val DY = intArrayOf(0, 1, 0, -1)

    /**
     * Walks round the outside edge of [region] with the region on the right-hand side, collecting
     * the cell on the left at each step. Every cell next to the region (up, down, left or right)
     * that is not in it is a dot of the capturer, because the region is everything their dots cut
     * off. Lines of dots that stick into the region show up as a there-and-back (a, b, a) and are
     * removed. The result starts at [start], or the nearest ring dot to it.
     */
    fun around(region: List<Pos>, start: Pos): List<Pos> {
        val inside = region.toHashSet()
        val first = region.minWith(compareBy<Pos>({ it.y }, { it.x }))
        val ring = ArrayList<Pos>()
        var cell = first
        var d = 0 // heading east along the top edge of the topmost, leftmost cell
        do {
            val left = (d + 3) % 4
            val wall = Pos(cell.x + DX[left], cell.y + DY[left])
            if (ring.lastOrNull() != wall) ring += wall
            val ahead = Pos(cell.x + DX[d], cell.y + DY[d])
            val diagonal = Pos(wall.x + DX[d], wall.y + DY[d])
            when {
                diagonal in inside -> { cell = diagonal; d = left } // the edge turns left round the wall
                ahead in inside -> cell = ahead
                else -> d = (d + 1) % 4 // the edge turns right round the cell
            }
        } while (cell != first || d != 0)
        if (ring.size > 1 && ring.first() == ring.last()) ring.removeAt(ring.size - 1)
        removeSpurs(ring)
        val at = ring.indexOf(start).takeIf { it >= 0 }
            ?: ring.indices.minBy { maxOf(abs(ring[it].x - start.x), abs(ring[it].y - start.y)) }
        return ring.subList(at, ring.size) + ring.subList(0, at)
    }

    /** Removes every there-and-back (a, b, a becomes a) from the closed path [ring]. */
    private fun removeSpurs(ring: MutableList<Pos>) {
        var i = 0
        while (ring.size >= 3 && i < ring.size) {
            val n = ring.size
            if (ring[(i + n - 1) % n] == ring[(i + 1) % n]) {
                // Drop the tip b and the second a; remove the higher index first.
                val tip = i
                val again = (i + 1) % n
                ring.removeAt(maxOf(tip, again))
                ring.removeAt(minOf(tip, again))
                i = 0
            } else {
                i++
            }
        }
    }
}
