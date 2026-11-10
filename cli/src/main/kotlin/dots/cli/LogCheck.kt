package dots.cli

import dots.campaign.LevelRecord
import dots.engine.Player
import dots.log.GameLogEntry
import dots.log.Outcome
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * `cli log-check <file>...`: replays every game in exported app logs (docs/poc/test-protocol.md)
 * and prints each game with its result, marking any game whose logged result the replay does not
 * reproduce. Returns the number of problems (unreadable lines and mismatches).
 */
object LogCheck {
    private val time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC)

    fun run(paths: List<String>): Int {
        if (paths.isEmpty()) {
            println("usage: log-check <log file>...   (the file a tester shared from the app)")
            return 1
        }
        var games = 0
        var problems = 0
        for (path in paths) {
            val file = File(path)
            if (!file.isFile) {
                println("$path: no such file")
                problems++
                continue
            }
            println(path)
            val entries = GameLogEntry.parseLog(file.readText()) { line, problem ->
                println("  line $line: unreadable - $problem")
                problems++
            }
            for ((index, entry) in entries.withIndex()) {
                val issue = entry.check()
                if (issue != null) problems++
                println("  %3d  %s".format(index + 1, describe(entry)) + (issue?.let { "  MISMATCH: $it" } ?: "  ok"))
            }
            games += entries.size
        }
        println()
        println("$games games replayed, " + if (problems == 0) "all consistent" else "$problems with problems")
        return problems
    }

    private fun describe(entry: GameLogEntry): String {
        val start = entry.record.start()
        val record = entry.record
        val result = when (entry.outcome) {
            Outcome.FINISHED -> entry.winner?.let { "${it.name.lowercase()} wins" } ?: "draw"
            else -> entry.outcome.name.lowercase()
        }
        val level = if (record is LevelRecord) "  level ${record.level.code}" else ""
        return "%s UTC  %-6s  %dx%d %2d dots  %-9s  red %2d - blue %2d  %3d moves  %4.1f min  %d undos%s".format(
            time.format(Instant.ofEpochMilli(entry.startedAt)),
            entry.opponent,
            start.width,
            start.height,
            start.dotsLeft(Player.RED) ?: 0,
            result,
            entry.red,
            entry.blue,
            entry.record.actions.size,
            entry.playMillis / 60_000.0,
            entry.undos,
            level,
        )
    }
}
