package dots.cli

import dots.campaign.Campaign
import dots.campaign.Level
import dots.campaign.LevelFormat
import dots.campaign.LevelFormatException
import dots.campaign.Mission
import dots.campaign.Solution
import dots.campaign.Solutions
import dots.campaign.Solver
import dots.campaign.Step
import dots.campaign.Verdict
import dots.engine.Action
import dots.engine.Notation
import dots.engine.Pos
import java.io.File
import kotlin.time.Duration.Companion.seconds

/**
 * `cli check-levels`: runs the [Solver] on every puzzle level. A puzzle fails if the computer can
 * stop the player, if the solver cannot decide it within its time cap, or if more than
 * [MAX_FIRST_MOVES] first moves pass it and its file does not say `solutions: open`.
 */
object CheckLevels {
    const val MAX_FIRST_MOVES = 3

    enum class Result { PASSED, FAILED, NOT_CHECKED }

    class Check(val level: Level, val result: Result, val note: String, val solution: Solution? = null, val seconds: Double = 0.0)

    private val USAGE = """Usage: check-levels [file...] [--seconds N]
  Without files, checks every campaign level. N is the time cap per level (default ${Solver.TIME_LIMIT.inWholeSeconds})."""

    /** Checks the levels named in [args] (default: the campaign) and prints a line for each; returns the failures. */
    fun run(args: List<String>): Int {
        var seconds = Solver.TIME_LIMIT.inWholeSeconds.toInt()
        val files = ArrayList<String>()
        var i = 0
        while (i < args.size) {
            when (val arg = args[i]) {
                "--seconds" -> seconds = args.getOrNull(++i)?.toIntOrNull()?.takeIf { it >= 1 }
                    ?: exitWith("--seconds needs a whole number of seconds, 1 or more")
                else -> if (arg.startsWith("-")) exitWith(USAGE) else files += arg
            }
            i++
        }
        val levels = if (files.isEmpty()) Campaign.levels else files.map { read(it) }
        val solver = Solver(timeLimit = seconds.seconds, firstMovesLimit = MAX_FIRST_MOVES + 1)

        val checks = levels.map { level -> check(level, solver).also { println(line(it)) } }
        val failed = checks.count { it.result == Result.FAILED }
        val passed = checks.count { it.result == Result.PASSED }
        val skipped = checks.count { it.result == Result.NOT_CHECKED }
        println("\n${count(checks.size, "level")}: $passed passed, $failed failed, $skipped not checked.")
        return failed
    }

    /** Solves [level] if it is a puzzle and judges the answer. */
    fun check(level: Level, solver: Solver = Solver(firstMovesLimit = MAX_FIRST_MOVES + 1)): Check {
        if (!solver.isPuzzle(level.mission)) {
            val why = when (level.mission) {
                Mission.Win, is Mission.WinBy, is Mission.WinWithFewerDots -> "a whole game, not a puzzle"
                else -> "longer than ${solver.maxMoves} moves, not a puzzle"
            }
            return Check(level, Result.NOT_CHECKED, "not checked: $why")
        }
        level.rule?.takeIf { it.turnRule != null }?.let {
            return Check(level, Result.NOT_CHECKED, "not checked: the solver does not play the rule '${it.text}'")
        }

        val started = System.nanoTime()
        val solution = solver.solve(level)
        val seconds = (System.nanoTime() - started) / 1e9
        fun check(result: Result, note: String) = Check(level, result, note, solution, seconds)

        return when (solution.verdict) {
            Verdict.SOLVED -> {
                val first = solution.winningFirstMoves
                val counted = (if (solution.allFirstMovesCounted) "" else "at least ") + count(first.size, "first move")
                val summary = "passes in ${moves(solution.shortest!!)}: ${line(solution.line)}; " +
                    "$counted pass" + (if (first.size == 1) "es" else "") + ": ${points(first)}"
                if (first.size > MAX_FIRST_MOVES && level.solutions == Solutions.FEW) {
                    check(
                        Result.FAILED,
                        "too easy: $summary. A puzzle allows $MAX_FIRST_MOVES unless its file says 'solutions: ${Solutions.OPEN.text}'",
                    )
                } else {
                    check(Result.PASSED, summary)
                }
            }
            Verdict.UNSOLVABLE -> check(Result.FAILED, "unsolvable: the computer can stop every try within ${moves(solution.depth)}")
            Verdict.UNDECIDED -> check(
                Result.NOT_CHECKED,
                "not checked: neither side can force the mission within ${moves(solution.depth)}, so it plays as a longer game",
            )
            Verdict.TIMED_OUT -> check(
                Result.FAILED,
                "not decided within ${solver.timeLimit.inWholeSeconds} s: make the board or the mission smaller",
            )
        }
    }

    private fun read(path: String): Level {
        val file = File(path)
        if (!file.isFile) exitWith("No level file $path")
        return try {
            LevelFormat.parse(file.readText(), file.path)
        } catch (e: LevelFormatException) {
            exitWith(e.message!!)
        }
    }

    private fun line(check: Check): String {
        val mark = when (check.result) {
            Result.PASSED -> "ok    "
            Result.FAILED -> "FAILED"
            Result.NOT_CHECKED -> "-     "
        }
        val time = if (check.solution == null) "" else "  (%.1f s, %,d positions)".format(check.seconds, check.solution.positions)
        return "%-5s %-12s %-22s %s  %s%s".format(check.level.code, check.level.name, check.level.mission.text, mark, check.note, time)
    }

    /** A line of play from the player's first move: the player's moves, the computer's replies in brackets. */
    fun line(steps: List<Step>): String = steps.joinToString(" ") { step ->
        val move = when (val action = step.action) {
            is Action.Place -> Notation.name(action.pos)
            else -> "pass"
        }
        if (step.player == steps.first().player) move else "($move)"
    }

    /** At most [SHOWN] points by name, then "...". */
    private fun points(list: List<Pos>): String =
        list.take(SHOWN).joinToString(" ") { Notation.name(it) } + if (list.size > SHOWN) " ..." else ""

    private const val SHOWN = 8

    private fun count(n: Int, word: String) = if (n == 1) "1 $word" else "$n ${word}s"

    private fun moves(n: Int) = count(n, "move")
}
