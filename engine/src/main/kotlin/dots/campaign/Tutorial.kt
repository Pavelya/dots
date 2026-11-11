package dots.campaign

import dots.engine.Action
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos

/**
 * The tutorial: World 1's levels 1 to 3, played on the real board with guided steps, one sentence
 * at a time and a point to tap (docs/design/flows.md, First launch; world-1.md). Each level teaches
 * one [Lesson]: placing a dot, linking two rings with one dot, and building a ring of one's own.
 *
 * A step is worked out from the position alone, so it follows whatever the computer does, comes
 * back when a saved game is resumed, and points at a move that is good in that position: close a
 * ring when one closes in one dot, else block the computer's ring around the player's dots, else
 * start a ring. The app turns each [Prompt] into a sentence for the player's placing method.
 */
object Tutorial {
    /** What a tutorial level teaches. */
    enum class Lesson {
        /** Level 1-1: placing a dot. One dot closes a ring that is almost done. */
        PLACING,

        /** Level 1-2: one dot can join two rings, so one of them closes whatever the computer blocks. */
        LINKING,

        /** Level 1-3: a ring of one's own, from a plain cross. */
        CAPTURING,
    }

    /** What a step asks the player to do. */
    enum class Prompt {
        /** Level 1's first step: place a dot on the circled point. */
        PLACE,

        /** A dot is shown on the board but not placed yet: confirm it (tap again, or lift the finger). */
        CONFIRM,

        /** Place the dot that joins two rings. */
        JOIN,

        /** Close a ring around the computer's dots. */
        CLOSE,

        /** The computer can close a ring around the player's dots: block it. */
        BLOCK,

        /** Start a ring around a computer dot. */
        SURROUND,
    }

    /** One guided step: the [lesson] of the level, what to do, and the [point] to circle (null: none). */
    data class Step(val lesson: Lesson, val prompt: Prompt, val point: Pos?) {
        /** The step's number in the whole tutorial, 1 to [LESSONS]: one per level. */
        val number: Int get() = lesson.ordinal + 1
    }

    /** How many levels the tutorial has. */
    val LESSONS: Int = Lesson.entries.size

    private val codes = listOf("1-1", "1-2", "1-3")

    /** The tutorial's levels, in order. */
    val levels: List<Level> by lazy { codes.map { code -> Campaign.levels.first { it.code == code } } }

    /** The lesson [level] teaches, or null if it is not a tutorial level. */
    fun lesson(level: Level): Lesson? = codes.indexOf(level.code).takeIf { it >= 0 }?.let { Lesson.entries[it] }

    /** The tutorial level after [level], or null after the last one or outside the tutorial. */
    fun next(level: Level): Level? = codes.indexOf(level.code).takeIf { it >= 0 }?.let { levels.getOrNull(it + 1) }

    /**
     * The step to show in [state], a position of [level]'s game, when the player has a dot shown at
     * [preview] but not yet placed (null: none). Null when [level] is not a tutorial level, when it
     * is the computer's turn, or once the level is over.
     */
    fun step(level: Level, state: GameState, preview: Pos? = null): Step? {
        val lesson = lesson(level) ?: return null
        val human = level.human
        if (level.isOver(state) || state.toMove != human) return null
        if (preview != null) return Step(lesson, Prompt.CONFIRM, preview)

        val capture = bestCapture(state, human)
        if (lesson == Lesson.PLACING) return Step(lesson, Prompt.PLACE, capture ?: bestStart(state, human)?.first)
        if (capture != null) return Step(lesson, Prompt.CLOSE, capture)

        // A point where the computer would close a ring around the player's dots next turn.
        val block = turnOf(state, human.opponent)?.let { bestCapture(it, human.opponent) }?.takeIf(state::isLegal)
        if (block != null) return Step(lesson, Prompt.BLOCK, block)

        val start = bestStart(state, human)
        if (lesson == Lesson.LINKING && start != null && start.second >= 2) return Step(lesson, Prompt.JOIN, start.first)
        return Step(lesson, Prompt.SURROUND, start?.first ?: closest(state, human))
    }

    /**
     * When no ring is two dots from closing: the point next to a computer dot with the most of
     * [player]'s dots around it, away from the border, so the ring grows from what is there.
     */
    private fun closest(state: GameState, player: Player): Pos? = nextTo(state, player.opponent)
        .filterNot(state::isBorder)
        .maxByOrNull { point -> neighbours(state, point).count { state.ownerAt(it) == player && !state.isCaptured(it) } }

    private fun neighbours(state: GameState, point: Pos): List<Pos> =
        (-1..1).flatMap { dy -> (-1..1).map { dx -> Pos(point.x + dx, point.y + dy) } }
            .filter { it != point && state.inBounds(it.x, it.y) }

    /** The point where [player], to move in [state], captures the most points with one dot; null if none captures. */
    private fun bestCapture(state: GameState, player: Player): Pos? = nextTo(state, player.opponent)
        .map { it to state.play(it).captures.sumOf { capture -> capture.points } }
        .filter { it.second > 0 }
        .maxByOrNull { it.second }?.first

    /**
     * The point where a dot of [player], to move in [state], leaves the most points from which a
     * second dot captures, with that count; null if no dot leaves any. Two or more means the
     * computer cannot block them all with one dot.
     */
    private fun bestStart(state: GameState, player: Player): Pair<Pos, Int>? = nextTo(state, player.opponent)
        .mapNotNull { first ->
            val after = turnOf(state.play(first).state, player) ?: return@mapNotNull null
            first to nextTo(after, player.opponent).count { after.play(it).captures.isNotEmpty() }
        }
        .filter { it.second > 0 }
        .maxByOrNull { it.second }

    /** The points the player to move may play next to a live dot of [other], across or diagonally. */
    private fun nextTo(state: GameState, other: Player): List<Pos> = state.legalMoves().filter { point ->
        (-1..1).any { dy ->
            (-1..1).any { dx ->
                val x = point.x + dx
                val y = point.y + dy
                state.inBounds(x, y) && state.ownerAt(x, y) == other && !state.isCaptured(x, y)
            }
        }
    }

    /** [state] with [player] to move: as it is, or after the other side passes; null if that is not allowed. */
    private fun turnOf(state: GameState, player: Player): GameState? = when {
        state.toMove == player -> state
        state.isLegal(Action.Pass) -> state.pass().takeIf { it.toMove == player }
        else -> null
    }
}
