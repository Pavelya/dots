package io.github.pavelya.dots.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dots.log.GameLogEntry
import dots.log.GameStats
import dots.log.PlayerStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The player's totals on Stats ([PlayerStats]): games played and won, dots captured, the best
 * capture. Kept in the progress store, so Android's backup takes them with the stars. Each
 * finished game is counted once ([finished]); [scope] counts them in order, off the main thread.
 */
class StatsRepository(private val store: DataStore<Preferences>, scope: CoroutineScope) {

    /** The totals as last read; null until they are first read. */
    val stats: StateFlow<PlayerStats?> = store.data.map { PlayerStats.parse(it[STATS].orEmpty()) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val games = Channel<GameLogEntry>(Channel.UNLIMITED)

    init {
        scope.launch { for (entry in games) count(entry) }
    }

    /** A game has ended: it is counted soon, if it counts ([GameStats.of]). Returns at once. */
    fun finished(entry: GameLogEntry) {
        games.trySend(entry)
    }

    /**
     * Adds [entry] to the totals, if it counts. A game already counted (the same start time, among
     * the last [KEEP_COUNTED]) is not counted again, so a game reported twice counts once.
     */
    suspend fun count(entry: GameLogEntry) {
        // A record that no longer replays (its level changed since) is left out rather than fatal.
        val game = runCatching { GameStats.of(entry) }.getOrNull() ?: return
        store.edit { prefs ->
            val counted = prefs[COUNTED].orEmpty().split(' ').mapNotNull(String::toLongOrNull)
            if (entry.startedAt in counted) return@edit
            prefs[STATS] = (PlayerStats.parse(prefs[STATS].orEmpty()) + game).text
            prefs[COUNTED] = (counted + entry.startedAt).takeLast(KEEP_COUNTED).joinToString(" ")
        }
    }

    private companion object {
        /** The totals: `played:48 won:31 captured:212 best:9`. */
        val STATS = stringPreferencesKey("stats")

        /** The start times of the games counted last, oldest first: `1731600000000 1731600400000`. */
        val COUNTED = stringPreferencesKey("stats_counted")

        /** How many start times [COUNTED] keeps: more than the game modes, which each have one game open. */
        const val KEEP_COUNTED = 16
    }
}

/** [journal], which also counts every finished game in [stats]. */
class CountingJournal(private val journal: GameJournal, private val stats: StatsRepository) : GameJournal by journal {
    override fun finish(entry: GameLogEntry) {
        journal.finish(entry)
        stats.finished(entry)
    }
}
