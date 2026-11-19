package io.github.pavelya.dots.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * The game's vibrations on a phone. Nothing vibrates until the player's settings have been read,
 * nor while the Vibration setting is off, nor when the phone's own touch feedback is off (Android
 * Settings, Sound and vibration). From Android 13 the vibrations are touch feedback to the phone, so
 * its touch-feedback strength and its "Use vibration and haptics" switch apply to them too.
 *
 * Phones that offer them use the maker's own tuned clicks: a tick, a click and a heavy click. The
 * strong one, where the phone can play it, is three clicks dying away, in step with the board's
 * three swings of the shake. Older phones get short pulses.
 */
class AndroidHaptics(context: Context) : Haptics {
    private val context = context.applicationContext
    private val vibrator: Vibrator? = context.getSystemService(Vibrator::class.java)

    @Volatile
    private var on = false

    override fun settings(vibration: Boolean) {
        on = vibration
    }

    @Suppress("DEPRECATION") // vibrate(Long), the only way before Android 8
    override fun buzz(buzz: Buzz) {
        val vibrator = vibrator ?: return
        if (!on || !touchFeedback() || !vibrator.hasVibrator()) return
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                    vibrator.vibrate(effect(vibrator, buzz), VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> vibrator.vibrate(effect(vibrator, buzz))
                else -> vibrator.vibrate(pulse(buzz))
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "Cannot vibrate", e)
        }
    }

    /** The phone's own touch feedback, on unless the player turned it off. */
    private fun touchFeedback(): Boolean = Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0

    companion object {
        private const val TAG = "Haptics"

        /** The strong vibration's clicks, each weaker, [SWING] ms apart: the shake's three swings (motion.md, Big capture). */
        val STRONG_CLICKS = floatArrayOf(1f, 0.6f, 0.3f)
        const val SWING = 50

        /** A pulse's length in ms on phones without tuned clicks. */
        fun pulse(buzz: Buzz): Long = when (buzz) {
            Buzz.LIGHT -> 12
            Buzz.MEDIUM -> 25
            Buzz.STRONG -> 60
        }

        /** A pulse's strength, 1 to 255, on phones that can set it. */
        fun amplitude(buzz: Buzz): Int = when (buzz) {
            Buzz.LIGHT -> 80
            Buzz.MEDIUM -> 170
            Buzz.STRONG -> 255
        }

        /** The phone's tuned click for [buzz]. */
        @RequiresApi(Build.VERSION_CODES.Q)
        fun predefined(buzz: Buzz): Int = when (buzz) {
            Buzz.LIGHT -> VibrationEffect.EFFECT_TICK
            Buzz.MEDIUM -> VibrationEffect.EFFECT_CLICK
            Buzz.STRONG -> VibrationEffect.EFFECT_HEAVY_CLICK
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun effect(vibrator: Vibrator, buzz: Buzz): VibrationEffect {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && buzz == Buzz.STRONG &&
                vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
            ) {
                val clicks = VibrationEffect.startComposition()
                STRONG_CLICKS.forEachIndexed { i, scale ->
                    clicks.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, scale, if (i == 0) 0 else SWING)
                }
                return clicks.compose()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return VibrationEffect.createPredefined(predefined(buzz))
            val strength = if (vibrator.hasAmplitudeControl()) amplitude(buzz) else VibrationEffect.DEFAULT_AMPLITUDE
            return VibrationEffect.createOneShot(pulse(buzz), strength)
        }
    }
}
