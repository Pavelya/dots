package io.github.pavelya.dots.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
            prefs[DIFFICULTY] = next.difficulty.name
            prefs[THREATS] = next.threatHighlight
            prefs[SOUND] = next.sound
            prefs[PLACING] = next.placing.name
            prefs[TUTORIAL_SKIPPED] = next.tutorialSkipped
        }
    }

    private fun read(prefs: Preferences): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            boardSize = enumOrNull<BoardSize>(prefs[BOARD]) ?: defaults.boardSize,
            length = enumOrNull<GameLength>(prefs[LENGTH]) ?: defaults.length,
            difficulty = enumOrNull<Difficulty>(prefs[DIFFICULTY]) ?: defaults.difficulty,
            threatHighlight = prefs[THREATS] ?: defaults.threatHighlight,
            sound = prefs[SOUND] ?: defaults.sound,
            placing = enumOrNull<PlacingMethod>(prefs[PLACING]) ?: defaults.placing,
            tutorialSkipped = prefs[TUTORIAL_SKIPPED] ?: defaults.tutorialSkipped,
        )
    }

    private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
        enumValues<E>().firstOrNull { it.name == name }

    private companion object {
        val BOARD = stringPreferencesKey("board_size")
        val LENGTH = stringPreferencesKey("game_length")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val THREATS = booleanPreferencesKey("threat_highlight")
        val SOUND = booleanPreferencesKey("sound")
        val PLACING = stringPreferencesKey("placing")
        val TUTORIAL_SKIPPED = booleanPreferencesKey("tutorial_skipped")
    }
}
