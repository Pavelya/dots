package dots.engine

/**
 * A whole game as settings plus the actions taken, which replays to exactly the same position.
 * Its text form is one line, for logs and for sending games around:
 *
 *     dots/1 15x15 dots=33 opening=crosses seed=42 first=R | e5 f6 pass stop a1 done
 */
data class GameRecord(val settings: GameSettings, val actions: List<Action>) {

    fun replay(): GameState = actions.fold(settings.newGame()) { state, action -> state.apply(action).state }

    fun toText(): String = buildString {
        append("$FORMAT ${settings.width}x${settings.height}")
        append(" dots=${settings.dotsPerPlayer ?: "none"}")
        append(" opening=${settings.opening.name.lowercase()}")
        append(" seed=${settings.seed} first=${settings.first.symbol} |")
        for (action in actions) append(' ').append(Action.word(action))
    }

    companion object {
        private const val FORMAT = "dots/1"

        fun parse(text: String): GameRecord {
            val (head, body) = text.split('|', limit = 2).let { it[0].trim() to it.getOrElse(1) { "" }.trim() }
            val words = head.split(Regex("\\s+"))
            require(words.firstOrNull() == FORMAT) { "Not a $FORMAT record: $text" }
            val size = words.getOrNull(1)?.split('x')?.map { it.toIntOrNull() }
            require(size != null && size.size == 2 && size.all { it != null }) { "Bad board size in: $text" }
            val fields = words.drop(2).associate { word ->
                val parts = word.split('=', limit = 2)
                require(parts.size == 2) { "Bad field '$word' in: $text" }
                parts[0] to parts[1]
            }
            val settings = GameSettings(
                width = size[0]!!,
                height = size[1]!!,
                dotsPerPlayer = fields["dots"].let { if (it == null || it == "none") null else it.toInt() },
                opening = Opening.valueOf(fields.getValue("opening").uppercase()),
                seed = fields.getValue("seed").toLong(),
                first = Player.entries.first { it.symbol.toString() == fields.getValue("first") },
            )
            val actions = if (body.isEmpty()) emptyList() else body.split(Regex("\\s+")).map(Action::parse)
            return GameRecord(settings, actions)
        }
    }
}
