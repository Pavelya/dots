package dots.campaign

import dots.engine.GameState
import dots.engine.Notation
import dots.engine.Player
import dots.engine.Pos

/** A level file that cannot be read. [line] is 1-based, or null when no single line is at fault. */
class LevelFormatException(val source: String, val line: Int?, val reason: String) :
    IllegalArgumentException(if (line == null) "$source: $reason" else "$source, line $line: $reason")

/**
 * Reads and writes level files. A level file is plain text: one `field: value` line per field, in
 * the order of [FIELDS], then `map:` and the board's rows. [write] always gives that canonical
 * form, and [parse] reads it back to an equal [Level]. The format is described in the README
 * ("Level files").
 */
object LevelFormat {
    val FIELDS = listOf("world", "level", "name", "computer", "dots", "mission", "solutions", "boosters", "rule", "stars")
    private const val MAP = "map"
    private const val NONE = "none"

    fun write(level: Level): String = buildString {
        appendLine("world: ${level.world}")
        appendLine("level: ${level.number}")
        appendLine("name: ${level.name}")
        appendLine("computer: ${level.strength.text}")
        appendLine("dots: ${level.dotsPerPlayer}")
        appendLine("mission: ${level.mission.text}")
        appendLine("solutions: ${level.solutions.text}")
        appendLine("boosters: ${boostersText(level.boosters)}")
        appendLine("rule: ${level.rule?.text ?: NONE}")
        appendLine("stars: ${level.stars?.let { "${it.two} ${it.three}" } ?: NONE}")
        appendLine("$MAP:")
        level.map.rows.forEach { appendLine(it) }
    }

    /** Reads a level from [text]; [source] (usually the file name) starts every error message. */
    fun parse(text: String, source: String = "level"): Level = Parser(text, source).level()

    private fun boostersText(boosters: Map<Booster, Int>): String =
        if (boosters.isEmpty()) NONE
        else Booster.entries.filter { it in boosters }.joinToString(", ") { "${it.text} ${boosters.getValue(it)}" }

    private class Parser(text: String, private val source: String) {
        private val lines = text.split('\n').map { it.trimEnd('\r') }
        private val fields = HashMap<String, Pair<Int, String>>()
        private var mapLine = 0
        private val rows = ArrayList<Pair<Int, String>>()

        fun level(): Level {
            readLines()
            val map = map()
            val mission = field("mission") { mission(it, map) }
            val solutions = field("solutions") { solutions(it) }
            val world = field("world") { number(it, 1..999) }
            val number = field("level") { number(it, 1..99) }
            val name = field("name") { it.ifEmpty { fail("the name is empty") } }
            val strength = field("computer") { strength(it) }
            val dotsPerPlayer = field("dots") { number(it, 1..9999) }
            val boosters = field("boosters") { boosters(it) }
            val rule = field("rule") { rule(it) }
            val stars = field("stars") { stars(it) }
            // The fields are each fine on their own; what Level still refuses is how the mission
            // fits the rest of the level, so the error points at the mission.
            return try {
                Level(world, number, name, strength, dotsPerPlayer, map, mission, boosters, rule, stars, solutions)
            } catch (e: IllegalArgumentException) {
                throw LevelFormatException(source, fields.getValue("mission").first, e.message ?: "the mission does not fit the level")
            }
        }

        private fun readLines() {
            for ((index, raw) in lines.withIndex()) {
                val lineNo = index + 1
                val line = raw.trim()
                if (mapLine > 0) {
                    if (line.isNotEmpty()) rows += lineNo to line
                    continue
                }
                if (line.isEmpty()) continue
                val colon = line.indexOf(':')
                if (colon < 0) throw LevelFormatException(source, lineNo, "expected 'field: value' or 'map:', found '$line'")
                val key = line.substring(0, colon).trim()
                val value = line.substring(colon + 1).trim()
                when {
                    key == MAP -> {
                        if (value.isNotEmpty()) throw LevelFormatException(source, lineNo, "the map's rows start on the line after 'map:'")
                        mapLine = lineNo
                    }
                    key !in FIELDS -> throw LevelFormatException(
                        source, lineNo, "unknown field '$key' (fields: ${FIELDS.joinToString()}, $MAP)",
                    )
                    key in fields -> throw LevelFormatException(
                        source, lineNo, "'$key' appears twice (first on line ${fields.getValue(key).first})",
                    )
                    else -> fields[key] = lineNo to value
                }
            }
            if (mapLine == 0) throw LevelFormatException(source, null, "missing 'map:' and the board's rows")
        }

        /** Parses one field with [read], which reports problems through [fail]. */
        private fun <T> field(key: String, read: Line.(String) -> T): T {
            val (lineNo, value) = fields[key] ?: throw LevelFormatException(source, null, "missing '$key:'")
            return Line(lineNo).read(value)
        }

