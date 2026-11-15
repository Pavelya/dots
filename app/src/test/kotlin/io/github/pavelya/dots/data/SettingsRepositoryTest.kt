package io.github.pavelya.dots.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dots.engine.Opening
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    /** Opens the settings file as the app would after a start; close() is what a restart does. */
    private class Session(file: java.io.File) {
        private val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
        val repository = SettingsRepository(store)

        /**
         * Waits until the store has let go of the file. Cancelling without waiting lets the next
         * session open the file while this one still holds it, which DataStore refuses.
         */
        suspend fun close() = job.cancelAndJoin()
    }

    private fun file() = folder.root.resolve("settings.preferences_pb")

    @Test
    fun `a fresh install uses the POC format`() = runBlocking {
        val session = Session(file())
        assertEquals(AppSettings(), session.repository.settings.first())
        session.close()
    }

    @Test
    fun `settings survive a restart`() = runBlocking {
        // Every setting away from its default, so a value that is not saved shows up.
        val chosen = AppSettings(
            boardSize = BoardSize.LARGE,
            length = GameLength.OPEN_ENDED,
            threatHighlight = true,
            sound = false,
            musicVolume = 0,
            effectsVolume = 35,
            vibration = false,
            highContrast = true,
            links = true,
            placing = PlacingMethod.DRAG,
            tutorialSkipped = true,
        )
        for (property in AppSettings::class.java.declaredFields.filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }) {
            property.isAccessible = true
            assertNotEquals("${property.name} is at its default", property.get(AppSettings()), property.get(chosen))
        }
        val first = Session(file())
        first.repository.update { chosen }
        first.close()

        val second = Session(file())
        assertEquals(chosen, second.repository.settings.first())
        second.close()
    }

    @Test
    fun `a stored value the app no longer knows falls back to its default`() = runBlocking {
        val session = Session(file())
        session.store.edit { it[stringPreferencesKey("board_size")] = "GIGANTIC" }
        assertEquals(BoardSize.STANDARD, session.repository.settings.first().boardSize)
        session.store.edit { it[intPreferencesKey("music_volume")] = 250 }
        assertEquals(AppSettings.DEFAULT_VOLUME, session.repository.settings.first().musicVolume)
        session.close()
    }

    @Test
    fun `dots scale with the board, and games start from four crosses`() {
        val expected = mapOf(
            (BoardSize.SMALL to GameLength.SHORT) to 21, (BoardSize.SMALL to GameLength.LONG) to 43,
            (BoardSize.STANDARD to GameLength.SHORT) to 33, (BoardSize.STANDARD to GameLength.LONG) to 67,
            (BoardSize.LARGE to GameLength.SHORT) to 60, (BoardSize.LARGE to GameLength.LONG) to 120,
        )
        for ((key, dots) in expected) assertEquals("$key", dots, AppSettings.dotsFor(key.first, key.second))
        for (size in BoardSize.entries) assertEquals(null, AppSettings.dotsFor(size, GameLength.OPEN_ENDED))

        val game = AppSettings(boardSize = BoardSize.SMALL).gameSettings(seed = 9)
        assertEquals(12, game.width)
        assertEquals(21, game.dotsPerPlayer)
        assertEquals(Opening.CROSSES, game.opening)
    }
}
