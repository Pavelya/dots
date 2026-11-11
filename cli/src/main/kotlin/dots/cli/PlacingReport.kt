package dots.cli

import dots.log.GameLogEntry
import java.io.File
import java.util.Locale

/**
 * `cli placing-report <log>...`: compares the ways of placing a dot (A-13) from the logs testers
 * share, one file per tester, named after the tester. For each method it gives the two checks of
 * A-13: undone moves per game against the computer (the POC's H5 measure of misplaced moves) and
 * taps per dot. Games logged before the method was recorded are left out.
 */
object PlacingReport {

    /** One method's numbers, from the games played with it. */
    data class Numbers(
        val method: String,
        val games: Int,
        val gamesVsComputer: Int,
        val undos: Int,
        val touches: Int,
        val placed: Int,
    ) {
        val undosPerGame: Double? get() = if (gamesVsComputer == 0) null else undos.toDouble() / gamesVsComputer
        val tapsPerDot: Double? get() = if (placed == 0) null else touches.toDouble() / placed
    }

    /** The numbers of each method found in [games], in the order the methods first appear. */
    fun numbers(games: List<GameLogEntry>): List<Numbers> =
        games.filter { it.placing.isNotEmpty() }.groupBy { it.placing }.map { (method, list) ->
            val vsComputer = list.filter { it.vsComputer }
            Numbers(
                method = method,
                games = list.size,
                gamesVsComputer = vsComputer.size,
                undos = vsComputer.sumOf { it.undos },
                touches = list.sumOf { it.touches },
                placed = list.sumOf { it.placed },
            )
        }

    fun run(paths: List<String>): Int {
        if (paths.isEmpty()) {
            println("usage: placing-report <log file>...   (one shared log per tester; the file name names the tester)")
            return 1
        }
        var problems = 0
        val testers = paths.mapNotNull { path ->
            val file = File(path)
            if (!file.isFile) {
                println("$path: no such file")
                problems++
                return@mapNotNull null
            }
            val entries = GameLogEntry.parseLog(file.readText()) { line, problem ->
                println("$path line $line: unreadable - $problem")
                problems++
            }
            PocReport.Tester(file.nameWithoutExtension, entries)
        }
        println(format(testers))
        return problems
    }

    fun format(testers: List<PocReport.Tester>): String = buildString {
        appendLine("Placing report: ${testers.size} testers")
        for (tester in testers) {
            appendLine()
            appendLine("${tester.name}:")
            appendTable(numbers(tester.games))
        }
        appendLine()
        appendLine("Everyone:")
        appendTable(numbers(testers.flatMap { it.games }))
    }

    private fun StringBuilder.appendTable(rows: List<Numbers>) {
        if (rows.isEmpty()) {
            appendLine("  no games with a placing method")
            return
        }
        appendLine("  %-12s %6s %12s %10s".format("method", "games", "undos/game", "taps/dot"))
        for (n in rows) {
            appendLine("  %-12s %6d %12s %10s".format(n.method, n.games, n.undosPerGame.fmt(), n.tapsPerDot.fmt()))
        }
    }

    private fun Double?.fmt() = this?.let { String.format(Locale.ROOT, "%.2f", it) } ?: "-"
}
