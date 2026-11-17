package io.github.pavelya.dots.ui

import dots.campaign.Campaign
import dots.campaign.Progress
import dots.campaign.Tutorial
import io.github.pavelya.dots.audio.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which track plays where (docs/design/sound.md, "Music"). */
class MusicTest {
    @Test
    fun `the map and everything over it play the map's track, the loading screen none`() {
        for (route in Routes.ALL - setOf(Routes.LOADING, Routes.GAME, Routes.RULES)) {
            assertEquals(route, Track.MAP, musicFor(route, game = Track.SEA, playing = null))
        }
        assertNull(musicFor(Routes.LOADING, game = null, playing = null))
        assertNull(musicFor(null, game = null, playing = Track.MAP))
    }

    @Test
    fun `the board plays the game's track, and Rules keeps what played before it`() {
        assertEquals(Track.BOSS, musicFor(Routes.GAME, game = Track.BOSS, playing = Track.MAP))
        assertEquals(Track.BOSS, musicFor(Routes.RULES, game = null, playing = Track.BOSS))
        assertEquals(Track.MAP, musicFor(Routes.RULES, game = null, playing = Track.MAP))
    }

    @Test
    fun `a level plays its world's track, a boss level the boss's`() {
        assertEquals(Track.NOTEBOOK, gameMusic(Tutorial.levels.first(), vsComputer = true, map = null))
        assertEquals(Track.GARDEN, gameMusic(Campaign.levels.first { it.world == 2 && it.rule == null }, vsComputer = true, map = null))
        assertEquals(Track.SEA, gameMusic(Campaign.levels.first { it.world == 4 && it.rule == null }, vsComputer = true, map = null))
        val bosses = Campaign.levels.filter { it.rule != null }
        assertEquals(bosses.map { Track.BOSS }, bosses.map { gameMusic(it, vsComputer = true, map = null) })
    }

    @Test
    fun `free play and the daily puzzle play the track of the world reached, two players the notebook's`() {
        assertEquals(Track.NOTEBOOK, gameMusic(null, vsComputer = true, map = Campaign.map(Progress())))
        val world2 = Progress(Campaign.levels.filter { it.world == 1 }.associate { it.code to 3 })
        assertEquals(Track.GARDEN, gameMusic(null, vsComputer = true, map = Campaign.map(world2)))
        assertEquals(Track.NOTEBOOK, gameMusic(null, vsComputer = false, map = Campaign.map(world2)))
    }
}
