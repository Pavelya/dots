package io.github.pavelya.dots.ui.motion

import dots.engine.Booster
import dots.engine.Capture
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import io.github.pavelya.dots.audio.Effect
import io.github.pavelya.dots.game.BoardMoment
import io.github.pavelya.dots.game.BoosterMoment
import io.github.pavelya.dots.haptics.Buzz
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which vibration starts when (docs/design/motion.md, rule 6), with the sounds (sound.md, rule 3). */
class BeatsTest {
    private val board = GameState.start(width = 6, height = 6, dotsPerPlayer = 20)

    private fun moment(by: Player, captured: Int = 0) = BoardMoment(
        id = 1,
        dot = Pos(3, 3),
        by = by,
        announced = by == Player.BLUE,
        captures = if (captured > 0) listOf(Capture(by, List(captured) { Pos(it, 0) }, List(captured) { Pos(it, 0) }, closing = Pos(0, 1))) else emptyList(),
    )

    @Test
    fun `a dot lands with a light vibration, the computer's too`() {
        for (by in Player.entries) assertEquals(listOf(Beat(Buzz.LIGHT, 0f)), Beats.moment(moment(by)))
    }

    @Test
    fun `a capture closes its ring with a medium vibration at 550 ms`() {
        assertEquals(listOf(Beat(Buzz.LIGHT, 0f), Beat(Buzz.MEDIUM, 550f)), Beats.moment(moment(Player.RED, captured = 2)))
        assertEquals(Beat(Buzz.MEDIUM, 550f), Beats.moment(moment(Player.BLUE, captured = Capturing.BIG - 1)).last())
    }

    @Test
    fun `a big capture closes with the strong vibration at 650 ms, with the shake`() {
        val beats = Beats.moment(moment(Player.RED, captured = Capturing.BIG))
        assertEquals(listOf(Beat(Buzz.LIGHT, 0f), Beat(Buzz.STRONG, 650f)), beats)
        assertEquals(Capturing.SHAKE_FROM, beats.last().at)
    }

    @Test
    fun `the bomb's blast is strong at 150 ms, and no other booster vibrates`() {
        for (booster in Booster.entries) {
            val beats = Beats.booster(BoosterMoment(1, booster, Player.RED))
            if (booster == Booster.BOMB) assertEquals(listOf(Beat(Buzz.STRONG, 150f)), beats) else assertTrue("$booster", beats.isEmpty())
        }
    }

    @Test
    fun `each earned star lands with a light vibration`() {
        val beats = Beats.result(stars = 3, still = false)
        assertEquals(List(3) { Buzz.LIGHT }, beats.map { it.buzz })
        assertEquals(List(3) { ResultReveal.starStart(it) + ResultReveal.STAR }, beats.map { it.at })
        assertEquals(2, Beats.result(stars = 2, still = false).size)
        assertTrue(Beats.result(stars = 0, still = false).isEmpty())
    }

    @Test
    fun `with animations removed the stars are not stamped, and nothing is felt`() {
        assertTrue(Beats.result(stars = 3, still = true).isEmpty())
    }

    @Test
    fun `every vibration starts with a sound`() {
        val captures = listOf(0, 2, Capturing.BIG).map { moment(Player.RED, captured = it) }
        for (m in captures) {
            val heard = Cues.moment(m, board, human = Player.RED, dotsLowHeard = true).map { it.at }
            for (beat in Beats.moment(m)) assertTrue("$beat in $heard", beat.at in heard)
        }
        val stars = Cues.result(Effect.WIN, stars = 3, streak = false, still = false).filter { it.effect == Effect.STAR }.map { it.at }
        assertEquals(stars, Beats.result(stars = 3, still = false).map { it.at })
    }
}
