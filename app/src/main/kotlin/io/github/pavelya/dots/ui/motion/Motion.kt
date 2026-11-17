package io.github.pavelya.dots.ui.motion

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * True when the phone's **Remove animations** setting is on (Android Settings, Accessibility):
 * every moment then shows its end state at once (motion.md, "Rules for all motion", rule 5).
 */
val LocalReduceMotion = compositionLocalOf { false }

/** Whether the phone's animations are removed: Android sets their duration scale to 0. */
fun animationsRemoved(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** The easings of motion.md ("Easing"). Times are exact, so every moment uses timed animations, never springs. */
object Ease {
    /** Starts fast, settles gently: things arriving. */
    val OUT: Easing = LinearOutSlowInEasing

    /** Starts gently, leaves fast: things leaving. */
    val IN: Easing = FastOutLinearInEasing

    /** Gentle at both ends: things moving across. */
    val IN_OUT: Easing = FastOutSlowInEasing

    /** The same speed throughout: handwriting, counting. */
    val LINEAR: Easing = LinearEasing
}

/** How far a movement from [start] to [end] ms has got at [t] ms, from 0 to 1, after [easing]. */
fun phase(t: Float, start: Float, end: Float, easing: Easing = Ease.LINEAR): Float = when {
    t <= start -> 0f
    t >= end -> 1f
    else -> easing.transform((t - start) / (end - start))
}

/**
 * A swell from [start] to [end] ms: 0, up to 1 halfway, back to 0, gentle at both ends. A score
 * that swells to 130% and back is `1 + 0.3 * swell(...)`.
 */
fun swell(t: Float, start: Float, end: Float): Float {
    if (t <= start || t >= end) return 0f
    val p = (t - start) / (end - start)
    return Ease.IN_OUT.transform(if (p <= 0.5f) 2 * p else 2 * (1 - p))
}

/**
 * The clock of one moment: [now] runs from 0 to [length] ms. A [finish] jumps to the end, as a tap
 * does during a capture (motion.md, rule 1).
 */
@Stable
class Timeline internal constructor(
    private val clock: Animatable<Float, AnimationVector1D>,
    val length: Float,
    private val scope: CoroutineScope,
) {
    /** The time since the moment started, in ms; [length] once it is over. Read it while drawing, so only drawing follows it. */
    val now: Float get() = clock.value

    /** True while the moment plays. */
    val running: Boolean get() = clock.value < length

    private val finished = mutableIntStateOf(0)

    /** How often a [finish] cut the moment short: its sounds fade out then ([PlayCues]). */
    val finishes: Int get() = finished.intValue

    /** Jumps to the end state. */
    fun finish() {
        if (running) {
            finished.intValue++
            scope.launch { clock.snapTo(length) }
        }
    }

    companion object {
        /** A clock that is always at its end, for screens drawn without motion. */
        fun ended(length: Float = 0f): Timeline = Timeline(Animatable(length), length, EndedScope)

        private val EndedScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
    }
}

/**
 * A clock that plays [length] ms each time [key] changes, from the first frame after the change.
 * With no [key], or with animations removed, it stays at its end. A clock that should not play
 * when the screen first shows (the state was already there) passes [play] false.
 */
@Composable
fun rememberTimeline(key: Any?, length: Float, play: Boolean = true): Timeline {
    val reduce = LocalReduceMotion.current
    val still = key == null || reduce || !play
    val clock = remember(key) { Animatable(if (still) length else 0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(key, reduce) {
        if (still || reduce) clock.snapTo(length) else clock.animateTo(length, tween(length.toInt(), easing = LinearEasing))
    }
    return remember(clock, length) { Timeline(clock, length, scope) }
}

/**
 * A clock that plays [length] ms each time [value] changes in a way that [plays] (old to new
 * value). It does not play for the value the screen first shows.
 */
@Composable
fun <T> rememberChangeTimeline(value: T, length: Float, plays: (old: T, new: T) -> Boolean = { _, _ -> true }): Timeline {
    val seen = remember { Seen(value) }
    if (seen.value != value) {
        if (plays(seen.value, value)) seen.changes++
        seen.value = value
    }
    return rememberTimeline(seen.changes.takeIf { it > 0 }, length)
}

/** The last value [rememberChangeTimeline] saw, and how many of its changes played. */
private class Seen<T>(var value: T) {
    var changes = 0
}

/**
 * Handwriting that writes in, left to right, as [written] goes from 0 to 1. Slanted letters reach
 * past their box, so while it writes the edge runs a little beyond the box, and once written
 * nothing is cut.
 */
fun Modifier.writeIn(written: () -> Float): Modifier = drawWithContent {
    val p = written()
    if (p >= 1f) {
        drawContent()
    } else if (p > 0f) {
        val reach = size.width + 12.dp.toPx()
        clipRect(left = -12.dp.toPx(), right = reach * p) { this@drawWithContent.drawContent() }
    }
}
