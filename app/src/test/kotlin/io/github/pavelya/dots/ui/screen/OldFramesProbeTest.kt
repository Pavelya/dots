package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.github.pavelya.dots.ui.motion.Timeline
import io.github.pavelya.dots.ui.motion.rememberTimeline
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Temporary (Q-08): what the old show() and frames() stepping showed. Deleted before the PR. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class OldFramesProbeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun oldStepping() {
        var clock: Timeline? = null
        var setAt = -1L
        var firstFrame = -1L
        compose.mainClock.autoAdvance = false
        val t0 = compose.mainClock.currentTime
        compose.setContent {
            LaunchedEffect(Unit) { withFrameNanos { firstFrame = it / 1_000_000 } }
            Box { clock = rememberTimeline(1, 10_000f) }
        }
        setAt = compose.mainClock.currentTime
        val log = StringBuilder("Q08 t0=$t0 afterSetContent=$setAt composed=${clock != null} now=${clock?.now} firstFrame=$firstFrame\n")
        compose.mainClock.advanceTimeByFrame()
        log.append("Q08 frame1 time=${compose.mainClock.currentTime} now=${clock?.now} firstFrame=$firstFrame\n")
        compose.mainClock.advanceTimeByFrame()
        log.append("Q08 frame2 time=${compose.mainClock.currentTime} now=${clock?.now} firstFrame=$firstFrame\n")
        var now = 0L
        for (time in longArrayOf(40, 80, 120, 200, 300, 600, 750, 950, 1250, 5250)) {
            compose.mainClock.advanceTimeBy(time - now)
            now = time
            log.append("Q08 named=$time clockNow=${clock?.now}\n")
        }
        println(log)
    }
}
