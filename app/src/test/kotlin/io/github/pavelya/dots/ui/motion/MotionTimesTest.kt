package io.github.pavelya.dots.ui.motion

import dots.engine.Booster
import dots.engine.Capture
import dots.engine.Player
import dots.engine.Pos
import io.github.pavelya.dots.game.BoardMoment
import io.github.pavelya.dots.game.ThreatMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The times of docs/design/motion.md, as the drawing reads them. Each check names the row of the
 * page it comes from; a movement must start and end within 10% of the page's times (V-05).
 */
class MotionTimesTest {
    /** [actual] ms is within 10% of the page's [expected] ms. */
    private fun near(expected: Float, actual: Float) =
        assertTrue("$actual ms is not within 10% of $expected ms", abs(actual - expected) <= expected * 0.1f)

    /** The first time from [from] at which [moved] is past 0, in 1 ms steps. */
    private fun starts(from: Float = 0f, moved: (Float) -> Float): Float {
        var t = from
        while (moved(t) <= 0f && t < 10_000f) t++
        return t
    }

    /** The first time at which [moved] has reached 1. */
    private fun ends(moved: (Float) -> Float): Float {
        var t = 0f
        while (moved(t) < 1f && t < 10_000f) t++
        return t
    }

    @Test
    fun `placing a dot grows it to 115 percent at 120 ms and settles it at 200 ms`() {
        assertEquals(0f, Placing.dotScale(0f))
        assertEquals(1.15f, Placing.dotScale(120f), 0.001f)
        assertEquals(1f, Placing.dotScale(200f))
        assertEquals(1f, Placing.dotScale(5_000f))
        near(80f, starts { Placing.links(it) })
        near(230f, ends { Placing.links(it) })
        near(200f, Placing.PINHOLE)
        near(200f, Placing.PEN_STROKE)
    }

    @Test
    fun `drag's guides are drawn out in 100 ms`() {
        assertEquals(0f, Guides.length(0f))
        near(100f, ends { Guides.length(it) })
    }

    @Test
    fun `the computer's move spreads two ripples of 700 ms, from 22 to 96 dp, and grows its edge marks in 150 ms`() {
        assertEquals(0f, Announce.ripple(0f, 0))
        assertNull(Announce.ripple(700f, 0))
        assertNull(Announce.ripple(199f, 1))
        assertEquals(0f, Announce.ripple(200f, 1))
        assertNull(Announce.ripple(900f, 1))
        assertEquals(22f to 96f, Announce.RIPPLE_FROM to Announce.RIPPLE_TO)
        assertEquals(3f to 0.5f, Announce.LINE_FROM to Announce.LINE_TO)
        near(150f, ends { Announce.marksIn(it) })
        assertEquals(1f, Announce.marksOut(0f))
        assertEquals(0f, Announce.marksOut(150f))
        near(900f, Announce.LENGTH)
    }

    @Test
    fun `a capture draws its ring from 150 to 550 ms, spreads its wash, cuts its dots, and lands +N at 1100 ms`() {
        val capture = Capturing(big = false)
        near(150f, starts { capture.ring(it) })
        near(550f, ends { capture.ring(it) })
        near(500f, starts { capture.wash(it) })
        near(800f, ends { capture.wash(it) })
        near(600f, starts { capture.cut(it, 0) })
        near(700f, ends { capture.cut(it, 0) })
        near(640f, starts { capture.cut(it, 1) })
        near(700f, starts { capture.plusGrow(it) })
        near(850f, starts { capture.plusFlight(it) })
        near(1100f, ends { capture.plusFlight(it) })
        near(800f, capture.face)
        near(1100f, capture.score)
        assertEquals(1.3f, capture.scoreScale(1_175f), 0.001f)
        assertEquals(1.06f, capture.noteScale(1_175f), 0.001f)
        assertEquals(1f, capture.scoreScale(1_250f))
        near(1250f, capture.length)
        assertEquals(0f, capture.shake(700f))
        assertNull(capture.burst(700f))
    }

