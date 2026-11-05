package dots.log

import dots.engine.GameRecord
import dots.engine.GameSettings
import dots.engine.Player

/** Where a game stands when its entry was written. */
enum class Outcome {
    /** Still being played: the app's saved game, never in the log. */
    PLAYING,

    /** Played to the end. */
    FINISHED,

    /** Left unfinished: the player started another game instead. */
    ABANDONED,
}

/**
 * One game as the app saves and logs it: the [record] (which replays exactly) plus what the
 * test week measures (docs/poc/test-protocol.md). Written as one line of JSON, so a log is a
 * file of such lines, one per game. The app's saved game is the same line with [Outcome.PLAYING].
 *
 * @property appVersion the app's version name, so logs from different builds can be told apart
 * @property startedAt when the game started, in milliseconds since 1970 (UTC)
 * @property updatedAt when the entry was last written: the end of the game once it is over
 * @property playMillis time spent playing, not counting pauses of over [IDLE_CAP_MILLIS] between moves
 * @property opponent [HUMAN] for two players on one phone, else the computer's difficulty
 *   ("easy", "normal", "hard")
 * @property threatHighlight whether threats were shown
 * @property undos how often the player took a move back
 * @property red dots captured by red at the time of writing; [blue] likewise
 */
data class GameLogEntry(
    val appVersion: String,
    val startedAt: Long,
    val updatedAt: Long,
    val playMillis: Long,
    val opponent: String,
    val threatHighlight: Boolean,
    val undos: Int,
    val outcome: Outcome,
    val red: Int,
    val blue: Int,
    val record: GameRecord,
) {
    val vsComputer: Boolean get() = opponent != HUMAN

    /** The winner of a finished game, or null for a draw or an unfinished game. */
    val winner: Player?
        get() = when {
            outcome != Outcome.FINISHED || red == blue -> null
            red > blue -> Player.RED
            else -> Player.BLUE
        }

    fun toJson(): String = buildString {
        append('{')
        field("app", appVersion).append(',')
        field("started", startedAt).append(',')
        field("updated", updatedAt).append(',')
        field("play_ms", playMillis).append(',')
        field("opponent", opponent).append(',')
        field("threats", threatHighlight).append(',')
        field("undos", undos).append(',')
        field("outcome", outcome.name.lowercase()).append(',')
        field("red", red).append(',')
        field("blue", blue).append(',')
        field("record", record.toText())
        append('}')
    }

    /**
     * Replays the [record] and returns what does not match the entry (scores, or a "finished"
     * game that is not over), or null when the entry is consistent.
     */
    fun check(): String? {
        val end = try {
            record.replay()
        } catch (e: IllegalArgumentException) {
            return "the record does not replay: ${e.message}"
        } catch (e: IllegalStateException) {
            return "the record does not replay: ${e.message}"
        }
        val problems = buildList {
            if (end.score(Player.RED) != red || end.score(Player.BLUE) != blue) {
                add("logged $red-$blue, replay gives ${end.score(Player.RED)}-${end.score(Player.BLUE)}")
            }
            if (outcome == Outcome.FINISHED && !end.isOver) add("logged as finished, but the game is not over")
            if (outcome != Outcome.FINISHED && end.isOver) add("the game is over, but logged as ${outcome.name.lowercase()}")
        }
        return problems.takeIf { it.isNotEmpty() }?.joinToString("; ")
    }

    companion object {
        const val HUMAN = "human"

        /** A pause between moves longer than this counts as this long (the player left). */
        const val IDLE_CAP_MILLIS = 120_000L

        /** A new game, before its first move. */
        fun start(settings: GameSettings, opponent: String, threatHighlight: Boolean, now: Long, appVersion: String = "") =
            GameLogEntry(
                appVersion = appVersion,
                startedAt = now,
                updatedAt = now,
                playMillis = 0,
                opponent = opponent,
                threatHighlight = threatHighlight,
                undos = 0,
                outcome = Outcome.PLAYING,
                red = 0,
                blue = 0,
                record = GameRecord(settings, emptyList()),
            )

        /** Reads one line written by [toJson]. Throws [IllegalArgumentException] if it is not one. */
        fun parse(line: String): GameLogEntry {
            val fields = FlatJson.parse(line)
            fun text(name: String) = fields[name] as? String ?: throw IllegalArgumentException("'$name' missing or not text in: $line")
            fun number(name: String) = fields[name] as? Long ?: throw IllegalArgumentException("'$name' missing or not a number in: $line")
            fun flag(name: String) = fields[name] as? Boolean ?: throw IllegalArgumentException("'$name' missing or not true/false in: $line")
            val outcome = Outcome.entries.firstOrNull { it.name.lowercase() == text("outcome") }
                ?: throw IllegalArgumentException("unknown outcome in: $line")
            return GameLogEntry(
                appVersion = text("app"),
                startedAt = number("started"),
                updatedAt = number("updated"),
                playMillis = number("play_ms"),
                opponent = text("opponent"),
                threatHighlight = flag("threats"),
                undos = number("undos").toInt(),
                outcome = outcome,
                red = number("red").toInt(),
                blue = number("blue").toInt(),
                record = GameRecord.parse(text("record")),
            )
        }

        /** Reads a whole log, skipping blank lines. Lines that cannot be read go to [onBadLine]. */
        fun parseLog(text: String, onBadLine: (lineNumber: Int, problem: String) -> Unit = { _, _ -> }): List<GameLogEntry> =
            text.lines().mapIndexedNotNull { index, line ->
                if (line.isBlank()) return@mapIndexedNotNull null
                try {
                    parse(line)
                } catch (e: IllegalArgumentException) {
                    onBadLine(index + 1, e.message ?: "unreadable")
                    null
                } catch (e: NoSuchElementException) {
                    onBadLine(index + 1, e.message ?: "unreadable")
                    null
                }
            }

        private fun StringBuilder.field(name: String, value: Any): StringBuilder {
            append('"').append(name).append("\":")
            when (value) {
                is String -> FlatJson.quote(value, this)
                else -> append(value)
            }
            return this
        }
    }
}

