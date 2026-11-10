package dots.daily

import dots.campaign.LevelFormatException
import dots.campaign.Strength
import java.util.concurrent.ConcurrentHashMap

/**
 * The daily puzzles that ship with the game, so the puzzle works offline. They are read from the
 * engine's resources: `daily/2027.txt` holds every day of 2027 ([DailyFormat]), written by
 * `cli daily 2027` ([DailyGenerator]). A day whose year has no file has no puzzle.
 */
object Daily {
    const val FOLDER = "daily"

    /** The day of puzzle 1. */
    val FIRST_DAY = Day(2027, 1, 1)

    /** How the computer defends in every daily puzzle. */
    val STRENGTH = Strength.HARD

    private val years = ConcurrentHashMap<Int, List<DailyPuzzle>>()

    /** The puzzle of [day], or null if no file holds it. */
    fun puzzle(day: Day): DailyPuzzle? = year(day.year).firstOrNull { it.day == day }

    /** The puzzles of [year], in date order; empty if the year has no file. */
    fun year(year: Int): List<DailyPuzzle> = years.getOrPut(year) { load(year, ::resource) }

    /** Players see a puzzle as "Puzzle #12": the number of days since [FIRST_DAY], plus one. */
    fun number(day: Day): Int = (day.epochDay - FIRST_DAY.epochDay + 1).toInt()

    fun fileName(year: Int): String = "$year.txt"

    /**
     * Reads the file of [year] with [read], which gives a file's text by name, or null if there
     * is none. Every puzzle in it must be of that year and come after the one before.
     */
    internal fun load(year: Int, read: (String) -> String?): List<DailyPuzzle> {
        val name = fileName(year)
        val text = read(name) ?: return emptyList()
        val puzzles = DailyFormat.parse(text, name)
        puzzles.firstOrNull { it.day.year != year }?.let { throw LevelFormatException(name, null, "${it.day} is not in $year") }
        puzzles.zipWithNext().firstOrNull { (a, b) -> b.day <= a.day }?.let { (a, b) ->
            throw LevelFormatException(name, null, "${b.day} comes after ${a.day}; puzzles go in date order, one per day")
        }
        return puzzles
    }

    private fun resource(name: String): String? =
        Daily::class.java.getResourceAsStream("/$FOLDER/$name")?.use { it.readBytes().toString(Charsets.UTF_8) }
}
