package dots.daily

import dots.campaign.LevelFormat
import dots.campaign.LevelFormatException

/**
 * Reads and writes a file of daily puzzles: one block per day, each starting with its `date:`,
 * then the `computer:`, the `mission:` and the `map:`, written as in a level file:
 *
 *     date: 2027-01-01
 *     computer: hard
 *     mission: capture 1 in 3 moves
 *     map:
 *     .......
 *     ..RB...
 *     ...
 *
 * Each side may place as many dots as the mission has moves ([DailyPuzzle.dots]).
 */
object DailyFormat {
    private const val DATE = "date:"

    /** Fields the puzzle never sets, so that a block reads as a level file. */
    private val FIXED = listOf(
        "world: 1", "level: 1", "name: Daily", "dots: 99", "solutions: few", "boosters: none",
        "rule: none", "stars: none", "holes: none", "note: none",
    )

    fun write(puzzles: List<DailyPuzzle>): String = buildString {
        for ((i, puzzle) in puzzles.withIndex()) {
            if (i > 0) appendLine()
            appendLine("$DATE ${puzzle.day}")
            appendLine("computer: ${puzzle.strength.text}")
            appendLine("mission: ${puzzle.mission.text}")
            appendLine("map:")
            puzzle.map.rows.forEach { appendLine(it) }
        }
    }

    /** Reads the puzzles in [text]; [source] (usually the file name) starts every error message. */
    fun parse(text: String, source: String = "daily"): List<DailyPuzzle> {
        val lines = text.split('\n').map { it.trimEnd('\r') }
        val starts = lines.indices.filter { lines[it].trim().startsWith(DATE) }
        lines.indices.firstOrNull { it < (starts.firstOrNull() ?: lines.size) && lines[it].isNotBlank() }?.let {
            throw LevelFormatException(source, it + 1, "a puzzle starts with its '$DATE' line")
        }
        return starts.mapIndexed { n, start ->
            val end = starts.getOrNull(n + 1) ?: lines.size
            val date = lines[start].trim().removePrefix(DATE).trim()
            val day = Day.parse(date) ?: throw LevelFormatException(source, start + 1, "'$date' is not a date like 2027-01-31")
            val body = lines.subList(start + 1, end)
            val level = try {
                LevelFormat.parse((FIXED + body).joinToString("\n"), source)
            } catch (e: LevelFormatException) {
                // Point at the line of this file, not of the level text built from it.
                val line = e.line?.let { if (it > FIXED.size) start + 1 + it - FIXED.size else start + 1 }
                throw LevelFormatException(source, line, e.reason)
            }
            try {
                DailyPuzzle(day, level.map, level.mission, level.strength)
            } catch (e: IllegalArgumentException) {
                throw LevelFormatException(source, start + 1, e.message ?: "not a daily puzzle")
            }
        }
    }
}
