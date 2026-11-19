package io.github.pavelya.dots.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A theme the player can have (decision 0028), by the [key] it is saved under. Notebook is the
 * default. The ui's `GameTheme.of` gives the theme itself.
 */
enum class ThemeId(val key: String) {
    NOTEBOOK("notebook"),
    STAR_CHART("star-chart"),
    NEON_ARCADE("neon-arcade"),
    ;

    companion object {
        val DEFAULT = NOTEBOOK

        fun of(key: String?): ThemeId? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Which themes the player can choose (decision 0029). C-02 replaces [beforeShop] with "the player
 * owns this theme"; nothing else needs to change.
 */
fun interface ThemeRule {
    fun canChoose(theme: ThemeId): Boolean

    companion object {
        /** Before the Shop exists: every theme on a debug build ([debug]), only Notebook on a release build. */
        fun beforeShop(debug: Boolean): ThemeRule = ThemeRule { debug || it == ThemeId.DEFAULT }
    }
}

/**
 * The theme in use (docs/design/flows.md, "Choosing a theme"): one saved value, which the Theme page
 * and, once it exists, the Shop's Use it both read and set. It is kept in the progress store, beside
 * the stars and chests that earn cosmetics, so Android's backup brings it back with them. [rule]
 * says which themes the player can choose; a saved theme the player can no longer choose (after a
 * refund, or a debug build's backup restored on a release build) reads as Notebook, without a
 * message.
 */
class ThemeRepository(private val store: DataStore<Preferences>, private val rule: () -> ThemeRule) {

    /** The themes the player can choose, in the Theme page's order: Notebook, Star chart, Neon arcade. */
    val choices: List<ThemeId> get() = ThemeId.entries.filter(rule()::canChoose)

    /** The theme the game screen is drawn in. */
    val inUse: Flow<ThemeId> = store.data.map { prefs ->
        ThemeId.of(prefs[THEME])?.takeIf(rule()::canChoose) ?: ThemeId.DEFAULT
    }

    /** Puts [theme] in use; a theme the player can't choose changes nothing. */
    suspend fun use(theme: ThemeId) {
        if (!rule().canChoose(theme)) return
        store.edit { it[THEME] = theme.key }
    }

    private companion object {
        /** The theme in use, by [ThemeId.key]. */
        val THEME = stringPreferencesKey("theme")
    }
}
