package dots.campaign

import dots.ai.AiConfig
import dots.engine.Booster
import dots.engine.GameState
import dots.engine.Notation
import dots.engine.Player
import dots.engine.Pos
import dots.engine.TurnRule

/**
 * A single-player level: the human (red, moving first) plays the computer (blue). Levels are
 * stored as text files; [LevelFormat] reads and writes them.
 *
 * Difficulty comes from the computer's [strength], the [map] it starts with (`B` dots are its head
 * start, `R` dots are the human's), the [dotsPerPlayer] budget and the [mission]. [boosters] are
 * the ones the human may use in the level, by kind and count. A boss level has a [rule] the
 * computer bends. [stars] are the targets for two and three stars. [solutions] says how many first
 * moves may pass a puzzle mission (see [Solver]).
 * The map may have holes, blocked points and bonus points; [holes] says whether the edge of a hole
 * counts as border. [note] is the designer's one line on what the level is for; players never see it.
 */
data class Level(
    val world: Int,
    val number: Int,
    val name: String,
    val strength: Strength,
    val dotsPerPlayer: Int,
    val map: LevelMap,
    val mission: Mission = Mission.Win,
    val boosters: Map<Booster, Int> = emptyMap(),
    val rule: SpecialRule? = null,
    val stars: Stars? = null,
    val solutions: Solutions = Solutions.FEW,
    val holes: Holes = Holes.NONE,
    val note: String? = null,
) {
    init {
        require(world >= 1) { "world must be 1 or more" }
        require(number in 1..99) { "level number must be 1 to 99" }
        require(name.isNotBlank() && name.lines().size == 1 && name == name.trim()) { "name must be one line of text" }
        require(note == null || (note.isNotBlank() && note.lines().size == 1 && note == note.trim())) { "note must be one line of text" }
        require(dotsPerPlayer >= 1) { "dotsPerPlayer must be 1 or more" }
        require(boosters.values.all { it >= 1 }) { "booster counts must be 1 or more" }
        rule?.problem(map, dotsPerPlayer)?.let { throw IllegalArgumentException(it) }
        holes.problem(map)?.let { throw IllegalArgumentException(it) }
        checkMission()
        stars?.problem(mission, map, dotsPerPlayer, boosters)?.let { throw IllegalArgumentException(it) }
    }

    val human: Player get() = Player.RED
    val computer: Player get() = Player.BLUE

    /** World and number, as players see it: `1-3`. */
    val code: String get() = "$world-$number"

    val ai: AiConfig get() = strength.config

    /** Dots the human may place: [dotsPerPlayer], or fewer in a "win with fewer dots" mission. */
    val humanDots: Int get() = (mission as? Mission.WinWithFewerDots)?.dots ?: dotsPerPlayer

    /** How many more dots than the human the computer starts with. */
    val headStart: Int get() = map.points(LevelMap.COMPUTER).size - map.points(LevelMap.HUMAN).size

    /**
     * The computer's dots that make up its fortress, for the app to draw: empty unless the [rule]
     * is a [SpecialRule.Fortress].
     */
    val fortress: List<Pos> get() = if (rule is SpecialRule.Fortress) map.groups(LevelMap.COMPUTER).first() else emptyList()

    /** A new game of this level; [gift] gives the player boosters on top of the level's own. */
    fun newGame(gift: Map<Booster, Int> = emptyMap()): GameState = start(gift)

    /** Where the mission stands in [state], a position of this level's game. */
    fun status(state: GameState): MissionStatus = judge(mission, state, human)

    /** The level ends when its game is over or its mission is decided, whichever comes first. */
    fun isOver(state: GameState): Boolean = state.isOver || status(state).isDecided

    /** The level is passed when its mission is; in "win", a draw (for instance 0-0) is not enough. */
    fun isPassed(end: GameState): Boolean = status(end).outcome == Outcome.PASSED

    /** What the [stars]' measure counts in [state], or null if the level has no star targets. */
    fun starScore(state: GameState): Int? = stars?.let { count(it, this, state) }

    /**
     * The result of a level game that ended in [end]: the mission's status and the stars. A passed
     * level earns 1 to 3 stars (1 when it has no star targets), a failed one none.
     */
    fun result(end: GameState): LevelResult {
        val status = status(end)
        val score = starScore(end)
        val earned = when {
            status.outcome != Outcome.PASSED -> 0
            score == null -> 1
            else -> stars!!.earned(score)
        }
        return LevelResult(status, earned, score)
    }

    /**
     * The human has the level's [boosters], plus any [gift]; the computer has none (decision 0006).
     * The computer has the [rule]'s turn rule, if any.
     */
    private fun start(gift: Map<Booster, Int> = emptyMap()) = map.start(
        holes, dotsPerPlayer, human, dotsFor = mapOf(human to humanDots),
        boosters = mapOf(human to (boosters.keys + gift.keys).associateWith { (boosters[it] ?: 0) + (gift[it] ?: 0) }),
        turnRules = rule?.turnRule?.let { mapOf(computer to it) } ?: emptyMap(),
    )

    private fun checkMission() {
        val what = "the mission '${mission.text}'"
        when (mission) {
            is Mission.WinWithFewerDots -> require(mission.dots < dotsPerPlayer) {
                "$what must give the player fewer dots than the computer's $dotsPerPlayer"
            }
            is Mission.Capture -> require(mission.moves <= humanDots) {
                "$what has more moves than the $humanDots dots the player may place"
            }
            is Mission.Survive -> require(mission.moves <= dotsPerPlayer) {
                "$what has more moves than the $dotsPerPlayer dots the computer may place"
            }
            is Mission.Rescue -> require(map[mission.dot] == LevelMap.HUMAN) {
                "${Notation.name(mission.dot)} must hold a human dot ('R' on the map) for $what"
            }
            is Mission.Target -> {
                require(map[mission.dot] == LevelMap.COMPUTER) {
                    "${Notation.name(mission.dot)} must hold a computer dot ('B' on the map) for $what"
                }
                // A bomb would remove the target instead of capturing it.
                require(Booster.BOMB !in boosters) { "$what cannot allow the bomb, which would remove the target" }
            }
            Mission.Win, is Mission.WinBy -> Unit
        }
        require(!status(start()).isDecided) { "$what is already decided before the first move" }
    }
}

