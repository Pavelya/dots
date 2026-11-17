package io.github.pavelya.dots.ui.screen

import dots.campaign.Campaign
import dots.campaign.Strength
import io.github.pavelya.dots.game.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FacesTest {
    @Test
    fun `the computer's plain look shows how strong it plays, in three steps`() {
        val steps = Strength.entries.map { Faces.of(Mood.DETERMINED, it) }
        assertEquals(steps[0], steps[1]) // beginner, easy: sleepy
        assertEquals(steps[2], steps[3]) // steady, normal: calm
        assertEquals(steps[4], steps[5]) // hard, expert: sharp
        assertEquals(3, steps.distinct().size)
        assertEquals(Faces.strength(Strength.STEADY), Faces.of(Mood.DETERMINED, null))
    }

    @Test
    fun `each mood has its own face, and every face differs from the strengths`() {
        val moods = listOf(Mood.HAPPY, Mood.SMUG, Mood.UPSET, Mood.SULKY).map { Faces.of(it, Strength.STEADY) }
        assertEquals(4, moods.distinct().size)
        for (s in Strength.entries) assertEquals(false, Faces.strength(s) in moods)
        // A boss without faces of its own looks smug on its special turn.
        assertEquals(Faces.of(Mood.SMUG, null), Faces.of(Mood.ATTACKING, null))
    }

    @Test
    fun `the Ink Bottle is sure of itself, attacks on its turn and is beaten by a capture or a loss`() {
        val ink = Faces.boss(Campaign.level(1, 15))
        assertEquals(Faces.INK_BOTTLE, ink)
        assertEquals(ink!!.smug, Faces.of(Mood.HAPPY, Strength.STEADY, ink))
        assertEquals(ink.smug, Faces.of(Mood.SMUG, Strength.STEADY, ink))
        assertEquals(ink.smug, Faces.of(Mood.DETERMINED, Strength.STEADY, ink))
        assertEquals(ink.attacking, Faces.of(Mood.ATTACKING, Strength.STEADY, ink))
        assertEquals(ink.beaten, Faces.of(Mood.UPSET, Strength.STEADY, ink))
        assertEquals(ink.beaten, Faces.of(Mood.SULKY, Strength.STEADY, ink))
    }

    @Test
    fun `only World 1's boss has faces of its own so far`() {
        assertNull(Faces.boss(Campaign.level(1, 7)))
        assertNull(Faces.boss(null))
        for (world in 2..4) {
            val boss = Campaign.levels.last { it.world == world && it.rule != null }
            assertNull(boss.name, Faces.boss(boss))
        }
        assertNotEquals(null, Faces.boss(Campaign.levels.first { it.rule != null }))
    }
}
