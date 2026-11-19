package io.github.pavelya.dots.ui.motion

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.github.pavelya.dots.audio.Audio
import io.github.pavelya.dots.audio.Effect
import io.github.pavelya.dots.audio.LocalAudio
import io.github.pavelya.dots.audio.Sounding
import io.github.pavelya.dots.audio.Track
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.GameTheme
import java.util.Collections
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The screens' sounds follow the theme they are drawn in (docs/design/sound.md, "Board sounds of the other themes"). */
@RunWith(RobolectricTestRunner::class)
class PlayCuesTest {
    @get:Rule
    val compose = createComposeRule()

    /** Notes each effect asked for, and the file it plays. */
    private class Recorder : Audio {
        val played: MutableList<Pair<Effect, String>> = Collections.synchronizedList(ArrayList())

        override fun play(effect: Effect, rate: Float, file: String): Sounding {
            played += effect to file
            return Sounding.NONE
        }

        override fun music(track: Track?) = Unit
        override fun settings(sound: Boolean, musicVolume: Int, effectsVolume: Int) = Unit
        override fun onScreen(shown: Boolean) = Unit
    }

    /** A capture, then the result's headline. */
    private val cues = listOf(
        Cue(Effect.PLACE_1, 0f),
        Cue(Effect.RING_DRAW, 150f),
        Cue(Effect.RING_CLOSE, 550f),
        Cue(Effect.WIN, 700f),
    )

    private fun heard(theme: GameTheme): List<Pair<Effect, String>> {
        val audio = Recorder()
        compose.setContent {
            DotsTheme(theme, dark = false) {
                CompositionLocalProvider(LocalAudio provides audio) {
                    PlayCues(key = 1, clock = null) { cues }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitUntil(timeoutMillis = 5_000) { audio.played.size == cues.size }
        return audio.played.toList()
    }

    @Test
    fun `Notebook plays the game's own files`() {
        assertEquals(cues.map { it.effect to it.effect.file }, heard(GameTheme.NOTEBOOK))
    }

    @Test
    fun `Star chart plays its own board sounds and Notebook's result`() {
        assertEquals(
            listOf(
                Effect.PLACE_1 to "sfx_star_chart_place_1",
                Effect.RING_DRAW to "sfx_star_chart_ring_draw",
                Effect.RING_CLOSE to "sfx_star_chart_ring_close",
                Effect.WIN to "sfx_win",
            ),
            heard(GameTheme.STAR_CHART),
        )
    }

    @Test
    fun `Neon arcade plays its own board sounds and Notebook's result`() {
        assertEquals(
            listOf(
                Effect.PLACE_1 to "sfx_neon_arcade_place_1",
                Effect.RING_DRAW to "sfx_neon_arcade_ring_draw",
                Effect.RING_CLOSE to "sfx_neon_arcade_ring_close",
                Effect.WIN to "sfx_win",
            ),
            heard(GameTheme.NEON_ARCADE),
        )
    }
}
