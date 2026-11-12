package io.github.pavelya.dots.data

import dots.campaign.LevelRecord
import dots.log.GameLogEntry
import dots.log.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.concurrent.Executor

/**
 * The ways to play, each with its own saved game (docs/design/flows.md, "Saved games"). A saved
 * game in one mode never blocks the others. [key] names the mode in routes and file names. The
 * daily puzzle (A-07) adds its own.
 */
enum class GameMode(val key: String) {
    /** A campaign level, a practice game of the tutorial included. */
    CAMPAIGN("campaign"),

    /** Free play against the computer. */
    FREE_PLAY("free-play"),

    /** Two players on one phone. */
    TWO_PLAYERS("two-players"),
    ;

    companion object {
        fun of(entry: GameLogEntry): GameMode = when {
            entry.record is LevelRecord -> CAMPAIGN
            entry.vsComputer -> FREE_PLAY
            else -> TWO_PLAYERS
        }

        fun of(key: String?): GameMode? = entries.firstOrNull { it.key == key }
    }
}

/** Where a game reports itself, so it can be resumed and logged. */
interface GameJournal {
    /** A new game begins; an unfinished game saved in the same mode is logged as abandoned. */
    fun start(entry: GameLogEntry)

    /** The game changed (a move, an undo). */
    fun save(entry: GameLogEntry)

    /** The game is over: it goes to the log and is no longer resumable. */
    fun finish(entry: GameLogEntry)
}

/**
 * The saved games and the game log, in app-private files in the folder [directory] gives:
 *
 * - `saved-<mode>.json`, one per [GameMode]: that mode's unfinished game, one [GameLogEntry] line,
 *   replaced after every move (written to a temporary file first, so a kill mid-write leaves the
 *   previous version).
 * - `game-log.txt`: one line per finished or abandoned game; testers share this file.
 *
 * Callers never wait for the disk: [saved] and [loggedGames] change at once, and the files are
 * read and written on [io] (one thread in the app, so writes happen in order). [directory] is
 * only called there too, since finding an app's folder may touch the disk.
 */
class GameStore(directory: () -> File, private val appVersion: String, private val io: Executor) : GameJournal {
    private val dir by lazy(directory)

    private fun savedFile(mode: GameMode) = File(dir, "saved-${mode.key}.json")

    /** The log file, for sharing. It touches the disk: call it off the main thread. */
    val logFile: File get() = File(dir, "game-log.txt")

    private val _saved = MutableStateFlow<Map<GameMode, GameLogEntry>>(emptyMap())

    /** Each mode's unfinished game, if it has one (empty until the saved games have been read). */
    val saved: StateFlow<Map<GameMode, GameLogEntry>> = _saved.asStateFlow()

    /** The unfinished game of [mode], if there is one. */
    fun saved(mode: GameMode): GameLogEntry? = _saved.value[mode]

    private val _loggedGames = MutableStateFlow(0)

    /** How many games are in the log. */
    val loggedGames: StateFlow<Int> = _loggedGames.asStateFlow()

    private val _loaded = MutableStateFlow(false)

    /** True once the saved games have been read, so [saved] can be trusted. */
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        io.execute {
            dir.mkdirs()
            moveOldSavedGame()
            val onDisk = GameMode.entries.mapNotNull { mode -> readSaved(savedFile(mode))?.let { mode to it } }.toMap()
            // A game started before this read finished is newer than what is on disk.
            _saved.update { started -> onDisk + started }
            val log = logFile
            _loggedGames.value = if (log.exists()) log.useLines { lines -> lines.count { it.isNotBlank() } } else 0
            _loaded.value = true
        }
    }

    override fun start(entry: GameLogEntry) {
        val stamped = entry.copy(appVersion = appVersion)
        val mode = GameMode.of(stamped)
        _saved.update { it + (mode to stamped) }
        io.execute {
            val previous = readSaved(savedFile(mode))
            if (previous != null && previous.record.actions.isNotEmpty()) {
                append(previous.copy(outcome = Outcome.ABANDONED))
            }
            write(mode, stamped)
        }
    }

    override fun save(entry: GameLogEntry) {
        val stamped = entry.copy(appVersion = appVersion)
        val mode = GameMode.of(stamped)
        _saved.update { it + (mode to stamped) }
        io.execute { write(mode, stamped) }
    }

    override fun finish(entry: GameLogEntry) {
        val stamped = entry.copy(appVersion = appVersion)
        val mode = GameMode.of(stamped)
        _saved.update { it - mode }
        io.execute {
            append(stamped)
            savedFile(mode).delete()
        }
    }

    /**
     * Before each mode had its own saved game, one file held the only one, whatever its mode. It
     * becomes the saved game of its mode.
     */
    private fun moveOldSavedGame() {
        val old = File(dir, "current-game.json")
        if (!old.exists()) return
        val entry = readSaved(old)
        if (entry != null && !savedFile(GameMode.of(entry)).exists()) write(GameMode.of(entry), entry)
        old.delete()
    }

    private fun readSaved(file: File): GameLogEntry? {
        if (!file.exists()) return null
        // A damaged or outdated file is ignored, not fatal: at worst one game is lost.
        return runCatching { GameLogEntry.parse(file.readText().trim()).also { it.record.replay() } }
            .getOrNull()
            ?.takeIf { it.outcome == Outcome.PLAYING }
    }

    private fun write(mode: GameMode, entry: GameLogEntry) {
        dir.mkdirs()
        val file = savedFile(mode)
        val temporary = File(dir, "${file.name}.tmp")
        temporary.writeText(entry.toJson() + "\n")
        if (!temporary.renameTo(file)) {
            file.delete()
            temporary.renameTo(file)
        }
    }

    private fun append(entry: GameLogEntry) {
        dir.mkdirs()
        logFile.appendText(entry.toJson() + "\n")
        _loggedGames.value += 1
    }
}