/**
 * How many first moves may pass a level's mission, as `cli check-levels` checks it. Only puzzle
 * missions are checked ([Solver.isPuzzle]).
 */
enum class Solutions(val text: String) {
    /** At most three: the player has to find the answer. */
    FEW("few"),

    /** Any number: a teaching level, where any sensible move should work. */
    OPEN("open"),
}

/**
 * Whether the edge of a level's holes counts as border. It is [NONE] exactly when the map has no
 * holes.
 */
enum class Holes(val text: String) {
    /** The map has no holes. */
    NONE("none"),

    /** A hole's edge is border: a dot next to a hole can never be captured, and no ring goes round a hole. */
    BORDER("border"),

    /** A hole is open: rings go round and over it as over a blocked point, and a dot next to one can be captured. */
    OPEN("open"),
    ;

    /** Why this does not fit [map], or null if it fits. */
    fun problem(map: LevelMap): String? {
        val hasHoles = map.points(LevelMap.HOLE).isNotEmpty()
        return when {
            hasHoles && this == NONE ->
                "the map has holes: say whether their edge counts as border ('holes: border') or not ('holes: open')"
            !hasHoles && this != NONE -> "the map has no holes, so 'holes' must be 'none'"
            else -> null
        }
    }
}

/** How strongly the computer plays a level. */
enum class Strength(val config: AiConfig) {
    BEGINNER(AiConfig.BEGINNER),
    EASY(AiConfig.EASY),
    STEADY(AiConfig.STEADY),
    NORMAL(AiConfig.NORMAL),
    HARD(AiConfig.HARD),
    EXPERT(AiConfig.EXPERT),
    ;

    val text: String get() = name.lowercase()
}

/**
 * The board of a level, one character per point, rows from the top:
 * `.` an empty point, `R` a human (red) dot, `B` a computer (blue) dot, `-` a hole (no point there,
 * which shapes the board), `#` a blocked point, `+` a bonus point.
 */
