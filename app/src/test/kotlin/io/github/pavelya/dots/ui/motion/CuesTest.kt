package io.github.pavelya.dots.ui.motion

import dots.engine.Booster
import dots.engine.Capture
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import dots.engine.TurnRule
import io.github.pavelya.dots.audio.Effect
import io.github.pavelya.dots.game.BoardMoment
import io.github.pavelya.dots.game.BoosterMoment
import io.github.pavelya.dots.game.ThreatMark
import io.github.pavelya.dots.haptics.Buzz
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which sound starts when (docs/design/sound.md, "Check against the motion storyboard"). */
class CuesTest {
    private val board = GameState.start(width = 6, height = 6, dotsPerPlayer = 20)

    private fun capture(by: Player, dots: Int) =
        Capture(by, List(dots) { Pos(it, 0) }, List(dots) { Pos(it, 0) }, closing = Pos(0, 1))

    private fun moment(by: Player, id: Int = 1, captured: Int = 0, threats: Set<ThreatMark> = emptySet()) = BoardMoment(
        id = id,
        dot = Pos(3, 3),
        by = by,
        announced = by == Player.BLUE,
        captures = if (captured > 0) listOf(capture(by, captured)) else emptyList(),
        freshThreats = threats,
    )

    private fun List<Cue>.effects() = map { it.effect }

    @Test
    fun `a dot lands with one of three takes in turn, Blue's lower, each at 95 to 105 percent`() {
        val takes = (1..6).map { Cues.place(it, Player.RED).effect }
        assertEquals(Effect.PLACE + Effect.PLACE, takes.drop(2) + takes.take(2))
        for (id in 0 until 30) {
            val red = Cues.place(id, Player.RED)
            assertEquals(0f, red.at)
            assertTrue("speed ${red.rate}", red.rate in 0.95f..1.05f)
            assertEquals(red.rate * Cues.BLUE, Cues.place(id, Player.BLUE).rate, 0.0001f)
        }
        assertTrue((0 until 7).map { Cues.place(it, Player.RED).rate }.toSet().size > 3)
    }

    @Test
    fun `the player's capture draws its ring and closes it, on the motion's beats`() {
        val cues = Cues.moment(moment(Player.RED, captured = 2), board, human = Player.RED, dotsLowHeard = false)
        assertEquals(listOf(Effect.PLACE[1], Effect.RING_DRAW, Effect.RING_CLOSE), cues.effects())
        assertEquals(listOf(0f, 150f, 550f), cues.map { it.at })
    }

    @Test
    fun `the computer's capture of the player's dots says oh no instead`() {
        val cues = Cues.moment(moment(Player.BLUE, captured = 1), board, human = Player.RED, dotsLowHeard = false)
        assertEquals(Effect.CAPTURED, cues.last().effect)
        assertEquals(550f, cues.last().at)
    }

    @Test
    fun `between two people every capture closes the same way`() {
        val cues = Cues.moment(moment(Player.BLUE, captured = 1), board, human = null, dotsLowHeard = false)
        assertEquals(Effect.RING_CLOSE, cues.last().effect)
    }

    @Test
    fun `a big capture draws a slower ring and bursts at 650 ms`() {
        val cues = Cues.moment(moment(Player.RED, captured = Capturing.BIG), board, human = Player.RED, dotsLowHeard = false)
        assertEquals(Cue(Effect.RING_DRAW, 150f, Cues.BIG_RING), cues[1])
        assertEquals(Cue(Effect.CAPTURE_BIG, 650f), cues[2])
    }

    @Test
    fun `only a threat to the player's own dots knocks, as its rings draw in`() {
        val state = board.play(Pos(1, 1)).state.play(Pos(4, 4)).state // Red at (1, 1), Blue at (4, 4)
        val toRed = setOf(ThreatMark(setOf(Pos(1, 1)), 1))
        val toBlue = setOf(ThreatMark(setOf(Pos(4, 4)), 1))
        val againstRed = Cues.moment(moment(Player.BLUE, threats = toRed), state, human = Player.RED, dotsLowHeard = false)
        assertEquals(Cue(Effect.THREAT, Threat.DELAY), againstRed.last())
        val againstBlue = Cues.moment(moment(Player.RED, threats = toBlue), state, human = Player.RED, dotsLowHeard = false)
        assertTrue(Effect.THREAT !in againstBlue.effects())
        // Between two people: the side to move next is warned about its own dots.
        val twoPlayers = Cues.moment(moment(Player.RED, threats = toBlue), state, human = null, dotsLowHeard = false)
        assertTrue(Effect.THREAT in twoPlayers.effects())
    }

