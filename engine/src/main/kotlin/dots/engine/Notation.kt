package dots.engine

/**
 * Names of board points as players and logs write them: a column and a row, `e5`. Columns run
 * like a spreadsheet's, a to z and then aa, ab, ...; rows count from 1 at the top.
 */
object Notation {
    fun column(x: Int): String = if (x < 26) "${'a' + x}" else "${'a' + (x / 26 - 1)}${'a' + x % 26}"

    fun name(pos: Pos): String = "${column(pos.x)}${pos.y + 1}"

    /** The point named by [text] (`e5`, `ab12`), or null if it is not a point's name. */
    fun parse(text: String): Pos? {
        val match = POINT.matchEntire(text.trim().lowercase()) ?: return null
        val letters = match.groupValues[1]
        val x = if (letters.length == 1) letters[0] - 'a' else (letters[0] - 'a' + 1) * 26 + (letters[1] - 'a')
        val y = match.groupValues[2].toInt() - 1
        return if (y >= 0) Pos(x, y) else null
    }

    private val POINT = Regex("([a-z]{1,2})\\s*(\\d{1,3})")
}