data class LevelMap(val rows: List<String>) {
    init {
        require(rows.isNotEmpty()) { "the map has no rows" }
        require(rows.all { it.length == rows[0].length }) { "all map rows must have the same length" }
        require(rows.size in SIZES && rows[0].length in SIZES) { "boards are 3 to 99 points in each direction" }
        require(rows.all { row -> row.all { it in SYMBOLS } }) { "unknown map symbol" }
    }

    val width: Int get() = rows[0].length
    val height: Int get() = rows.size

    operator fun get(pos: Pos): Char? = rows.getOrNull(pos.y)?.getOrNull(pos.x)

    fun points(symbol: Char): List<Pos> =
        rows.flatMapIndexed { y, row -> row.indices.filter { row[it] == symbol }.map { Pos(it, y) } }

    fun startDots(): Map<Pos, Player> =
        points(HUMAN).associateWith { Player.RED } + points(COMPUTER).associateWith { Player.BLUE }

    /**
     * A game on this board: its shape, special points and starting dots, with [holes] saying
     * whether the edge of a hole counts as border. The other arguments are those of [GameState.start].
     */
    fun start(
        holes: Holes,
        dotsPerPlayer: Int? = null,
        first: Player = Player.RED,
        dotsFor: Map<Player, Int> = emptyMap(),
        boosters: Map<Player, Map<Booster, Int>> = emptyMap(),
        turnRules: Map<Player, TurnRule> = emptyMap(),
    ): GameState = GameState.start(
        width, height, dotsPerPlayer, first, startDots(), dotsFor, boosters, turnRules,
        holes = points(HOLE).toSet(), holesAreBorder = holes != Holes.OPEN,
        blocked = points(BLOCKED).toSet(), bonus = points(BONUS).toSet(),
    )

    /**
     * The groups of [symbol]'s dots, biggest first: dots joined up, down, left, right or
     * diagonally, as the links of a chain are. Each group lists its dots row by row.
     */
    fun groups(symbol: Char): List<List<Pos>> {
        val left = points(symbol).toMutableSet()
        val groups = ArrayList<List<Pos>>()
        while (left.isNotEmpty()) {
            val first = left.first()
            left.remove(first)
            val group = arrayListOf(first)
            var i = 0
            while (i < group.size) {
                val (x, y) = group[i++]
                for (dy in -1..1) for (dx in -1..1) {
                    val next = Pos(x + dx, y + dy)
                    if (left.remove(next)) group += next
                }
            }
            groups += group.sortedWith(compareBy({ it.y }, { it.x }))
        }
        return groups.sortedByDescending { it.size }
    }

    companion object {
        const val EMPTY = '.'
        const val HUMAN = 'R'
        const val COMPUTER = 'B'
        const val HOLE = '-'
        const val BLOCKED = '#'
        const val BONUS = '+'
        const val SYMBOLS = "$EMPTY$HUMAN$COMPUTER$HOLE$BLOCKED$BONUS"
        val SIZES = 3..99
    }
}

/**
 * What the human must do to pass a level. A "move" is one dot placed. Capture, survive, rescue and
 * target end the level as soon as they are decided; the three win missions pass only when the game
 * is over, because the margin still counts for stars, but fail as soon as the target is out of
 * reach. [Level.status] judges them.
 */
sealed interface Mission {
    /** The words a level file uses for it. */
    val text: String

    /** End with more captured dots than the computer. */
    data object Win : Mission {
        override val text = "win"
    }

    /** Win with at least [margin] more captured dots than the computer. */
    data class WinBy(val margin: Int) : Mission {
        init {
            require(margin >= 1) { "margin must be 1 or more" }
        }

        override val text get() = "win by $margin"
    }

    /** Win with only [dots] dots to place, against the computer's level budget. */
    data class WinWithFewerDots(val dots: Int) : Mission {
        init {
            require(dots >= 1) { "dots must be 1 or more" }
        }

        override val text get() = "win with ${count(dots, "dot")}"
    }

