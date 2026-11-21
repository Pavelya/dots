package io.github.pavelya.dots.ui.motion

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Notes and cards over the map or the board (motion.md, "Level opening, notes and cards"): a note
 * comes up in 300 ms and goes down in 200 ms, and with animations removed it is in place, or gone,
 * at once.
 */
@RunWith(RobolectricTestRunner::class)
class NoteMotionTest {
    @get:Rule
    val compose = createComposeRule()

    private val open = mutableStateOf(true)
    private lateinit var motion: NoteMotion

    private fun show(reduce: Boolean) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalReduceMotion provides reduce) { motion = rememberNoteMotion(open.value) }
        }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun step(ms: Long) = compose.mainClock.advanceTimeBy(ms, ignoreFrameDuration = true)

    /** Closes the note: the change reaches the screen on the next frame, and the note starts down on the one after. */
    private fun close() {
        compose.runOnUiThread {
            open.value = false
            Snapshot.sendApplyNotifications()
        }
        repeat(2) { compose.mainClock.advanceTimeByFrame() }
    }

    @Test
    fun `a note comes up in 300 ms and goes down in 200 ms`() {
        show(reduce = false)
        assertTrue("${motion.up}", motion.up < 0.2f)
        step(150)
        assertTrue("${motion.up}", motion.up > 0.2f && motion.up < 1f)
        step(200)
        assertEquals(1f, motion.up)
        assertFalse(motion.gone)

        close()
        assertTrue("${motion.up}", motion.up > 0.5f)
        assertFalse(motion.gone)
        step(250)
        assertEquals(0f, motion.up)
        assertTrue(motion.gone)
    }

    @Test
    fun `a note closed while it comes up goes down from where it got to`() {
        show(reduce = false)
        step(100)
        val got = motion.up
        close()
        val leaving = motion.up
        // No jump up to its place, nor down out of sight: it starts down from about where it was.
        assertTrue("$leaving after $got", leaving > got - 0.2f && leaving < got + 0.2f && leaving < 1f)
        step(50)
        assertTrue("${motion.up} after $leaving", motion.up < leaving)
        step(250)
        assertTrue(motion.gone)
    }

    @Test
    fun `with animations removed a note is in place, and gone, at once`() {
        show(reduce = true)
        assertEquals(1f, motion.up)
        close()
        assertEquals(0f, motion.up)
        assertTrue(motion.gone)
    }
}
