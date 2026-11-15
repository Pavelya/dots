package io.github.pavelya.dots.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
            prefs[TUTORIAL_SKIPPED] = next.tutorialSkipped
        }
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
        val PLACING = stringPreferencesKey("placing")
        val TUTORIAL_SKIPPED = booleanPreferencesKey("tutorial_skipped")
    }
}
