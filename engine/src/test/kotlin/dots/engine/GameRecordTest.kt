package dots.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GameRecordTest {

    /** Plays random legal actions, now and then a pass or a stop, to the end. */
    private fun randomGame(settings: GameSettings, random: Random): Game {
        val game = Game(settings)
        while (!game.state.isOver) {
            val state = game.state
            val moves = state.legalMoves()
            val action = when {
                state.isCleanup && (moves.isEmpty() || random.nextInt(10) == 0) -> Action.Finish
                !state.isCleanup && random.nextInt(60) == 0 -> Action.Stop
                !state.isCleanup && (moves.isEmpty() || random.nextInt(40) == 0) -> Action.Pass
                else -> Action.Place(moves[random.nextInt(moves.size)])
            }
            game.apply(action)
        }
        return game
    }

    @Test
    fun `the POC format starts with four crosses that depend only on the seed`() {
        val start = GameSettings.POC.copy(seed = 7).newGame()
        val reds = (0 until 15).sumOf { y -> (0 until 15).count { x -> start.ownerAt(x, y) == Player.RED } }
        val blues = (0 until 15).sumOf { y -> (0 until 15).count { x -> start.ownerAt(x, y) == Player.BLUE } }

        assertEquals(8, reds)
        assertEquals(8, blues)
        assertEquals(33, start.dotsLeft(Player.RED))
        assertEquals(start.toAscii(), GameSettings.POC.copy(seed = 7).newGame().toAscii())
        assertNotEquals(start.toAscii(), GameSettings.POC.copy(seed = 8).newGame().toAscii())
    }

    @Test
    fun `records replay exactly and survive their text form`() {
        val random = Random(1)
        val formats = listOf(
            GameSettings.POC,
            GameSettings(width = 9, height = 9, dotsPerPlayer = 20, opening = Opening.CROSS),
            GameSettings(width = 30, height = 12, dotsPerPlayer = null, opening = Opening.DOUBLE_CROSS, first = Player.BLUE),
            GameSettings(width = 8, height = 8, dotsPerPlayer = 15, opening = Opening.EMPTY),
        )
        repeat(500) { i ->
            val settings = formats[i % formats.size].copy(seed = random.nextLong())
            val game = randomGame(settings, random)
            val text = game.record.toText()
            val parsed = GameRecord.parse(text)

            assertEquals(game.record, parsed, text)
            val replayed = parsed.replay()
            assertEquals(game.state.toAscii(), replayed.toAscii(), text)
            assertEquals(game.state.score(Player.RED), replayed.score(Player.RED), text)
            assertEquals(game.state.score(Player.BLUE), replayed.score(Player.BLUE), text)
            assertEquals(game.state.isOver, replayed.isOver, text)
        }
    }

    @Test
    fun `the text form is one readable line`() {
        val game = Game(GameSettings.POC.copy(seed = 42))
        game.apply(Action.Place(Pos(0, 0))) // corners are never part of the crosses
        game.apply(Action.Pass)

        assertEquals("dots/1 15x15 dots=33 opening=crosses seed=42 first=R | a1 pass", game.record.toText())
    }

    @Test
    fun `broken records are refused with a reason`() {
        assertFailsWith<IllegalArgumentException> { GameRecord.parse("hello") }
        assertFailsWith<IllegalArgumentException> { GameRecord.parse("dots/1 15 dots=33 opening=crosses seed=1 first=R |") }
        assertFailsWith<IllegalArgumentException> { GameRecord.parse("dots/1 15x15 dots=33 opening=crosses seed=1 first=R | e5 ??") }
        assertTrue(GameRecord.parse("dots/1 15x15 dots=none opening=empty seed=1 first=B |").actions.isEmpty())
    }
}
