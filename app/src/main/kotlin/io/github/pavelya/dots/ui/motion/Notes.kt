package io.github.pavelya.dots.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Where a note or card over the map or the board stands (motion.md, "Level opening, notes and
 * cards"). While [open] it comes up from below in [NoteSlide.IN] ms; once closed it goes down in
 * [NoteSlide.OUT] ms, and then it is [gone]. A note closed while still coming up goes down from
 * where it got to, and one opened again while leaving comes up from there. With animations
 * removed it is in place, or gone, at once.
 */
@Stable
class NoteMotion internal constructor(open: Boolean, still: Boolean) {
    /** Milliseconds since the note was last opened or closed. */
    private val clock = Animatable(if (still || !open) end(open) else 0f)

    /** Whether the note is meant to be up. */
    var open by mutableStateOf(open)
        private set

    /** How far up the note was when it was last opened or closed. */
    private var from by mutableFloatStateOf(if (open) 0f else 1f)

    /** How far up the note is, from 0 (below the screen, the background clear) to 1 (in place, the background dimmed). */
    val up: Float
        get() = if (open) from + (1f - from) * NoteSlide.coming(clock.value) else from * NoteSlide.leaving(clock.value)

    /** True once a closed note has gone down; it is then no longer drawn. */
    val gone: Boolean by derivedStateOf { !this.open && clock.value >= NoteSlide.OUT }

    internal suspend fun go(open: Boolean, still: Boolean) {
        if (open != this.open) {
            from = up
            this.open = open
            clock.snapTo(0f)
        }
        val end = end(open)
        if (still) {
            clock.snapTo(end)
        } else if (clock.value < end) {
            clock.animateTo(end, tween((end - clock.value).toInt(), easing = LinearEasing))
        }
    }

    private companion object {
        fun end(open: Boolean): Float = if (open) NoteSlide.IN else NoteSlide.OUT
    }
}

/**
 * The motion of a note that is up while [open]. A note opened with the screen (a note in its own
 * window, Pause after Rules) comes up as it first shows.
 */
@Composable
fun rememberNoteMotion(open: Boolean = true): NoteMotion {
    val still = LocalReduceMotion.current
    val motion = remember { NoteMotion(open, still) }
    LaunchedEffect(open, still) { motion.go(open, still) }
    return motion
}

/**
 * Slides this note below the bottom of the screen as [up] goes from 1 to 0. Put it on the part
 * that sits on the screen's bottom edge, before its padding, so that the whole note, its shadow
 * and its slant go out of sight.
 */
fun Modifier.slideUp(up: () -> Float): Modifier = graphicsLayer {
    val p = up()
    translationY = if (p >= 1f) 0f else (1f - p) * size.height
}

/** The background behind a note, dimmed by [scrim] as far as the note is [up]. */
fun Modifier.dim(scrim: Color, up: () -> Float): Modifier = drawBehind { drawRect(scrim, alpha = up()) }
