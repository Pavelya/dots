package dots.daily

import dots.campaign.LevelMap
import dots.campaign.Mission
import dots.campaign.Solver
import dots.campaign.Verdict
import kotlin.time.Duration

/**
 * Makes the daily puzzle of a day. The day is the seed: from it the generator draws the mission
 * (what to do and in how many moves), then small fights of red and blue dots, one after another,
 * and keeps the first fight that the [Solver] rates as a fair puzzle for that mission ([problem]):
 * one first move passes it, in exactly that many moves, whatever the computer does. The same day
 * gives the same puzzle on every machine: the random numbers are the generator's own, and each
 * search stops after a count of positions, not a time.
 */
object DailyGenerator {
    /** The fewest and the most moves of a daily puzzle. */
    val MOVES = 2..4

    /** How many positions each search of a candidate may look at; a candidate that needs more is dropped. */
    const val POSITION_LIMIT = 100_000L

    /** The puzzle of [day]. Takes from a fraction of a second to a few seconds. */
    fun generate(day: Day): DailyPuzzle {
        val random = SplitMix64(seed(day))
        val mission = mission(random)
        val solver = Solver(timeLimit = Duration.INFINITE, firstMovesLimit = 2, positionLimit = POSITION_LIMIT)
        while (true) {
            val map = fight(random) ?: continue
            val puzzle = puzzle(day, map, mission) ?: continue
            if (problem(puzzle, solver) == null) return puzzle
        }
    }

    /**
     * Why [puzzle] is not a fair daily puzzle, or null if it is: the [solver] must find that the
     * player passes whatever the computer does, in [DailyPuzzle.best] moves and no fewer, with
     * exactly one first move.
     */
    fun problem(puzzle: DailyPuzzle, solver: Solver = Solver(firstMovesLimit = 2)): String? {
        if (puzzle.best !in MOVES) return "it takes ${puzzle.best} moves, not ${MOVES.first} to ${MOVES.last}"
        val solution = solver.solve(puzzle.level)
        val shortest = solution.shortest
        return when {
            solution.verdict != Verdict.SOLVED -> "the solver's verdict is ${solution.verdict.name.lowercase()}"
            shortest != puzzle.best -> "it passes in ${if (shortest == 1) "1 move" else "$shortest moves"}, not ${puzzle.best}"
            solution.winningFirstMoves.size != 1 -> "more than one first move passes it"
            else -> null
        }
    }

    /** The seed of [day]'s random numbers. */
    internal fun seed(day: Day): Long = SplitMix64.mix(day.epochDay xor SALT)

    /**
     * The day's mission: "capture 1" on [CAPTURE_ONE] days in 100, "capture 2" on [CAPTURE_TWO],
     * "survive" on the rest; 2, 3 or 4 moves, each as often.
     */
    private fun mission(random: SplitMix64): Mission {
        val roll = random.nextInt(100)
        val moves = MOVES.first + random.nextInt(MOVES.last - MOVES.first + 1)
        return when {
            roll < CAPTURE_ONE -> Mission.Capture(1, moves)
            roll < CAPTURE_ONE + CAPTURE_TWO -> Mission.Capture(2, moves)
            else -> Mission.Survive(moves)
        }
    }

    /** The puzzle, or null if the rules refuse it (for instance, its mission is decided before the first move). */
    private fun puzzle(day: Day, map: LevelMap, mission: Mission): DailyPuzzle? = try {
        DailyPuzzle(day, map, mission)
    } catch (_: IllegalArgumentException) {
        null
    }

    /**
     * A board of 6 to 8 points each way with 7 to 12 dots in one tangle away from the border: each
     * new dot goes next to one already placed (diagonals included), red or blue at random. Null if
     * one side has fewer than [MIN_EACH] dots.
     */
    private fun fight(random: SplitMix64): LevelMap? {
        val width = 6 + random.nextInt(3)
        val height = 6 + random.nextInt(3)
        val count = 7 + random.nextInt(6)
        val board = Array(height) { CharArray(width) { LevelMap.EMPTY } }
        val placed = ArrayList<Pair<Int, Int>>()
        fun inside(x: Int, y: Int) = x in 1..width - 2 && y in 1..height - 2
        val firstX = 1 + random.nextInt(width - 2)
        val firstY = 1 + random.nextInt(height - 2)
        board[firstY][firstX] = if (random.nextInt(2) == 0) LevelMap.HUMAN else LevelMap.COMPUTER
        placed += firstX to firstY
        var tries = 0
        while (placed.size < count && tries++ < count * 20) {
            val (x0, y0) = placed[random.nextInt(placed.size)]
            val step = random.nextInt(8)
            val x = x0 + DX[step]
            val y = y0 + DY[step]
            val color = if (random.nextInt(2) == 0) LevelMap.HUMAN else LevelMap.COMPUTER
            if (!inside(x, y) || board[y][x] != LevelMap.EMPTY) continue
            board[y][x] = color
            placed += x to y
        }
        val rows = board.map { String(it) }
        val reds = rows.sumOf { row -> row.count { it == LevelMap.HUMAN } }
        val blues = rows.sumOf { row -> row.count { it == LevelMap.COMPUTER } }
        if (reds < MIN_EACH || blues < MIN_EACH) return null
        return LevelMap(rows)
    }

    /** Out of 100 days, how many are "capture 1" and "capture 2"; the rest are "survive". */
    private const val CAPTURE_ONE = 45
    private const val CAPTURE_TWO = 15
    private const val MIN_EACH = 3

    /** Mixed into every day's seed, so that the puzzles are not those of any other use of SplitMix64. */
    private const val SALT = 0x50696E666F6C64L

    private val DX = intArrayOf(-1, 0, 1, -1, 1, -1, 0, 1)
    private val DY = intArrayOf(-1, -1, -1, 0, 0, 1, 1, 1)
}

/**
 * SplitMix64, a small random number generator. Its numbers depend only on the seed, so they are
 * the same on every machine and every Kotlin version, which the daily puzzles need.
 */
internal class SplitMix64(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += GOLDEN
        return mix(state)
    }

    /** A number from 0 to [bound] - 1. */
    fun nextInt(bound: Int): Int {
        require(bound >= 1) { "bound must be 1 or more" }
        return ((nextLong() ushr 1) % bound).toInt()
    }

    companion object {
        private const val GOLDEN = -0x61c8864680b583ebL

        /** SplitMix64's finaliser: spreads the bits of [z] over the whole word. */
        fun mix(z0: Long): Long {
            var z = z0
            z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
            z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
            return z xor (z ushr 31)
        }
    }
}
