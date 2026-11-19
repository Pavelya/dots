package io.github.pavelya.dots.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dots.campaign.Campaign
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
 * restart and a backup restore, and only ever a theme the player can choose (decision 0029).
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
        val themes = ThemeRepository(store) { ThemeRule.beforeShop(debug) }
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
}
