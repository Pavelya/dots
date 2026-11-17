package io.github.pavelya.dots.audio

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** The phone's own state reaches the rules: its ringer, the game leaving the screen, a call. */
@RunWith(RobolectricTestRunner::class)
class AndroidAudioTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val manager: AudioManager get() = context.getSystemService(AudioManager::class.java)

    private fun shownWithMusic(): AndroidAudio = AndroidAudio(context).apply {
        onScreen(true)
        music(Track.MAP)
    }

    @Test
    fun `the game on the screen holds the focus and plays`() {
        val audio = shownWithMusic()
        val state = audio.stateNow()
        assertEquals(Focus.HELD, state.focus)
        assertTrue(state.effectsGain > 0f)
        assertTrue(state.musicGain > 0f)
    }

    @Test
    fun `silent mode set on the phone silences the game, and ringing brings it back`() {
        val audio = shownWithMusic()
        manager.ringerMode = AudioManager.RINGER_MODE_SILENT
        context.sendBroadcast(Intent(AudioManager.RINGER_MODE_CHANGED_ACTION))
        shadowOf(Looper.getMainLooper()).idle()
        val silent = audio.stateNow()
        assertEquals(Ringer.SILENT, silent.ringer)
        assertEquals(0f, silent.effectsGain)
        assertEquals(0f, silent.musicGain)

        manager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        context.sendBroadcast(Intent(AudioManager.RINGER_MODE_CHANGED_ACTION))
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(audio.stateNow().musicGain > 0f)
    }

    @Test
    fun `a phone already in vibrate mode starts silent`() {
        manager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        val audio = shownWithMusic()
        assertEquals(0f, audio.stateNow().effectsGain)
    }

    @Test
    fun `leaving the screen silences everything and gives the focus back`() {
        val audio = shownWithMusic()
        audio.onScreen(false)
        val away = audio.stateNow()
        assertEquals(0f, away.effectsGain)
        assertEquals(0f, away.musicGain)
        assertEquals(Focus.NONE, away.focus)
    }

    @Test
    fun `an effect asked for during a call stays silent`() {
        val audio = shownWithMusic()
        manager.mode = AudioManager.MODE_IN_CALL
        audio.play(Effect.WIN)
        val calling = audio.stateNow()
        assertTrue(calling.inCall)
        assertEquals(0f, calling.effectsGain)
        assertEquals(0f, calling.musicGain)
    }

    @Test
    fun `sound off in Pause silences the game`() {
        val audio = shownWithMusic()
        audio.settings(sound = false, musicVolume = 100, effectsVolume = 100)
        val off = audio.stateNow()
        assertEquals(0f, off.effectsGain)
        assertEquals(0f, off.musicGain)
    }
}