/**
 * Just enough JSON for the log: one object of text, whole numbers and true/false, no nesting.
 * Hand-written so the engine keeps depending on the Kotlin standard library only.
 */
internal object FlatJson {
    fun quote(text: String, out: StringBuilder) {
        out.append('"')
        for (c in text) {
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c < ' ' -> out.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                else -> out.append(c)
            }
        }
        out.append('"')
    }

    /** Values are String, Long, Boolean or null. */
    fun parse(text: String): Map<String, Any?> {
        var i = 0
        fun fail(what: String): Nothing = throw IllegalArgumentException("bad JSON ($what at ${i + 1}): $text")
        fun skipSpace() {
            while (i < text.length && text[i].isWhitespace()) i++
        }
        fun expect(c: Char) {
            skipSpace()
            if (i >= text.length || text[i] != c) fail("expected '$c'")
            i++
        }
        fun string(): String {
            expect('"')
            val out = StringBuilder()
            while (true) {
                if (i >= text.length) fail("unterminated text")
                val c = text[i++]
                when (c) {
                    '"' -> return out.toString()
                    '\\' -> {
                        if (i >= text.length) fail("unterminated escape")
                        when (val e = text[i++]) {
                            '"', '\\', '/' -> out.append(e)
                            'n' -> out.append('\n')
                            't' -> out.append('\t')
                            'r' -> out.append('\r')
                            'u' -> {
                                if (i + 4 > text.length) fail("short \\u escape")
                                out.append(text.substring(i, i + 4).toIntOrNull(16)?.toChar() ?: fail("bad \\u escape"))
                                i += 4
                            }
                            else -> fail("unknown escape")
                        }
                    }
                    else -> out.append(c)
                }
            }
        }
        fun value(): Any? {
            skipSpace()
            if (i >= text.length) fail("missing value")
            return when {
                text[i] == '"' -> string()
                text.startsWith("true", i) -> true.also { i += 4 }
                text.startsWith("false", i) -> false.also { i += 5 }
                text.startsWith("null", i) -> null.also { i += 4 }
                text[i] == '-' || text[i].isDigit() -> {
                    val start = i
                    i++
                    while (i < text.length && text[i].isDigit()) i++
                    text.substring(start, i).toLongOrNull() ?: fail("bad number")
                }
                else -> fail("unexpected '${text[i]}'")
            }
        }

        val fields = LinkedHashMap<String, Any?>()
        expect('{')
        skipSpace()
        if (i < text.length && text[i] == '}') {
            i++
        } else {
            while (true) {
                val name = string()
                expect(':')
                fields[name] = value()
                skipSpace()
                if (i < text.length && text[i] == ',') {
                    i++
                    continue
                }
                expect('}')
                break
            }
        }
        skipSpace()
        if (i != text.length) fail("text after the object")
        return fields
    }
}
