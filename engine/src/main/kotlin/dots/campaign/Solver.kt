package dots.campaign

import dots.engine.Action
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** What [Solver] found for a level. */
enum class Verdict {
    /** The player can pass the mission whatever the computer does. */
    SOLVED,

    /** The computer can stop the player whatever the player does. */
    UNSOLVABLE,

    /**
     * Neither side can force the mission within the moves searched, and the mission can last
     * longer than that: the solver cannot say.
     */
    UNDECIDED,

    /** The time cap ran out before the search was done. */
    TIMED_OUT,
}

/** One move of a line of play. */
data class Step(val player: Player, val action: Action)

/** What [Solver.solve] reports for a level. */
data class Solution(
    val verdict: Verdict,
    /** The most moves of the player's that were searched. */
    val depth: Int,
    /**
     * The first moves that pass the mission within [depth] moves against any replies, in board
     * order (top row first). Empty unless [Verdict.SOLVED].
     */
    val winningFirstMoves: List<Pos> = emptyList(),
    /**
     * False when the solver stopped counting at [Solver.firstMovesLimit]: more first moves than
     * [winningFirstMoves] may pass.
     */
    val allFirstMovesCounted: Boolean = true,
    /** The fewest moves of the player's that pass the mission against any replies; null unless solved. */
    val shortest: Int? = null,
    /**
     * A shortest pass: the player's moves, each with the computer's reply that holds out longest.
     * Empty unless solved.
     */
    val line: List<Step> = emptyList(),
    /** How many positions the search looked at. */
    val positions: Long = 0,
)

/**
 * Solves the short missions of single-player levels: can the player pass the mission whatever the
 * computer does, which first moves do it, and how fast?
 *
 * It tries every move of the player's against every reply of the computer's: the replies of a
 * perfect opponent. The computer a player meets is weaker, so a level the solver passes can be
 * passed in the game. What it does not try:
 *  - **Points more than two from every live dot, one by one.** Such a point is too far to take
 *    part in a capture within a few moves. One of them stands for all; if it passes the mission,
 *    they all count as passing first moves.
 *  - **A pass by the player**, who always places a dot. The computer may pass, except in
 *    "survive", where passing would only give the player a free move.
 *  - **Anything beyond [maxMoves] of the player's moves.**
 *
 * Lower bounds ([WallCost]) cut lines short. A dot that needs more dots to wall in than the
 * moves left cannot be captured in time, and one that needs more dots to reach the border cannot
 * be made safe in time. In "survive", once no player's dot can be captured in time, the player
 * passes: a dot on the border can never be captured, so there is always a safe move.
 *
 * Not thread-safe; one solver can solve many levels, one after another.
 */
