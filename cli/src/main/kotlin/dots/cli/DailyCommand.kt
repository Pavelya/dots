package dots.cli

import dots.daily.Day
import dots.daily.DailyFormat
import dots.daily.DailyGenerator
import dots.daily.DailyPuzzle
import java.io.File

/**
 * `cli daily`: makes daily puzzles with [DailyGenerator]. `daily 2027 file` writes every day of
 * 2027 to the file; `daily 2027-03-14` prints one day's puzzle.
 */
object DailyCommand {
    private const val USAGE = """Usage: daily <year> <file>    write the puzzle of every day of the year to the file
       daily <date>             print one day's puzzle (date like 2027-03-14)"""

    fun run(args: List<String>) {
        val what = args.firstOrNull() ?: exitWith(USAGE)
        val day = Day.parse(what)
        if (day != null && args.size == 1) {
            print(DailyFormat.write(listOf(DailyGenerator.generate(day))))
            return
        }
        val year = what.toIntOrNull()?.takeIf { it in 1..9999 } ?: exitWith(USAGE)
        val file = args.getOrNull(1)?.takeIf { args.size == 2 }?.let(::File) ?: exitWith(USAGE)
        val started = System.nanoTime()
        val puzzles = year(year) { done, total -> System.err.print("\r$done of $total days") }
        file.parentFile?.mkdirs()
        file.writeText(DailyFormat.write(puzzles))
        val seconds = (System.nanoTime() - started) / 1e9
        System.err.println()
        println("Wrote ${puzzles.size} puzzles to ${file.path} in %.0f s.".format(seconds))
        for ((mission, count) in puzzles.groupingBy { it.mission.text }.eachCount().toSortedMap()) {
            println("  %-24s %d".format(mission, count))
        }
    }

    /** The puzzles of every day of [year], made in parallel; [progress] hears of each one done. */
    fun year(year: Int, progress: (done: Int, total: Int) -> Unit = { _, _ -> }): List<DailyPuzzle> {
        val days = Day.allOf(year)
        val done = java.util.concurrent.atomic.AtomicInteger()
        return days.parallelStream().map { day ->
            DailyGenerator.generate(day).also { progress(done.incrementAndGet(), days.size) }
        }.toList()
    }
}
