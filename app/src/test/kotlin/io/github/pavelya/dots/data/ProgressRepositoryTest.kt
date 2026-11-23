package io.github.pavelya.dots.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dots.campaign.Campaign
import dots.campaign.ChestState
import dots.campaign.Level
import dots.campaign.LevelResult
import dots.campaign.MissionStatus
import dots.campaign.Outcome
import dots.campaign.Progress
import dots.campaign.Reason
import dots.campaign.Reward
import dots.campaign.Title
import dots.engine.Booster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProgressRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    /** Opens the progress file as the app would after a start; close() is what a restart does. */
    private class Session(file: File) {
        private val job = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.IO + job)
        val repository = ProgressRepository(ProgressRepository.store(scope) { file }, scope)

        suspend fun close() = job.cancelAndJoin()
    }

    private fun file() = folder.root.resolve("progress.preferences_pb")

    private fun level(code: String): Level = code.split('-').map(String::toInt).let { (world, number) -> Campaign.level(world, number) }

    private fun passed(stars: Int) = LevelResult(MissionStatus(Outcome.PASSED, Reason.GAME_OVER, 1, 0, null), stars, null)

    /** Waits until the saved progress meets [condition]: spent boosters are saved in the background. */
    private suspend fun ProgressRepository.eventually(condition: (Progress) -> Boolean): Progress = withTimeout(5_000) {
        var progress = progress.first()
        while (!condition(progress)) {
            delay(10)
            progress = this@eventually.progress.first()
        }
        progress
    }

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

    @Test
    fun `stars, own boosters, chests and cosmetics survive a restart, and so does the title`() = runBlocking {
        val chest = Campaign.chests.first()
        val saved = Progress(
            stars = Campaign.levels.take(10).associate { it.code to 2 },
            cosmetics = setOf("ink-splash"),
        ).open(chest, chest.choices.single())
        val first = Session(file())
        first.repository.replace(saved)
        first.close()

        val second = Session(file())
        val read = second.repository.progress.first()
        assertEquals(saved, read)
        assertEquals("the chest after 1-10 gives two walls (decision 0038)", mapOf(Booster.WALL to 2), read.boosters)
        assertEquals(Title.APPRENTICE, second.repository.map.filterNotNull().first().title)
        assertEquals(ChestState.OPENED, second.repository.map.filterNotNull().first().chests.first().state)
        second.close()
    }

    @Test
    fun `progress saved by an older version keeps its stars, and a chest it passed waits to be opened`() = runBlocking {
        // Before A-04 the store held only the stars and the failures.
        val job = SupervisorJob()
        val old = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file() }
        val stars = Campaign.levels.take(10).joinToString(" ") { "${it.code}:3" }
        old.edit {
            it[stringPreferencesKey("stars")] = stars
            it[stringPreferencesKey("failures")] = "1-11:2"
        }
        job.cancelAndJoin()

        val session = Session(file())
        val progress = session.repository.progress.first()
        assertEquals(Progress.parse(stars), progress)
        assertEquals(emptyMap<Booster, Int>(), progress.boosters)
        assertEquals(ChestState.READY, session.repository.map.filterNotNull().first().chests.first().state)
        // The failures are still counted.
        val failed = LevelResult(MissionStatus(Outcome.FAILED, Reason.GAME_OVER, 0, 1, null), 0, null)
        assertEquals(3, session.repository.finished(level("1-11"), failed).failed)
        session.close()
    }

    @Test
    fun `a damaged file starts empty instead of stopping the app, and saves again`() = runBlocking {
        // Not a store at all: its first byte, "n", names a kind of field no store has.
        file().writeText("not a progress file")

        val session = Session(file())
        assertEquals(Progress.NONE, session.repository.progress.first())
        session.repository.record(level("1-1"), 2)
        assertEquals(Progress(mapOf("1-1" to 2)), session.repository.progress.first())
        session.close()
    }

    @Test
    fun `a pass of level 10 makes its chest ready once, and its one reward is kept once`() = runBlocking {
        val session = Session(file())
        session.repository.replace(Progress(Campaign.levels.take(9).associate { it.code to 2 }))
        val chest = Campaign.chests.first()

        val end = session.repository.finished(level("1-10"), passed(2))
        assertEquals(chest, end.chest)
        assertEquals(level("1-11"), end.next)

        // One reward, two walls, and no pick (decision 0038). Keeping it twice keeps it once.
        assertEquals(listOf(Reward(Booster.WALL, 2)), chest.choices)
        session.repository.open(chest, chest.choices.single())
        session.repository.open(chest, chest.choices.single())
        assertEquals(mapOf(Booster.WALL to 2), session.repository.progress.first().boosters)
        assertNull("an opened chest does not come back", session.repository.finished(level("1-10"), passed(3)).chest)
        session.close()
    }

    @Test
    fun `when a pass opens a world, Next goes to its first level`() = runBlocking {
        val session = Session(file())
        // World 1 passed with 18 stars: World 2 needs 20. A replay of 1-4 for 3 stars opens it.
        val world1 = Campaign.worlds.first().levels
        session.repository.replace(Progress(world1.withIndex().associate { (i, it) -> it.code to if (i < 3) 2 else 1 }))

        val end = session.repository.finished(level("1-4"), passed(3))
        assertEquals(listOf(Campaign.worlds[1]), end.opened.worlds)
        assertEquals(level("2-1"), end.next)
        session.close()
    }

    @Test
    fun `a game spends the player's own boosters as it uses them, once, and an undo gives them back`() = runBlocking {
        val session = Session(file())
        val repository = session.repository
        repository.replace(Progress(boosters = mapOf(Booster.WALL to 3, Booster.BOMB to 1)))
        val level = level("4-6")

        repository.spent(game = 1, level, UsedBoosters(owned = mapOf(Booster.WALL to 1)))
        repository.spent(game = 1, level, UsedBoosters(owned = mapOf(Booster.WALL to 2)))
        repository.eventually { it.boosters == mapOf(Booster.WALL to 1, Booster.BOMB to 1) }

        repository.spent(game = 1, level, UsedBoosters(owned = mapOf(Booster.WALL to 2))) // reported again on resume
        repository.spent(game = 1, level, UsedBoosters(owned = mapOf(Booster.WALL to 1))) // an undo
        repository.eventually { it.boosters == mapOf(Booster.WALL to 2, Booster.BOMB to 1) }

        repository.spent(game = 2, level, UsedBoosters(owned = mapOf(Booster.BOMB to 1))) // the next game
        assertEquals(mapOf(Booster.WALL to 2), repository.eventually { Booster.BOMB !in it.boosters }.boosters)
        session.close()

        val again = Session(file())
        assertEquals(mapOf(Booster.WALL to 2), again.repository.progress.first().boosters)
        again.close()
    }

    @Test
    fun `a level's own boosters are spent once, come back with an undo, and stay spent after a restart`() = runBlocking {
        val session = Session(file())
        val repository = session.repository
        val level = level("2-1") // wall 2, undo 1

        repository.spent(game = 1, level, UsedBoosters(own = mapOf(Booster.WALL to 1)))
        repository.spent(game = 1, level, UsedBoosters(own = mapOf(Booster.WALL to 1))) // reported again on resume
        repository.eventually { it.spent(level) == mapOf(Booster.WALL to 1) }
        repository.spent(game = 1, level, UsedBoosters(own = mapOf(Booster.UNDO to 1))) // an undo took the wall's turn back
        repository.eventually { it.spent(level) == mapOf(Booster.UNDO to 1) }
        repository.spent(game = 1, level, UsedBoosters(own = mapOf(Booster.WALL to 1, Booster.UNDO to 1)))

        // The level's end is saved after every booster its game reported.
        repository.finished(level, LevelResult(MissionStatus(Outcome.FAILED, Reason.GAME_OVER, 0, 1, null), 0, null))
        assertEquals(mapOf(Booster.WALL to 1, Booster.UNDO to 1), repository.progress.first().spent(level))

        repository.spent(game = 2, level, UsedBoosters(own = mapOf(Booster.WALL to 1))) // the retry uses the last wall
        repository.eventually { it.spent(level) == mapOf(Booster.WALL to 2, Booster.UNDO to 1) }
        session.close()

        val again = Session(file())
        val progress = again.repository.progress.first()
        assertEquals(mapOf(Booster.WALL to 2, Booster.UNDO to 1), progress.spent(level))
        assertEquals(mapOf(Booster.WALL to 0, Booster.UNDO to 0), progress.boostersFor(level))
        assertEquals(0, progress.game(level).start().boostersLeft(level.human, Booster.WALL))
        assertEquals(mapOf(Booster.WALL to 1, Booster.UNDO to 1), progress.boostersFor(level("2-2")), "other levels keep theirs")
        again.close()
    }
}
