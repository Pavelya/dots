package io.github.pavelya.dots.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * The game's sound on a phone. Effects play from a [SoundPool], which starts them with little
 * delay; the music from [MediaPlayer]s, in a loop, two at a time while one fades over to the next.
 * Everything runs on one thread of its own, so the screen never waits for a file.
 *
 * It follows [AudioState]: the phone's ringer (silent and vibrate silence the game), calls (the
 * phone's audio mode), Android's audio focus, the headphones coming out, the game leaving the
 * screen, and the player's settings.
 */
class AndroidAudio(context: Context) : Audio {
    private val context = context.applicationContext
    private val manager = context.getSystemService(AudioManager::class.java)
    private val thread = HandlerThread("audio").apply {
        isDaemon = true
        start()
    }
    private val handler = Handler(thread.looper)

    // Everything below is used only on the audio thread.
    private var state = AudioState()
    private val pool = SoundPool.Builder().setMaxStreams(MAX_STREAMS).setAudioAttributes(EFFECTS).build()
    /** Every effect file in the assets, Notebook's and the themes', by its name ([Effect.file]). */
    private val loaded = HashMap<String, Int>()
    private val deck = MusicDeck(handler, ::open)
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change -> handler.post { focusChanged(change) } }
    private val focusRequest: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) buildFocusRequest() else null

    init {
        handler.post {
            for (file in effectFiles()) {
                try {
                    this.context.assets.openFd("$FOLDER/$file.ogg").use { loaded[file] = pool.load(it, 1) }
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot load $FOLDER/$file.ogg", e)
                }
            }
            update(state.copy(ringer = ringer(), inCall = inCall()))
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    AudioManager.RINGER_MODE_CHANGED_ACTION -> update(state.copy(ringer = ringer()))
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> update(state.unplug())
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        // Both are the system's own broadcasts; they arrive on the audio thread.
        ContextCompat.registerReceiver(this.context, receiver, filter, null, handler, ContextCompat.RECEIVER_NOT_EXPORTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Without it, calls are still noticed when an effect plays and when the focus changes.
            try {
                listenForCalls()
            } catch (e: Exception) {
                Log.w(TAG, "Cannot follow the audio mode", e)
            }
        }
    }

    override fun play(effect: Effect, rate: Float, file: String): Sounding {
        val sound = PlayingEffect()
        handler.post { start(sound, effect, rate, file) }
        return sound
    }

    override fun music(track: Track?) {
        handler.post { update(state.copy(track = track)) }
    }

    override fun settings(sound: Boolean, musicVolume: Int, effectsVolume: Int) {
        handler.post { update(state.copy(sound = sound, musicVolume = musicVolume, effectsVolume = effectsVolume)) }
    }

    override fun onScreen(shown: Boolean) {
        handler.post {
            // Music already playing when the game comes back is another app's: the game's own is paused by now.
            if (!shown) pool.autoPause() // effects still sounding stop with the game
            update(if (shown) state.shown(otherMusic = manager.isMusicActive && !deck.playing) else state.hidden())
        }
    }

    /** What the rules see now, read on the audio thread. */
    @VisibleForTesting
    internal fun stateNow(): AudioState {
        val read = FutureTask { state }
        handler.post(read)
        return read.get(5, TimeUnit.SECONDS)
    }

    /** The effect files to load: every `sfx_` file in the assets' audio folder, and Notebook's in any case. */
    private fun effectFiles(): Set<String> {
        val shipped = try {
            context.assets.list(FOLDER).orEmpty().filter { it.startsWith("sfx_") && it.endsWith(".ogg") }.map { it.removeSuffix(".ogg") }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot list $FOLDER", e)
            emptyList()
        }
        return Effect.entries.map { it.file }.toSet() + shipped
    }

    private fun start(sound: PlayingEffect, effect: Effect, rate: Float, file: String) {
        val calling = inCall()
        if (calling != state.inCall) update(state.copy(inCall = calling))
        val gain = state.effectsGain * effect.share
        val id = loaded[file] ?: loaded[effect.file] ?: return
        if (gain <= 0f) return
        sound.stream = pool.play(id, gain, gain, 1, 0, rate.coerceIn(MIN_RATE, MAX_RATE))
        sound.volume = gain
        if (effect.ducksMusic) deck.duck(effect.millis)
    }

    /** Takes in a change and makes the sound follow it: the audio focus, then the music. */
    private fun update(next: AudioState) {
        state = next
        if (state.wantsMusic && state.focus == Focus.NONE) {
            state = state.copy(focus = askFocus())
        } else if (!state.wantsMusic && state.focus != Focus.NONE && state.focus != Focus.LOST) {
            dropFocus()
            state = state.copy(focus = Focus.NONE)
        }
        deck.set(state.track, state.musicGain)
    }

    private fun focusChanged(change: Int) {
        val focus = when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> Focus.HELD
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> Focus.PAUSED
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> Focus.DUCKED
            AudioManager.AUDIOFOCUS_LOSS -> Focus.LOST
            else -> return
        }
        if (focus == Focus.LOST) dropFocus()
        update(state.copy(focus = focus, inCall = inCall()))
    }

    /** Asks for the audio focus; a refusal is asked again at the next change, a wait ends with a call back. */
    private fun askFocus(): Focus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        when (manager.requestAudioFocus(focusRequest as AudioFocusRequest)) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> Focus.HELD
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> Focus.PAUSED
            else -> Focus.NONE
        }
    } else {
        @Suppress("DEPRECATION")
        val result = manager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) Focus.HELD else Focus.NONE
    }

    private fun dropFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.abandonAudioFocusRequest(focusRequest as AudioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(focusListener)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildFocusRequest(): AudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(MUSIC)
        .setAcceptsDelayedFocusGain(true)
        .setOnAudioFocusChangeListener(focusListener, handler)
        .build()

    @RequiresApi(Build.VERSION_CODES.S)
    private fun listenForCalls() {
        manager.addOnModeChangedListener({ handler.post(it) }) { update(state.copy(inCall = inCall())) }
    }

    private fun ringer(): Ringer = when (manager.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> Ringer.SILENT
        AudioManager.RINGER_MODE_VIBRATE -> Ringer.VIBRATE
        else -> Ringer.NORMAL
    }

    /** A call rings, is under way, or a voice or video call app has the sound. */
    private fun inCall(): Boolean = manager.mode != AudioManager.MODE_NORMAL

    /** A music loop, ready to start; null if it cannot be read. */
    private fun open(track: Track): MediaPlayer? {
        val player = MediaPlayer()
        return try {
            context.assets.openFd(track.asset).use { player.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            player.setAudioAttributes(MUSIC)
            player.isLooping = true
            player.setVolume(0f, 0f)
            player.prepare()
            player
        } catch (e: Exception) {
            Log.w(TAG, "Cannot play ${track.asset}", e)
            player.release()
            null
        }
    }

    /** A started effect: its stream, once it plays, and the volume it plays at. */
    private inner class PlayingEffect : Sounding {
        var stream = 0
        var volume = 0f

        override fun fadeOut() {
            handler.post {
                val stream = stream.takeIf { it != 0 } ?: return@post
                for (step in 1..FADE_STEPS) {
                    handler.postDelayed({
                        val left = volume * (FADE_STEPS - step) / FADE_STEPS
                        if (step == FADE_STEPS) pool.stop(stream) else pool.setVolume(stream, left, left)
                    }, step * FADE_OUT_MILLIS / FADE_STEPS)
                }
            }
        }
    }

    companion object {
        private const val TAG = "Audio"
        private const val MAX_STREAMS = 8

        /** The effects' and the music's folder in the assets. */
        private const val FOLDER = "audio"

        /** What [SoundPool] can play: half speed (an octave down) to double (an octave up). */
        const val MIN_RATE = 0.5f
        const val MAX_RATE = 2f

        /** A tap that finishes an animation fades its sounds out in this long (sound.md, rule 8). */
        const val FADE_OUT_MILLIS = 50L
        private const val FADE_STEPS = 5

        private val EFFECTS: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        private val MUSIC: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }
}

/**
 * The music players, on the audio thread. The track of the screen fades in and the one before
 * fades out; the one before is kept, paused, so going back to it carries on where it was. The
 * whole deck follows the volume the rules allow: it fades out and pauses when that is 0, and drops
 * to half under the result's sounds ([duck]).
 */
private class MusicDeck(private val handler: Handler, private val open: (Track) -> MediaPlayer?) {
    private class Voice(val track: Track, val player: MediaPlayer) {
        /** How far it has faded in, from 0 to 1, and where it is going. */
        var level = 0f
        var goal = 1f
    }

    /** The newest last: the track of the screen, and before it the one fading out. */
    private val voices = ArrayList<Voice>(2)
    private var gain = 0f
    private var shown = 0f
    private var duckUntil = 0L
    private var last = 0L
    private var moving = false
    private val ticker = Runnable { tick() }

    /** True while a loop is playing. */
    val playing: Boolean get() = voices.any { safe(it) { player.isPlaying } == true }

    /** Plays [track] (null: none) at [gain], fading as needed. */
    fun set(track: Track?, gain: Float) {
        this.gain = gain
        val current = voices.lastOrNull()?.takeIf { it.goal > 0f }
        if (track != current?.track) {
            voices.forEach { it.goal = 0f }
            if (track != null) {
                val voice = voices.firstOrNull { it.track == track }?.also { voices.remove(it) } ?: open(track)?.let { Voice(track, it) }
                if (voice != null) {
                    voice.goal = 1f
                    voices.add(voice)
                }
            }
            while (voices.size > 2) release(voices.removeAt(0))
        }
        tick()
    }

    /** Drops the music to half for [millis], then brings it back. */
    fun duck(millis: Long) {
        duckUntil = max(duckUntil, SystemClock.uptimeMillis() + millis)
        tick()
    }

    private fun tick() {
        handler.removeCallbacks(ticker)
        val now = SystemClock.uptimeMillis()
        val step = if (moving) (now - last).toFloat() else STEP_MILLIS.toFloat()
        last = now
        val target = gain * if (now < duckUntil) AudioState.UNDER_RESULT else 1f
        shown = towards(shown, target, step / MASTER_FADE_MILLIS)
        for (voice in voices.toList()) {
            // Nothing is heard: the crossfade is over at once.
            voice.level = if (shown == 0f) voice.goal else towards(voice.level, voice.goal, step / if (voice.goal > voice.level) FADE_IN_MILLIS else FADE_OUT_MILLIS)
            val volume = shown * voice.level
            val ok = safe(voice) {
                if (volume > 0f) {
                    player.setVolume(volume, volume)
                    if (!player.isPlaying) player.start()
                } else if (player.isPlaying) {
                    player.pause()
                }
            }
            if (ok == null) {
                voices.remove(voice)
                release(voice)
            }
        }
        moving = shown != target || voices.any { it.level != it.goal } || now < duckUntil
        if (moving) handler.postDelayed(ticker, STEP_MILLIS)
    }

    private fun towards(from: Float, to: Float, by: Float): Float = if (from < to) min(to, from + by) else max(to, from - by)

    private fun release(voice: Voice) {
        runCatching { voice.player.release() }
    }

    /** Runs [action] on the voice's player; null if the player has failed. */
    private fun <T> safe(voice: Voice, action: Voice.() -> T): T? = try {
        voice.action()
    } catch (e: IllegalStateException) {
        Log.w("Audio", "Music player failed: ${voice.track.asset}", e)
        null
    }

    companion object {
        /** A new screen's music fades in in this long, the one before fades out in [FADE_OUT_MILLIS]. */
        const val FADE_IN_MILLIS = 1_000f
        const val FADE_OUT_MILLIS = 800f

        /** All the music fades in or out in this long: leaving the screen, coming back, a change of volume, ducking. */
        const val MASTER_FADE_MILLIS = 300f

        const val STEP_MILLIS = 40L
    }
}
