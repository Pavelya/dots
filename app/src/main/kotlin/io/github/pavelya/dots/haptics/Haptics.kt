package io.github.pavelya.dots.haptics

import androidx.compose.runtime.staticCompositionLocalOf

/** The three vibrations of docs/design/motion.md, rule 6. */
enum class Buzz {
    /** A dot lands; an earned star is stamped. */
    LIGHT,

    /** A capture's ring closes. */
    MEDIUM,

    /** A big capture's ring closes; a bomb goes off. Both shake the board. */
    STRONG,
}

/**
 * The game's vibrations. Screens ask for a [Buzz] on the beat of their animation; whether the
 * phone vibrates follows the player's Vibration setting and the phone's own touch feedback.
 */
interface Haptics {
    /** Vibrates as [buzz] says, unless vibration is off. */
    fun buzz(buzz: Buzz)

    /** The player's Vibration setting. */
    fun settings(vibration: Boolean)

    companion object {
        /** Never vibrates: for screens drawn in tests and previews. */
        val NONE: Haptics = object : Haptics {
            override fun buzz(buzz: Buzz) = Unit
            override fun settings(vibration: Boolean) = Unit
        }
    }
}

/** The game's vibrations, for the screens; still unless the app provides its own. */
val LocalHaptics = staticCompositionLocalOf { Haptics.NONE }
