package dots.cli

import dots.campaign.Campaign
import dots.campaign.LevelRecord
import dots.engine.Action
import dots.engine.Booster
import dots.engine.Game
import dots.engine.GameSettings
import dots.engine.Player
import dots.log.GameLogEntry
import dots.log.Outcome
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogCheckTest {
    private fun logged(seed: Long): GameLogEntry {
        val random = Random(seed)
        val game = Game(GameSettings.POC.copy(seed = seed))
        while (!game.state.isOver) {
            val moves = game.state.legalMoves()
            game.apply(Action.Place(moves[random.nextInt(moves.size)]))
        }
        return GameLogEntry.start(game.record, "easy", threatHighlight = false, now = 0)
            .copy(outcome = Outcome.FINISHED, red = game.state.score(Player.RED), blue = game.state.score(Player.BLUE), record = game.record)
    }

    private fun logFile(vararg lines: String) =
        File.createTempFile("dots-log", ".txt").apply { deleteOnExit(); writeText(lines.joinToString("\n")) }

    @Test
    fun `a consistent log replays without problems`() {
        val file = logFile(*Array(5) { logged(it.toLong()).toJson() })
        assertEquals(0, LogCheck.run(listOf(file.path)))
    }

    @Test
    fun `a game whose logged result the replay does not give, and an unreadable line, are both counted`() {
        val wrong = logged(7).let { it.copy(red = it.red + 1) }
        val file = logFile(logged(1).toJson(), wrong.toJson(), "garbage")
        assertEquals(2, LogCheck.run(listOf(file.path)))
    }

    @Test
    fun `a level game that used all five boosters replays, Undo included`() {
        val level = Campaign.level(4, 6)
        assertEquals(Booster.entries.toSet(), level.boosters.keys)
        val game = Game(LevelRecord(level, emptyList()))
        fun first(points: (dots.engine.Pos) -> Action) =
            (0 until game.state.height).flatMap { y -> (0 until game.state.width).map { x -> points(dots.engine.Pos(x, y)) } }
                .first { game.state.isLegal(it) }
        fun place() = game.apply(Action.Place(game.state.legalMoves().first()))
        fun reply() = place().also { assertEquals(Player.RED, game.state.toMove) }

        place(); reply()
        game.apply(Action.Undo)
        game.apply(first { Action.Wall(it) }); place(); reply()
        game.apply(first { Action.Hint(it) }); place(); reply()
        game.apply(first { Action.Bomb(it) }); place(); reply()
        game.apply(Action.DoubleDot); place(); place(); reply()

        for (booster in Booster.entries) assertEquals(0, game.state.boostersLeft(Player.RED, booster))
        val entry = GameLogEntry.start(game.record, level.strength.text, threatHighlight = false, now = 0)
            .copy(red = game.state.score(Player.RED), blue = game.state.score(Player.BLUE), record = game.record)
        assertEquals(0, LogCheck.run(listOf(logFile(entry.toJson()).path)))
    }

    @Test
    fun `a missing file is a problem, not a crash`() {
        assertTrue(LogCheck.run(listOf("/no/such/log.txt")) > 0)
    }
}