        private fun map(): LevelMap {
            if (rows.isEmpty()) throw LevelFormatException(source, mapLine, "the map has no rows")
            val width = rows[0].second.length
            for ((lineNo, row) in rows) {
                val line = Line(lineNo)
                if (':' in row) line.fail("fields must come before 'map:'")
                row.indexOfFirst { it !in LevelMap.SYMBOLS }.takeIf { it >= 0 }?.let {
                    line.fail("unknown map symbol '${row[it]}' in column ${it + 1} (symbols: ${LevelMap.SYMBOLS.toList().joinToString(" ")})")
                }
                if (row.length != width) line.fail("this row has ${row.length} points, the first row has $width")
            }
            val first = Line(mapLine)
            if (width !in LevelMap.SIZES || rows.size !in LevelMap.SIZES) {
                first.fail("the board is ${width}x${rows.size}; boards are 3 to 99 points in each direction")
            }
            val map = LevelMap(rows.map { it.second })
            try {
                GameState.start(map.width, map.height, dots = map.startDots())
            } catch (e: IllegalArgumentException) {
                first.fail(e.message ?: "the starting position is not valid")
            }
            return map
        }

        private inner class Line(val number: Int) {
            fun fail(reason: String): Nothing = throw LevelFormatException(source, number, reason)

            fun number(text: String, range: IntRange): Int {
                val n = text.toIntOrNull() ?: fail("'$text' is not a whole number")
                if (n !in range) fail("$n is not between ${range.first} and ${range.last}")
                return n
            }

            fun strength(text: String): Strength = Strength.entries.firstOrNull { it.text == text }
                ?: fail("unknown computer strength '$text' (${Strength.entries.joinToString { it.text }})")

            fun mission(text: String, map: LevelMap): Mission {
                val words = text.split(Regex("\\s+"))
                fun count(i: Int) = number(words[i], 1..9999)
                fun point(owner: Char, whose: String): Pos {
                    val pos = Notation.parse(words[1]) ?: fail("'${words[1]}' is not a point like e5")
                    if (map[pos] != owner) fail("${words[1]} must hold a $whose dot ('$owner' on the map)")
                    return pos
                }
                val moves = words.lastOrNull() == "moves" || words.lastOrNull() == "move"
                return when {
                    text == Mission.Win.text -> Mission.Win
                    words.size == 4 && words[0] == "win" && words[1] == "with" && words[3] in setOf("dot", "dots") ->
                        Mission.WinWithFewerDots(count(2))
                    words.size == 3 && words[0] == "win" && words[1] == "by" -> Mission.WinBy(count(2))
                    words.size == 5 && words[0] == "capture" && words[2] == "in" && moves -> Mission.Capture(count(1), count(3))
                    words.size == 3 && words[0] == "survive" && moves -> Mission.Survive(count(1))
                    words.size == 2 && words[0] == "rescue" -> Mission.Rescue(point(LevelMap.HUMAN, "human"))
                    words.size == 2 && words[0] == "target" -> Mission.Target(point(LevelMap.COMPUTER, "computer"))
                    else -> fail(
                        "unknown mission '$text' (win; win by 3; win with 8 dots; capture 5 in 8 moves; " +
                            "survive 6 moves; rescue e5; target e5)",
                    )
                }
            }

            fun solutions(text: String): Solutions = Solutions.entries.firstOrNull { it.text == text }
                ?: fail("unknown solutions '$text' (${Solutions.entries.joinToString { it.text }})")

            fun boosters(text: String): Map<Booster, Int> {
                if (text == NONE) return emptyMap()
                val boosters = LinkedHashMap<Booster, Int>()
                for (part in text.split(',').map { it.trim() }) {
                    val words = part.split(Regex("\\s+"))
                    if (words.size != 2) fail("write each booster as a name and a count, like 'wall 2', not '$part'")
                    val booster = Booster.entries.firstOrNull { it.text == words[0] }
                        ?: fail("unknown booster '${words[0]}' (${Booster.entries.joinToString { it.text }})")
                    if (booster in boosters) fail("'${booster.text}' appears twice")
                    boosters[booster] = number(words[1], 1..99)
                }
                return boosters
            }

            fun rule(text: String): SpecialRule? {
                if (text == NONE) return null
                val match = Regex("double move every (\\S+) turns").matchEntire(text)
                    ?: fail("unknown special rule '$text' (none; double move every 5 turns)")
                return SpecialRule.DoubleMove(number(match.groupValues[1], 2..999))
            }

            fun stars(text: String): Stars? {
                if (text == NONE) return null
                val words = text.split(Regex("\\s+"))
                if (words.size != 2) fail("stars are 'none' or two scores, for two and for three stars, like '3 6'")
                val two = number(words[0], 0..9999)
                val three = number(words[1], 0..9999)
                if (three <= two) fail("the three-star score ($three) must be higher than the two-star score ($two)")
                return Stars(two, three)
            }
        }
    }
}
