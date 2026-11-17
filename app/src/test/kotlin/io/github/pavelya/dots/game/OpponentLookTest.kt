package io.github.pavelya.dots.game

import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import dots.engine.TurnRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OpponentLookTest {
    private val human = Player.RED
    private val empty = GameState.start(5, 5, dotsPerPlayer = 3)
    private val afterAMove = empty.play(Pos(0, 0)).state.play(Pos(4, 4)).state

    private fun look(state: GameState, capture: LastCapture? = null, inDanger: Boolean = false) =
        OpponentLook.of(state, human, capture, inDanger)

    @Test
    fun `the level opens with a happy face and good luck`() {
        assertEquals(OpponentLook(Mood.HAPPY, Bubble.GOOD_LUCK), look(empty))
    }

    @Test
    fun `a threat to the player's dots makes it smug, on the player's move`() {
        assertEquals(OpponentLook(Mood.SMUG, Bubble.YOUR_MOVE), look(afterAMove, inDanger = true))
    }

    @Test
    fun `a capture by the player upsets it, and a big one makes it say ouch`() {
        assertEquals(OpponentLook(Mood.UPSET, Bubble.HEY), look(afterAMove, LastCapture(human, 2)))
        assertEquals(OpponentLook(Mood.UPSET, Bubble.OUCH), look(afterAMove, LastCapture(human, OpponentLook.BIG_CAPTURE)))
    }

    @Test
    fun `its own capture makes it smug, without a word`() {
        assertEquals(OpponentLook(Mood.SMUG), look(afterAMove, LastCapture(Player.BLUE, 1)))
    }

    @Test
    fun `otherwise it plays on, determined`() {
        assertEquals(OpponentLook(Mood.DETERMINED), look(afterAMove))
    }

    @Test
    fun `a boss attacks while it places the two dots of its special turn`() {
        // Blue doubles every 2nd turn of its own. Red moves first: R, B, R, B B, R, B, R, B B.
        var state = GameState.start(7, 7, dotsPerPlayer = 9, turnRules = mapOf(Player.BLUE to TurnRule.DoubleMove(2)))
        val attacking = (0 until 10).filter { ply ->
            val mood = look(state).mood
            if (mood == Mood.ATTACKING) {
                // A capture by the player still upsets it first.
                assertEquals(Mood.UPSET, look(state, LastCapture(human, 1)).mood)
            }
            // Dots along the top rows, all touching the border: nothing is captured.
            state = state.play(Pos(ply % 7, ply / 7)).state
            mood == Mood.ATTACKING
        }
        assertEquals(listOf(3, 4, 8, 9), attacking)
    }

    @Test
    fun `a computer without a boss's rule never attacks`() {
        var state = empty
        repeat(6) { ply ->
            assertNotEquals(Mood.ATTACKING, look(state).mood)
            state = state.play(Pos(ply % 5, ply / 5)).state
        }
    }

    @Test
    fun `at the end it sulks when it lost and gloats when it won`() {
        // Red captures Blue's dot with its last dot; then the game is over.
        val start = GameState.fromAscii(
            """
            .....
            ..R..
            .RBR.
            .....
            .....
            """,
            dotsPerPlayer = 1,
        )
        val won = start.play(Pos(2, 3)).state.play(Pos(0, 0)).state
        assertEquals(true, won.isOver)
        assertEquals(OpponentLook(Mood.SULKY, Bubble.WELL_PLAYED), look(won))
        assertEquals(OpponentLook(Mood.SMUG, Bubble.AGAIN), OpponentLook.of(won, Player.BLUE, null, false))
    }
}
