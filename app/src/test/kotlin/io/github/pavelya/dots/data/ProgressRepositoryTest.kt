package io.github.pavelya.dots.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dots.campaign.Campaign
import dots.campaign.Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProgressRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    /** Opens the progress file as the app would after a start; close() is what a restart does. */
    private class Session(file: java.io.File) {
        private val job = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.IO + job)
        val repository = ProgressRepository(PreferenceDataStoreFactory.create(scope = scope) { file }, scope)

        suspend fun close() = job.cancelAndJoin()
    }

    private fun file() = folder.root.resolve("progress.preferences_pb")

    @Test
    fun `a fresh install has no progress and level 1-1 next`() = runBlocking {
        val session = Session(file())
        assertEquals(Progress.NONE, session.repository.progress.first())
        assertEquals(Campaign.level(1, 1), session.repository.map.filterNotNull().first().next)
        session.close()
    }

    @Test
    fun `stars survive a restart, and a replay keeps the best`() = runBlocking {
        val first = Session(file())
        first.repository.record(Campaign.level(1, 1), 3)
        first.repository.record(Campaign.level(1, 1), 1)
        first.repository.record(Campaign.level(1, 2), 2)
        first.close()

        val second = Session(file())
        assertEquals(Progress(mapOf("1-1" to 3, "1-2" to 2)), second.repository.progress.first())
        assertEquals(Campaign.level(1, 3), second.repository.map.filterNotNull().first().next)
        second.close()
    }
}
