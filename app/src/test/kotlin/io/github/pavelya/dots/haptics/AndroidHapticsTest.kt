package io.github.pavelya.dots.haptics

import android.annotation.SuppressLint
import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowVibrator

/** Off means off: the Vibration setting and the phone's own touch feedback. Robolectric runs Android 16 here (robolectric.properties). */
@RunWith(RobolectricTestRunner::class)
@SuppressLint("NewApi") // Robolectric runs one Android version, newer than every call here
class AndroidHapticsTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val vibrator: Vibrator get() = context.getSystemService(Vibrator::class.java)
    private val shadow: ShadowVibrator get() = shadowOf(vibrator)

    private fun buzzEveryKind(haptics: Haptics) = Buzz.entries.forEach(haptics::buzz)

    /** True if the phone was asked to vibrate at all. */
    private fun vibrated(): Boolean = shadow.pattern != null || shadow.vibrationAttributesFromLastVibration != null

    @Test
    fun `nothing vibrates before the player's settings are read`() {
        buzzEveryKind(AndroidHaptics(context))
        assertEquals(false, vibrated())
    }

    @Test
    fun `nothing vibrates with the Vibration setting off`() {
        val haptics = AndroidHaptics(context)
        haptics.settings(vibration = true)
        haptics.settings(vibration = false)
        buzzEveryKind(haptics)
        assertEquals(false, vibrated())
    }

    @Test
    fun `nothing vibrates with the phone's touch feedback off`() {
        Settings.System.putInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0)
        val haptics = AndroidHaptics(context).apply { settings(vibration = true) }
        buzzEveryKind(haptics)
        assertEquals(false, vibrated())
    }

    @Test
    fun `nothing vibrates on a phone without a vibrator`() {
        shadow.setHasVibrator(false)
        val haptics = AndroidHaptics(context).apply { settings(vibration = true) }
        buzzEveryKind(haptics)
        assertEquals(false, vibrated())
    }

    @Test
    fun `with vibration on the phone vibrates as touch feedback`() {
        val haptics = AndroidHaptics(context).apply { settings(vibration = true) }
        haptics.buzz(Buzz.LIGHT)
        val attributes = shadow.vibrationAttributesFromLastVibration as VibrationAttributes?
        assertNotNull(attributes)
        assertEquals(VibrationAttributes.USAGE_TOUCH, attributes!!.usage)
    }

    @Test
    fun `light, medium and strong are the phone's tick, click and heavy click`() {
        assertEquals(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK), AndroidHaptics.effect(vibrator, Buzz.LIGHT))
        assertEquals(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK), AndroidHaptics.effect(vibrator, Buzz.MEDIUM))
        assertEquals(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK), AndroidHaptics.effect(vibrator, Buzz.STRONG))
    }

    @Test
    fun `where the phone can, strong is three clicks dying away, the shake's three swings`() {
        shadow.setSupportedPrimitives(listOf(VibrationEffect.Composition.PRIMITIVE_CLICK))
        AndroidHaptics(context).apply { settings(vibration = true) }.buzz(Buzz.STRONG)
        val clicks = shadow.primitiveSegmentsInPrimitiveEffects
        assertEquals(listOf(1f, 0.6f, 0.3f), clicks.map { it.scale })
        assertEquals(listOf(0, AndroidHaptics.SWING, AndroidHaptics.SWING), clicks.map { it.delay })
        assertEquals(List(3) { VibrationEffect.Composition.PRIMITIVE_CLICK }, clicks.map { it.id })
        // Light and medium stay the phone's own tuned clicks.
        assertEquals(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK), AndroidHaptics.effect(vibrator, Buzz.MEDIUM))
    }

    @Test
    fun `the setting can be turned back on`() {
        val haptics = AndroidHaptics(context)
        haptics.settings(vibration = false)
        haptics.settings(vibration = true)
        haptics.buzz(Buzz.MEDIUM)
        assertEquals(true, vibrated())
    }
}
