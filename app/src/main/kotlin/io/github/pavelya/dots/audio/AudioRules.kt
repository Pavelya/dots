package io.github.pavelya.dots.audio

import io.github.pavelya.dots.data.AppSettings

/** The phone's ring mode, set by its silent switch or volume keys. */
enum class Ringer { NORMAL, VIBRATE, SILENT }

/**
 * Where the game stands with Android's audio focus: which app the phone's sound belongs to. The
 * game asks for it only while it wants its music.
 */
enum class Focus {
    /** Not held: no music is wanted, or the game has not asked yet. */
    NONE,

    /** The game holds it: the music plays. */
    HELD,

    /** Another app speaks over the game for a moment (a navigation voice): the music plays lower. */
    DUCKED,

    /**
     * Another app has the sound for a while, such as a phone call or an alarm, or the game asked
     * and must wait: the music pauses and the effects are silent until the focus comes back.
     */
    PAUSED,

    /** Another app took the sound for good, such as the player's own music: the game's music stays off. */
    LOST,
}

/**
 * Everything that decides what the game may play, and the rules that decide it (V-10, "sound
 * behaves well on every phone"). Sound comes only from the game on the screen, with sound on in
 * Pause, the phone's ringer on, and no call. Music also needs the audio focus, a music volume above
 * 0, the headphones still in if they were in, and no other app's music already playing. A call
 * pauses the music but keeps the focus asked for, so the music comes back when the call ends.
 *
 * - [sound]: the sound icon in Pause; [musicVolume] and [effectsVolume]: Settings, 0 to 100.
 * - [ringer]: the phone's ring mode. Silent and vibrate both silence the game.
 * - [inCall]: a call is ringing or under way (Android's audio mode is not normal).
 * - [onScreen]: the game is on the screen. Leaving it (Home, another app, the screen turning off)
 *   silences everything.
 * - [focus]: Android's audio focus, see [Focus].
 * - [unplugged]: the headphones came out while the game was on the screen. The music stays off,
 *   so it never carries on out loud, until the game next comes to the screen.
 * - [otherMusic]: another app's music was playing when the game came to the screen. The game
 *   leaves it playing and keeps its own music off until it next comes to the screen.
 * - [track]: the music of the screen shown; null for none (the loading screen).
 */
data class AudioState(
    val sound: Boolean = true,
    val musicVolume: Int = AppSettings.DEFAULT_VOLUME,
    val effectsVolume: Int = AppSettings.DEFAULT_VOLUME,
    val ringer: Ringer = Ringer.NORMAL,
    val inCall: Boolean = false,
    val onScreen: Boolean = false,
    val focus: Focus = Focus.NONE,
    val unplugged: Boolean = false,
    val otherMusic: Boolean = false,
    val track: Track? = null,
) {
    /** True when the player lets the game make sounds: sound on, the ringer on, the game on the screen. */
    val audible: Boolean get() = sound && ringer == Ringer.NORMAL && onScreen

    /** The effects' volume, from 0 (silent) to 1, before each effect's own share. */
    val effectsGain: Float get() = if (!audible || inCall || focus == Focus.PAUSED) 0f else loudness(effectsVolume)

    /** True when the game wants to play music: it then asks for, or holds, the audio focus. */
    val wantsMusic: Boolean
        get() = audible && track != null && musicVolume > 0 && !unplugged && !otherMusic && focus != Focus.LOST

    /** The music's volume, from 0 (silent: paused) to 1. */
    val musicGain: Float
        get() = when {
            !wantsMusic || inCall -> 0f
            focus == Focus.HELD -> loudness(musicVolume)
            focus == Focus.DUCKED -> loudness(musicVolume) * DUCKED
            else -> 0f
        }

    /**
     * The game comes to the screen, when [otherMusic] is playing or not. A pulled plug and a lost
     * focus are forgotten: the player is back, so the game asks again.
     */
    fun shown(otherMusic: Boolean): AudioState = copy(onScreen = true, unplugged = false, otherMusic = otherMusic, focus = Focus.NONE)

    /** The game leaves the screen. */
    fun hidden(): AudioState = copy(onScreen = false)

    /** The headphones came out. Only music that is playing stops; music that was not playing stays off anyway. */
    fun unplug(): AudioState = if (onScreen) copy(unplugged = true) else this

    companion object {
        /** The music's share of its volume while another app speaks over it. */
        const val DUCKED = 0.3f

        /** The music's share of its volume under the result's and a new world's sounds (sound.md, "Music"). */
        const val UNDER_RESULT = 0.5f

        /**
         * A volume of 0 to 100, as the slider shows it, as a share of full loudness. The ear hears
         * loudness roughly by its square, so 50 on the slider sounds about half as loud as 100.
         */
        fun loudness(volume: Int): Float {
            val share = volume.coerceIn(0, AppSettings.MAX_VOLUME) / AppSettings.MAX_VOLUME.toFloat()
            return share * share
        }
    }
}
