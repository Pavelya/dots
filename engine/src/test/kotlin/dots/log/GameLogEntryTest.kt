package dots.log

import dots.ai.SearchAi
import dots.campaign.Campaign
import dots.campaign.LevelRecord
import dots.engine.Action
import dots.engine.Game
import dots.engine.GameSettings
import dots.engine.Player
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameLogEntryTest {

    /** A whole POC game of random moves, logged the way the app logs it. */
    private fun finishedGame(seed: Long): GameLogEntry {
        val random = Random(seed)
        val game = Game(GameSettings.POC.copy(seed = seed))
        while (!game.state.isOver) {
            val moves = game.state.legalMoves()
            game.apply(Action.Place(moves[random.nextInt(moves.size)]))
        }
        val end = game.state
        return GameLogEntry.start(game.record, opponent = "normal", threatHighlight = true, now = 1_762_300_000_000, appVersion = "0.1-poc")
            .copy(updatedAt = 1_762_300_400_000, playMillis = 395_000, undos = 2, outcome = Outcome.FINISHED,
                placing = "magnifier", touches = 40, placed = 34,
                red = end.score(Player.RED), blue = end.score(Player.BLUE), record = game.record)
    }

    @Test
    fun `an entry survives the trip to JSON and back, and replays consistently`() {
        repeat(20) { seed ->
            val entry = finishedGame(seed.toLong())
            val line = entry.toJson()
            assertTrue('\n' !in line, "one line per game")
            assertEquals(entry, GameLogEntry.parse(line))
            assertNull(entry.check())
        }
    }

    @Test
    fun `the line format is stable`() {
        val entry = GameLogEntry.start(GameSettings.POC.copy(seed = 42), GameLogEntry.HUMAN, threatHighlight = false, now = 5, appVersion = "0.1")
        assertEquals(
            """{"app":"0.1","started":5,"updated":5,"play_ms":0,"opponent":"human","threats":false,"undos":0,""" +
                """"outcome":"playing","red":0,"blue":0,"placing":"","touches":0,"placed":0,""" +
                """"record":"dots/1 15x15 dots=33 opening=crosses seed=42 first=R |"}""",
            entry.toJson(),
        )
    }

    @Test
    fun `a line written before placing was recorded still reads, with no method and no touches`() {
        val old = finishedGame(2).toJson().replace(Regex(""""placing":"[^"]*","touches":\d+,"placed":\d+,"""), "")
        val entry = GameLogEntry.parse(old)
        assertEquals("", entry.placing)
        assertEquals(0, entry.touches)
        assertEquals(0, entry.placed)
        assertEquals(finishedGame(2).copy(placing = "", touches = 0, placed = 0), entry)
    }

    @Test
    fun `a campaign level's game is logged with its level record`() {
        val level = Campaign.level(1, 7)
        val game = Game(LevelRecord(level, emptyList()))
        while (!game.isOver) {
            val state = game.state
            val move = if (state.toMove == level.human) Action.Place(state.legalMoves().first()) else SearchAi(level.ai, Random(1)).choose(state)
            game.apply(move)
        }
        val entry = GameLogEntry.start(game.record, level.strength.text, threatHighlight = false, now = 5)
            .copy(outcome = Outcome.FINISHED, red = game.state.score(Player.RED), blue = game.state.score(Player.BLUE), record = game.record)

        assertTrue(entry.toJson().contains("\"record\":\"level/1 1-7 |"), entry.toJson())
        assertEquals(entry, GameLogEntry.parse(entry.toJson()))
        assertNull(entry.check(), "a level is over when its mission is decided")
        assertNotNull(entry.copy(outcome = Outcome.PLAYING).check())
        // A level that no longer exists cannot be read back; the app then forgets the saved game.
        assertFailsWith<NoSuchElementException> { GameLogEntry.parse(entry.toJson().replace("level/1 1-7", "level/1 9-99")) }
    }

    @Test
    fun `text with quotes and control characters is escaped`() {
        val entry = finishedGame(1).copy(appVersion = "a \"quoted\" \\ name\n\u0001")
        assertEquals(entry, GameLogEntry.parse(entry.toJson()))
    }

    @Test
    fun `replaying finds entries that do not match their record`() {
        val entry = finishedGame(3)
        assertNotNull(entry.copy(red = entry.red + 1).check())
        assertNotNull(entry.copy(outcome = Outcome.ABANDONED).check())
        val unfinished = entry.copy(record = entry.record.withActions(entry.record.actions.take(10)))
        assertNotNull(unfinished.check(), "logged as finished after 10 moves")
    }

    @Test
    fun `the winner is known only for finished games`() {
        val entry = finishedGame(4)
        assertEquals(Player.RED, entry.copy(red = 3, blue = 1).winner)
        assertEquals(Player.BLUE, entry.copy(red = 0, blue = 1).winner)
        assertNull(entry.copy(red = 2, blue = 2).winner)
        assertNull(entry.copy(red = 3, blue = 1, outcome = Outcome.ABANDONED).winner)
    }

    @Test
    fun `a log is read line by line, and unreadable lines are reported, not fatal`() {
        val good = finishedGame(5).toJson()
        val bad = mutableListOf<Int>()
        val entries = GameLogEntry.parseLog("$good\n\n{\"app\":\"x\"}\nnot json\n$good\n") { line, _ -> bad += line }
        assertEquals(2, entries.size)
        assertEquals(listOf(3, 4), bad)
    }

    @Test
    fun `broken lines are rejected with a reason`() {
        val good = finishedGame(6).toJson()
        for (broken in listOf("", "{", good.dropLast(1), good + "x", good.replace("\"undos\":2", "\"undos\":\"2\""))) {
            assertFailsWith<IllegalArgumentException>(broken) { GameLogEntry.parse(broken) }
        }
    }
}
