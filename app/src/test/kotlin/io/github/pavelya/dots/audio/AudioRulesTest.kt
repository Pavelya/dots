package io.github.pavelya.dots.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the game may make a sound (V-10): silent mode, the background, calls, the focus, the headphones and the settings. */
class AudioRulesTest {
    /** On the screen on the map, ringer on, focus held: everything plays. */
    private val playing = AudioState(onScreen = true, focus = Focus.HELD, track = Track.MAP)

    private fun assertSilent(state: AudioState) {
        assertEquals("effects", 0f, state.effectsGain)
        assertEquals("music", 0f, state.musicGain)
    }

    @Test
    fun `the game on the screen plays its effects and music at the volumes set`() {
        assertEquals(AudioState.loudness(70), playing.effectsGain)
        assertEquals(AudioState.loudness(70), playing.musicGain)
        val quieter = playing.copy(musicVolume = 30, effectsVolume = 100)
        assertEquals(AudioState.loudness(30), quieter.musicGain)
        assertEquals(1f, quieter.effectsGain)
    }

    @Test
    fun `silent and vibrate modes silence the game`() {
        assertSilent(playing.copy(ringer = Ringer.SILENT))
        assertSilent(playing.copy(ringer = Ringer.VIBRATE))
        assertFalse(playing.copy(ringer = Ringer.SILENT).wantsMusic)
    }

    @Test
    fun `nothing plays once the game leaves the screen`() {
        val gone = playing.hidden()
        assertSilent(gone)
        assertFalse(gone.wantsMusic)
    }

    @Test
    fun `sound off in Pause silences both, whatever the volumes`() {
        assertSilent(playing.copy(sound = false, musicVolume = 100, effectsVolume = 100))
    }

    @Test
    fun `a call silences the game but keeps the music waiting for the call to end`() {
        val calling = playing.copy(inCall = true)
        assertSilent(calling)
        assertTrue(calling.wantsMusic)
        assertEquals(AudioState.loudness(70), calling.copy(inCall = false).musicGain)
    }

    @Test
    fun `another app taking the sound for a while pauses the music and the effects`() {
        assertSilent(playing.copy(focus = Focus.PAUSED))
    }

    @Test
    fun `another app speaking over the game lowers the music only`() {
        val ducked = playing.copy(focus = Focus.DUCKED)
        assertEquals(AudioState.loudness(70) * AudioState.DUCKED, ducked.musicGain)
        assertEquals(AudioState.loudness(70), ducked.effectsGain)
    }

    @Test
    fun `music lost to another app stays off until the game comes back, while the effects play`() {
        val lost = playing.copy(focus = Focus.LOST)
        assertEquals(0f, lost.musicGain)
        assertFalse(lost.wantsMusic)
        assertEquals(AudioState.loudness(70), lost.effectsGain)
        assertTrue(lost.hidden().shown(otherMusic = false).wantsMusic)
    }

    @Test
    fun `the music waits for the focus before it plays`() {
        val asking = playing.copy(focus = Focus.NONE)
        assertTrue(asking.wantsMusic)
        assertEquals(0f, asking.musicGain)
    }

    @Test
    fun `headphones coming out stop the music, never the effects, until the game comes back`() {
        val unplugged = playing.unplug()
        assertFalse(unplugged.wantsMusic)
        assertEquals(0f, unplugged.musicGain)
        assertEquals(AudioState.loudness(70), unplugged.effectsGain)
        assertTrue(unplugged.hidden().shown(otherMusic = false).wantsMusic)
        // Headphones that come out while the game is away change nothing.
        assertFalse(playing.hidden().unplug().unplugged)
    }

    @Test
    fun `the player's own music keeps playing and the game's stays off`() {
        val theirs = playing.hidden().shown(otherMusic = true)
        assertFalse(theirs.wantsMusic)
        assertEquals(AudioState.loudness(70), theirs.copy(focus = Focus.HELD).effectsGain)
        assertTrue(theirs.hidden().shown(otherMusic = false).wantsMusic)
    }

    @Test
    fun `no music is wanted at volume 0 or on a screen without music`() {
        assertFalse(playing.copy(musicVolume = 0).wantsMusic)
        assertFalse(playing.copy(track = null).wantsMusic)
        assertEquals(0f, playing.copy(effectsVolume = 0).effectsGain)
    }

    @Test
    fun `coming back to the screen asks for the focus again`() {
        assertEquals(Focus.NONE, playing.hidden().shown(otherMusic = false).focus)
    }

    @Test
    fun `loudness grows with the slider, from silent to full`() {
        assertEquals(0f, AudioState.loudness(0))
        assertEquals(0.25f, AudioState.loudness(50))
        assertEquals(1f, AudioState.loudness(100))
        assertEquals(1f, AudioState.loudness(250))
        val steps = (0..100).map(AudioState::loudness)
        assertEquals(steps.sorted(), steps)
    }
}
