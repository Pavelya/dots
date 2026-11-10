package io.github.pavelya.dots.game

import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import org.junit.Assert.assertEquals
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
