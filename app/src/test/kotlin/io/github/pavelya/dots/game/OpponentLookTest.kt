package io.github.pavelya.dots.game

import dots.ai.Hints
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

    private fun look(state: GameState, capture: LastCapture? = null, inDanger: Boolean = false, computerInDanger: Boolean = false) =
        OpponentLook.of(state, human, capture, inDanger, computerInDanger = computerInDanger)

    @Test
    fun `the level opens with a happy face and good luck, even with its own dots in danger`() {
        assertEquals(OpponentLook(Mood.HAPPY, Bubble.GOOD_LUCK), look(empty))
        assertEquals(OpponentLook(Mood.HAPPY, Bubble.GOOD_LUCK), look(empty, computerInDanger = true))
    }

    @Test
    fun `a threat to the player's dots makes it smug, on the player's move`() {
        assertEquals(OpponentLook(Mood.SMUG, Bubble.YOUR_MOVE), look(afterAMove, inDanger = true))
    }

    @Test
    fun `a capture by the player surprises it, and a big one upsets it and makes it say ouch`() {
        assertEquals(OpponentLook(Mood.SURPRISED, Bubble.HEY), look(afterAMove, LastCapture(human, 1)))
        assertEquals(OpponentLook(Mood.SURPRISED, Bubble.HEY), look(afterAMove, LastCapture(human, OpponentLook.BIG_CAPTURE - 1)))
        assertEquals(OpponentLook(Mood.UPSET, Bubble.OUCH), look(afterAMove, LastCapture(human, OpponentLook.BIG_CAPTURE)))
        // Even with its own dots still in danger.
        assertEquals(Mood.SURPRISED, look(afterAMove, LastCapture(human, 1), computerInDanger = true).mood)
    }

    @Test
    fun `its own dots in danger worry it, without a word, unless the player's are in danger too on the player's move`() {
        assertEquals(OpponentLook(Mood.WORRIED), look(afterAMove, computerInDanger = true))
        assertEquals(OpponentLook(Mood.SMUG, Bubble.YOUR_MOVE), look(afterAMove, inDanger = true, computerInDanger = true))
        // While it thinks, its own danger shows.
        val itsMove = afterAMove.play(Pos(2, 2)).state
        assertEquals(Player.BLUE, itsMove.toMove)
        assertEquals(OpponentLook(Mood.WORRIED), look(itsMove, inDanger = true, computerInDanger = true))
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
                // A capture by the player still surprises it first; its own danger does not stop the attack.
                assertEquals(Mood.SURPRISED, look(state, LastCapture(human, 1)).mood)
                assertEquals(Mood.ATTACKING, look(state, computerInDanger = true).mood)
            }
            // Dots along the top rows, all touching the border: nothing is captured.
            state = state.play(Pos(ply % 7, ply / 7)).state
            mood == Mood.ATTACKING
        }
        assertEquals(listOf(3, 4, 8, 9), attacking)
    }

    @Test
    fun `the Kraken attacks while it places two dots that can capture, until its second dot`() {
        // Blue, the Kraken, places one dot in each half every turn. Red's (1, 2) needs only (1, 3) to be captured.
        val start = GameState.start(
            6, 6, dotsPerPlayer = 9,
            dots = mapOf(Pos(1, 2) to Player.RED, Pos(1, 1) to Player.BLUE, Pos(0, 2) to Player.BLUE, Pos(2, 2) to Player.BLUE),
            turnRules = mapOf(Player.BLUE to TurnRule.TwoHalves),
        )
        fun inDanger(state: GameState) = Hints.dangers(state, human, maxDots = 1).isNotEmpty()

        // On the player's move the threat makes it smug, as for any computer.
        assertEquals(OpponentLook(Mood.SMUG, Bubble.YOUR_MOVE), look(start, inDanger = inDanger(start)))

        // The player leaves it: the Kraken's turn starts with a capture to make.
        val itsTurn = start.play(Pos(5, 5)).state
        assertEquals(Player.BLUE, itsTurn.toMove)
        assertEquals(true, inDanger(itsTurn))
        assertEquals(OpponentLook(Mood.ATTACKING), look(itsTurn, inDanger = true))
        // Its own danger does not stop the attack; a capture by the player still surprises it first.
        assertEquals(Mood.ATTACKING, look(itsTurn, inDanger = true, computerInDanger = true).mood)
        assertEquals(Mood.SURPRISED, look(itsTurn, LastCapture(human, 1), inDanger = true).mood)

        // Its first dot captures: the danger is gone, and it attacks on until its second dot.
        val first = itsTurn.play(Pos(1, 3))
        assertEquals(1, first.captures.sumOf { it.points })
        assertEquals(Player.BLUE, first.state.toMove)
        assertEquals(false, inDanger(first.state))
        assertEquals(OpponentLook(Mood.ATTACKING), look(first.state, LastCapture(Player.BLUE, 1)))

        // After its second dot the player is to move, and its capture makes it smug.
        val second = first.state.play(Pos(4, 0)).state
        assertEquals(human, second.toMove)
        assertEquals(OpponentLook(Mood.SMUG), look(second, LastCapture(Player.BLUE, 1)))
    }

    @Test
    fun `the Kraken does not attack on a turn with nothing to capture`() {
        var state = GameState.start(6, 6, dotsPerPlayer = 9, turnRules = mapOf(Player.BLUE to TurnRule.TwoHalves))
        // Red, then Blue's two dots, one in each half, along the top row: nothing is in danger.
        listOf(Pos(0, 0), Pos(1, 0), Pos(4, 0), Pos(5, 0), Pos(2, 0), Pos(3, 0)).forEach { dot ->
            assertEquals(false, Hints.dangers(state, human, maxDots = 1).isNotEmpty())
            assertNotEquals(Mood.ATTACKING, look(state).mood)
            state = state.play(dot).state
        }
        assertEquals(6, state.dotsPlaced(human) + state.dotsPlaced(Player.BLUE))
    }

    @Test
    fun `a boss that counts down attacks only on its turn of two, even with the player's dots in danger`() {
        val state = GameState.start(7, 7, dotsPerPlayer = 9, turnRules = mapOf(Player.BLUE to TurnRule.DoubleMove(3)))
            .play(Pos(0, 0)).state
        assertEquals(Player.BLUE, state.toMove)
        assertNotEquals(Mood.ATTACKING, look(state, inDanger = true).mood)
        assertNotEquals(Mood.ATTACKING, look(state, LastCapture(Player.BLUE, 1)).mood)
    }

    @Test
    fun `a computer without a boss's rule never attacks`() {
        var state = empty
        repeat(6) { ply ->
            assertNotEquals(Mood.ATTACKING, look(state).mood)
            assertNotEquals(Mood.ATTACKING, look(state, inDanger = true).mood)
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

    @Test
    fun `once it has stopped it is smug and says so while the player collects, unless the player captures`() {
        val stopped = afterAMove.play(Pos(2, 2)).state.stop() // Blue stops; Red collects
        assertEquals(OpponentLook(Mood.SMUG, Bubble.STOPPED), look(stopped))
        assertEquals(OpponentLook(Mood.SURPRISED, Bubble.HEY), look(stopped, LastCapture(human, 1)))
        assertEquals(OpponentLook(Mood.SMUG, Bubble.STOPPED), look(stopped, computerInDanger = true))
        // The player's own stop changes nothing in its look.
        assertEquals(OpponentLook(Mood.DETERMINED), look(afterAMove.stop()))
    }
}