    /** Capture at least [dots] dots within the human's first [moves] moves. */
    data class Capture(val dots: Int, val moves: Int) : Mission {
        init {
            require(dots >= 1 && moves >= 1) { "dots and moves must be 1 or more" }
        }

        override val text get() = "capture $dots in ${count(moves, "move")}"
    }

    /** Lose no dot while the computer places its next [moves] dots. */
    data class Survive(val moves: Int) : Mission {
        init {
            require(moves >= 1) { "moves must be 1 or more" }
        }

        override val text get() = "survive ${count(moves, "move")}"
    }

    /**
     * Keep the human's dot at [dot] (and so the group holding it) from being captured. It passes
     * once the dot is joined to the border by a straight chain (see `Grounding.safe`), or at the
     * end of the game.
     */
    data class Rescue(val dot: Pos) : Mission {
        override val text get() = "rescue ${Notation.name(dot)}"
    }

    /** Capture the computer's dot at [dot]. It fails once that dot can never be captured. */
    data class Target(val dot: Pos) : Mission {
        override val text get() = "target ${Notation.name(dot)}"
    }

    private companion object {
        fun count(n: Int, word: String) = if (n == 1) "1 $word" else "$n ${word}s"
    }
}

/**
 * A rule the computer bends on a boss level, announced to the player before the first move. A
 * level has at most one.
 */
sealed interface SpecialRule {
    /** The words a level file uses for it. */
    val text: String

    /** The engine's rule for the computer's turns, or null for a rule about the start. */
    val turnRule: TurnRule? get() = null

    /** Why this rule does not fit a level with [map] and [dotsPerPlayer], or null if it fits. */
    fun problem(map: LevelMap, dotsPerPlayer: Int): String?

    /**
     * The computer places two dots on every [every]th turn of its own ([TurnRule.DoubleMove]).
     * `GameState.doubleMoveIn` counts down to the next one.
     */
    data class DoubleMove(val every: Int) : SpecialRule {
        init {
            require(every >= 2) { "a double move comes at most every 2nd turn" }
        }

        override val text get() = "double move every $every turns"

        override val turnRule get() = TurnRule.DoubleMove(every)

        // The turns before the first double move take one dot each, and the double move two.
        override fun problem(map: LevelMap, dotsPerPlayer: Int) = if (dotsPerPlayer < every + 1) {
            "the computer's $dotsPerPlayer dots run out before its first double move, which needs ${every + 1}"
        } else {
            null
        }
    }

    /**
     * The computer starts with a fortress: a group of [dots] of its dots on the map, joined up,
     * down, left, right or diagonally, and bigger than any other group of its dots. A large head
     * start in one piece; [Level.fortress] lists its dots.
     */
    data class Fortress(val dots: Int) : SpecialRule {
        init {
            require(dots >= MIN_DOTS) { "a fortress has at least $MIN_DOTS dots" }
        }

        override val text get() = "fortress of $dots dots"

        override fun problem(map: LevelMap, dotsPerPlayer: Int): String? {
            val sizes = map.groups(LevelMap.COMPUTER).map { it.size }
            val biggest = sizes.firstOrNull() ?: 0
            return when {
                biggest != dots -> "the fortress has $dots dots, but the biggest group of computer dots on the map has $biggest"
                sizes.getOrNull(1) == dots -> "two groups of computer dots on the map have $dots dots; the fortress must be bigger than the others"
                else -> null
            }
        }

        companion object {
            /** The fewest dots of a fortress: enough for a ring around four points. */
            const val MIN_DOTS = 8
        }
    }

    /**
     * Every turn the computer places two dots, one in each half of the board, left and right
     * ([TurnRule.TwoHalves]): it plays two groups at once. The board must be of even width.
     */
    data object TwoGroups : SpecialRule {
        override val text = "two groups at once"

        override val turnRule get() = TurnRule.TwoHalves

        override fun problem(map: LevelMap, dotsPerPlayer: Int) = when {
            map.width % 2 != 0 -> "the rule '$text' needs a board of even width, so that its halves are equal; this one is ${map.width} points wide"
            dotsPerPlayer < 2 -> "the rule '$text' needs at least 2 dots for the computer"
            else -> null
        }
    }
}
