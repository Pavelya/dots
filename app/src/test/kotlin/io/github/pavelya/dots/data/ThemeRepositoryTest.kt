package io.github.pavelya.dots.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dots.campaign.Campaign
import dots.campaign.Cosmetic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The theme in use (docs/design/flows.md, "Choosing a theme"): Notebook by default, kept across a
 * restart and a backup restore, and only ever a theme the player can choose (decision 0029). The
 * piece style and ring effect in use (docs/design/cosmetics.md) are kept the same way, beside it.
 */
class ThemeRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    /**
     * The progress file opened as the app opens it, on a debug build or not ([debug]); close() is
     * what a restart does.
     */
    private class Session(file: File, debug: Boolean = true) {
        private val job = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.IO + job)
        val store = ProgressRepository.store(scope) { file }
        val themes = ThemeRepository(store) { CosmeticRule.beforeShop(debug) }
        val progress = ProgressRepository(store, scope)

        suspend fun close() = job.cancelAndJoin()
    }

    /** The progress file, by the name Android's backup takes (BackupRulesTest). */
    private fun file(phone: String = "phone"): File =
        folder.root.resolve(phone).apply { mkdirs() }.resolve("${ProgressRepository.NAME}.preferences_pb")

    @Test
    fun `a fresh install uses Notebook`() = runBlocking {
        val session = Session(file())
        assertEquals(ThemeId.NOTEBOOK, session.themes.inUse.first())
        session.close()
    }

    @Test
    fun `the choice survives a restart`() = runBlocking {
        val first = Session(file())
        first.themes.use(ThemeId.NEON_ARCADE)
        assertEquals(ThemeId.NEON_ARCADE, first.themes.inUse.first())
        first.close()

        val second = Session(file())
        assertEquals(ThemeId.NEON_ARCADE, second.themes.inUse.first())
        second.close()
    }

    @Test
    fun `the choice is in the backed-up progress file, so a backup restore brings it back with the stars`() = runBlocking {
        val old = Session(file("old phone"))
        old.progress.record(Campaign.level(1, 1), 3)
        old.themes.use(ThemeId.STAR_CHART)
        old.close()

        // Android's backup and device transfer copy this one file to the new phone.
        file("old phone").copyTo(file("new phone"))
        val restored = Session(file("new phone"))
        assertEquals(ThemeId.STAR_CHART, restored.themes.inUse.first())
        assertEquals(3, restored.progress.progress.first().stars(Campaign.level(1, 1)))
        restored.close()
    }

    @Test
    fun `a debug build's backup restored on a release build goes back to Notebook`() = runBlocking {
        val debug = Session(file("debug"))
        debug.themes.use(ThemeId.STAR_CHART)
        debug.close()

        file("debug").copyTo(file("release"))
        val release = Session(file("release"), debug = false)
        assertEquals(ThemeId.NOTEBOOK, release.themes.inUse.first())
        release.close()
    }

    @Test
    fun `a debug build offers every theme in order, a release build only Notebook`() = runBlocking {
        val debug = Session(file("debug"))
        assertEquals(listOf(ThemeId.NOTEBOOK, ThemeId.STAR_CHART, ThemeId.NEON_ARCADE), debug.themes.choices)
        debug.close()

        val release = Session(file("release"), debug = false)
        assertEquals(listOf(ThemeId.NOTEBOOK), release.themes.choices)
        release.close()
    }

    @Test
    fun `a release build can't put another theme in use`() = runBlocking {
        val release = Session(file(), debug = false)
        release.themes.use(ThemeId.NEON_ARCADE)
        release.close()

        val debug = Session(file())
        assertEquals("nothing was saved", ThemeId.NOTEBOOK, debug.themes.inUse.first())
        debug.close()
    }

    @Test
    fun `sample progress from the developer menu keeps the theme`() = runBlocking {
        val session = Session(file())
        session.themes.use(ThemeId.STAR_CHART)
        session.progress.replace(SampleProgress.everything)
        assertEquals(ThemeId.STAR_CHART, session.themes.inUse.first())
        session.close()
    }

    @Test
    fun `a saved theme this version doesn't know reads as Notebook`() = runBlocking {
        val session = Session(file())
        session.store.edit { it[stringPreferencesKey("theme")] = "paper-planes" }
        assertEquals(ThemeId.NOTEBOOK, session.themes.inUse.first())
        session.close()
    }

    @Test
    fun `a fresh install uses the theme's own pieces and ring`() = runBlocking {
        val session = Session(file())
        assertEquals(PieceStyleId.STANDARD, session.themes.pieces.first())
        assertEquals(RingEffectId.STANDARD, session.themes.rings.first())
        session.close()
    }

    @Test
    fun `the theme, piece style and ring effect are chosen apart and survive a restart`() = runBlocking {
        val first = Session(file())
        first.themes.use(ThemeId.STAR_CHART)
        first.themes.use(PieceStyleId.HEARTS_AND_CLUBS)
        first.themes.use(RingEffectId.RUBBER_STAMP)
        first.themes.use(PieceStyleId.FLAME_AND_FROST)
        first.close()

        val second = Session(file())
        assertEquals(ThemeId.STAR_CHART, second.themes.inUse.first())
        assertEquals(PieceStyleId.FLAME_AND_FROST, second.themes.pieces.first())
        assertEquals(RingEffectId.RUBBER_STAMP, second.themes.rings.first())
        second.close()
    }

    @Test
    fun `the piece style and ring effect come back with a backup restore`() = runBlocking {
        val old = Session(file("old phone"))
        old.themes.use(PieceStyleId.HEARTS_AND_CLUBS)
        old.themes.use(RingEffectId.LASSO)
        old.close()

        file("old phone").copyTo(file("new phone"))
        val restored = Session(file("new phone"))
        assertEquals(PieceStyleId.HEARTS_AND_CLUBS, restored.themes.pieces.first())
        assertEquals(RingEffectId.LASSO, restored.themes.rings.first())
        restored.close()
    }

    @Test
    fun `a debug build offers every piece style and ring effect, a release build only Standard`() = runBlocking {
        val debug = Session(file("debug"))
        assertEquals(listOf(PieceStyleId.STANDARD, PieceStyleId.HEARTS_AND_CLUBS, PieceStyleId.FLAME_AND_FROST), debug.themes.pieceChoices)
        assertEquals(listOf(RingEffectId.STANDARD, RingEffectId.LASSO, RingEffectId.RUBBER_STAMP), debug.themes.ringChoices)
        debug.themes.use(PieceStyleId.FLAME_AND_FROST)
        debug.themes.use(RingEffectId.LASSO)
        debug.close()

        file("debug").copyTo(file("release"))
        val release = Session(file("release"), debug = false)
        assertEquals(listOf(PieceStyleId.STANDARD), release.themes.pieceChoices)
        assertEquals(listOf(RingEffectId.STANDARD), release.themes.ringChoices)
        assertEquals("a debug build's choice reads as Standard", PieceStyleId.STANDARD, release.themes.pieces.first())
        assertEquals(RingEffectId.STANDARD, release.themes.rings.first())
        release.themes.use(RingEffectId.RUBBER_STAMP)
        assertEquals("nothing was saved", RingEffectId.STANDARD, release.themes.rings.first())
        release.close()
    }

    @Test
    fun `only the items the player owns are listed, and one no longer owned goes back to Standard`() = runBlocking {
        val session = Session(file())
        var owned = setOfNotNull(PieceStyleId.HEARTS_AND_CLUBS.cosmetic, RingEffectId.RUBBER_STAMP.cosmetic)
        val themes = ThemeRepository(session.store) { CosmeticRule { it in owned } }
        assertEquals(listOf(PieceStyleId.STANDARD, PieceStyleId.HEARTS_AND_CLUBS), themes.pieceChoices)
        assertEquals(listOf(RingEffectId.STANDARD, RingEffectId.RUBBER_STAMP), themes.ringChoices)

        themes.use(PieceStyleId.FLAME_AND_FROST)
        assertEquals("one the player doesn't own is not put in use", PieceStyleId.STANDARD, themes.pieces.first())
        themes.use(PieceStyleId.HEARTS_AND_CLUBS)
        themes.use(RingEffectId.RUBBER_STAMP)
        assertEquals(PieceStyleId.HEARTS_AND_CLUBS, themes.pieces.first())
        assertEquals(RingEffectId.RUBBER_STAMP, themes.rings.first())

        // A refund of Hearts and clubs: the pieces go back to Standard without a message; the ring stays.
        owned = setOfNotNull(RingEffectId.RUBBER_STAMP.cosmetic)
        assertEquals(listOf(PieceStyleId.STANDARD), themes.pieceChoices)
        assertEquals(PieceStyleId.STANDARD, themes.pieces.first())
        assertEquals(RingEffectId.RUBBER_STAMP, themes.rings.first())
        session.close()
    }

    @Test
    fun `each piece style and ring effect but Standard is a cosmetic of its kind`() {
        assertEquals(null, PieceStyleId.STANDARD.cosmetic)
        assertEquals(null, RingEffectId.STANDARD.cosmetic)
        assertEquals(null, ThemeId.NOTEBOOK.cosmetic)
        for (style in PieceStyleId.entries - PieceStyleId.STANDARD) assertEquals(style.key, Cosmetic.Kind.PIECE_STYLE, style.cosmetic?.kind)
        for (effect in RingEffectId.entries - RingEffectId.STANDARD) assertEquals(effect.key, Cosmetic.Kind.RING_EFFECT, effect.cosmetic?.kind)
        for (theme in ThemeId.entries - ThemeId.NOTEBOOK) assertEquals(theme.key, Cosmetic.Kind.THEME, theme.cosmetic?.kind)
        val named = (PieceStyleId.entries + RingEffectId.entries + ThemeId.entries).mapNotNull { it.cosmetic }.toSet()
        assertEquals("every cosmetic can be chosen", Cosmetic.entries.toSet(), named)
    }

    @Test
    fun `a saved piece style or ring effect this version doesn't know reads as Standard`() = runBlocking {
        val session = Session(file())
        session.store.edit {
            it[stringPreferencesKey("pieces")] = "paper-planes"
            it[stringPreferencesKey("rings")] = "ink-splash"
        }
        assertEquals(PieceStyleId.STANDARD, session.themes.pieces.first())
        assertEquals(RingEffectId.STANDARD, session.themes.rings.first())
        session.close()
    }
}