    @Test
    fun `a big capture of 5 dots draws its ring until 650 ms, shakes and bursts, and ends at 1400 ms`() {
        assertEquals(false, Capturing.of(4).big)
        val big = Capturing.of(5)
        assertEquals(true, big.big)
        near(650f, ends { big.ring(it) })
        near(650f, starts { abs(big.shake(it)) })
        assertEquals(0f, big.shake(830f))
        assertTrue((650..830).all { abs(big.shake(it.toFloat())) <= 2f })
        assertEquals(0f, big.burst(650f))
        assertNull(big.burst(900f))
        near(650f, starts { big.wash(it) })
        near(950f, ends { big.wash(it) })
        near(700f, starts { big.cut(it, 0) })
        near(730f, starts { big.cut(it, 1) })
        near(850f, starts { big.plusGrow(it) })
        near(1250f, ends { big.plusFlight(it) })
        near(900f, big.face)
        assertEquals(1f, big.words(1_100f))
        assertEquals(0f, big.words(1_400f))
        near(1400f, big.length)
    }

    @Test
    fun `a threat draws in in 250 ms, pulses three times in 1600 ms each, and is still from 5050 ms`() {
        near(200f, Threat.DELAY)
        near(250f, ends { Threat.drawIn(it) })
        assertEquals(0f, Threat.pulse(250f))
        assertEquals(1f, Threat.pulse(250f + 800f), 0.001f)
        assertEquals(1f, Threat.pulse(250f + 1600f + 800f), 0.001f)
        assertEquals(1f, Threat.pulse(250f + 3200f + 800f), 0.001f)
        assertEquals(0f, Threat.pulse(5_050f))
        assertEquals(0f, Threat.pulse(9_000f))
        near(5050f, Threat.STILL)
    }

    @Test
    fun `dots running out write the row in 50 ms apart, swell the number in 300 ms and write Last dot in 300 ms`() {
        assertEquals(5, RunningOut.LOW)
        near(50f, starts { RunningOut.pieceIn(it, 1) })
        near(200f, ends { RunningOut.pieceIn(it, 1) })
        near(150f, ends { 1f - RunningOut.pieceOut(it) })
        assertEquals(1.3f, RunningOut.numberScale(150f), 0.001f)
        assertEquals(1f, RunningOut.numberScale(300f))
        assertEquals(0f, RunningOut.nudge(400f))
        assertEquals(3f, RunningOut.nudge(100f), 0.001f)
        near(300f, ends { RunningOut.lastDot(it) })
    }

    @Test
    fun `the end of Level 7, with three areas, brings the note at 1100 ms`() {
        val level7 = EndOfGame(areas = 3)
        near(250f, ends { level7.toolsOut(it) })
        near(300f, level7.areaStart(0))
        near(450f, level7.areaStart(1))
        near(1100f, level7.noteStart)
        assertEquals(1f, level7.glow(450f, 0), 0.001f)
        // With many areas the gap shrinks, so that all of them have lit up by 1500 ms.
        val many = EndOfGame(areas = 12)
        assertTrue(many.lastArea <= 1_500f)
        assertEquals(500f, EndOfGame(areas = 0).noteStart)
    }

    @Test
    fun `the result note slides up in 300 ms, then writes its headline and stamps a star every 300 ms`() {
        near(300f, ends { ResultReveal.slide(it) })
        assertEquals(-1f, ResultReveal.tip(150f))
        assertEquals(0f, ResultReveal.tip(300f))
        // The star reveal's 0 ms is the note settled.
        val settled = ResultReveal.SETTLED
        near(300f, ends { ResultReveal.headline(it + settled) })
        near(300f, starts { ResultReveal.star(it + settled, 0, earned = true) })
        near(500f, ends { ResultReveal.star(it + settled, 0, earned = true) })
        near(800f, ends { ResultReveal.star(it + settled, 1, earned = true) })
        near(1100f, ends { ResultReveal.star(it + settled, 2, earned = false) })
        near(1100f, ResultReveal.LENGTH - settled)
    }

    @Test
    fun `the level unlock fills the stop at 500 ms, inks the path, circles the next level, and brings its card at 1800 ms`() {
        val unlock = Unlocking(worldOpens = false)
        near(400f, ends { unlock.star(it, 0) })
        near(500f, ends { unlock.star(it, 1) })
        near(500f, starts { unlock.fill(it) })
        near(800f, ends { unlock.fill(it) })
        near(650f, ends { 1f - unlock.rings(it) })
        near(800f, starts { unlock.path(it) })
        near(1200f, ends { unlock.path(it) })
        near(1200f, starts { unlock.circle(it) })
        assertEquals(2f, unlock.circle(1_600f))
        near(1300f, starts { unlock.next(it) })
        near(1800f, unlock.card)
        assertEquals(0f, unlock.page(1_000f))
    }

