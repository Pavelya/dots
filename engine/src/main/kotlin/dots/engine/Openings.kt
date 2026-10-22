package dots.engine

import kotlin.math.abs
import kotlin.random.Random

/**
 * Starting positions. A player who only plays on the border can never be captured, which forces
 * draws, so games usually start from a few dots in the middle. A "cross" is a 2x2 block with the
 * colours alternating; more crosses make for a more active, less drawish game.
 */
object Openings {
    /** One cross in the middle of the board. */
    fun cross(width: Int, height: Int): Map<Pos, Player> =
        block((width - 2) / 2, (height - 2) / 2, redOnDiagonal = true)

    /** Two crosses side by side in the middle of the board. */
    fun doubleCross(width: Int, height: Int): Map<Pos, Player> {
        val left = (width - 4) / 2
        val top = (height - 2) / 2
        return block(left, top, true) + block(left + 2, top, true)
    }

    /**
     * [count] crosses scattered over the middle half of the board, never touching each other so
     * that no chain is ready at the start. Fewer are placed if the board is too small.
     */
    fun randomCrosses(width: Int, height: Int, count: Int = 4, random: Random = Random.Default): Map<Pos, Player> {
        val minX = width / 4
        val maxX = width - width / 4 - 2
        val minY = height / 4
        val maxY = height - height / 4 - 2
        val corners = ArrayList<Pos>()
        var attempts = 0
        while (corners.size < count && attempts++ < 200 && maxX >= minX && maxY >= minY) {
            val corner = Pos(random.nextInt(minX, maxX + 1), random.nextInt(minY, maxY + 1))
            if (corners.none { maxOf(abs(it.x - corner.x), abs(it.y - corner.y)) < 3 }) {
                corners += corner
            }
        }
        return corners.fold(emptyMap()) { dots, corner -> dots + block(corner.x, corner.y, random.nextBoolean()) }
    }

    private fun block(x: Int, y: Int, redOnDiagonal: Boolean): Map<Pos, Player> {
        val a = if (redOnDiagonal) Player.RED else Player.BLUE
        return mapOf(
            Pos(x, y) to a,
            Pos(x + 1, y + 1) to a,
            Pos(x + 1, y) to a.opponent,
            Pos(x, y + 1) to a.opponent,
        )
    }
}
