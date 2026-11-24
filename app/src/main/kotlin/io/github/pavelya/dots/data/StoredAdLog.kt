package io.github.pavelya.dots.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import io.github.pavelya.dots.ads.AdLog
import io.github.pavelya.dots.ads.AdRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The ad limits' record ([AdRecord]) on the phone, in a store of its own: the rewarded ads watched
 * today and the last purchase. Not backed up: on a new phone the limits start afresh.
 */
class StoredAdLog(private val store: DataStore<Preferences>, scope: CoroutineScope) : AdLog {
    override val record: StateFlow<AdRecord?> = store.data
        .map { prefs -> AdRecord(day = prefs[DAY] ?: -1, watched = prefs[WATCHED] ?: 0, paidAt = prefs[PAID_AT]) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun watched(today: Long) {
        store.edit { prefs ->
            val before = if (prefs[DAY] == today) prefs[WATCHED] ?: 0 else 0
            prefs[DAY] = today
            prefs[WATCHED] = before + 1
        }
    }

    override suspend fun paid(at: Long) {
        store.edit { it[PAID_AT] = at }
    }

    companion object {
        /** The store's file name. */
        const val NAME = "ads"

        /** The day the ads in [WATCHED] were watched, as [dots.daily.Day.epochDay]. */
        private val DAY = longPreferencesKey("watched_day")
        private val WATCHED = intPreferencesKey("watched")

        /** When the player last paid for an item, in milliseconds since 1970. */
        private val PAID_AT = longPreferencesKey("paid_at")
    }
}
