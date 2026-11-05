package dots.cli

import dots.engine.Player
import dots.log.GameLogEntry
import dots.log.Outcome
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * `cli poc-report <log>...`: the log-based numbers of the POC test week (docs/poc/test-protocol.md),
 * one shared log file per tester, named after the tester. Prints each tester, then everyone, then
 * the log-based part of each hypothesis in the charter (docs/poc/README.md); the survey supplies
 * the rest.
 */
object PocReport {

    /** One tester's games, read from their shared log. */
    class Tester(val name: String, entries: List<GameLogEntry>) {
        /** A tester may share their log twice; the second file repeats the first, so games are counted once. */
        val games: List<GameLogEntry> = entries.distinctBy { it.startedAt to it.record.toText() }
        val finished = games.filter { it.outcome == Outcome.FINISHED }
        val abandoned = games.count { it.outcome == Outcome.ABANDONED }

        /** Days (UTC) on which the tester started at least one game. */
        val daysPlayed: Int = games.map { LocalDate.ofInstant(Instant.ofEpochMilli(it.startedAt), ZoneOffset.UTC) }.distinct().size
    }

    /** Wins, losses and draws of the human (red) against one computer difficulty, finished games only. */
    data class Record(val wins: Int, val losses: Int, val draws: Int) {
        val games get() = wins + losses + draws
        val winRate: Double get() = if (games == 0) 0.0 else wins.toDouble() / games
    }

    data class Numbers(
        val games: Int,
        val finished: Int,
        val abandoned: Int,
        val medianMinutes: Double?,
        val winnerShare: Double?,
        val vsComputer: Map<String, Record>,
        val undosPerGame: Double?,
        val capturesWithThreats: Double?,
        val capturesWithoutThreats: Double?,
    )

