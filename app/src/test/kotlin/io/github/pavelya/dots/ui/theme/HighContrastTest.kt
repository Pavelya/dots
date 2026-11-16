package io.github.pavelya.dots.ui.theme

import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.github.pavelya.dots.ui.board.BoardStyle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The "High-contrast pieces" setting reaches every board through [DotsTheme]. */
@RunWith(RobolectricTestRunner::class)
class HighContrastTest {
    @get:Rule
    val compose = createComposeRule()

    private fun lookWith(highContrast: Boolean): BoardLook {
        var look: BoardLook? = null
        compose.setContent {
            DotsTheme(GameTheme.NOTEBOOK, dark = false, highContrast = highContrast) { look = BoardStyle.current.look }
        }
        compose.waitForIdle()
        return checkNotNull(look)
    }

    @Test
    fun boardsDrawHighContrastPiecesWhenTheSettingIsOn() {
        assertEquals(GameTheme.NOTEBOOK.highContrastBoard, lookWith(highContrast = true))
    }

    @Test
    fun boardsDrawTheUsualPiecesWhenTheSettingIsOff() {
        assertEquals(GameTheme.NOTEBOOK.board, lookWith(highContrast = false))
    }
}
