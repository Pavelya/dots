package io.github.pavelya.dots.data

import dots.log.GameLogEntry
import dots.log.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.Executor

/** Where a game reports itself, so it can be resumed and logged. */
interface GameJournal {
    /** A new game begins; an unfinished game that was saved is logged as abandoned. */
    fun start(entry: GameLogEntry)

    /** The game changed (a move, an undo). */
    fun save(entry: GameLogEntry)

    /** The game is over: it goes to the log and is no longer resumable. */
    fun finish(entry: GameLogEntry)
}

/**
 * The saved game and the game log, in app-private files in the folder [directory] gives:
 *
 * - `current-game.json`: the unfinished game, one [GameLogEntry] line, replaced after every move
 *   (written to a temporary file first, so a kill mid-write leaves the previous version).
 * - `game-log.txt`: one line per finished or abandoned game; testers share this file.
 *
 * Callers never wait for the disk: [current] and [loggedGames] change at once, and the files are
 * read and written on [io] (one thread in the app, so writes happen in order). [directory] is
 * only called there too, since finding an app's folder may touch the disk.
 */
class GameStore(directory: () -> File, private val appVersion: String, private val io: Executor) : GameJournal {
    private val dir by lazy(directory)
    private val savedFile by lazy { File(dir, "current-game.json") }

    /** The log file, for sharing. It touches the disk: call it off the main thread. */
    val logFile: File get() = File(dir, "game-log.txt")

    private val _current = MutableStateFlow<GameLogEntry?>(null)

    /** The unfinished game, if there is one (null until the saved game has been read). */
    val current: StateFlow<GameLogEntry?> = _current.asStateFlow()

    private val _loggedGames = MutableStateFlow(0)

    /** How many games are in the log. */
    val loggedGames: StateFlow<Int> = _loggedGames.asStateFlow()

    private val _loaded = MutableStateFlow(false)

    /** True once the saved game has been read, so [current] can be trusted. */
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        io.execute {
            dir.mkdirs()
            // A game started before this read finished is newer than what is on disk.
            _current.compareAndSet(null, readSaved())
            val log = logFile
            _loggedGames.value = if (log.exists()) log.useLines { lines -> lines.count { it.isNotBlank() } } else 0
            _loaded.value = true
        }
    }

    override fun start(entry: GameLogEntry) {
        val stamped = entry.copy(appVersion = appVersion)
        _current.value = stamped
        io.execute {
            val previous = readSaved()
            if (previous != null && previous.record.actions.isNotEmpty()) {
                append(previous.copy(outcome = Outcome.ABANDONED))
            }
            write(stamped)
        }
    }

    override fun save(entry: GameLogEntry) {
        val stamped = entry.copy(appVersion = appVersion)
        _current.value = stamped
        io.execute { write(stamped) }
    }

    override fun finish(entry: GameLogEntry) {
        val stamped = entry.copy(appVersion = appVersion)
        _current.value = null
        io.execute {
            append(stamped)
            savedFile.delete()
        }
    }

    private fun readSaved(): GameLogEntry? {
        if (!savedFile.exists()) return null
        // A damaged or outdated file is ignored, not fatal: at worst one game is lost.
        return runCatching { GameLogEntry.parse(savedFile.readText().trim()).also { it.record.replay() } }
            .getOrNull()
            ?.takeIf { it.outcome == Outcome.PLAYING }
    }

    private fun write(entry: GameLogEntry) {
        dir.mkdirs()
        val temporary = File(dir, "current-game.json.tmp")
        temporary.writeText(entry.toJson() + "\n")
        if (!temporary.renameTo(savedFile)) {
            savedFile.delete()
            temporary.renameTo(savedFile)
        }
    }

    private fun append(entry: GameLogEntry) {
        dir.mkdirs()
        logFile.appendText(entry.toJson() + "\n")
        _loggedGames.value += 1
    }
}
