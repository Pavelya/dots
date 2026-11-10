package dots.campaign

import dots.engine.Action
import dots.engine.Booster
import dots.engine.GameState
import dots.engine.PlayRecord

/**
 * A level game as the level plus the actions taken, boosters included, which replays to exactly
 * the same position. A record names its level by world and number, so it replays against the
 * level as its file is now. [gift] holds boosters given on top of the level's own, such as the
 * free Hint offered after three failures. Its text form is one line, with the same action words
 * as a [dots.engine.GameRecord], and a `+` word for each booster given:
 *
 *     level/1 2-5 +hint | e5 f6 wall:g7 g6 f8 double-dot d4 e4 c3
 */
data class LevelRecord(
    val level: Level,
    override val actions: List<Action>,
    val gift: Map<Booster, Int> = emptyMap(),
) : PlayRecord {
    init {
        require(gift.values.all { it >= 1 }) { "a gift has 1 or more of each booster" }
    }

    override fun start(): GameState = level.newGame(gift)

    /** The level ends when its game is over or its mission is decided. */
    override fun isOver(state: GameState): Boolean = level.isOver(state)

    override fun withActions(actions: List<Action>): LevelRecord = copy(actions = actions)

    override fun toText(): String = buildString {
        append("$FORMAT ${level.code}")
        for ((booster, count) in gift.entries.sortedBy { it.key }) repeat(count) { append(" +").append(booster.text) }
        append(" |")
        for (action in actions) append(' ').append(Action.word(action))
    }

    companion object {
        const val FORMAT = "level/1"

        /** Reads a record written by [toText]; [find] gives the level for a world and number. */
        fun parse(text: String, find: (world: Int, number: Int) -> Level = Campaign::level): LevelRecord {
            val (head, body) = text.split('|', limit = 2).let { it[0].trim() to it.getOrElse(1) { "" }.trim() }
            val words = head.split(Regex("\\s+"))
            require(words.size >= 2 && words[0] == FORMAT) { "Not a $FORMAT record: $text" }
            val code = words[1].split('-').map { it.toIntOrNull() }
            require(code.size == 2 && code.all { it != null }) { "Bad level '${words[1]}' in: $text" }
            val gift = words.drop(2).map { word ->
                require(word.startsWith('+')) { "Bad gift '$word' in: $text" }
                Booster.entries.firstOrNull { it.text == word.drop(1) } ?: throw IllegalArgumentException("Bad gift '$word' in: $text")
            }.groupingBy { it }.eachCount()
            val actions = if (body.isEmpty()) emptyList() else body.split(Regex("\\s+")).map(Action::parse)
            return LevelRecord(find(code[0]!!, code[1]!!), actions, gift)
        }
    }
}
