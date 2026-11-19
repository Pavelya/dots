package io.github.pavelya.dots.audio

import io.github.pavelya.dots.ui.theme.GameTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/** The app plays every sound file it ships, and ships every sound it plays. Paths are from the app module. */
class SoundsTest {
    private val assets = File("src/main/assets")

    private fun shipped(prefix: String): List<String> = assets.resolve("audio").listFiles().orEmpty()
        .filter { it.isFile && it.name.startsWith(prefix) }
        .map { "audio/${it.name}" }
        .sorted()

    @Test
    fun `every effect file is played, and every effect has its file`() {
        val themes = GameTheme.ALL.flatMap { it.sounds.values }.map { "audio/$it.ogg" }
        assertEquals(shipped("sfx_"), (Effect.entries.map { it.asset } + themes).sorted())
    }

    @Test
    fun `every music file is played, and every track has its file`() {
        assertEquals(shipped("music_"), Track.entries.map { it.asset }.sorted())
    }

    @Test
    fun `each world has its own track, and a world after the fourth has the notebook's`() {
        assertEquals(listOf(Track.NOTEBOOK, Track.GARDEN, Track.NIGHT_SKY, Track.SEA, Track.NOTEBOOK), (1..5).map(Track::ofWorld))
    }

    @Test
    fun `only the result's and a new world's sounds lower the music`() {
        assertEquals(setOf(Effect.WIN, Effect.LOSE, Effect.DRAW, Effect.WORLD_OPEN), Effect.entries.filter { it.ducksMusic }.toSet())
    }
}
