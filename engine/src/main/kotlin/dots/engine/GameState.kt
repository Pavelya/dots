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
 *  - Optional dot budget per player, for short mobile sessions and puzzle levels. The budget
 *    counts turns: the second dot of a turn of two dots is an extra dot (decision 0039), so a
 *    turn of two never makes a player run out early.
 *  - Boosters for the solo campaign ([Booster], [useBooster]): a player may use at most one per
 *    turn, before placing their dot, and only during normal play (not after a [stop]). Undo is one
 *    of them: the state keeps where the player's last turn began, so an undo is an action like any
 *    other and a record with undos replays by applying its actions in order. A bomb that removes a
 *    dot of a ring breaks that capture (decision 0042): the area goes back into play.
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
    /** The extra dots each player has placed: second dots of two-dot turns, by [Player.ordinal]. */
    private val extra: IntArray,
    /** The cell of the dot each player placed last, by [Player.ordinal]; -1 before their first. */
    private val lastPlaced: IntArray,
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
    /**
     * True if the second dot of a two-dot turn is an extra dot, which takes nothing from the
     * player's dots left (decision 0039). False only to replay records from before that rule, when
     * both dots came out of the budget. Shared between states, never changed.
     */
    private val extraSecondDot: Boolean,
    /**
     * For [Booster.UNDO]: at [Player.ordinal] * 2, the state at the start of the player's current
     * turn; at the next index, the state at the start of their turn before it, where an undo goes
     * back to. Null in a game where nobody has an Undo, so other games pay nothing for it.
     */
    private val turnStarts: Array<GameState?>?,
    /**
     * The captures that stand on the board, oldest first, so a bomb can break one exactly. The
     * list is never changed, only replaced, so states share it.
     */
    private var held: List<Held>,
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

    /**
     * True between the two dots of a two-dot turn, while its extra second dot is still to place.
     * The turn already counts in [movesMade] and [dotsLeft], from its first dot.
     */
    val extraDotToCome: Boolean get() = extraSecondDot && doubleDots == 1 && !isOver

    /** The [TurnRule] of [player], if any. */
    fun turnRule(player: Player): TurnRule? = turnRules[player.ordinal]

    /**
     * How many of [player]'s turns until their next turn of two dots under their [TurnRule]: 0
     * while they are placing it, 1 if their next turn is one, and so on. The player's current
     * turn, if it is a turn of one dot, is not counted. Null when it cannot come: no rule, the game
     * is over or in collecting after a [stop], or the player's dots left run out before that turn.
     */
    fun doubleMoveIn(player: Player): Int? {
        val rule = turnRules[player.ordinal] ?: return null
        if (isOver || isCleanup) return null
        val turn = turns[player.ordinal]
        if (toMove == player && doubleDots > 0 && rule.isDouble(turn)) return 0
        val wait = (1..Int.MAX_VALUE).first { rule.isDouble(turn + it) }
        val left = dotsLeft(player) ?: return wait
        if (!extraSecondDot) {
            // The old rule: the dots still to place this turn and one on each turn before come out
            // of the budget, and the double move needs 2 more.
            return wait.takeIf { left - (wait - 1 + if (toMove == player) dotsThisTurn else 0) >= 2 }
        }
        // One from the budget for each turn before it, and for this turn if it is theirs and its
        // first dot is still to place. The double move then needs 1.
        val before = wait - 1 + if (toMove == player && doubleDots != 1) 1 else 0
        return wait.takeIf { left - before >= 1 }
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

    /**
     * [player]'s budget still to use, or null when there is no limit: one for each turn they may
     * still begin, and for their current turn until its first dot is placed. A turn of two dots
     * takes one, like any other turn; its second dot is extra ([dotsToPlace] counts those too).
     */
    fun dotsLeft(player: Player): Int? = dotsLeft[player.ordinal].takeIf { it >= 0 }

    /**
     * The most dots [player] can still place in normal play, or null when there is no limit:
     * their [dotsLeft], plus the extra second dot of each two-dot turn they have left. Those are
     * the turns their [TurnRule] gives them and the turn they are placing, if it has two dots. A
     * double dot they have not used yet is not counted. After a [stop], just their [dotsLeft].
     */
    fun dotsToPlace(player: Player): Int? {
        val left = dotsLeft(player) ?: return null
        if (!extraSecondDot || isCleanup) return left
        val mine = toMove == player && !isOver
        // The current turn's second dot, if it has one still to place.
        var dots = left + if (mine && doubleDots > 0) 1 else 0
        val rule = turnRules[player.ordinal] ?: return dots
        // The turns still to begin, each taking one from the budget.
        val next = turns[player.ordinal] + 1
        val count = left - if (mine && doubleDots != 1) 1 else 0
        for (turn in next until next + count) if (rule.isDouble(turn)) dots++
        return dots
    }

    /** Dots [player] has placed in this game, collecting after a [stop] included; starting dots are not counted. */
    fun dotsPlaced(player: Player): Int = placed[player.ordinal]

    /**
     * [player]'s moves so far: the [dotsPlaced], less the extra second dots of their two-dot
     * turns. A turn of two dots is one move, as missions count them ("capture 2 in 4 moves").
     */
    fun movesMade(player: Player): Int = placed[player.ordinal] - extra[player.ordinal]

    /** Where [player] placed their last dot, or null if they have not placed one; starting dots do not count. */
    fun lastPlaced(player: Player): Pos? = lastPlaced[player.ordinal].takeIf { it >= 0 }?.let { Pos(it % width, it / width) }

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
     * dot is not allowed in a turn that already has two dots. An undo needs a turn of theirs to take
     * back, and is used at the start of a turn: before any booster or dot of it.
     */
    fun canUse(booster: Booster): Boolean =
        !isOver && !isCleanup && !boosterUsed && boostersLeft(toMove, booster) > 0 &&
            when (booster) {
                Booster.DOUBLE_DOT -> doubleDots == 0 && (dotsLeft(toMove) ?: Int.MAX_VALUE) >= dotsForTwo
                Booster.UNDO -> doubleDots != 1 && turnStarts?.get(toMove.ordinal * 2 + 1) != null
                else -> true
            }

    /**
     * The opponent's dots a [Booster.BOMB] dropped on [center] would remove: those on [center] and
     * the four points next to it, up, down, left and right, that are not out of play. Captured dots
     * and dots inside a capturing chain are never touched. A dot of a ring can be hit: the bomb then
     * breaks that capture ([bombBreaks]).
     */
    fun bombHits(center: Pos): List<Pos> {
        if (!inBounds(center.x, center.y)) return emptyList()
        val enemy = toMove.opponent.code
        return PLUS.map { (dx, dy) -> Pos(center.x + dx, center.y + dy) }.filter { p ->
            inBounds(p.x, p.y) && owner[p.y * width + p.x] == enemy && !dead[p.y * width + p.x]
        }
    }

    /**
     * The captured areas a [Booster.BOMB] dropped on [center] would break, as they lie on the
     * board now: those of the opponent's captures that one of [bombHits] holds closed (decision
     * 0042). A dot holds a capture closed when it is next to one of its points, up, down, left or
     * right; a dot that touches it only at a corner does not.
     */
    fun bombBreaks(center: Pos): List<Area> {
        val broken = breaking(bombHits(center).map { index(it) })
        if (broken.isEmpty()) return emptyList()
        val standing = BooleanArray(width * height)
        for (record in held) if (record !in broken) for (i in record.region) standing[i] = true
        val freed = HashSet<Pos>()
        for (record in broken) for (i in record.region) if (!standing[i]) freed += Pos(i % width, i / width)
        return areas().filter { area -> area.region.any { it in freed } }
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
        !isOver && (isCleanup || hasDot(toMove)) && inBounds(x, y) &&
            owner[y * width + x] == EMPTY && !dead[y * width + x] && (half == NO_HALF || halfOf(x) != half)

    fun isLegal(pos: Pos): Boolean = isLegal(pos.x, pos.y)

    fun legalMoves(): List<Pos> {
        if (isOver || (!isCleanup && !hasDot(toMove))) return emptyList()
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
            Action.DoubleDot, Action.Undo -> true
        }
    }

    fun apply(action: Action): MoveResult = when (action) {
        is Action.Place -> play(action.pos)
        Action.Pass -> MoveResult(pass(), emptyList())
        Action.Stop -> MoveResult(stop(), emptyList())
        Action.Finish -> MoveResult(finish(), emptyList())
        is Action.UseBooster -> {
            val pos = when (action) {
                is Action.Wall -> action.pos
                is Action.Hint -> action.pos
                is Action.Bomb -> action.pos
                Action.DoubleDot, Action.Undo -> null
            }
            val removed = if (action is Action.Bomb) bombHits(action.pos) else emptyList()
            val broken = if (action is Action.Bomb) bombBreaks(action.pos) else emptyList()
            MoveResult(useBooster(action), emptyList(), BoosterEffect(toMove, action.booster, pos, removed, broken))
        }
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
     *    become empty and playable. Each capture one of them held closed breaks ([bombBreaks],
     *    decision 0042): the dots it took are alive again, its captor's score drops by the points
     *    it gave, and its points go back into play, except those an earlier capture still holds.
     *    The bomb captures nothing itself; the captor may close the ring again with a new dot.
     *    Dots left and dots placed do not change.
     *  - [Action.DoubleDot] gives the player an extra dot this turn: the second, which takes
     *    nothing from their dots left, so 1 left is enough. The turn passes after the second dot,
     *    or after the first if the board leaves no point for another. No pass or stop in between.
     *  - [Action.Undo] goes back to the start of the player's last turn: their dot or dots, any
     *    booster they used in it (which they get back) and the opponent's reply are taken back.
     *    Only the Undo count stays as it is now, less the one used. It needs a turn to take back
     *    and is used at the start of a turn, so it is never the turn's one booster: the player may
     *    still use another in the turn they play again.
     * Each needs [canUse]: one left, none used this turn, normal play.
     */
    fun useBooster(action: Action.UseBooster): GameState {
        require(isLegal(action)) { "Cannot use $action now for $toMove" }
        if (action == Action.Undo) return takenBack()
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
            is Action.Bomb -> next.bomb(bombHits(action.pos).map { index(it) })
            Action.DoubleDot -> next.doubleDots = 2
            Action.Undo -> Unit // returned above
        }
        return next
    }

    /** The state an [Action.Undo] leads to: see [useBooster]. */
    private fun takenBack(): GameState {
        val i = toMove.ordinal * 2
        val next = turnStarts!![i + 1]!!.copy()
        for (player in Player.entries) {
            next.boosters[boosterIndex(player, Booster.UNDO)] = boostersLeft(player, Booster.UNDO)
        }
        next.boosters[boosterIndex(toMove, Booster.UNDO)]--
        next.turnStarts!![i] = next
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

    private fun canMove(player: Player): Boolean = hasDot(player) && playableCells > 0

    /** True if [player] has a dot to place: some left, or the extra second dot of the turn they are placing. */
    private fun hasDot(player: Player): Boolean =
        dotsLeft[player.ordinal] != 0 || (extraSecondDot && player == toMove && doubleDots == 1)

    /** The dots left a turn of two dots needs: 1 under the rule of decision 0039, 2 under the old one. */
    private val dotsForTwo: Int get() = if (extraSecondDot) 1 else 2

    private fun copy() = GameState(
        width, height, owner.clone(), captured.clone(), dead.clone(), enclosedBy.clone(), scores.clone(),
        liveDots.clone(), dotsLeft.clone(), placed.clone(), extra.clone(), lastPlaced.clone(), blocked.clone(), hole, bonus, edge, holesAreBorder,
        boosters.clone(), playableCells, toMove,
        passesInRow, moveCount, cleaner, finished, boosterUsed, doubleDots, turnRules, turns.clone(), half,
        extraSecondDot, turnStarts?.clone(), held,
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
     * [TurnRule] says so and they have a dot left (2 under the old rule).
     */
    private fun beginTurn(player: Player) {
        turnStarts?.let { starts ->
            val i = player.ordinal * 2
            starts[i + 1] = starts[i]
            starts[i] = this
        }
        toMove = player
        turns[player.ordinal]++
        boosterUsed = false
        half = NO_HALF
        doubleDots = if (isRuleTurn() && (dotsLeft(player) ?: Int.MAX_VALUE) >= dotsForTwo) 2 else 0
    }

    private fun applyMove(pos: Pos): MoveResult {
        val mover = toMove
        val index = pos.y * width + pos.x
        owner[index] = mover.code
        liveDots[mover.ordinal]++
        placed[mover.ordinal]++
        lastPlaced[mover.ordinal] = index
        playableCells--
        // The second dot of a two-dot turn is extra: it takes nothing from the budget.
        if (extraSecondDot && doubleDots == 1) {
            extra[mover.ordinal]++
        } else if (!isCleanup && dotsLeft[mover.ordinal] > 0) {
            dotsLeft[mover.ordinal]--
        }

        val captures = ArrayList<Capture>(0)
        captureFrom(index, mover, captures)

        moveCount++
        passesInRow = 0
        if (!isCleanup) {
            val twoHalves = turnRules[mover.ordinal] == TurnRule.TwoHalves
            val other = mover.opponent
            val second = playableCells > 0 && (extraSecondDot || dotsLeft[mover.ordinal] != 0)
            if (doubleDots == 2 && second && (!twoHalves || hasRoomIn(LEFT + RIGHT - halfOf(pos.x)))) {
                doubleDots = 1 // the second dot: same player, same turn
                if (twoHalves) half = halfOf(pos.x)
            } else {
                doubleDots = 0 // the turn is done
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
            if (holdsEnemy) claim(buffer, starts[k], ends[k], player, Pos(px, py), out)
        }
    }

    private fun claim(cells: IntArray, from: Int, to: Int, by: Player, closing: Pos, out: MutableList<Capture>) {
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
        held = held + Held(by, cells.copyOfRange(from, to), IntArray(dots.size) { index(dots[it]) }, points)
        out += Capture(by, dots, region, closing, points)
    }

    /**
     * Removes the opponent's dots on the cells [hits] and breaks the captures they held closed:
     * see [useBooster].
     */
    private fun bomb(hits: List<Int>) {
        val enemy = toMove.opponent
        val broken = breaking(hits)
        for (i in hits) {
            owner[i] = EMPTY
            liveDots[enemy.ordinal]--
            playableCells++
        }
        if (broken.isEmpty()) return
        held = held.filter { it !in broken }
        for (record in broken) {
            for (i in record.dots) {
                captured[i] = false
                liveDots[record.by.opponent.ordinal]++
            }
            scores[record.by.ordinal] -= record.points
        }
        // A point a standing capture still holds stays out of play, inside its latest captor's ring.
        val holder = arrayOfNulls<Held>(width * height)
        for (record in held) for (i in record.region) holder[i] = record
        for (record in broken) for (i in record.region) {
            val by = holder[i]?.by
            enclosedBy[i] = by?.code ?: EMPTY
            if (dead[i] && by == null && !hole[i] && !blocked[i]) {
                dead[i] = false
                if (owner[i] == EMPTY) playableCells++
            }
        }
    }

    /** The opponent's captures that a dot on one of the cells [hits] holds closed: one next to a point of the area. */
    private fun breaking(hits: List<Int>): List<Held> {
        if (hits.isEmpty()) return emptyList()
        val hit = BooleanArray(width * height)
        for (i in hits) hit[i] = true
        val captor = toMove.opponent
        return held.filter { record ->
            record.by == captor && record.region.any { i ->
                val x = i % width
                val y = i / width
                (x > 0 && hit[i - 1]) || (x < width - 1 && hit[i + 1]) || (y > 0 && hit[i - width]) || (y < height - 1 && hit[i + width])
            }
        }
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

    /**
     * A capture that stands on the board: [by] closed a ring round the cells [region] and took the
     * dots on the cells [dots], worth [points]. Cells are indexes, y * width + x.
     */
    private class Held(val by: Player, val region: IntArray, val dots: IntArray, val points: Int)

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
         *
         * [extraSecondDot] false plays the old rule, where both dots of a two-dot turn came out of
         * the budget; it is only for replaying records from before decision 0039.
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
            extraSecondDot: Boolean = true,
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
                IntArray(2), budgets, IntArray(2), IntArray(2), IntArray(2) { -1 }, blockedCells, hole, bonusCells, edge, holesAreBorder, stock,
                n - dead.count { it }, first, 0, 0, null, false, false, 0,
                Array(2) { turnRules[Player.entries[it]] }, IntArray(2), NO_HALF, extraSecondDot,
                if (Player.entries.any { stock[boosterIndex(it, Booster.UNDO)] > 0 }) arrayOfNulls(4) else null,
                emptyList(),
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
