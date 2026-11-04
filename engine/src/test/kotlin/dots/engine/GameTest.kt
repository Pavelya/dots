package dots.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameTest {
    private val settings = GameSettings(width = 9, height = 9, dotsPerPlayer = 10, opening = Opening.CROSS)

    @Test
    fun `undo against the computer takes back its reply as well`() {
        val game = Game(settings)
        val start = game.state.toAscii()
        game.apply(Action.Place(Pos(0, 0)))   // red, the human
        game.apply(Action.Place(Pos(8, 8)))   // blue, the computer

        assertTrue(game.undo(Player.RED))
        assertEquals(start, game.state.toAscii())
        assertEquals(Player.RED, game.state.toMove)
        assertTrue(game.record.actions.isEmpty())
    }

    @Test
    fun `undo with nothing to take back does nothing`() {
        val game = Game(settings)

        assertFalse(game.canUndo(Player.RED))
        assertFalse(game.undo(Player.RED))
        game.apply(Action.Place(Pos(0, 0)))
        assertFalse(game.undo(Player.BLUE))
        assertEquals(1, game.record.actions.size)
    }

    @Test
    fun `undo restores scores after a capture`() {
        val game = Game(GameSettings(width = 5, height = 5, dotsPerPlayer = null, opening = Opening.EMPTY))
        for (move in listOf(Pos(2, 1), Pos(2, 2), Pos(1, 2), Pos(0, 0), Pos(3, 2), Pos(4, 4))) {
            game.apply(Action.Place(move))
        }
        game.apply(Action.Place(Pos(2, 3)))
        assertEquals(1, game.state.score(Player.RED))

        game.undo(Player.RED)
        assertEquals(0, game.state.score(Player.RED))
        assertEquals(Player.RED, game.state.toMove)
    }

    @Test
    fun `a game rebuilt from its record is the same game`() {
        val game = Game(GameSettings.POC.copy(seed = 3))
        game.apply(Action.Place(Pos(1, 1)))
        game.apply(Action.Place(Pos(13, 13)))

        val rebuilt = Game.from(game.record)
        assertEquals(game.state.toAscii(), rebuilt.state.toAscii())
        assertEquals(game.record, rebuilt.record)
        assertTrue(rebuilt.undo(Player.BLUE))
    }
}
