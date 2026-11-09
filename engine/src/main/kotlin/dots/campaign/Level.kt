package dots.campaign

import dots.ai.AiConfig
import dots.engine.GameState
import dots.engine.Notation
import dots.engine.Player
import dots.engine.Pos

/**
 * A single-player level: the human (red, moving first) plays the computer (blue). Levels are
 * stored as text files; [LevelFormat] reads and writes them.
 *
 * Difficulty comes from the computer's [strength], the [map] it starts with (`B` dots are its head
 * start, `R` dots are the human's), the [dotsPerPlayer] budget and the [mission].
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
) {
    init {
        require(world >= 1) { "world must be 1 or more" }
        require(number in 1..99) { "level number must be 1 to 99" }
        require(name.isNotBlank() && name.lines().size == 1 && name == name.trim()) { "name must be one line of text" }
        require(dotsPerPlayer >= 1) { "dotsPerPlayer must be 1 or more" }
        require(boosters.values.all { it >= 1 }) { "booster counts must be 1 or more" }
        checkMission()
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
     * Why the engine cannot play this level as written, or null if it can. The file format already
     * holds special rules and special points that the rules do not use.
     */
    val unplayable: String?
        get() = when {
            map.hasSpecialPoints -> "its board has holes, blocked points or bonus points, which the engine does not play"
            rule != null -> "it has the special rule '${rule.text}', which the engine does not play"
            else -> null
        }

    fun newGame(): GameState {
        unplayable?.let { throw UnsupportedOperationException("Level $code cannot be played: $it") }
        return start()
    }

    /** Where the mission stands in [state], a position of this level's game. */
    fun status(state: GameState): MissionStatus = judge(mission, state, human)

    /** The level ends when its game is over or its mission is decided, whichever comes first. */
    fun isOver(state: GameState): Boolean = state.isOver || status(state).isDecided

    /** The level is passed when its mission is; in "win", a draw (for instance 0-0) is not enough. */
    fun isPassed(end: GameState): Boolean = status(end).outcome == Outcome.PASSED

    private fun start() = GameState.start(
        map.width, map.height, dotsPerPlayer, human, map.startDots(), dotsFor = mapOf(human to humanDots),
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
            is Mission.Target -> require(map[mission.dot] == LevelMap.COMPUTER) {
                "${Notation.name(mission.dot)} must hold a computer dot ('B' on the map) for $what"
            }
            Mission.Win, is Mission.WinBy -> Unit
        }
        require(!status(start()).isDecided) { "$what is already decided before the first move" }
    }
}

/** How strongly the computer plays a level. */
enum class Strength(val config: AiConfig) {
    EASY(AiConfig.EASY),
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

    val hasSpecialPoints: Boolean get() = rows.any { row -> row.any { it == HOLE || it == BLOCKED || it == BONUS } }

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

/** The boosters a level may allow, with the word a level file uses for each. */
enum class Booster(val text: String) {
    WALL("wall"),
    HINT("hint"),
    BOMB("bomb"),
    DOUBLE_DOT("double-dot"),
}

/** A rule of a level that the computer bends, announced to the player (boss levels). */
sealed interface SpecialRule {
    val text: String

    /** The computer places two dots on every [every]th turn of its own. */
    data class DoubleMove(val every: Int) : SpecialRule {
        init {
            require(every >= 2) { "a double move comes at most every 2nd turn" }
        }

        override val text get() = "double move every $every turns"
    }
}

/**
 * The scores that earn two and three stars; passing the level earns one. What is counted (margin
 * of victory, moves to spare, ...) depends on the mission.
 */
data class Stars(val two: Int, val three: Int) {
    init {
        require(two >= 0 && three > two) { "the three-star score must be higher than the two-star score, both 0 or more" }
    }
}
