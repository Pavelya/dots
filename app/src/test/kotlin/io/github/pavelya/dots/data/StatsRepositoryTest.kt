package io.github.pavelya.dots.data

import dots.engine.Action
import dots.engine.GameRecord
import dots.engine.GameSettings
import dots.engine.Opening
import dots.engine.Player
import dots.log.GameLogEntry
import dots.log.Outcome
import dots.log.PlayerStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** The totals on Stats, from a scripted history of games reported the way the game screen reports them (A-11). */
class StatsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    /** Opens the progress file as the app would after a start; close() is what a restart does. */
    private inner class Session(file: File) {
        private val job = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.IO + job)
        private val store = ProgressRepository.store(scope) { file }
        val stats = StatsRepository(store, scope)

        /** The log behind the journal: every game it was told about. */
        val log = mutableListOf<GameLogEntry>()
        val journal = CountingJournal(
            object : GameJournal {
                override fun start(entry: GameLogEntry) {}
                override fun save(entry: GameLogEntry) {}
                override fun finish(entry: GameLogEntry) {
                    log += entry
                }
            },
            stats,
        )

        suspend fun close() = job.cancelAndJoin()
    }

    private fun file() = folder.root.resolve("progress.preferences_pb")

    /** A small free board: 5 x 5, empty, 4 dots each. */
    private val free = GameSettings(width = 5, height = 5, dotsPerPlayer = 4, opening = Opening.EMPTY)

    private fun game(words: String, startedAt: Long, opponent: String = "steady", outcome: Outcome = Outcome.FINISHED): GameLogEntry {
        val record = GameRecord(free, words.split(' ').map(Action::parse))
        val end = record.replay()
        return GameLogEntry.start(record, opponent, threatHighlight = true, now = startedAt)
            .copy(outcome = outcome, red = end.score(Player.RED), blue = end.score(Player.BLUE))
    }

    /** Red closes a ring round c3 and wins 1-0. */
    private fun won(startedAt: Long, opponent: String = "steady") = game("b3 c3 c2 a1 d3 a5 c4 e5", startedAt, opponent)

    /** Blue closes the ring and wins 0-1. */
    private fun lost(startedAt: Long) = game("c3 b3 a1 c2 a5 d3 e5 c4", startedAt)

    private suspend fun Session.counted(played: Int): PlayerStats =
        withTimeout(5_000) { stats.stats.first { it != null && it.played == played }!! }

    @Test
    fun `a new player has no totals`() = runBlocking {
        val session = Session(file())
        assertEquals(PlayerStats.NONE, withTimeout(5_000) { session.stats.stats.first { it != null } })
        session.close()
    }

    @Test
    fun `the journal counts the player's finished games, each once, and they survive a restart`() = runBlocking {
        val first = Session(file())
        val history = listOf(
            won(1_000),
            lost(2_000),
            // Two players on one phone: logged, not counted.
            won(3_000, opponent = GameLogEntry.HUMAN),
            won(4_000),
            // The same game reported again counts once.
            won(4_000),
        )
        for (entry in history) first.journal.finish(entry)
        // An abandoned game never reaches finish; counted directly, it still does not count.
        first.stats.count(game("b3 c3", 5_000, outcome = Outcome.ABANDONED))
        // A later game, so the totals below are known to include everything before it.
        first.journal.finish(lost(6_000))

        val expected = PlayerStats(played = 4, won = 2, captured = 2, bestCapture = 1)
        assertEquals(expected, first.counted(played = 4))
        assertEquals(history + lost(6_000), first.log)
        first.close()

        val second = Session(file())
        assertEquals(expected, withTimeout(5_000) { second.stats.stats.first { it != null } })
        second.close()
    }
}
