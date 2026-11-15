package dots.daily

import dots.engine.Action
import dots.engine.GameState
import dots.engine.PlayRecord

/**
 * One attempt at a daily puzzle: the puzzle plus the actions taken, which replays to exactly the
 * same position. It names the puzzle by its day, so an attempt belongs to that day even when it
 * ends after midnight. Its text form is one line, with the same action words as a
 * [dots.engine.GameRecord]:
 *
 *     daily/1 2027-01-05 | e5 f6 d4 c3
 */
data class DailyRecord(val puzzle: DailyPuzzle, override val actions: List<Action>) : PlayRecord {
    val day: Day get() = puzzle.day

    override fun start(): GameState = puzzle.newGame()

    /** The attempt ends when its mission is decided (or, in the rare case first, its game is over). */
    override fun isOver(state: GameState): Boolean = puzzle.isOver(state)

    override fun withActions(actions: List<Action>): DailyRecord = copy(actions = actions)

    override fun toText(): String = buildString {
        append("$FORMAT ${puzzle.day} |")
        for (action in actions) append(' ').append(Action.word(action))
    }

    companion object {
        const val FORMAT = "daily/1"

        /**
         * Reads a record written by [toText]; [find] gives the puzzle of a day. Throws
         * [IllegalArgumentException] if the text is not such a record, and [NoSuchElementException]
         * if the day has no puzzle.
         */
        fun parse(text: String, find: (Day) -> DailyPuzzle? = Daily::puzzle): DailyRecord {
            val (head, body) = text.split('|', limit = 2).let { it[0].trim() to it.getOrElse(1) { "" }.trim() }
            val words = head.split(Regex("\\s+"))
            require(words.size == 2 && words[0] == FORMAT) { "Not a $FORMAT record: $text" }
            val day = Day.parse(words[1]) ?: throw IllegalArgumentException("Bad day '${words[1]}' in: $text")
            val puzzle = find(day) ?: throw NoSuchElementException("No daily puzzle for $day")
            val actions = if (body.isEmpty()) emptyList() else body.split(Regex("\\s+")).map(Action::parse)
            return DailyRecord(puzzle, actions)
        }
    }
}
