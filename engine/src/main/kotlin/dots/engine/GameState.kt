package dots.engine

/**
 * A position in the dots game. Instances are immutable from the outside: [play] and [pass] return
 * new states, so search code and undo/replay need no bookkeeping.
 *
 * Rules as implemented (see README for the open questions):
 *  - Players alternate placing one dot on an empty, playable intersection.
 *  - A player's dots connect orthogonally and diagonally. When they form a closed ring around at
 *    least one live enemy dot, every enemy dot inside is captured and scores for the ringer.
 *  - Everything inside a capturing ring is out of play: no more moves there, captured dots no
 *    longer count as part of any chain.
 *  - A ring around only empty cells or the ringer's own dots captures nothing and the inside stays
 *    playable, but an enemy dot placed inside it is captured immediately.
 *  - Dots on the border of the board can never be enclosed.
 *  - Optionally each player has a fixed budget of dots to place (short sessions suit mobile).
 */
class GameState private constructor(
    val width: Int,
    val height: Int,
    private val owner: ByteArray,
    private val captured: BooleanArray,
    private val dead: BooleanArray,
    private val scores: IntArray,
    private val liveDots: IntArray,
    private val dotsLeft: IntArray,
    private var playableCells: Int,
    toMove: Player,
    passesInRow: Int,
    moveCount: Int,
) {
    var toMove: Player = toMove
        private set
    var passesInRow: Int = passesInRow
        private set
    var moveCount: Int = moveCount
        private set

    val isOver: Boolean get() = passesInRow >= 2 || (!canMove(Player.RED) && !canMove(Player.BLUE))

    /** The player with more captured dots once the game is over; null for a draw or an unfinished game. */
    val winner: Player?
        get() = when {
            !isOver -> null
            scores[0] > scores[1] -> Player.RED
            scores[1] > scores[0] -> Player.BLUE
            else -> null
        }

    fun score(player: Player): Int = scores[player.ordinal]

    /** Dots [player] may still place, or null when there is no limit. */
    fun dotsLeft(player: Player): Int? = dotsLeft[player.ordinal].takeIf { it >= 0 }

    fun inBounds(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    fun ownerAt(x: Int, y: Int): Player? = when (owner[y * width + x]) {
        Player.RED.code -> Player.RED
        Player.BLUE.code -> Player.BLUE
        else -> null
    }

    fun ownerAt(pos: Pos): Player? = ownerAt(pos.x, pos.y)

    /** True for a dot that has been captured; such dots no longer take part in play. */
    fun isCaptured(x: Int, y: Int): Boolean = captured[y * width + x]

    fun isCaptured(pos: Pos): Boolean = isCaptured(pos.x, pos.y)

    /** True for cells inside a capturing ring: no further moves there. */
    fun isDead(x: Int, y: Int): Boolean = dead[y * width + x]

    fun isDead(pos: Pos): Boolean = isDead(pos.x, pos.y)

    fun isLegal(x: Int, y: Int): Boolean =
        !isOver && dotsLeft[toMove.ordinal] != 0 && inBounds(x, y) &&
            owner[y * width + x] == EMPTY && !dead[y * width + x]

    fun isLegal(pos: Pos): Boolean = isLegal(pos.x, pos.y)

    fun legalMoves(): List<Pos> {
        if (isOver || dotsLeft[toMove.ordinal] == 0) return emptyList()
        val moves = ArrayList<Pos>()
        for (i in owner.indices) {
            if (owner[i] == EMPTY && !dead[i]) moves += Pos(i % width, i / width)
        }
        return moves
    }

    fun play(pos: Pos): MoveResult {
        require(isLegal(pos)) { "Illegal move $pos for $toMove" }
        return copy().applyMove(pos)
    }

    /** Skips the turn. Two passes in a row end the game. */
    fun pass(): GameState {
        check(!isOver) { "The game is over" }
        val next = copy()
        next.passesInRow++
        val other = toMove.opponent
        next.toMove = if (next.canMove(other)) other else toMove
        return next
    }

    /** One row per line: `.` empty, `,` out of play, `R`/`B` dots, `r`/`b` captured dots. */
    fun toAscii(): String = buildString {
        for (y in 0 until height) {
            if (y > 0) append('\n')
            for (x in 0 until width) {
                val i = y * width + x
                val player = ownerAt(x, y)
                append(
                    when {
                        player == null -> if (dead[i]) ',' else '.'
                        captured[i] -> player.symbol.lowercaseChar()
                        else -> player.symbol
                    },
                )
            }
        }
    }

    private fun canMove(player: Player): Boolean = dotsLeft[player.ordinal] != 0 && playableCells > 0

    private fun copy() = GameState(
        width, height, owner.clone(), captured.clone(), dead.clone(), scores.clone(),
        liveDots.clone(), dotsLeft.clone(), playableCells, toMove, passesInRow, moveCount,
    )

    private fun applyMove(pos: Pos): MoveResult {
        val mover = toMove
        owner[pos.y * width + pos.x] = mover.code
        liveDots[mover.ordinal]++
        playableCells--
        if (dotsLeft[mover.ordinal] > 0) dotsLeft[mover.ordinal]--

        val captures = ArrayList<Capture>(0)
        resolveCaptures(mover, captures)
        // A dot dropped into an enemy ring that was already closed is lost on the spot.
        resolveCaptures(mover.opponent, captures)

        moveCount++
        passesInRow = 0
        val other = mover.opponent
        toMove = if (canMove(other) || !canMove(mover)) other else mover
        return MoveResult(this, captures)
    }

    private fun isWall(i: Int, code: Byte): Boolean = owner[i] == code && !captured[i]

    /**
     * Finds every region enclosed by [player]'s standing dots that holds a live enemy dot and
     * captures it.
     *
     * Walls are [player]'s uncaptured dots. A flood fill from the border through the remaining
     * cells (moving orthogonally, so a diagonal chain of walls is tight) marks everything that is
     * not enclosed; each leftover orthogonally-connected group of non-wall cells is one enclosure.
     */
    private fun resolveCaptures(player: Player, out: MutableList<Capture>) {
        val enemy = player.opponent
        if (liveDots[player.ordinal] < MIN_RING_DOTS || liveDots[enemy.ordinal] == 0) return

        val wall = player.code
        val n = width * height
        val outside = BooleanArray(n)
        val stack = IntArray(n)
        var top = 0

        for (i in 0 until n) {
            val x = i % width
            val y = i / width
            val onBorder = x == 0 || y == 0 || x == width - 1 || y == height - 1
            if (onBorder && !isWall(i, wall)) {
                outside[i] = true
                stack[top++] = i
            }
        }
        while (top > 0) {
            val i = stack[--top]
            val x = i % width
            val y = i / width
            for (d in 0 until 4) {
                val nx = x + DX[d]
                val ny = y + DY[d]
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
                val j = ny * width + nx
                if (!outside[j] && !isWall(j, wall)) {
                    outside[j] = true
                    stack[top++] = j
                }
            }
        }

        // The flood is done, so `stack` doubles as the queue for collecting enclosures.
        val seen = BooleanArray(n)
        for (start in 0 until n) {
            if (outside[start] || seen[start] || isWall(start, wall)) continue
            var head = 0
            var tail = 0
            stack[tail++] = start
            seen[start] = true
            var holdsEnemy = false
            while (head < tail) {
                val i = stack[head++]
                if (owner[i] == enemy.code && !captured[i]) holdsEnemy = true
                val x = i % width
                val y = i / width
                for (d in 0 until 4) {
                    val nx = x + DX[d]
                    val ny = y + DY[d]
                    if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
                    val j = ny * width + nx
                    if (!seen[j] && !isWall(j, wall)) {
                        seen[j] = true
                        stack[tail++] = j
                    }
                }
            }
            if (holdsEnemy) claim(stack, tail, player, out)
        }
    }

    private fun claim(cells: IntArray, count: Int, by: Player, out: MutableList<Capture>) {
        val enemy = by.opponent
        val dots = ArrayList<Pos>()
        val region = ArrayList<Pos>(count)
        for (k in 0 until count) {
            val i = cells[k]
            val pos = Pos(i % width, i / width)
            region += pos
            if (owner[i] == enemy.code && !captured[i]) {
                captured[i] = true
                liveDots[enemy.ordinal]--
                dots += pos
            }
            if (!dead[i]) {
                dead[i] = true
                if (owner[i] == EMPTY) playableCells--
            }
        }
        scores[by.ordinal] += dots.size
        out += Capture(by, dots, region)
    }

    companion object {
        private const val EMPTY: Byte = 0

        /** The smallest ring that encloses a cell is the four-dot diamond. */
        private const val MIN_RING_DOTS = 4

        private val DX = intArrayOf(-1, 1, 0, 0)
        private val DY = intArrayOf(0, 0, -1, 1)

        /**
         * Starts a game. [dots] are placed up front (a head start) and do not count against
         * [dotsPerPlayer]; they must not already enclose anything.
         */
        fun start(
            width: Int,
            height: Int,
            dotsPerPlayer: Int? = null,
            first: Player = Player.RED,
            dots: Map<Pos, Player> = emptyMap(),
        ): GameState {
            require(width >= 3 && height >= 3) { "Board must be at least 3x3" }
            require(dotsPerPlayer == null || dotsPerPlayer >= 0) { "dotsPerPlayer must not be negative" }
            val n = width * height
            val budget = dotsPerPlayer ?: -1
            val state = GameState(
                width, height, ByteArray(n), BooleanArray(n), BooleanArray(n), IntArray(2),
                IntArray(2), intArrayOf(budget, budget), n, first, 0, 0,
            )
            for ((pos, player) in dots) {
                require(state.inBounds(pos.x, pos.y)) { "Starting dot $pos is off the board" }
                val i = pos.y * width + pos.x
                require(state.owner[i] == EMPTY) { "Two starting dots on $pos" }
                state.owner[i] = player.code
                state.liveDots[player.ordinal]++
                state.playableCells--
            }
            val captures = ArrayList<Capture>(0)
            state.resolveCaptures(Player.RED, captures)
            state.resolveCaptures(Player.BLUE, captures)
            require(captures.isEmpty()) { "The starting position already contains a capture" }
            if (!state.canMove(first) && state.canMove(first.opponent)) state.toMove = first.opponent
            return state
        }

        /** Builds a start position from a map: `.` empty, `R`/`B` dots; whitespace is ignored. */
        fun fromAscii(
            ascii: String,
            dotsPerPlayer: Int? = null,
            first: Player = Player.RED,
        ): GameState {
            val rows = ascii.lines().map { line -> line.filterNot { it.isWhitespace() } }.filter { it.isNotEmpty() }
            require(rows.isNotEmpty()) { "Empty map" }
            val width = rows[0].length
            require(rows.all { it.length == width }) { "All map rows must have the same length" }
            val dots = HashMap<Pos, Player>()
            for ((y, row) in rows.withIndex()) {
                for ((x, ch) in row.withIndex()) {
                    when (ch) {
                        '.' -> Unit
                        Player.RED.symbol -> dots[Pos(x, y)] = Player.RED
                        Player.BLUE.symbol -> dots[Pos(x, y)] = Player.BLUE
                        else -> throw IllegalArgumentException("Unknown map symbol '$ch' at ($x, $y)")
                    }
                }
            }
            return start(width, rows.size, dotsPerPlayer, first, dots)
        }
    }
}