    fun numbers(games: List<GameLogEntry>): Numbers {
        val finished = games.filter { it.outcome == Outcome.FINISHED }
        val vsComputer = finished.filter { it.vsComputer }.groupBy { it.opponent }.mapValues { (_, list) ->
            Record(
                wins = list.count { it.winner == Player.RED },
                losses = list.count { it.winner == Player.BLUE },
                draws = list.count { it.winner == null },
            )
        }.toSortedMap(compareBy { DIFFICULTY_ORDER.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } })
        val computerGames = games.filter { it.vsComputer }
        fun meanCaptures(threats: Boolean) = finished.filter { it.threatHighlight == threats }
            .map { (it.red + it.blue).toDouble() }.takeIf { it.isNotEmpty() }?.average()
        return Numbers(
            games = games.size,
            finished = finished.size,
            abandoned = games.count { it.outcome == Outcome.ABANDONED },
            medianMinutes = median(finished.map { it.playMillis / 60_000.0 }),
            winnerShare = if (finished.isEmpty()) null else finished.count { it.winner != null }.toDouble() / finished.size,
            vsComputer = vsComputer,
            undosPerGame = if (computerGames.isEmpty()) null else computerGames.sumOf { it.undos }.toDouble() / computerGames.size,
            capturesWithThreats = meanCaptures(threats = true),
            capturesWithoutThreats = meanCaptures(threats = false),
        )
    }

    /** The log-based verdict for one hypothesis: PASS, FAIL, or "no data". */
    data class Verdict(val hypothesis: String, val mark: String, val detail: String)

    fun verdicts(testers: List<Tester>): List<Verdict> {
        val all = numbers(testers.flatMap { it.games })
        fun mark(pass: Boolean?) = when (pass) {
            true -> "PASS"
            false -> "FAIL"
            null -> "no data"
        }
        val threeDays = testers.count { it.daysPlayed >= 3 }
        val median = all.medianMinutes
        val share = all.winnerShare
        // H4: a tester "found a difficulty" if they won 20% to 70% of at least 3 games at it.
        val fitting = testers.count { tester ->
            numbers(tester.games).vsComputer.values.any { it.games >= 3 && it.winRate in 0.2..0.7 }
        }
        val on = all.capturesWithThreats
        val off = all.capturesWithoutThreats
        return listOf(
            Verdict(
                "H1 people come back",
                mark(if (testers.isEmpty()) null else threeDays * 2 >= testers.size),
                "$threeDays of ${testers.size} testers played on 3 or more days (needs at least half; the survey adds \"I want to keep playing\")",
            ),
            Verdict(
                "H3 the format is right",
                mark(if (median == null || share == null) null else median in 4.0..10.0 && share >= 0.8),
                "median game ${median.fmt(1)} min (needs 4 to 10), ${share.percent()} of games have a winner (needs 80%); the survey adds the length rating",
            ),
            Verdict(
                "H4 a fun difficulty exists",
                mark(if (testers.isEmpty()) null else fitting == testers.size),
                "$fitting of ${testers.size} testers won 20% to 70% of at least 3 games at some difficulty (needs every tester)",
            ),
            Verdict(
                "H5 placing by finger works",
                mark(all.undosPerGame?.let { it < 1.0 }),
                "${all.undosPerGame.fmt(2)} undos per game against the computer (needs under 1); the survey adds annoyances",
            ),
            Verdict(
                "H6 showing threats helps",
                mark(if (on == null || off == null) null else on >= off),
                "captures per game ${on.fmt(1)} with threats shown, ${off.fmt(1)} without (needs not lower with them); the survey adds whether it helped",
            ),
        )
    }

    fun run(paths: List<String>): Int {
        if (paths.isEmpty()) {
            println("usage: poc-report <log file>...   (one shared log per tester; the file name names the tester)")
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
            for (entry in entries) {
                entry.check()?.let {
                    println("$path: game started ${Instant.ofEpochMilli(entry.startedAt)} does not replay: $it")
                    problems++
                }
            }
            Tester(file.nameWithoutExtension, entries)
        }
        println(format(testers))
        println(if (problems == 0) "All games replay consistently." else "$problems problems in the logs (see above); numbers include the readable games.")
        return problems
    }

    fun format(testers: List<Tester>): String = buildString {
        appendLine("POC report: ${testers.size} testers")
        appendLine()
        for (tester in testers) {
            appendLine("${tester.name}: played on ${tester.daysPlayed} days (UTC)")
            appendNumbers(numbers(tester.games))
            appendLine()
        }
        appendLine("Everyone:")
        appendNumbers(numbers(testers.flatMap { it.games }))
        appendLine()
        appendLine("Hypotheses, log-based part:")
        for (verdict in verdicts(testers)) appendLine("  %-28s %-8s %s".format(verdict.hypothesis, verdict.mark, verdict.detail))
    }

    private fun StringBuilder.appendNumbers(n: Numbers) {
        appendLine("  games: ${n.games} (${n.finished} finished, ${n.abandoned} abandoned)")
        appendLine("  median game length: ${n.medianMinutes.fmt(1)} min; games with a winner: ${n.winnerShare.percent()}")
        if (n.vsComputer.isEmpty()) {
            appendLine("  against the computer: no finished games")
        } else {
            for ((difficulty, r) in n.vsComputer) {
                appendLine("  against $difficulty: ${r.wins} won, ${r.losses} lost, ${r.draws} drawn (win rate ${r.winRate.percent()})")
            }
        }
        appendLine("  undos per game against the computer: ${n.undosPerGame.fmt(2)}")
        appendLine("  captures per finished game: ${n.capturesWithThreats.fmt(1)} with threats shown, ${n.capturesWithoutThreats.fmt(1)} without")
    }

    private val DIFFICULTY_ORDER = listOf("easy", "normal", "hard")

    private fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }

    private fun Double?.fmt(decimals: Int) = this?.let { "%.${decimals}f".format(it) } ?: "-"

    private fun Double?.percent() = this?.let { "%.0f%%".format(it * 100) } ?: "-"
}
