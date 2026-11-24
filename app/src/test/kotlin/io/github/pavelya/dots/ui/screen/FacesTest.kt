package io.github.pavelya.dots.ui.screen

import dots.campaign.Campaign
import dots.campaign.Strength
import io.github.pavelya.dots.game.Mood
import io.github.pavelya.dots.ui.theme.FaceInks
import io.github.pavelya.dots.ui.theme.GameTheme
import org.junit.Assert.assertEquals
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
        val moods = listOf(Mood.HAPPY, Mood.SMUG, Mood.WORRIED, Mood.UPSET, Mood.SURPRISED, Mood.SULKY).map { Faces.of(it, Strength.STEADY) }
        assertEquals(6, moods.distinct().size)
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
        assertEquals(ink.smug, Faces.of(Mood.WORRIED, Strength.STEADY, ink))
        assertEquals(ink.attacking, Faces.of(Mood.ATTACKING, Strength.STEADY, ink))
        assertEquals(ink.beaten, Faces.of(Mood.UPSET, Strength.STEADY, ink))
        assertEquals(ink.beaten, Faces.of(Mood.SURPRISED, Strength.STEADY, ink))
        assertEquals(ink.beaten, Faces.of(Mood.SULKY, Strength.STEADY, ink))
    }

    @Test
    fun `each world's boss has faces of its own, and a level without a boss has none`() {
        assertNull(Faces.boss(Campaign.level(1, 7)))
        assertNull(Faces.boss(Campaign.level(2, 19)))
        assertNull(Faces.boss(null))
        val bosses = Campaign.levels.filter { it.rule != null }
        assertEquals(listOf("1-15", "2-20", "3-20", "4-20"), bosses.map { it.code })
        assertEquals(
            listOf(Faces.INK_BOTTLE, Faces.MOLE, Faces.COMET, Faces.KRAKEN),
            bosses.map { Faces.boss(it) },
        )
        for (boss in listOf(Faces.MOLE, Faces.COMET, Faces.KRAKEN)) {
            assertEquals(boss.smug, Faces.of(Mood.DETERMINED, Strength.STEADY, boss))
            assertEquals(boss.attacking, Faces.of(Mood.ATTACKING, Strength.STEADY, boss))
            assertEquals(boss.beaten, Faces.of(Mood.SULKY, Strength.STEADY, boss))
        }
    }

    @Test
    fun `every face has its own picture in each theme's inks, light and dark`() {
        val faces = listOf(Mood.HAPPY, Mood.SMUG, Mood.WORRIED, Mood.UPSET, Mood.SURPRISED, Mood.SULKY).map { Faces.of(it, null) } +
            Strength.entries.map { Faces.strength(it) }.distinct() +
            listOf(Faces.INK_BOTTLE, Faces.MOLE, Faces.COMET, Faces.KRAKEN).flatMap { listOf(it.smug, it.attacking, it.beaten) }
        assertEquals(21, faces.size)
        val drawables = faces.flatMap { face -> FaceInks.entries.flatMap { face.of(it).let { p -> listOf(p.light, p.dark) } } }
        assertEquals(21 * 3 * 2, drawables.distinct().size)
    }

    @Test
    fun `Notebook draws Notebook's faces`() {
        assertEquals(FaceInks.NOTEBOOK, GameTheme.NOTEBOOK.faceInks)
        assertEquals(FaceInks.NOTEBOOK, GameTheme.PLAIN.faceInks)
    }
}
