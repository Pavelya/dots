package io.github.pavelya.dots.audio

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The game's sound: the effects and the music (docs/design/sound.md). Screens play effects; the
 * app says which music its screen has, what the player set, and whether it is on the screen.
 * Whether anything is heard follows [AudioState]'s rules.
 */
interface Audio {
    /**
     * Plays [effect] at [rate] (1 is as recorded; 0.5 to 2), unless the game is silent now. [file]
     * is the sound heard: the effect's own, or a theme's in its place (`GameTheme.sounds`), at
     * [share] of the effects' volume, the effect's own unless a moment plays it louder or softer.
     * A file that cannot be played gives way to the effect's own.
     */
    fun play(effect: Effect, rate: Float = 1f, file: String = effect.file, share: Float = effect.share): Sounding

    /** The music of the screen shown: it fades over from the one before. Null fades the music out. */
    fun music(track: Track?)

    /** The player's settings: the sound icon in Pause and the two volumes in Settings, 0 to 100. */
    fun settings(sound: Boolean, musicVolume: Int, effectsVolume: Int)

    /** The game came to the screen ([shown]) or left it. */
    fun onScreen(shown: Boolean)

    companion object {
        /** Plays nothing: for screens drawn in tests and previews. */
        val SILENT: Audio = object : Audio {
            override fun play(effect: Effect, rate: Float, file: String, share: Float): Sounding = Sounding.NONE
            override fun music(track: Track?) = Unit
            override fun settings(sound: Boolean, musicVolume: Int, effectsVolume: Int) = Unit
            override fun onScreen(shown: Boolean) = Unit
        }
    }
}

/** An effect that has started. */
fun interface Sounding {
    /** Fades it out in 50 ms: a tap finished its animation (sound.md, rule 8). */
    fun fadeOut()

    companion object {
        val NONE = Sounding {}
    }
}

/** The game's sound, for the screens; silent unless the app provides its own. */
val LocalAudio = staticCompositionLocalOf { Audio.SILENT }
