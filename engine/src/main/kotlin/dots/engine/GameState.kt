package dots.engine

/**
 * A position in the dots game. Instances are immutable from the outside: [play], [pass], [stop] and
 * [finish] return new states, so search code and undo/replay need no bookkeeping.
 *
 * Rules follow the Wikipedia article "Dots (paper-and-pencil game)":
 *  - Players alternate placing one dot on an empty intersection.
 *  - Dots of one colour form a chain through orthogonal and diagonal neighbours. If a newly placed
 *    dot completes a closed chain around at least one live enemy dot, the area inside is
 *    surrounded: the enemy dots in it score for the surrounding player (own dots do not), and the
 *    whole area is out of play from then on.
 *  - A closed chain around no enemy dot captures nothing and leaves the area playable. An enemy
 *    dot dropped into such an area stays alive until the owner closes a new chain around it.
 *  - Groups of dots touching the border can never be enclosed ("grounded").
 *  - A player may [stop] on their turn: their opponent then moves freely to capture what is left
 *    and ends the game with [finish].
 *
 * Deliberate additions and choices:
 *  - Optional dot budget per player, for short mobile sessions and puzzle levels.
 *  - When one dot closes several chains the wiki lets players choose; this engine captures every
 *    newly enclosed area that holds an enemy dot.
 */
