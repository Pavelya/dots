package dots.engine

enum class Player(val symbol: Char) {
    RED('R'),
    BLUE('B');

    val opponent: Player get() = if (this == RED) BLUE else RED

    internal val code: Byte get() = (ordinal + 1).toByte()
}

/** An intersection of the grid; (0, 0) is the top-left corner, x grows right, y grows down. */
data class Pos(val x: Int, val y: Int)