    @Test
    fun `the player's dots running low ticks once, at 5 left`() {
        var state = GameState.start(width = 6, height = 6, dotsPerPlayer = 6)
        state = state.play(Pos(0, 0)).state.play(Pos(5, 5)).state // Red has 5 left
        val low = Cues.moment(moment(Player.RED), state, human = Player.RED, dotsLowHeard = false)
        assertTrue(Effect.DOTS_LOW in low.effects())
        assertTrue(Effect.DOTS_LOW !in Cues.moment(moment(Player.RED), state, human = Player.RED, dotsLowHeard = true).effects())
        assertTrue(Effect.DOTS_LOW !in Cues.moment(moment(Player.BLUE), state, human = Player.RED, dotsLowHeard = false).effects())
    }

    @Test
    fun `the boss's special turn sounds after the player's dot that brings it`() {
        // Two dots on every third turn of the boss's.
        val rules = mapOf(Player.BLUE to TurnRule.DoubleMove(3))
        val start = GameState.start(width = 6, height = 6, dotsPerPlayer = 10, turnRules = rules)
        val single = start.play(Pos(0, 0)).state.play(Pos(5, 5)).state.play(Pos(0, 1)).state // the boss's second turn comes
        assertTrue(Effect.BOSS_TURN !in Cues.moment(moment(Player.RED), single, human = Player.RED, dotsLowHeard = false).effects())
        val double = single.play(Pos(5, 4)).state.play(Pos(0, 2)).state // its third
        assertEquals(Cue(Effect.BOSS_TURN, Placing.SETTLED), Cues.moment(moment(Player.RED), double, human = Player.RED, dotsLowHeard = false).last())
    }

    @Test
    fun `each booster has its sound from Confirm`() {
        fun cues(booster: Booster) = Cues.booster(BoosterMoment(1, booster, Player.RED))
        assertEquals(listOf(Cue(Effect.RING_DRAW, 0f)), cues(Booster.WALL))
        assertEquals(listOf(Cue(Effect.RING_DRAW, 0f, 1.6f), Cue(Effect.RING_DRAW, 250f, 1.6f)), cues(Booster.HINT))
        assertEquals(listOf(Cue(Effect.BOMB, 0f)), cues(Booster.BOMB))
        assertEquals(listOf(Cue(Effect.DOUBLE_DOT, 0f)), cues(Booster.DOUBLE_DOT))
        assertEquals(listOf(Cue(Effect.RING_DRAW, 0f, 0.75f)), cues(Booster.UNDO))
    }

    @Test
    fun `the areas light up counting up D, E, G, A, then an octave higher, never past double speed`() {
        val end = EndOfGame(7)
        val cues = Cues.ending(end)
        assertEquals((0 until 7).map(end::areaStart), cues.map { it.at })
        val semitones = cues.map { Math.round(12 * Math.log(it.rate.toDouble()) / Math.log(2.0)).toInt() }
        assertEquals(listOf(0, 2, 5, 7, 12, 12, 12), semitones)
        assertTrue(Cues.ending(EndOfGame(0)).isEmpty())
    }

    @Test
    fun `the result's sound comes as the headline writes in, then a stamp for each earned star`() {
        val cues = Cues.result(Effect.WIN, stars = 2, streak = false, still = false)
        assertEquals(Cue(Effect.WIN, ResultReveal.SETTLED), cues[0])
        assertEquals(listOf(Effect.STAR, Effect.STAR), cues.drop(1).effects())
        assertEquals(listOf(1f, 1.12f), cues.drop(1).map { it.rate })
        assertEquals(ResultReveal.starStart(1) + ResultReveal.STAR, cues[2].at)
        assertEquals(listOf(Effect.LOSE), Cues.result(Effect.LOSE, stars = 0, streak = false, still = false).effects())
        assertEquals(listOf(Effect.WIN), Cues.result(Effect.WIN, stars = 3, streak = false, still = true).effects())
        assertEquals(listOf(Effect.WIN, Effect.REWARD), Cues.result(Effect.WIN, stars = 0, streak = true, still = false).effects())
    }

    @Test
    fun `a world opening sounds 300 ms into its page turn, a level unlock is silent`() {
        assertEquals(listOf(Cue(Effect.WORLD_OPEN, Unlocking.FILL_TO + 300f)), Cues.unlock(worldOpens = true, still = false))
        assertEquals(listOf(Cue(Effect.WORLD_OPEN, 0f)), Cues.unlock(worldOpens = true, still = true))
        assertTrue(Cues.unlock(worldOpens = false, still = false).isEmpty())
    }

    @Test
    fun `a stop, by either side, plays the dot's first take at 70 percent speed and full volume, and is felt as a dot`() {
        assertEquals(listOf(Cue(Effect.PLACE_1, 0f, 0.7f, share = 1f)), Cues.stop())
        assertEquals(listOf(Beat(Buzz.LIGHT, 0f)), Beats.stop())
    }
}
