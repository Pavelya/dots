package io.github.pavelya.dots.data

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
 * What the Shop has shown of the items earned (docs/design/flows.md, Campaign), and the purchases
 * kept for offline (money.md, "Owning an item"): each in its own place, across a restart.
 */
class ShopMarksTest {
    @get:Rule
    val folder = TemporaryFolder()

    private class Session(progress: File, purchases: File) {
        private val job = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.IO + job)
        val store = ProgressRepository.store(scope) { progress }
        val marks = ShopMarks(store)
        val progress = ProgressRepository(store, scope)
        val bought = StoredPurchases(ProgressRepository.store(scope) { purchases })

        suspend fun close() = job.cancelAndJoin()
    }

    private fun session() = Session(folder.root.resolve("progress.preferences_pb"), folder.root.resolve("purchases.preferences_pb"))

    @Test
    fun `opening the Shop and a preview are remembered across a restart`() = runBlocking {
        val first = session()
        assertEquals(emptySet<Cosmetic>(), first.marks.shown.first())
        first.marks.shopOpened(setOf(Cosmetic.HEARTS_AND_CLUBS, Cosmetic.LASSO))
        first.marks.previewOpened(Cosmetic.LASSO)
        first.marks.shopOpened(setOf(Cosmetic.FLAME_AND_FROST))
        first.close()

        val second = session()
        assertEquals(setOf(Cosmetic.HEARTS_AND_CLUBS, Cosmetic.LASSO, Cosmetic.FLAME_AND_FROST), second.marks.shown.first())
        assertEquals(setOf(Cosmetic.LASSO), second.marks.seen.first())
        second.close()
    }

    @Test
    fun `purchases are kept apart from the progress, which never holds them`() = runBlocking {
        val first = session()
        first.bought.save(setOf(Cosmetic.STAR_CHART))
        first.close()

        val second = session()
        assertEquals(setOf(Cosmetic.STAR_CHART), second.bought.read())
        assertEquals("the saved progress knows nothing of it", emptySet<String>(), second.progress.progress.first().cosmetics)
        second.close()
    }
}
