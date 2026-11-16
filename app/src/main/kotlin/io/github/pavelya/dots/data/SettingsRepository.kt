package io.github.pavelya.dots.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Settings kept on the phone. A stored value that is no longer valid falls back to its default. */
class SettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = store.data.map(::read)

    suspend fun update(change: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val next = change(read(prefs))
            prefs[BOARD] = next.boardSize.name
            prefs[LENGTH] = next.length.name
            prefs[THREATS] = next.threatHighlight
            prefs[SOUND] = next.sound
            prefs[MUSIC_VOLUME] = next.musicVolume
            prefs[EFFECTS_VOLUME] = next.effectsVolume
            prefs[VIBRATION] = next.vibration
            prefs[HIGH_CONTRAST] = next.highContrast
            prefs[LINKS] = next.links
            prefs[PLACING] = next.placing.name
            prefs.remove(OLD_PLACING)
            prefs[TUTORIAL_SKIPPED] = next.tutorialSkipped
            prefs[DAILY_AHEAD] = next.dailyAhead
        }
    }

    /**
     * Marks Google Play's review prompt as asked at [now] and returns true, if [ReviewRule.isDue];
     * otherwise changes nothing and returns false. Checked and marked in one save, so one ask
     * never turns into two. Not a setting: the player never sees it.
     */
    suspend fun askReview(now: Long): Boolean {
        var asked = false
        store.edit { prefs ->
            asked = ReviewRule.isDue(prefs[REVIEW_ASKED], now)
            if (asked) prefs[REVIEW_ASKED] = now
        }
        return asked
    }

    private fun read(prefs: Preferences): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            boardSize = enumOrNull<BoardSize>(prefs[BOARD]) ?: defaults.boardSize,
            length = enumOrNull<GameLength>(prefs[LENGTH]) ?: defaults.length,
            threatHighlight = prefs[THREATS] ?: defaults.threatHighlight,
            sound = prefs[SOUND] ?: defaults.sound,
            musicVolume = volumeOrNull(prefs[MUSIC_VOLUME]) ?: defaults.musicVolume,
            effectsVolume = volumeOrNull(prefs[EFFECTS_VOLUME]) ?: defaults.effectsVolume,
            vibration = prefs[VIBRATION] ?: defaults.vibration,
            highContrast = prefs[HIGH_CONTRAST] ?: defaults.highContrast,
            links = prefs[LINKS] ?: defaults.links,
            placing = enumOrNull<PlacingMethod>(prefs[PLACING]) ?: defaults.placing,
            tutorialSkipped = prefs[TUTORIAL_SKIPPED] ?: defaults.tutorialSkipped,
            dailyAhead = prefs[DAILY_AHEAD] ?: defaults.dailyAhead,
        )
    }

    private fun volumeOrNull(stored: Int?): Int? = stored?.takeIf { it in 0..AppSettings.MAX_VOLUME }

    private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
        enumValues<E>().firstOrNull { it.name == name }

    private companion object {
        val BOARD = stringPreferencesKey("board_size")
        val LENGTH = stringPreferencesKey("game_length")
        val THREATS = booleanPreferencesKey("threat_highlight")
        val SOUND = booleanPreferencesKey("sound")
        val MUSIC_VOLUME = intPreferencesKey("music_volume")
        val EFFECTS_VOLUME = intPreferencesKey("effects_volume")
        val VIBRATION = booleanPreferencesKey("vibration")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val LINKS = booleanPreferencesKey("links")
        /**
         * The placing method. Its name changed when drag became the default (decision 0026): every
         * save wrote the method, chosen or not, so the value under [OLD_PLACING] is ignored, and
         * every player who had the app starts on drag once. The next save removes it.
         */
        val PLACING = stringPreferencesKey("placing_method")
        val OLD_PLACING = stringPreferencesKey("placing")
        val TUTORIAL_SKIPPED = booleanPreferencesKey("tutorial_skipped")
        val DAILY_AHEAD = booleanPreferencesKey("developer_daily_ahead")

        /** When the review prompt was last asked for, in milliseconds since 1970 ([askReview]). */
        val REVIEW_ASKED = longPreferencesKey("review_asked_at")
    }
}