    @Test
    fun `when the stars open a world, its page turns in 600 ms and the rest moves 600 ms later`() {
        val world = Unlocking(worldOpens = true)
        assertEquals(0f, world.page(799f))
        assertEquals(1f, world.page(800f))
        assertEquals(0f, world.page(1_400f))
        near(1400f, starts { world.path(it) })
        near(2400f, world.card)
    }

    @Test
    fun `a moment plays out with its dot, ripples or capture, and lasts as long as a new threat's pulses`() {
        val dot = Pos(3, 3)
        assertEquals(Placing.LENGTH, BoardMoment(1, dot, Player.RED, announced = false).playOut)
        assertEquals(Announce.LENGTH, BoardMoment(2, dot, Player.BLUE, announced = true).playOut)
        val capture = Capture(Player.RED, listOf(Pos(3, 4)), listOf(Pos(3, 4)), dot)
        assertEquals(1250f, BoardMoment(3, dot, Player.RED, announced = false, captures = listOf(capture)).playOut)
        val threat = BoardMoment(4, dot, Player.BLUE, announced = true, freshThreats = setOf(ThreatMark(setOf(Pos(1, 1)), 1)))
        assertEquals(Announce.LENGTH, threat.playOut)
        assertEquals(Threat.LENGTH, threat.length)
    }

    @Test
    fun `booster effects stay under 1500 ms, Wall 400, Hint 500, Bomb 450, Double dot 300 then 250 per dot, Undo 600`() {
        near(400f, BoosterTimes.length(Booster.WALL))
        near(500f, BoosterTimes.length(Booster.HINT))
        near(450f, BoosterTimes.length(Booster.BOMB))
        near(300f, BoosterTimes.length(Booster.DOUBLE_DOT))
        near(250f, DoubleDotEffect.SPARK)
        near(600f, BoosterTimes.length(Booster.UNDO))
        assertTrue(Booster.entries.all { BoosterTimes.length(it) < 1_500f })
        assertTrue(DoubleDotEffect.CHIP_IN + 2 * DoubleDotEffect.SPARK < 1_500f)
        // The used booster's count swells to 135% and back in 200 ms.
        assertEquals(1.35f, BoosterTimes.countScale(100f), 0.001f)
        assertEquals(1f, BoosterTimes.countScale(200f))
        assertEquals(listOf(Booster.BOMB, Booster.UNDO), Booster.entries.filter(BoosterTimes::tapFinishes))
    }

    @Test
    fun `a wall's square is drawn by 200 ms and hatched by 400 ms`() {
        assertEquals(0f, WallEffect.square(0f))
        near(200f, ends { WallEffect.square(it) })
        near(200f, starts { WallEffect.hatch(it) })
        near(400f, ends { WallEffect.hatch(it) })
    }

    @Test
    fun `a hint circles its point twice, 250 ms per loop, and the circles shrink away in 150 ms`() {
        assertEquals(0f, HintEffect.loops(0f))
        assertEquals(1f, HintEffect.loops(250f), 0.001f)
        assertEquals(2f, HintEffect.loops(500f))
        assertEquals(1f, HintEffect.shrink(0f))
        near(150f, ends { 1f - HintEffect.shrink(it) })
    }

    @Test
    fun `a bomb drops in 150 ms, then its hits swell to 130 percent and are gone at 350 ms, the board shakes and the specks fly until 450 ms`() {
        assertEquals(0.6f, BombEffect.drop(0f))
        assertTrue(BombEffect.drop(149f) > 0.99f)
        assertEquals(0f, BombEffect.drop(150f))
        assertEquals(1f, BombEffect.hit(150f))
        assertEquals(1.3f, BombEffect.hit(220f), 0.001f)
        assertEquals(0f, BombEffect.hit(350f))
        assertEquals(0f, BombEffect.shake(150f))
        assertTrue((150..300).all { abs(BombEffect.shake(it.toFloat())) <= 2f })
        assertEquals(0f, BombEffect.shake(300f))
        assertNull(BombEffect.specks(149f))
        assertEquals(0f, BombEffect.specks(150f))
        assertNull(BombEffect.specks(450f))
    }

    @Test
    fun `a double dot's chip grows in in 300 ms, swells to 125 percent at the first dot and leaves in 150 ms after the second`() {
        near(300f, ends { DoubleDotEffect.chipIn(it) })
        assertEquals(1.25f, DoubleDotEffect.chipSwell(100f), 0.001f)
        assertEquals(1f, DoubleDotEffect.chipSwell(200f))
        near(150f, ends { 1f - DoubleDotEffect.chipOut(it) })
        assertEquals(0f, DoubleDotEffect.spark(0f))
        assertNull(DoubleDotEffect.spark(250f))
    }