class Solver(
    val maxMoves: Int = MAX_MOVES,
    val timeLimit: Duration = TIME_LIMIT,
    /** The solver stops counting passing first moves once it has found this many. */
    val firstMovesLimit: Int = Int.MAX_VALUE,
) {
    init {
        require(maxMoves >= 1) { "maxMoves must be 1 or more" }
        require(firstMovesLimit >= 1) { "firstMovesLimit must be 1 or more" }
        require(!timeLimit.isNegative()) { "timeLimit must not be negative" }
    }

    /**
     * True for a mission this solver checks: "capture" and "survive" within [maxMoves] moves,
     * "rescue" and "target". The three win missions are whole games, not puzzles.
     */
    fun isPuzzle(mission: Mission): Boolean = when (mission) {
        is Mission.Capture -> mission.moves <= maxMoves
        is Mission.Survive -> mission.moves <= maxMoves
        is Mission.Rescue, is Mission.Target -> true
        Mission.Win, is Mission.WinBy, is Mission.WinWithFewerDots -> false
    }

    /**
     * Searches [level] from its first position, which must be the player's move. A level whose
     * [Level.rule] gives the computer two-dot turns is refused: the search and its lower bounds
     * count one computer dot per turn. The shortest
     * pass is found by searching one more move at a time (in "survive" the mission always takes
     * all its moves); the passing first moves are then counted with every move the mission allows
     * (at most [maxMoves]).
     */
    fun solve(level: Level): Solution {
        val start = level.newGame()
        require(start.toMove == level.human) { "Level ${level.code} does not start with the player's move" }
        require(level.rule?.turnRule == null) { "Level ${level.code} has the rule '${level.rule?.text}', which the solver does not play" }
        val depth = depth(level.mission)
        val deadline = TimeSource.Monotonic.markNow() + timeLimit
        val pass = Search(level, Outcome.PASSED, deadline)
        var fail: Search? = null
        fun positions() = pass.positions + (fail?.positions ?: 0)
        return try {
            val tries = if (level.mission is Mission.Survive) depth..depth else 1..depth
            val shortest = tries.firstOrNull { pass.forces(start, it) }
            when {
                shortest != null -> {
                    val first = pass.firstMoves(start, shortest, depth, firstMovesLimit)
                    val line = pass.line(start, shortest)
                    Solution(
                        Verdict.SOLVED, depth, first, allFirstMovesCounted = first.size < firstMovesLimit,
                        shortest = shortest, line = line, positions = positions(),
                    )
                }
                // The mission is over by then, so not passing it means failing it.
                !canOutlast(level.mission, depth) -> Solution(Verdict.UNSOLVABLE, depth, positions = positions())
                else -> {
                    val forcesFailure = Search(level, Outcome.FAILED, deadline).also { fail = it }.forces(start, depth)
                    Solution(if (forcesFailure) Verdict.UNSOLVABLE else Verdict.UNDECIDED, depth, positions = positions())
                }
            }
        } catch (_: TimeUp) {
            Solution(Verdict.TIMED_OUT, depth, positions = positions())
        }
    }

    private fun depth(mission: Mission): Int = when (mission) {
        is Mission.Capture -> min(mission.moves, maxMoves)
        // The player moves before each of the computer's dots.
        is Mission.Survive -> min(mission.moves, maxMoves)
        else -> maxMoves
    }

    /** True if the mission may still be open after the player's [depth]th move. */
    private fun canOutlast(mission: Mission, depth: Int): Boolean = when (mission) {
        is Mission.Capture -> mission.moves > depth
        is Mission.Survive -> mission.moves > depth
        else -> true
    }

    /** Thrown inside the search once the time cap has run out; never leaves [solve]. */
    private class TimeUp : RuntimeException(null, null, false, false)

    /** A move to try; the position after it is only worked out when the search gets to it. */
    private class Child(val step: Step, val cell: Int, val priority: Int, private val parent: GameState) {
        val state: GameState by lazy(LazyThreadSafetyMode.NONE) {
            when (val action = step.action) {
                is Action.Place -> parent.play(action.pos).state
                else -> parent.pass()
            }
        }
    }

    /** The points a side may play: [near] ones one by one, and [far], one point standing for the rest. */
    private class Points(val near: BooleanArray, val far: Pos?)

    /** What the transposition table knows about a position. */
    private class Entry {
        /** Forced with this many of the player's moves or more. */
        var forcedAt = Int.MAX_VALUE

        /** Not forced with this many of the player's moves or fewer. */
        var failsAt = -1
    }

    private data class Key(val a: Long, val b: Long)

    /**
     * One search: can [goal]'s side force it? For [Outcome.PASSED] that is the player, for
     * [Outcome.FAILED] the computer. Depths always count the player's moves still allowed.
     */
    private class Search(
        private val level: Level,
        private val goal: Outcome,
        private val deadline: TimeSource.Monotonic.ValueTimeMark,
    ) {
        private val human = level.human
        private val computer = level.computer
        private val attacker = if (goal == Outcome.PASSED) human else computer
        private val computerMayPass = level.mission !is Mission.Survive

        /**
         * In "capture" and "target" only the player's own moves can pass the mission, so with one
         * move left the player needs a move that passes it at once ([lastMoveDefence]).
         */
        private val passesOnOwnMove = goal == Outcome.PASSED &&
            (level.mission is Mission.Capture || level.mission is Mission.Target)

        private val table = HashMap<Key, Entry>()
        private var hashes: Hashes? = null
        private val walls = WallCost()

        /** For move ordering: how often a point has been the move that settled a position, per side. */
        private var history = Array(2) { IntArray(0) }

        var positions = 0L
            private set

        /** True if [attacker] can force [goal] before the player has placed [moves] more dots. */
        fun forces(state: GameState, moves: Int): Boolean {
            val status = level.status(state)
            if (status.isDecided) return status.outcome == goal
            if (state.isOver) return false
            if (state.toMove == human && moves == 0) return false
            tick()

            val key = hashesFor(state).key(state)
            val known = table[key]
            if (known != null) {
                if (moves >= known.forcedAt) return true
                if (moves <= known.failsAt) return false
            }
            val result = bound(state, moves) ?: search(state, moves)
            if (table.size >= MAX_TABLE) table.clear()
            val entry = table[key] ?: Entry().also { table[key] = it }
            if (result) entry.forcedAt = min(entry.forcedAt, moves) else entry.failsAt = max(entry.failsAt, moves)
            return result
        }

        /**
         * The answer when a lower bound already gives it, or null. [moves] is the player's moves
         * still allowed; the computer may pass in "capture", "target" and "rescue", so it never
         * has to add a dot the player could capture.
         */
        private fun bound(state: GameState, moves: Int): Boolean? {
            val width = state.width
            return when (val mission = level.mission) {
                is Mission.Capture -> {
                    if (goal != Outcome.PASSED) return null
                    val left = min(moves, mission.moves - state.dotsPlaced(human))
                    if (liveDots(state, computer).none { walls.toCapture(state, human, it, left) <= left }) false else null
                }
                is Mission.Target -> {
                    if (goal != Outcome.PASSED) return null
                    val target = mission.dot.y * width + mission.dot.x
                    if (walls.toCapture(state, human, target, moves) > moves) false else null
                }
                is Mission.Survive -> {
                    if (goal != Outcome.PASSED) return null
                    // The player can always place a dot on the border, which can never be captured,
                    // as long as there are border points left even if the computer fills some.
                    val left = min(mission.moves - state.dotsPlaced(computer), state.dotsLeft(computer) ?: Int.MAX_VALUE)
                    if (freeBorderPoints(state) < 2 * left) return null
                    if (liveDots(state, human).none { walls.toCapture(state, computer, it, left) <= left }) true else null
                }
                is Mission.Rescue -> {
                    val dot = mission.dot.y * width + mission.dot.x
                    if (goal == Outcome.PASSED) {
                        // It passes when the dot is safe, or when the game ends, which needs the
                        // player to run out of dots or points.
                        val playerCanGoOn = (state.dotsLeft(human) ?: Int.MAX_VALUE) > moves && freePoints(state) > 2 * moves + 2
                        if (playerCanGoOn && walls.toSave(state, human, dot) > moves) false else null
                    } else {
                        val left = computerMovesWithin(state, moves)
                        if (walls.toCapture(state, computer, dot, left) > left) false else null
                    }
                }
                else -> null
            }
        }

        /** The most dots the computer can place before the player has placed [moves] more. */
        private fun computerMovesWithin(state: GameState, moves: Int): Int {
            val budget = state.dotsLeft(computer) ?: Int.MAX_VALUE
            val playerDots = state.dotsLeft(human) ?: Int.MAX_VALUE
            // Once the player has no dots left, the computer places the rest of its own.
            if (playerDots < moves) return budget
            return min(budget, moves + if (state.toMove == computer) 1 else 0)
        }

        private fun liveDots(state: GameState, player: Player): List<Int> {
            val result = ArrayList<Int>()
            for (y in 0 until state.height) {
                for (x in 0 until state.width) {
                    if (state.ownerAt(x, y) == player && !state.isCaptured(x, y)) result += y * state.width + x
                }
            }
            return result
        }

        private fun freePoints(state: GameState): Int {
            var count = 0
            for (y in 0 until state.height) {
                for (x in 0 until state.width) if (state.ownerAt(x, y) == null && !state.isDead(x, y)) count++
            }
            return count
        }

        private fun freeBorderPoints(state: GameState): Int {
            var count = 0
            for (y in 0 until state.height) {
                for (x in 0 until state.width) {
                    val border = x == 0 || y == 0 || x == state.width - 1 || y == state.height - 1
                    if (border && state.ownerAt(x, y) == null && !state.isDead(x, y)) count++
                }
            }
            return count
        }

        private fun search(state: GameState, moves: Int): Boolean {
            val mover = state.toMove
            val next = if (mover == human) moves - 1 else moves
            if (mover != attacker && passesOnOwnMove && moves == 1) return lastMoveDefence(state)
            val children = children(state, moves)
            for (child in children) {
                val forced = forces(child.state, next)
                // The attacker needs one move that forces the goal; the defender one that does not.
                if (forced == (mover == attacker)) {
                    remember(mover, child, moves)
                    return forced
                }
            }
            // Every attacker move failed, or every defender move still lost.
            return mover != attacker
        }

        /**
         * The computer to move, in "capture" or "target", with the player one move from the end
         * of the search. The player can then pass only on a point that passes the mission at once.
         * A computer dot on another point does not stop that: the player's rings are walled by
         * the player's dots alone, so a computer dot elsewhere only adds to what a ring would
         * hold. Only a dot on such a point, or a computer capture, can save the computer; if there
         * is no such point, passing does.
         */
        private fun lastMoveDefence(state: GameState): Boolean {
            val asIfPassed = state.pass()
            if (asIfPassed.toMove != human) return false
            val winning = children(asIfPassed, 1).filter { level.status(it.state).outcome == Outcome.PASSED }.map { it.cell }.toSet()
            if (winning.isEmpty()) return false
            for (child in children(state, 1)) {
                if (child.step.action !is Action.Place) continue
                val captures = child.state.score(computer) > state.score(computer)
                if (child.cell !in winning && !captures) continue
                if (!forces(child.state, 1)) {
                    remember(computer, child, 1)
                    return false
                }
            }
            return true
        }

        /**
         * The first moves that force the goal within [moves] of the player's moves, until [limit]
         * are found. Moves that force it sooner are cheaper to prove, so they are looked for first:
         * with [shortest] moves, then one more at a time. When the one point that stands for the
         * far points forces it, every far point is counted.
         */
        fun firstMoves(start: GameState, shortest: Int, moves: Int, limit: Int): List<Pos> {
            val points = points(start)
            val children = children(start, moves)
            val winners = LinkedHashSet<Child>()
            fun found() = winners.sumOf { if (points.near[it.cell]) 1 else allFar(start, points.near).size }
            for (depth in shortest..moves) {
                for (child in children) {
                    if (found() >= limit) break
                    if (child !in winners && forces(child.state, depth - 1)) winners += child
                }
            }
            val result = winners.flatMap { child ->
                if (points.near[child.cell]) listOf((child.step.action as Action.Place).pos) else allFar(start, points.near)
            }
            return result.sortedWith(compareBy({ it.y }, { it.x }))
        }

        /**
         * A shortest forced line from [state], where the goal is forced within [moves]: the
         * attacker's quickest move, and the defender's reply that delays the goal the longest.
         */
        fun line(state: GameState, moves: Int): List<Step> {
            if (level.status(state).isDecided || state.isOver) return emptyList()
            val mover = state.toMove
            if (mover == human && moves == 0) return emptyList()
            val next = if (mover == human) moves - 1 else moves
            val children = children(state, moves)
            // A lower bound may have settled this position without a move to show; the line stops there.
            if (mover == attacker) {
                val child = children.firstOrNull { forces(it.state, next) } ?: return emptyList()
                return listOf(child.step) + line(child.state, next)
            }
            val needed = children.associateWith { child -> (0..next).firstOrNull { forces(child.state, it) } }
            if (needed.values.any { it == null }) return emptyList()
            val slowest = children.maxByOrNull { needed.getValue(it)!! } ?: return emptyList()
            return listOf(slowest.step) + line(slowest.state, needed.getValue(slowest)!!)
        }

        /**
         * The moves to try, most promising first: the [key] points of the mission, then points
         * that settled positions before (history), then points next to many dots, with a bonus
         * where the mover could close a ring around an enemy dot. Then the far point, then a pass
         * where the computer may pass.
         */
        private fun children(state: GameState, moves: Int): List<Child> {
            val mover = state.toMove
            val width = state.width
            val points = points(state)
            val near = points.near
            val moverHistory = historyFor(mover, state)
            val key = key(state, moves)
            val result = ArrayList<Child>()
            for (i in near.indices) {
                if (!near[i]) continue
                val pos = Pos(i % width, i / width)
                if (!state.isLegal(pos)) continue
                val own = neighbours(state, pos, mover)
                val enemy = neighbours(state, pos, mover.opponent)
                val ring = if (own >= 2 && enemy >= 1) RING_PRIORITY else 0
                val priority = (if (key[i]) KEY_PRIORITY else 0) + moverHistory[i] * HISTORY_PRIORITY + ring +
                    own * OWN_PRIORITY + enemy * ENEMY_PRIORITY
                result += Child(Step(mover, Action.Place(pos)), i, priority, state)
            }
            result.sortByDescending { it.priority }
            points.far?.let { pos -> result += Child(Step(mover, Action.Place(pos)), pos.y * width + pos.x, 0, state) }
            if (mover == computer && computerMayPass) result += Child(Step(mover, Action.Pass), -1, 0, state)
            return result
        }

        /**
         * The points near a live dot (within two points, diagonals included), and the legal point
         * furthest from every dot to stand for the others.
         */
        private fun points(state: GameState): Points {
            val width = state.width
            val height = state.height
            val distance = distances(state)
            val near = BooleanArray(width * height)
            var far: Pos? = null
            var farthest = REACH
            for (i in near.indices) {
                if (distance[i] <= REACH) {
                    near[i] = true
                } else if (distance[i] > farthest && state.isLegal(i % width, i / width)) {
                    farthest = distance[i]
                    far = Pos(i % width, i / width)
                }
            }
            return Points(near, far)
        }

        /** Every legal point that is not [near] a dot, in board order. */
        private fun allFar(state: GameState, near: BooleanArray): List<Pos> =
            near.indices.filter { !near[it] && state.isLegal(it % state.width, it / state.width) }.map { Pos(it % state.width, it / state.width) }

        /** For each point, the steps (diagonals included) to the nearest live dot. */
        private fun distances(state: GameState): IntArray {
            val width = state.width
            val height = state.height
            val distance = IntArray(width * height) { Int.MAX_VALUE }
            val queue = IntArray(width * height)
            var head = 0
            var tail = 0
            for (i in distance.indices) {
                val x = i % width
                val y = i / width
                if (state.ownerAt(x, y) != null && !state.isCaptured(x, y)) {
                    distance[i] = 0
                    queue[tail++] = i
                }
            }
            while (head < tail) {
                val i = queue[head++]
                val x = i % width
                val y = i / width
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
                        val j = ny * width + nx
                        if (distance[j] == Int.MAX_VALUE) {
                            distance[j] = distance[i] + 1
                            queue[tail++] = j
                        }
                    }
                }
            }
            return distance
        }

        /**
         * The points the mission turns on, where both sides should look first: in "capture" one
         * cheapest wall around each computer dot the player can capture in time; in "target" the
         * same around the target; in "rescue" the cheapest chains from the marked dot to the
         * border. In "survive" the history of earlier cut-offs orders moves well enough on its own.
         */
        private fun key(state: GameState, moves: Int): BooleanArray {
            val width = state.width
            val key = BooleanArray(width * state.height)
            if (goal != Outcome.PASSED) return key
            when (val mission = level.mission) {
                is Mission.Capture -> for (dot in liveDots(state, computer)) walls.toCapture(state, human, dot, moves, key)
                is Mission.Target -> walls.toCapture(state, human, mission.dot.y * width + mission.dot.x, moves, key)
                is Mission.Rescue -> walls.savePaths(state, human, mission.dot.y * width + mission.dot.x, key)
                is Mission.Survive -> {
                    val left = mission.moves - state.dotsPlaced(computer)
                    for (dot in liveDots(state, human)) walls.toCapture(state, computer, dot, left, key)
                }
                else -> Unit
            }
            return key
        }

        /** How many of [player]'s live dots touch [pos], diagonals included. */
        private fun neighbours(state: GameState, pos: Pos, player: Player): Int {
            var count = 0
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val x = pos.x + dx
                    val y = pos.y + dy
                    if ((dx != 0 || dy != 0) && state.inBounds(x, y) && state.ownerAt(x, y) == player && !state.isCaptured(x, y)) count++
                }
            }
            return count
        }

        private fun historyFor(player: Player, state: GameState): IntArray {
            if (history[0].size != state.width * state.height) history = Array(2) { IntArray(state.width * state.height) }
            return history[player.ordinal]
        }

        private fun remember(mover: Player, child: Child, moves: Int) {
            if (child.cell < 0) return
            val table = history[mover.ordinal]
            if (child.cell < table.size) table[child.cell] = min(table[child.cell] + moves * moves, MAX_HISTORY)
        }

        private fun hashesFor(state: GameState): Hashes =
            hashes?.takeIf { it.cells == state.width * state.height } ?: Hashes(state.width * state.height).also { hashes = it }

        private fun tick() {
            if (positions++ % CHECK_EVERY == 0L && deadline.hasPassedNow()) throw TimeUp()
        }
    }

    /**
     * Two independent 64-bit hashes of everything that matters in a position: every point (whose
     * dot, captured, out of play), who moves, the passes in a row, the dots each side has placed
     * and the scores. Two different positions share both hashes with a chance of about 1 in 10^38.
     */
    private class Hashes(val cells: Int) {
        private val first = LongArray(cells * CODES) { mix(it.toLong() * 2 + 1) }
        private val second = LongArray(cells * CODES) { mix(it.toLong() * 2 + 2 + (1L shl 40)) }

        fun key(state: GameState): Key {
            var a = 0L
            var b = 0L
            val width = state.width
            for (i in 0 until cells) {
                val x = i % width
                val y = i / width
                val owner = state.ownerAt(x, y)
                val code = (owner?.ordinal?.plus(1) ?: 0) + (if (state.isCaptured(x, y)) 3 else 0) + (if (state.isDead(x, y)) 6 else 0)
                if (code == 0) continue
                a = a xor first[i * CODES + code]
                b = b xor second[i * CODES + code]
            }
            val extras = longArrayOf(
                state.toMove.ordinal.toLong(), state.passesInRow.toLong(),
                state.dotsPlaced(Player.RED).toLong(), state.dotsPlaced(Player.BLUE).toLong(),
                state.score(Player.RED).toLong(), state.score(Player.BLUE).toLong(),
            )
            for ((k, value) in extras.withIndex()) {
                a = mix(a xor (value + 0x1000L * (k + 1)))
                b = mix(b xor (value * 31 + 0x9E3779B9L * (k + 1)))
            }
            return Key(a, b)
        }

        private companion object {
            const val CODES = 12

            /** SplitMix64's finaliser: spreads the bits of [z] over the whole word. */
            fun mix(z0: Long): Long {
                var z = z0 + -0x61c8864680b583ebL
                z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
                z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
                return z xor (z ushr 31)
            }
        }
    }

    companion object {
        /** The most moves of the player's that the solver searches: "short missions". */
        const val MAX_MOVES = 5

        /** How long one level may take before the solver gives up. */
        val TIME_LIMIT: Duration = 20.seconds

        private const val REACH = 2
        private const val CHECK_EVERY = 256L
        private const val MAX_TABLE = 2_000_000
        private const val KEY_PRIORITY = 1_000_000
        private const val HISTORY_PRIORITY = 100
        private const val RING_PRIORITY = 50
        private const val OWN_PRIORITY = 3
        private const val ENEMY_PRIORITY = 2
        private const val MAX_HISTORY = 5_000
    }
}
