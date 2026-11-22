package io.github.pavelya.dots.ui.motion

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.github.pavelya.dots.audio.Audio
import io.github.pavelya.dots.audio.Effect
import io.github.pavelya.dots.audio.LocalAudio
import io.github.pavelya.dots.audio.Sounding
import io.github.pavelya.dots.audio.Track
import io.github.pavelya.dots.haptics.Buzz
import io.github.pavelya.dots.haptics.Haptics
import io.github.pavelya.dots.haptics.LocalHaptics
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.GameTheme
import java.util.Collections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The screens' sounds follow the theme they are drawn in (docs/design/sound.md, "Board sounds of
 * the other themes"), and their vibrations start with them (sound.md, rule 3).
 */
@RunWith(RobolectricTestRunner::class)
class PlayCuesTest {
    @get:Rule
    val compose = createComposeRule()

    /** Notes each effect asked for, and the file it plays. */
    private class Recorder : Audio {
        val played: MutableList<Pair<Effect, String>> = Collections.synchronizedList(ArrayList())

        override fun play(effect: Effect, rate: Float, file: String, share: Float): Sounding {
            played += effect to file
            return Sounding.NONE
        }

        override fun music(track: Track?) = Unit
        override fun settings(sound: Boolean, musicVolume: Int, effectsVolume: Int) = Unit
        override fun onScreen(shown: Boolean) = Unit
    }

    /** Notes each sound and vibration, and when it started on the test clock. */
    private inner class Timed : Audio, Haptics {
        val events: MutableList<Pair<Any, Long>> = Collections.synchronizedList(ArrayList())

        override fun play(effect: Effect, rate: Float, file: String, share: Float): Sounding {
            events += effect to compose.mainClock.currentTime
            return Sounding.NONE
        }

        override fun buzz(buzz: Buzz) {
            events += buzz to compose.mainClock.currentTime
        }

        override fun music(track: Track?) = Unit
        override fun settings(sound: Boolean, musicVolume: Int, effectsVolume: Int) = Unit
        override fun onScreen(shown: Boolean) = Unit
        override fun settings(vibration: Boolean) = Unit
    }

    /** A capture's vibrations. */
    private val beats = listOf(Beat(Buzz.LIGHT, 0f), Beat(Buzz.MEDIUM, 550f))

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

    @Test
    fun `each vibration starts with its sound, on the same beat`() {
        val timed = Timed()
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalAudio provides timed, LocalHaptics provides timed) {
                PlayCues(key = 1, clock = null, beats = { beats }) { cues }
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitUntil(timeoutMillis = 5_000) { timed.events.size == cues.size + beats.size }
        val events = timed.events.toList()
        assertEquals(
            listOf(Effect.PLACE_1, Buzz.LIGHT, Effect.RING_DRAW, Effect.RING_CLOSE, Buzz.MEDIUM, Effect.WIN),
            events.map { it.first },
        )
        val start = events.first().second
        assertEquals(start, events[1].second)
        assertEquals(events[3].second, events[4].second)
        // The test clock may step a frame (16 ms) past the beat, never before it.
        assertTrue("closing at ${events[4].second - start} ms", events[4].second - start in 550L..566L)
    }

    @Test
    fun `a tap that finishes the moment stops the vibrations still to come`() {
        val timed = Timed()
        lateinit var clock: Timeline
        compose.mainClock.autoAdvance = false
        compose.setContent {
            clock = rememberTimeline(key = 1, length = 1_250f)
            CompositionLocalProvider(LocalAudio provides timed, LocalHaptics provides timed) {
                PlayCues(key = 1, clock = clock, beats = { beats }) { emptyList() }
            }
        }
        compose.mainClock.advanceTimeBy(300)
        compose.waitUntil(timeoutMillis = 5_000) { timed.events.isNotEmpty() }
        compose.runOnIdle { clock.finish() }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        assertEquals(listOf(Buzz.LIGHT), timed.events.map { it.first })
    }
}