    @Test
    fun `Undo rubs out the answer from 0 ms and the player's dot from 200 ms, 250 ms each, and is done at 600 ms`() {
        assertEquals(1f, UndoEffect.rubOut(250f, UndoEffect.ANSWER))
        near(200f, starts { UndoEffect.rubOut(it, UndoEffect.OWN) })
        near(450f, ends { UndoEffect.rubOut(it, UndoEffect.OWN) })
        assertEquals(0f, UndoEffect.strokes(100f, UndoEffect.OWN))
        assertTrue(UndoEffect.strokes(300f, UndoEffect.OWN) > 0f)
        assertEquals(0f, UndoEffect.strokes(600f, UndoEffect.ANSWER))
        near(200f, starts { UndoEffect.flight(it) })
        near(400f, ends { UndoEffect.flight(it) })
        assertEquals(0f, UndoEffect.growBack(200f))
        near(450f, ends { UndoEffect.growBack(it) })
        assertEquals(1.35f, UndoEffect.countScale(500f), 0.001f)
        assertEquals(1f, UndoEffect.countScale(600f))
        near(450f, UndoEffect.SETTLED)
        near(600f, UndoEffect.LENGTH)
    }

    @Test
    fun `a dot placed after a Double dot plays out its sparks, and one after a Hint its circles`() {
        val dot = Pos(3, 3)
        assertEquals(DoubleDotEffect.SPARK, BoardMoment(1, dot, Player.RED, announced = false, doubleDots = 2).playOut)
        assertEquals(Placing.LENGTH, BoardMoment(2, dot, Player.RED, announced = false, hintBefore = dot).playOut)
        assertNotNull(DoubleDotEffect.spark(Placing.LENGTH))
    }

    @Test
    fun `Star chart traces its line at an even speed from 150 to 550 ms, twinkles each star in 120 ms, and blends its haze in`() {
        val capture = Capturing(big = false)
        near(150f, starts { Twinkle.traced(capture, it) })
        near(550f, ends { Twinkle.traced(capture, it) })
        // An even speed: halfway round at halfway through.
        assertEquals(0.5f, Twinkle.traced(capture, 350f), 0.001f)
        // A star reached a quarter of the way round swells from 250 ms to 125% at 310 ms and is back at 370 ms.
        assertEquals(1f, Twinkle.size(capture, 250f, 0.25f))
        assertEquals(1.25f, Twinkle.size(capture, 310f, 0.25f), 0.001f)
        assertEquals(1f, Twinkle.size(capture, 370f, 0.25f))
        // The closing star twinkles last, as the line comes back to it at 550 ms (650 ms in a big capture).
        assertEquals(1f, Twinkle.size(capture, 550f, 1f))
        assertTrue(Twinkle.size(capture, 600f, 1f) > 1.2f)
        assertTrue(Twinkle.size(Capturing(big = true), 710f, 1f) > 1.2f)
        near(120f, Twinkle.LENGTH)
        near(500f, starts { Twinkle.haze(capture, it) })
        near(800f, ends { Twinkle.haze(capture, it) })
        near(650f, starts { Twinkle.haze(Capturing(big = true), it) })
        near(950f, ends { Twinkle.haze(Capturing(big = true), it) })
    }

    @Test
    fun `Neon arcade lights a piece from 120 to 200 ms, and a tube as it closes, with one 40 ms flicker and 250 ms of sparks`() {
        assertEquals(0f, NeonLight.glow(120f))
        near(200f, ends { NeonLight.glow(it) })
        assertEquals(Placing.SETTLED, NeonLight.GLOW_TO)
        for ((big, close) in listOf(false to 550f, true to 650f)) {
            val capture = Capturing(big)
            assertFalse(NeonLight.lit(capture, close - 1))
            assertTrue(NeonLight.lit(capture, close))
            assertTrue(NeonLight.lit(capture, close + 39))
            assertFalse(NeonLight.lit(capture, close + 40))
            assertFalse(NeonLight.lit(capture, close + 79))
            assertTrue(NeonLight.lit(capture, close + 80))
            assertTrue(NeonLight.lit(capture, capture.length))
            assertNull(NeonLight.sparks(capture, close - 1))
            assertEquals(0f, NeonLight.sparks(capture, close))
            assertNull(NeonLight.sparks(capture, close + 250))
        }
        assertEquals(10, NeonLight.SPARK_COUNT)
    }
}
