package dots.campaign

import dots.engine.Action
import dots.engine.GameState

/**
 * A level game as the level plus the actions taken, boosters included, which replays to exactly
 * the same position. A record names its level by world and number, so it replays against the
 * level as its file is now. Its text form is one line, with the same action words as a
 * [dots.engine.GameRecord]:
 *
 *     level/1 2-5 | e5 f6 wall:g7 g6 f8 double-dot d4 e4 c3
 */
data class LevelRecord(val level: Level, val actions: List<Action>) {

    fun replay(): GameState = actions.fold(level.newGame()) { state, action -> state.apply(action).state }

    fun toText(): String = buildString {
        append("$FORMAT ${level.code} |")
        for (action in actions) append(' ').append(Action.word(action))
    }

    companion object {
        private const val FORMAT = "level/1"

        /** Reads a record written by [toText]; [find] gives the level for a world and number. */
        fun parse(text: String, find: (world: Int, number: Int) -> Level = Campaign::level): LevelRecord {
            val (head, body) = text.split('|', limit = 2).let { it[0].trim() to it.getOrElse(1) { "" }.trim() }
            val words = head.split(Regex("\\s+"))
            require(words.size == 2 && words[0] == FORMAT) { "Not a $FORMAT record: $text" }
            val code = words[1].split('-').map { it.toIntOrNull() }
            require(code.size == 2 && code.all { it != null }) { "Bad level '${words[1]}' in: $text" }
            val actions = if (body.isEmpty()) emptyList() else body.split(Regex("\\s+")).map(Action::parse)
            return LevelRecord(find(code[0]!!, code[1]!!), actions)
        }
    }
}