class GameState private constructor(
    val width: Int,
    val height: Int,
    private val owner: ByteArray,
    private val captured: BooleanArray,
    private val dead: BooleanArray,
    private val enclosedBy: ByteArray,
    private val scores: IntArray,
    private val liveDots: IntArray,
    private val dotsLeft: IntArray,
    private val placed: IntArray,
    private var playableCells: Int,
    toMove: Player,
    passesInRow: Int,
    moveCount: Int,
    cleaner: Player?,
    finished: Boolean,
) {
    var toMove: Player = toMove
        private set
    var passesInRow: Int = passesInRow
        private set
    var moveCount: Int = moveCount
        private set

    /** After a [stop], the player who may now move freely to capture what is left. */
    var cleaner: Player? = cleaner
        private set
    private var finished: Boolean = finished

    val isCleanup: Boolean get() = cleaner != null

    /** The player who declared their dots grounded, if any. */
    val stopper: Player? get() = cleaner?.opponent

    val isOver: Boolean
        get() = when {
            finished -> true
            isCleanup -> playableCells == 0
            else -> passesInRow >= 2 || (!canMove(Player.RED) && !canMove(Player.BLUE))
        }

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

    /** Dots [player] has placed in this game, collecting after a [stop] included; starting dots are not counted. */
    fun dotsPlaced(player: Player): Int = placed[player.ordinal]

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

    /** True for cells inside a capturing chain: no further moves there. */
    fun isDead(x: Int, y: Int): Boolean = dead[y * width + x]

    fun isDead(pos: Pos): Boolean = isDead(pos.x, pos.y)

    /** For a cell inside a capturing chain: the player whose chain closed it most recently. */
    fun enclosedBy(x: Int, y: Int): Player? = when (enclosedBy[y * width + x]) {
        Player.RED.code -> Player.RED
        Player.BLUE.code -> Player.BLUE
        else -> null
    }

    fun enclosedBy(pos: Pos): Player? = enclosedBy(pos.x, pos.y)

    fun isLegal(x: Int, y: Int): Boolean =
        !isOver && (isCleanup || dotsLeft[toMove.ordinal] != 0) && inBounds(x, y) &&
            owner[y * width + x] == EMPTY && !dead[y * width + x]

    fun isLegal(pos: Pos): Boolean = isLegal(pos.x, pos.y)

    fun legalMoves(): List<Pos> {
        if (isOver || (!isCleanup && dotsLeft[toMove.ordinal] == 0)) return emptyList()
        val moves = ArrayList<Pos>()
        for (i in owner.indices) {
            if (owner[i] == EMPTY && !dead[i]) moves += Pos(i % width, i / width)
        }
        return moves
    }

    fun apply(action: Action): MoveResult = when (action) {
        is Action.Place -> play(action.pos)
        Action.Pass -> MoveResult(pass(), emptyList())
        Action.Stop -> MoveResult(stop(), emptyList())
        Action.Finish -> MoveResult(finish(), emptyList())
    }

    fun play(pos: Pos): MoveResult {
        require(isLegal(pos)) { "Illegal move $pos for $toMove" }
        return copy().applyMove(pos)
    }

    /** Skips the turn. Two passes in a row end the game. */
    fun pass(): GameState {
        check(!isOver && !isCleanup) { "Cannot pass now" }
        val next = copy()
        next.passesInRow++
        val other = toMove.opponent
        next.toMove = if (next.canMove(other)) other else toMove
        return next
    }

    /**
     * The player to move declares all the dots they want to keep grounded. From now on only their
     * opponent moves, with no dot limit, until they call [finish].
     */
    fun stop(): GameState {
        check(!isOver && !isCleanup) { "Cannot stop now" }
        val next = copy()
        next.cleaner = toMove.opponent
        next.toMove = toMove.opponent
        next.passesInRow = 0
        return next
    }

    /** The collecting player is done; the game ends and the captured-dot counts decide it. */
    fun finish(): GameState {
        check(isCleanup && !isOver) { "Nothing to finish" }
        val next = copy()
        next.finished = true
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
        width, height, owner.clone(), captured.clone(), dead.clone(), enclosedBy.clone(), scores.clone(),
        liveDots.clone(), dotsLeft.clone(), placed.clone(), playableCells, toMove, passesInRow, moveCount, cleaner, finished,
    )

    private fun applyMove(pos: Pos): MoveResult {
        val mover = toMove
        val index = pos.y * width + pos.x
        owner[index] = mover.code
        liveDots[mover.ordinal]++
        placed[mover.ordinal]++
        playableCells--
        if (!isCleanup && dotsLeft[mover.ordinal] > 0) dotsLeft[mover.ordinal]--

        val captures = ArrayList<Capture>(0)
        captureFrom(index, mover, captures)

        moveCount++
        passesInRow = 0
        if (!isCleanup) {
            val other = mover.opponent
            toMove = if (canMove(other) || !canMove(mover)) other else mover
        }
        return MoveResult(this, captures)
    }

    private fun isWall(i: Int, code: Byte): Boolean = owner[i] == code && !captured[i]

    /**
     * Captures what the dot just placed at [placed] has closed off.
     *
     * Walls are [player]'s uncaptured dots. Before the dot was placed, all of its non-wall
     * orthogonal neighbours were in one connected area. Placing it may split that area into
     * pieces, and each piece cut off by the new wall is bounded by a closed chain through the
     * new dot. Orthogonal flooding is right here: a diagonal join of walls is tight.
     *
     * If the old area reached the border, the pieces that no longer touch it are enclosed. If it
     * was already enclosed (an empty chain from earlier), every piece after a real split is.
     * Enclosed pieces holding a live enemy dot are captured; the rest stay playable.
     */
    private fun captureFrom(placed: Int, player: Player, out: MutableList<Capture>) {
        val enemy = player.opponent
        if (liveDots[player.ordinal] < MIN_RING_DOTS || liveDots[enemy.ordinal] == 0) return

        val wall = player.code
        val n = width * height
        val seen = BooleanArray(n)
        val buffer = IntArray(n)
        val starts = IntArray(4)
        val ends = IntArray(4)
        val touchesBorder = BooleanArray(4)
        var pieces = 0
        var used = 0

        val px = placed % width
        val py = placed / width
        for (d in 0 until 4) {
            val nx = px + DX[d]
            val ny = py + DY[d]
            if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
            val first = ny * width + nx
            if (seen[first] || isWall(first, wall)) continue

            val start = used
            buffer[used++] = first
            seen[first] = true
            var head = start
            var border = false
            while (head < used) {
                val i = buffer[head++]
                val x = i % width
                val y = i / width
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) border = true
                for (e in 0 until 4) {
                    val mx = x + DX[e]
                    val my = y + DY[e]
                    if (mx < 0 || my < 0 || mx >= width || my >= height) continue
                    val j = my * width + mx
                    if (!seen[j] && !isWall(j, wall)) {
                        seen[j] = true
                        buffer[used++] = j
                    }
                }
            }
            starts[pieces] = start
            ends[pieces] = used
            touchesBorder[pieces] = border
            pieces++
        }

        val placedOnBorder = px == 0 || py == 0 || px == width - 1 || py == height - 1
        val areaReachedBorder = placedOnBorder || (0 until pieces).any { touchesBorder[it] }
        for (k in 0 until pieces) {
            val enclosed = if (areaReachedBorder) !touchesBorder[k] else pieces >= 2
            if (!enclosed) continue
            var holdsEnemy = false
            for (m in starts[k] until ends[k]) {
                val i = buffer[m]
                if (owner[i] == enemy.code && !captured[i]) {
                    holdsEnemy = true
                    break
                }
            }
            if (holdsEnemy) claim(buffer, starts[k], ends[k], player, out)
        }
    }

    private fun claim(cells: IntArray, from: Int, to: Int, by: Player, out: MutableList<Capture>) {
        val enemy = by.opponent
        val dots = ArrayList<Pos>()
        val region = ArrayList<Pos>(to - from)
        for (k in from until to) {
            val i = cells[k]
            val pos = Pos(i % width, i / width)
            region += pos
            if (owner[i] == enemy.code && !captured[i]) {
                captured[i] = true
                liveDots[enemy.ordinal]--
                dots += pos
            }
            enclosedBy[i] = by.code
            if (!dead[i]) {
                dead[i] = true
                if (owner[i] == EMPTY) playableCells--
            }
        }
        scores[by.ordinal] += dots.size
        out += Capture(by, dots, region)
    }

    /** True if [player]'s dots already enclose a live enemy dot; used to vet starting positions. */
    private fun hasEnclosedEnemy(player: Player): Boolean {
        val wall = player.code
        val enemy = player.opponent.code
        val n = width * height
        val outside = BooleanArray(n)
        val stack = IntArray(n)
        var top = 0
        for (i in 0 until n) {
            val x = i % width
            val y = i / width
            if ((x == 0 || y == 0 || x == width - 1 || y == height - 1) && !isWall(i, wall)) {
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
        return (0 until n).any { owner[it] == enemy && !captured[it] && !outside[it] }
    }

    companion object {
        private const val EMPTY: Byte = 0

        /** The smallest chain that encloses a cell is the four-dot diamond. */
        private const val MIN_RING_DOTS = 4

        private val DX = intArrayOf(-1, 1, 0, 0)
        private val DY = intArrayOf(0, 0, -1, 1)

        /**
         * Starts a game. [dots] are placed up front (an opening or a head start) and do not count
         * against [dotsPerPlayer]; they must not already enclose an enemy dot. [dotsFor] gives a
         * player a budget of their own instead of [dotsPerPlayer].
         */
        fun start(
            width: Int,
            height: Int,
            dotsPerPlayer: Int? = null,
            first: Player = Player.RED,
            dots: Map<Pos, Player> = emptyMap(),
            dotsFor: Map<Player, Int> = emptyMap(),
        ): GameState {
            require(width >= 3 && height >= 3) { "Board must be at least 3x3" }
            require(dotsPerPlayer == null || dotsPerPlayer >= 0) { "dotsPerPlayer must not be negative" }
            require(dotsFor.values.all { it >= 0 }) { "a player's dots must not be negative" }
            val n = width * height
            val budgets = IntArray(2) { dotsFor[Player.entries[it]] ?: dotsPerPlayer ?: -1 }
            val state = GameState(
                width, height, ByteArray(n), BooleanArray(n), BooleanArray(n), ByteArray(n), IntArray(2),
                IntArray(2), budgets, IntArray(2), n, first, 0, 0, null, false,
            )
            for ((pos, player) in dots) {
                require(state.inBounds(pos.x, pos.y)) { "Starting dot $pos is off the board" }
                val i = pos.y * width + pos.x
                require(state.owner[i] == EMPTY) { "Two starting dots on $pos" }
                state.owner[i] = player.code
                state.liveDots[player.ordinal]++
                state.playableCells--
            }
            require(!state.hasEnclosedEnemy(Player.RED) && !state.hasEnclosedEnemy(Player.BLUE)) {
                "The starting position already encloses an enemy dot"
            }
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
