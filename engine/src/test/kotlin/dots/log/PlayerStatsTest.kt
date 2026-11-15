package dots.log

import dots.campaign.Level
import dots.campaign.LevelMap
import dots.campaign.LevelRecord
import dots.campaign.Mission
import dots.campaign.Strength
import dots.daily.DailyPuzzle
import dots.daily.DailyRecord
import dots.daily.Day
import dots.engine.Action
import dots.engine.Booster
import dots.engine.GameRecord
import dots.engine.GameSettings
import dots.engine.Opening
import dots.engine.Player
import dots.engine.PlayRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The player's totals on Stats, counted from a scripted history of games. */
class PlayerStatsTest {

    /** A small free board: 5 x 5, empty, 4 dots each. */
    private val free = GameSettings(width = 5, height = 5, dotsPerPlayer = 4, opening = Opening.EMPTY)

    /** Two blue dots that red closes in with c5. */
    private val twoInside = LevelMap(listOf(".....", "..R..", ".RBR.", ".RBR.", "....."))

    private val level = Level(2, 5, "Test", Strength.EASY, 3, twoInside, boosters = mapOf(Booster.UNDO to 1))

    /** One blue dot that red closes in with d3 and c4. */
    private val oneInside = LevelMap(listOf(".....", "..R..", ".RB..", ".....", "....."))

    private val puzzle = DailyPuzzle(Day(2027, 1, 5), oneInside, Mission.Capture(1, 2))

    private fun actions(words: String) = words.split(' ').map(Action::parse)

    /** [record] played to its end, logged as the app logs it; [check] makes sure the game is really over. */
    private fun finished(record: PlayRecord, opponent: String = "steady", practice: Boolean = false, outcome: Outcome = Outcome.FINISHED): GameLogEntry {
        val end = record.replay()
        return GameLogEntry.start(record, opponent, threatHighlight = true, now = 1_800_000_000_000)
            .copy(outcome = outcome, red = end.score(Player.RED), blue = end.score(Player.BLUE), record = record, practice = practice)
            .also { if (outcome == Outcome.FINISHED) assertNull(it.check()) }
    }

    /** Free play: red closes a ring round c3 and wins 1-0. */
    private val freeWon = finished(GameRecord(free, actions("b3 c3 c2 a1 d3 a5 c4 e5")))

    /** Free play: blue closes the ring and wins 0-1. */
    private val freeLost = finished(GameRecord(free, actions("c3 b3 a1 c2 a5 d3 e5 c4")))

    /** A level passed with one ring of two dots. */
    private val levelPassed = finished(LevelRecord(level, actions("c5 a1 a5 e5 e1 b5")))

    /** The same ring, taken back with Undo; the level then ends 0-0, not passed. */
    private val levelUndone = finished(LevelRecord(level, actions("c5 a1 undo a5 e5 e1 a1 e4 b5")))

    /** Today's puzzle solved in two moves, with a ring of one dot. */
    private val dailySolved = finished(DailyRecord(puzzle, actions("d3 a1 c4")))

    /** The same puzzle failed. */
    private val dailyFailed = finished(DailyRecord(puzzle, actions("a5 a1 e5")))

    @Test
    fun `each game adds what the player did in it`() {
        assertEquals(GameStats(won = true, captured = 1, bestCapture = 1), GameStats.of(freeWon))
        assertEquals(GameStats(won = false, captured = 0, bestCapture = 0), GameStats.of(freeLost))
        assertEquals(GameStats(won = true, captured = 2, bestCapture = 2), GameStats.of(levelPassed))
        assertEquals(GameStats(won = true, captured = 1, bestCapture = 1), GameStats.of(dailySolved))
        assertEquals(GameStats(won = false, captured = 0, bestCapture = 0), GameStats.of(dailyFailed))
    }

    @Test
    fun `a capture taken back with Undo does not count`() {
        assertEquals(GameStats(won = false, captured = 0, bestCapture = 0), GameStats.of(levelUndone))
    }

    @Test
    fun `two players, practice and abandoned games do not count`() {
        assertNull(GameStats.of(finished(freeWon.record, opponent = GameLogEntry.HUMAN)))
        assertNull(GameStats.of(finished(levelPassed.record, practice = true)))
        assertNull(GameStats.of(finished(GameRecord(free, actions("b3 c3")), outcome = Outcome.ABANDONED)))
    }

    @Test
    fun `the totals of a history`() {
        val history = listOf(
            freeWon, freeLost, levelPassed, levelUndone, dailySolved, dailyFailed,
            finished(freeWon.record, opponent = GameLogEntry.HUMAN),
            finished(levelPassed.record, practice = true),
        )
        assertEquals(PlayerStats(played = 6, won = 3, captured = 4, bestCapture = 2), PlayerStats.of(history))
    }

    @Test
    fun `the totals survive their text form`() {
        val stats = PlayerStats(played = 48, won = 31, captured = 212, bestCapture = 9)
        assertEquals("played:48 won:31 captured:212 best:9", stats.text)
        assertEquals(stats, PlayerStats.parse(stats.text))
    }

    @Test
    fun `what cannot be read counts as 0`() {
        assertEquals(PlayerStats.NONE, PlayerStats.parse(""))
        assertEquals(PlayerStats(played = 3, bestCapture = 2), PlayerStats.parse("played:3 won:x captured:-1 best:2 extra"))
    }
}
