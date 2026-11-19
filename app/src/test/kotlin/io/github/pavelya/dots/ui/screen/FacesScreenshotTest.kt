package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.campaign.Strength
import io.github.pavelya.dots.game.Mood
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.FaceInks
import io.github.pavelya.dots.ui.theme.GameTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Every face of the opponent as the app draws it (docs/design/illustrations.md, "Where each picture
 * goes"), so the owner can check the set in one picture: the computer's moods, its three strengths
 * and the World 1 boss at 108 dp, as above the board, then all of them at 48 dp, as on the level
 * card, where the eyes and the mouth must still read.
 *
 * Then the same faces in the Star chart and Neon arcade inks, at 108 dp on the theme's Card, the
 * colour of the opponent's photo (theme-star-chart.md and theme-neon-arcade.md, "The faces"). Only
 * the game screen draws them; the level card stays Notebook's. The Card colours come from the
 * theme pages until V-13 gives the app the two themes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class FacesScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val faces: List<FacePicture> = listOf(Mood.HAPPY, Mood.SMUG, Mood.UPSET, Mood.SULKY).map { Faces.of(it, null) } +
        listOf(Strength.BEGINNER, Strength.STEADY, Strength.EXPERT).map { Faces.strength(it) } +
        Faces.INK_BOTTLE.let { listOf(it.smug, it.attacking, it.beaten) }

    private fun gallery(dark: Boolean) {
        compose.setContent {
            DotsTheme(GameTheme.NOTEBOOK, dark) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Rows(faces.chunked(3), 108.dp)
                        Rows(faces.chunked(5), 48.dp)
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/faces${if (dark) "_dark" else ""}.png")
    }

    @Composable
    private fun Rows(rows: List<List<FacePicture>>, size: Dp) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (face in row) Face(face, size)
            }
        }
    }

    /** The faces in [inks] on the theme's [card] colour, light or [dark], as in the opponent's photo. */
    private fun themeGallery(inks: FaceInks, name: String, card: Color, dark: Boolean) {
        compose.setContent {
            DotsTheme(GameTheme.NOTEBOOK.copy(faceInks = inks), dark) {
                Column(
                    Modifier.fillMaxSize().background(card).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Rows(faces.chunked(3), 108.dp)
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/faces_$name${if (dark) "_dark" else ""}.png")
    }

    @Test
    fun light() = gallery(dark = false)

    @Test
    fun dark() = gallery(dark = true)

    @Test
    fun starChartLight() = themeGallery(FaceInks.STAR_CHART, "star_chart", Color(0xFFFFFFFF), dark = false)

    @Test
    fun starChartDark() = themeGallery(FaceInks.STAR_CHART, "star_chart", Color(0xFF1A2448), dark = true)

    @Test
    fun neonArcadeLight() = themeGallery(FaceInks.NEON_ARCADE, "neon_arcade", Color(0xFFFFFFFF), dark = false)

    @Test
    fun neonArcadeDark() = themeGallery(FaceInks.NEON_ARCADE, "neon_arcade", Color(0xFF1A1730), dark = true)
}
