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
 *  - Boosters for the solo campaign ([Booster], [useBooster]): a player may use at most one per
 *    turn, before placing their dot, and only during normal play (not after a [stop]).
 *  - Turn rules for the campaign's bosses ([TurnRule]): a player may place two dots on some of
 *    their turns, on a schedule announced in advance ([doubleMoveIn]).
 *  - When one dot closes several chains the wiki lets players choose; this engine captures every
 *    newly enclosed area that holds an enemy dot.
 *  - Board shapes and special points for the campaign: holes (no point there; their edge counts as
 *    border or not, see [holesAreBorder]), blocked points (nobody may place a dot there) and bonus
 *    points (a dot captured there is worth 2, see [worth]).
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
    private val blocked: BooleanArray,
    /** Holes: no point there. Shared between states, never changed. */
    private val hole: BooleanArray,
    /** Bonus points. Shared between states, never changed. */
    private val bonus: BooleanArray,
    /**
     * Cells a region reaches the border through: the outer edge of the grid and the holes that
     * are outside ([isOutside]). Shared between states, never changed.
     */
    private val edge: BooleanArray,
    /**
     * True if the edge of every hole counts as border: dots next to a hole can never be captured.
     * False if holes are open: a ring may go round a hole, unless the hole reaches the edge of the
     * map, directly or through other holes.
     */
    val holesAreBorder: Boolean,
    private val boosters: IntArray,
    private var playableCells: Int,
    toMove: Player,
    passesInRow: Int,
    moveCount: Int,
    cleaner: Player?,
    finished: Boolean,
    boosterUsed: Boolean,
    doubleDots: Int,
    /** Each player's [TurnRule], by [Player.ordinal]; shared between states, never changed. */
    private val turnRules: Array<TurnRule?>,
    /** How many turns each player has begun, the current one included. */
    private val turns: IntArray,
    /** In a [TurnRule.TwoHalves] turn, the half of the first dot ([LEFT] or [RIGHT]); else [NO_HALF]. */
    private var half: Int,
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

    /** True once the player to move has used a booster this turn: no other until the turn passes. */
    var boosterUsed: Boolean = boosterUsed
        private set

    /**
     * The dots of a two-dot turn still to place: 2, then 1; 0 in a turn of one dot. Two-dot turns
     * come from a [Booster.DOUBLE_DOT] or a [TurnRule].
     */
    private var doubleDots: Int = doubleDots

    /**
     * Dots the player to move still places this turn: 2 at the start of a two-dot turn (after a
     * double dot, or on a [TurnRule]'s turn), else 1. If the board leaves no point for the second
     * dot, the turn ends after the first.
     */
    val dotsThisTurn: Int get() = maxOf(1, doubleDots)

    /** The [TurnRule] of [player], if any. */
    fun turnRule(player: Player): TurnRule? = turnRules[player.ordinal]

    /**
     * How many of [player]'s turns until their next turn of two dots under their [TurnRule]: 0
     * while they are placing it, 1 if their next turn is one, and so on. The player's current
     * turn, if it is a turn of one dot, is not counted. Null when it cannot come: no rule, the game
     * is over or in collecting after a [stop], or the player would have fewer than 2 dots left by
     * then, placing one dot on each turn before it.
     */
    fun doubleMoveIn(player: Player): Int? {
        val rule = turnRules[player.ordinal] ?: return null
        if (isOver || isCleanup) return null
        val turn = turns[player.ordinal]
        if (toMove == player && doubleDots > 0 && rule.isDouble(turn)) return 0
        val wait = (1..Int.MAX_VALUE).first { rule.isDouble(turn + it) }
        // The dots still to place this turn, if it is theirs, and one on each turn before the double one.
        val before = wait - 1 + if (toMove == player) dotsThisTurn else 0
        val left = dotsLeft(player) ?: return wait
        return wait.takeIf { left - before >= 2 }
    }

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

    /**
     * True for cells out of play: inside a capturing chain, a hole, a blocked point, or blocked by
     * a [Booster.WALL]. No further moves there. Rings pass over them as over empty points, except
     * over holes that count as border ([holesAreBorder]).
     */
    fun isDead(x: Int, y: Int): Boolean = dead[y * width + x]

    fun isDead(pos: Pos): Boolean = isDead(pos.x, pos.y)

    /** True for a blocked point: one of the board's from the start, or one blocked by a [Booster.WALL]. */
    fun isBlocked(x: Int, y: Int): Boolean = blocked[y * width + x]

    fun isBlocked(pos: Pos): Boolean = isBlocked(pos.x, pos.y)

    /** True for a hole: no point there, so no dot ever. It shapes the board. */
    fun isHole(x: Int, y: Int): Boolean = hole[y * width + x]

    fun isHole(pos: Pos): Boolean = isHole(pos.x, pos.y)

    /** True for a bonus point: a dot captured there is worth 2 ([worth]). */
    fun isBonus(x: Int, y: Int): Boolean = bonus[y * width + x]

    fun isBonus(pos: Pos): Boolean = isBonus(pos.x, pos.y)

    /** What capturing a dot on ([x], [y]) adds to the score: 2 on a bonus point, else 1. */
    fun worth(x: Int, y: Int): Int = if (bonus[y * width + x]) 2 else 1

    fun worth(pos: Pos): Int = worth(pos.x, pos.y)

    /**
     * True on the border, where no ring can pass: the outer edge of the grid, the holes outside
     * the board and the points next to them, up, down, left or right. A hole is outside when holes
     * count as border ([holesAreBorder]), or when it reaches the edge of the map, directly or
     * through other holes. A dot on the border can never be captured.
     */
    fun isBorder(x: Int, y: Int): Boolean {
        val i = y * width + x
        return edge[i] || (x > 0 && isOutside(i - 1)) || (x < width - 1 && isOutside(i + 1)) ||
            (y > 0 && isOutside(i - width)) || (y < height - 1 && isOutside(i + width))
    }

    fun isBorder(pos: Pos): Boolean = isBorder(pos.x, pos.y)

    /** A hole no ring can go round. */
    private fun isOutside(i: Int) = hole[i] && edge[i]

    /** How many of [booster] [player] has left to use in this game. */
    fun boostersLeft(player: Player, booster: Booster): Int = boosters[boosterIndex(player, booster)]

    /**
     * True if the player to move may use [booster] now, on some point if it needs one. A double
     * dot is not allowed in a turn that already has two dots.
     */
    fun canUse(booster: Booster): Boolean =
        !isOver && !isCleanup && !boosterUsed && boostersLeft(toMove, booster) > 0 &&
            (booster != Booster.DOUBLE_DOT || (doubleDots == 0 && (dotsLeft(toMove) ?: Int.MAX_VALUE) >= 2))

    /**
     * The opponent's dots a [Booster.BOMB] dropped on [center] would remove: those on [center] and
     * the four points next to it, up, down, left and right, that are not out of play. Captured dots
     * and dots inside a capturing chain are never touched, so a bomb cannot undo a capture.
     */
    fun bombHits(center: Pos): List<Pos> {
        if (!inBounds(center.x, center.y)) return emptyList()
        val enemy = toMove.opponent.code
        return PLUS.map { (dx, dy) -> Pos(center.x + dx, center.y + dy) }.filter { p ->
            inBounds(p.x, p.y) && owner[p.y * width + p.x] == enemy && !dead[p.y * width + p.x]
        }
    }

    /** For a cell inside a capturing chain: the player whose chain closed it most recently. */
    fun enclosedBy(x: Int, y: Int): Player? = when (enclosedBy[y * width + x]) {
        Player.RED.code -> Player.RED
        Player.BLUE.code -> Player.BLUE
        else -> null
    }

    fun enclosedBy(pos: Pos): Player? = enclosedBy(pos.x, pos.y)

    /**
     * True if the player to move may place a dot on ([x], [y]). In a [TurnRule.TwoHalves] turn,
     * the second dot must go in the other half from the first.
     */
    fun isLegal(x: Int, y: Int): Boolean =
        !isOver && (isCleanup || dotsLeft[toMove.ordinal] != 0) && inBounds(x, y) &&
            owner[y * width + x] == EMPTY && !dead[y * width + x] && (half == NO_HALF || halfOf(x) != half)

    fun isLegal(pos: Pos): Boolean = isLegal(pos.x, pos.y)

    fun legalMoves(): List<Pos> {
        if (isOver || (!isCleanup && dotsLeft[toMove.ordinal] == 0)) return emptyList()
        val moves = ArrayList<Pos>()
        for (i in owner.indices) {
            if (owner[i] == EMPTY && !dead[i] && (half == NO_HALF || halfOf(i % width) != half)) moves += Pos(i % width, i / width)
        }
        return moves
    }

    /**
     * True if the player to move may take [action] now. A pass or a stop is allowed at the start
     * of a [TurnRule]'s two-dot turn, but not after a double dot or between two dots.
     */
    fun isLegal(action: Action): Boolean = when (action) {
        is Action.Place -> isLegal(action.pos)
        Action.Pass, Action.Stop -> !isOver && !isCleanup && (doubleDots == 0 || (doubleDots == 2 && isRuleTurn()))
        Action.Finish -> isCleanup && !isOver
        is Action.UseBooster -> canUse(action.booster) && when (action) {
            is Action.Wall -> inBounds(action.pos.x, action.pos.y) && owner[index(action.pos)] == EMPTY && !dead[index(action.pos)]
            is Action.Hint -> isLegal(action.pos)
            is Action.Bomb -> bombHits(action.pos).isNotEmpty()
            Action.DoubleDot -> true
        }
    }

    fun apply(action: Action): MoveResult = when (action) {
        is Action.Place -> play(action.pos)
        Action.Pass -> MoveResult(pass(), emptyList())
        Action.Stop -> MoveResult(stop(), emptyList())
        Action.Finish -> MoveResult(finish(), emptyList())
        is Action.UseBooster -> MoveResult(useBooster(action), emptyList())
    }

    fun play(pos: Pos): MoveResult {
        require(isLegal(pos)) { "Illegal move $pos for $toMove" }
        return copy().applyMove(pos)
    }

    /**
     * Skips the turn. Two passes in a row end the game. Not allowed after a double dot or between
     * two dots of one turn.
     */
    fun pass(): GameState {
        check(isLegal(Action.Pass)) { "Cannot pass now" }
        val next = copy()
        next.passesInRow++
        val other = toMove.opponent
        next.beginTurn(if (next.canMove(other)) other else toMove)
        return next
    }

    /**
     * The player to move declares all the dots they want to keep grounded. From now on only their
     * opponent moves, with no dot limit, until they call [finish].
     */
    fun stop(): GameState {
        check(isLegal(Action.Stop)) { "Cannot stop now" }
        val next = copy()
        next.boosterUsed = false
        next.doubleDots = 0
        next.half = NO_HALF
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

    /**
     * Uses a booster of the player to move. It does not end the turn: the player still places
     * their dot (two after [Action.DoubleDot]). Boosters, and the rules for each:
     *  - [Action.Wall] blocks an empty point that is in play. Nobody may place a dot there; rings
     *    pass over it as over an empty point, so a wall on the point that would close a ring stops
     *    that ring there.
     *  - [Action.Hint] changes nothing on the board; [Action.Hint.pos] must be a legal move.
     *  - [Action.Bomb] removes the dots of [bombHits]; there must be at least one. Their points
     *    become empty and playable. Scores, dots left and dots placed do not change.
     *  - [Action.DoubleDot] needs 2 dots left (or no limit). The turn passes after the second dot,
     *    or after the first if the player cannot place another. No pass or stop in between.
     * Each needs [canUse]: one left, none used this turn, normal play.
     */
    fun useBooster(action: Action.UseBooster): GameState {
        require(isLegal(action)) { "Cannot use $action now for $toMove" }
        val next = copy()
        next.boosters[boosterIndex(toMove, action.booster)]--
        next.boosterUsed = true
        when (action) {
            is Action.Wall -> {
                val i = index(action.pos)
                next.blocked[i] = true
                next.dead[i] = true
                next.playableCells--
            }
            is Action.Hint -> Unit
            is Action.Bomb -> for (pos in bombHits(action.pos)) {
                next.owner[index(pos)] = EMPTY
                next.liveDots[toMove.opponent.ordinal]--
                next.playableCells++
            }
            Action.DoubleDot -> next.doubleDots = 2
        }
        return next
    }

    /**
     * One row per line: `.` empty, `+` an empty bonus point, `,` out of play, `#` a blocked point,
     * `-` a hole, `R`/`B` dots, `r`/`b` captured dots.
     */
    fun toAscii(): String = buildString {
        for (y in 0 until height) {
            if (y > 0) append('\n')
            for (x in 0 until width) {
                val i = y * width + x
                val player = ownerAt(x, y)
                append(
                    when {
                        player == null -> when {
                            hole[i] -> '-'
                            blocked[i] -> '#'
                            dead[i] -> ','
                            bonus[i] -> '+'
                            else -> '.'
                        }
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
        liveDots.clone(), dotsLeft.clone(), placed.clone(), blocked.clone(), hole, bonus, edge, holesAreBorder,
        boosters.clone(), playableCells, toMove,
        passesInRow, moveCount, cleaner, finished, boosterUsed, doubleDots, turnRules, turns.clone(), half,
    )

    private fun index(pos: Pos) = pos.y * width + pos.x

    /** [LEFT] for the first width / 2 columns, else [RIGHT]. */
    private fun halfOf(x: Int) = if (x < width / 2) LEFT else RIGHT

    /** True if [side] of the board still has a point in play with no dot on it. */
    private fun hasRoomIn(side: Int): Boolean {
        val columns = if (side == LEFT) 0 until width / 2 else width / 2 until width
        for (y in 0 until height) for (x in columns) {
            val i = y * width + x
            if (owner[i] == EMPTY && !dead[i]) return true
        }
        return false
    }

    /** True if the player to move is in a turn their [TurnRule] makes a turn of two dots. */
    private fun isRuleTurn(): Boolean = turnRules[toMove.ordinal]?.isDouble(turns[toMove.ordinal]) == true

    /**
     * Starts [player]'s next turn of normal play: no booster used yet, and two dots if their
     * [TurnRule] says so and they have 2 dots left.
     */
    private fun beginTurn(player: Player) {
        toMove = player
        turns[player.ordinal]++
        boosterUsed = false
        half = NO_HALF
        doubleDots = if (isRuleTurn() && (dotsLeft(player) ?: Int.MAX_VALUE) >= 2) 2 else 0
    }

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
            val twoHalves = turnRules[mover.ordinal] == TurnRule.TwoHalves
            val other = mover.opponent
            if (doubleDots == 2 && canMove(mover) && (!twoHalves || hasRoomIn(LEFT + RIGHT - halfOf(pos.x)))) {
                doubleDots = 1 // the second dot: same player, same turn
                if (twoHalves) half = halfOf(pos.x)
            } else {
                beginTurn(if (canMove(other) || !canMove(mover)) other else mover)
            }
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
     * new dot. Orthogonal flooding is right here: a diagonal join of walls is tight. Holes are
     * crossed like empty points; when they count as border, a piece that reaches one touches the
     * border.
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
                if (edge[i]) border = true
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

        val placedOnBorder = edge[placed]
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
        var points = 0
        val region = ArrayList<Pos>(to - from)
        for (k in from until to) {
            val i = cells[k]
            val pos = Pos(i % width, i / width)
            region += pos
            if (owner[i] == enemy.code && !captured[i]) {
                captured[i] = true
                liveDots[enemy.ordinal]--
                dots += pos
                points += if (bonus[i]) 2 else 1
            }
            enclosedBy[i] = by.code
            if (!dead[i]) {
                dead[i] = true
                if (owner[i] == EMPTY) playableCells--
            }
        }
        scores[by.ordinal] += points
        out += Capture(by, dots, region, points)
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
            if (edge[i] && !isWall(i, wall)) {
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

        /** The halves of the board in a [TurnRule.TwoHalves] turn. */
        private const val NO_HALF = -1
        private const val LEFT = 0
        private const val RIGHT = 1

        private val DX = intArrayOf(-1, 1, 0, 0)
        private val DY = intArrayOf(0, 0, -1, 1)

        /** A bomb's points around its centre. */
        private val PLUS = listOf(0 to 0, -1 to 0, 1 to 0, 0 to -1, 0 to 1)

        private fun boosterIndex(player: Player, booster: Booster) = player.ordinal * Booster.entries.size + booster.ordinal

        /**
         * Starts a game. [dots] are placed up front (an opening or a head start) and do not count
         * against [dotsPerPlayer]; they must not already enclose an enemy dot. [dotsFor] gives a
         * player a budget of their own instead of [dotsPerPlayer]. [boosters] gives each player the
         * boosters they may use in this game, by kind and count. [turnRules] gives a player a
         * [TurnRule], with two dots on some turns.
         *
         * The board's shape and special points: [holes] have no point (and [holesAreBorder] says
         * whether their edge counts as border), nobody may place a dot on [blocked] points, and a
         * dot captured on a [bonus] point is worth 2. None of them may hold a starting dot.
         */
        fun start(
            width: Int,
            height: Int,
            dotsPerPlayer: Int? = null,
            first: Player = Player.RED,
            dots: Map<Pos, Player> = emptyMap(),
            dotsFor: Map<Player, Int> = emptyMap(),
            boosters: Map<Player, Map<Booster, Int>> = emptyMap(),
            turnRules: Map<Player, TurnRule> = emptyMap(),
            holes: Set<Pos> = emptySet(),
            holesAreBorder: Boolean = true,
            blocked: Set<Pos> = emptySet(),
            bonus: Set<Pos> = emptySet(),
        ): GameState {
            require(width >= 3 && height >= 3) { "Board must be at least 3x3" }
            require(dotsPerPlayer == null || dotsPerPlayer >= 0) { "dotsPerPlayer must not be negative" }
            require(dotsFor.values.all { it >= 0 }) { "a player's dots must not be negative" }
            require(boosters.values.all { kinds -> kinds.values.all { it >= 0 } }) { "booster counts must not be negative" }
            require(TurnRule.TwoHalves !in turnRules.values || width % 2 == 0) { "two halves need a board of even width" }
            val n = width * height
            val budgets = IntArray(2) { dotsFor[Player.entries[it]] ?: dotsPerPlayer ?: -1 }
            val stock = IntArray(2 * Booster.entries.size)
            for ((player, kinds) in boosters) for ((booster, count) in kinds) stock[boosterIndex(player, booster)] = count
            fun cells(points: Set<Pos>, what: String) = BooleanArray(n).also { cells ->
                for (pos in points) {
                    require(pos.x in 0 until width && pos.y in 0 until height) { "$what $pos is off the board" }
                    cells[pos.y * width + pos.x] = true
                }
            }
            val hole = cells(holes, "Hole")
            val blockedCells = cells(blocked, "Blocked point")
            val bonusCells = cells(bonus, "Bonus point")
            require((0 until n).none { (hole[it] && (blockedCells[it] || bonusCells[it])) || (blockedCells[it] && bonusCells[it]) }) {
                "A point can be only one of hole, blocked or bonus"
            }
            val edge = BooleanArray(n) { i ->
                val x = i % width
                val y = i / width
                x == 0 || y == 0 || x == width - 1 || y == height - 1 || (holesAreBorder && hole[i])
            }
            // Open holes that reach the edge of the map through other holes are outside as well.
            val reach = ArrayDeque((0 until n).filter { hole[it] && edge[it] })
            while (reach.isNotEmpty()) {
                val i = reach.removeLast()
                for (d in 0 until 4) {
                    val x = i % width + DX[d]
                    val y = i / width + DY[d]
                    val j = y * width + x
                    if (x in 0 until width && y in 0 until height && hole[j] && !edge[j]) {
                        edge[j] = true
                        reach.addLast(j)
                    }
                }
            }
            val dead = BooleanArray(n) { hole[it] || blockedCells[it] }
            val state = GameState(
                width, height, ByteArray(n), BooleanArray(n), dead, ByteArray(n), IntArray(2),
                IntArray(2), budgets, IntArray(2), blockedCells, hole, bonusCells, edge, holesAreBorder, stock,
                n - dead.count { it }, first, 0, 0, null, false, false, 0,
                Array(2) { turnRules[Player.entries[it]] }, IntArray(2), NO_HALF,
            )
            for ((pos, player) in dots) {
                require(state.inBounds(pos.x, pos.y)) { "Starting dot $pos is off the board" }
                val i = pos.y * width + pos.x
                require(state.owner[i] == EMPTY) { "Two starting dots on $pos" }
                require(!dead[i] && !bonusCells[i]) { "Starting dot $pos is on a hole, a blocked point or a bonus point" }
                state.owner[i] = player.code
                state.liveDots[player.ordinal]++
                state.playableCells--
            }
            require(!state.hasEnclosedEnemy(Player.RED) && !state.hasEnclosedEnemy(Player.BLUE)) {
                "The starting position already encloses an enemy dot"
            }
            state.beginTurn(if (!state.canMove(first) && state.canMove(first.opponent)) first.opponent else first)
            return state
        }

        /**
         * Builds a start position from a map: `.` empty, `R`/`B` dots, `-` a hole, `#` a blocked
         * point, `+` a bonus point; whitespace is ignored.
         */
        fun fromAscii(
            ascii: String,
            dotsPerPlayer: Int? = null,
            first: Player = Player.RED,
            holesAreBorder: Boolean = true,
        ): GameState {
            val rows = ascii.lines().map { line -> line.filterNot { it.isWhitespace() } }.filter { it.isNotEmpty() }
            require(rows.isNotEmpty()) { "Empty map" }
            val width = rows[0].length
            require(rows.all { it.length == width }) { "All map rows must have the same length" }
            val dots = HashMap<Pos, Player>()
            val holes = HashSet<Pos>()
            val blocked = HashSet<Pos>()
            val bonus = HashSet<Pos>()
            for ((y, row) in rows.withIndex()) {
                for ((x, ch) in row.withIndex()) {
                    when (ch) {
                        '.' -> Unit
                        Player.RED.symbol -> dots[Pos(x, y)] = Player.RED
                        Player.BLUE.symbol -> dots[Pos(x, y)] = Player.BLUE
                        '-' -> holes += Pos(x, y)
                        '#' -> blocked += Pos(x, y)
                        '+' -> bonus += Pos(x, y)
                        else -> throw IllegalArgumentException("Unknown map symbol '$ch' at ($x, $y)")
                    }
                }
            }
            return start(
                width, rows.size, dotsPerPlayer, first, dots,
                holes = holes, holesAreBorder = holesAreBorder, blocked = blocked, bonus = bonus,
            )
        }
    }
}
