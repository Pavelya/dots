package io.github.pavelya.dots.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dots.campaign.Cosmetic
import io.github.pavelya.dots.shop.Owned
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * A theme the player can have (decision 0028), by the [key] it is saved under. Notebook is the
 * default. The ui's `GameTheme.of` gives the theme itself.
 */
enum class ThemeId(override val key: String) : LookPart {
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
 * A piece style (docs/design/cosmetics.md), by the [key] it is saved under: the theme's own
 * pieces, called Standard, or one that draws each side's pieces in its own shape, in any theme.
 */
enum class PieceStyleId(override val key: String) : LookPart {
    STANDARD("standard"),
    HEARTS_AND_CLUBS("hearts-and-clubs"),
    FLAME_AND_FROST("flame-and-frost"),
    ;

    companion object {
        val DEFAULT = STANDARD

        fun of(key: String?): PieceStyleId? = entries.firstOrNull { it.key == key }
    }
}

/**
 * A ring effect (docs/design/cosmetics.md), by the [key] it is saved under: the theme's own ring,
 * called Standard, or one that changes how a ring and its fill are drawn and how a capture moves.
 */
enum class RingEffectId(override val key: String) : LookPart {
    STANDARD("standard"),
    LASSO("lasso"),
    RUBBER_STAMP("rubber-stamp"),
    ;

    companion object {
        val DEFAULT = STANDARD

        fun of(key: String?): RingEffectId? = entries.firstOrNull { it.key == key }
    }
}

/**
 * A theme, a piece style or a ring effect. Each but the default of its kind is a [Cosmetic] by the
 * same name; the default (Notebook, Standard) is always owned, so it has none.
 */
sealed interface LookPart {
    val key: String

    val cosmetic: Cosmetic? get() = Cosmetic.of(key)
}

/**
 * Which cosmetics the player can choose: themes, piece styles and ring effects alike. The default
 * of each kind needs no rule.
 */
fun interface CosmeticRule {
    fun canChoose(cosmetic: Cosmetic): Boolean

    companion object {
        /**
         * The cosmetics the player owns, bought or earned ([owned]; decision 0029 as C-02 settles
         * it). A debug build ([debug]) offers every one, so testers can check them on a phone.
         */
        fun owned(debug: Boolean, owned: Owned): CosmeticRule = CosmeticRule { debug || owned.owns(it) }
    }
}

/**
 * The game screen's look (docs/design/flows.md, "Choosing a theme"; cosmetics.md, Rules): the
 * theme, the piece style and the ring effect in use, three saved values chosen apart. The Theme,
 * Pieces and Rings pages in Settings and the Shop's Use it read and set them, so they always agree.
 * They are kept in the progress store, beside the stars and chests that earn cosmetics, so
 * Android's backup brings them back with them. [rule] says which cosmetics the player can choose,
 * and the values are read again each time [changes] says the rule's answer may have changed (a
 * purchase, a refund, new stars). A saved one the player can no longer choose (after a refund, or
 * a debug build's backup restored on a release build) reads as the default of its kind, without a
 * message.
 */
class ThemeRepository(
    private val store: DataStore<Preferences>,
    changes: Flow<*> = flowOf(Unit),
    private val rule: () -> CosmeticRule,
) {
    private val read: Flow<Preferences> = combine(store.data, changes) { prefs, _ -> prefs }

    /** Whether the player can put [part] in use: a default always, a cosmetic as the rule says. */
    fun canChoose(part: LookPart): Boolean = part.cosmetic?.let(rule()::canChoose) ?: true

    /** The themes the player can choose, in the Theme page's order: Notebook, Star chart, Neon arcade. */
    val choices: List<ThemeId> get() = ThemeId.entries.filter(::canChoose)

    /** The piece styles the player can choose: Standard first, then the Shop's order. */
    val pieceChoices: List<PieceStyleId> get() = PieceStyleId.entries.filter(::canChoose)

    /** The ring effects the player can choose: Standard first, then the Shop's order. */
    val ringChoices: List<RingEffectId> get() = RingEffectId.entries.filter(::canChoose)

    /** The theme the game screen is drawn in. */
    val inUse: Flow<ThemeId> = read.map { prefs ->
        ThemeId.of(prefs[THEME])?.takeIf(::canChoose) ?: ThemeId.DEFAULT
    }

    /** The pieces the game screen draws, in the theme's inks. */
    val pieces: Flow<PieceStyleId> = read.map { prefs ->
        PieceStyleId.of(prefs[PIECES])?.takeIf(::canChoose) ?: PieceStyleId.DEFAULT
    }

    /** The rings the game screen draws, and how its captures move. */
    val rings: Flow<RingEffectId> = read.map { prefs ->
        RingEffectId.of(prefs[RINGS])?.takeIf(::canChoose) ?: RingEffectId.DEFAULT
    }

    /** Puts [theme] in use; a theme the player can't choose changes nothing. */
    suspend fun use(theme: ThemeId) = save(THEME, theme)

    /** Puts [style] in use; one the player can't choose changes nothing. */
    suspend fun use(style: PieceStyleId) = save(PIECES, style)

    /** Puts [effect] in use; one the player can't choose changes nothing. */
    suspend fun use(effect: RingEffectId) = save(RINGS, effect)

    private suspend fun save(key: Preferences.Key<String>, part: LookPart) {
        if (!canChoose(part)) return
        store.edit { it[key] = part.key }
    }

    private companion object {
        /** The theme in use, by [ThemeId.key]. */
        val THEME = stringPreferencesKey("theme")

        /** The piece style in use, by [PieceStyleId.key]. */
        val PIECES = stringPreferencesKey("pieces")

        /** The ring effect in use, by [RingEffectId.key]. */
        val RINGS = stringPreferencesKey("rings")
    }
}
