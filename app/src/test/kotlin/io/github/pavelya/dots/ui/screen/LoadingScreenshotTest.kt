package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.pavelya.dots.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The loading screen, shown next to the wireframes' Loading frame in the PR. Reference images in
 * app/src/test/screenshots; they are recorded in CI (CLAUDE.md, "Recording screenshots").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class LoadingScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /**
     * Draws the loading screen as the phone would, 24 dp of status bar at the top and 12 dp of
     * gesture bar at the bottom, with paper under the bars as the app puts there.
     */
    private fun show(ring: Float, lastDot: Float, words: Float, dark: Boolean = false) {
        compose.setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Box(Modifier.fillMaxSize().background(colorResource(R.color.paper)).padding(top = 24.dp, bottom = 12.dp)) {
                    LoadingPicture(
                        tip = "Close a ring to capture dots.",
                        ring = { ring },
                        lastDot = { lastDot },
                        words = { words },
                        outside = WindowInsets(top = 24.dp, bottom = 12.dp),
                    )
                }
            }
        }
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")

    /** The first frame: what Android's start-up screen shows (res/drawable/loading_icon.xml), nothing more. */
    @Test
    fun start() {
        show(ring = 0f, lastDot = 0f, words = 0f)
        capture("loading_start")
    }

    /** The ring two thirds drawn, the fourth dot not yet placed, as in the wireframe. */
    @Test
    fun drawing() {
        show(ring = 0.65f, lastDot = 0f, words = 1f)
        capture("loading_drawing")
    }

    /** The ring closed and the fourth dot placed: the app icon, in the dark version. */
    @Test
    @Config(qualifiers = "+night")
    fun drawnDark() {
        show(ring = 1f, lastDot = 1f, words = 1f, dark = true)
        capture("loading_drawn_dark")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun smallPhone() {
        show(ring = 0.65f, lastDot = 0f, words = 1f)
        capture("loading_small_phone")
    }
}
