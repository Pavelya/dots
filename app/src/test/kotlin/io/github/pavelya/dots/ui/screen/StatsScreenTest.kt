package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.campaign.Campaign
import dots.campaign.Progress
import dots.log.PlayerStats
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.GameTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Stats (A-11), as the wireframes' frame "Stats" draws it: the title, six totals, the stars of each
 * world, and Leaderboards and Achievements. Reference images in app/src/test/screenshots; they are
 * recorded in CI (CLAUDE.md, "Recording screenshots").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class StatsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    /** World 1's first nine levels passed: 21 stars, Apprentice, as the wireframe. */
    private val progress = Progress(Campaign.levels.take(9).withIndex().associate { (i, it) -> it.code to listOf(3, 2, 2)[i % 3] })

    private val stats = PlayerStats(played = 48, won = 31, captured = 212, bestCapture = 9)

    private val tapped = mutableListOf<String>()

    /** Draws Stats as the phone would: 24 dp of status bar at the top, 12 dp of gesture bar at the bottom. */
    private fun show(dark: Boolean = false, stats: PlayerStats = this.stats, longestStreak: Int = 12, progress: Progress = this.progress) {
        compose.setContent {
            DotsTheme(GameTheme.NOTEBOOK, dark) {
                Surface {
                    Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp)) {
                        StatsScreen(
                            map = Campaign.map(progress),
                            stats = stats,
                            longestStreak = longestStreak,
                            onLeaderboards = { tapped += "leaderboards" },
                            onAchievements = { tapped += "achievements" },
                            onBack = { tapped += "back" },
                        )
                    }
                }
            }
        }
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")

    @Test
    fun `Stats shows the title, the totals and the stars of each world`() {
        show()

        compose.onNodeWithText("Stats").assertIsDisplayed()
        // The title card is read as one sentence.
        compose.onNodeWithContentDescription("Your title: Apprentice. Stars: 21. Strategist at 45.").assertIsDisplayed()
        for (total in listOf("games played: 48", "games won: 31", "dots captured: 212", "best capture: 9", "longest streak: 12", "stars: 21")) {
            compose.onNodeWithContentDescription(total).assertIsDisplayed()
        }
        compose.onNodeWithText("Stars per world").assertIsDisplayed()
        val first = Campaign.worlds.first()
        compose.onNodeWithContentDescription("World 1: 21 of ${first.maxStars} stars").assertIsDisplayed()
        compose.onNodeWithContentDescription("World 2: 0 of ${Campaign.worlds[1].maxStars} stars").assertIsDisplayed()
        capture("stats_screen")
    }

    @Test
    fun `a new player sees zeros`() {
        show(stats = PlayerStats.NONE, longestStreak = 0, progress = Progress.NONE)

        compose.onNodeWithContentDescription("Your title: Novice. Stars: 0. Apprentice at 10.").assertIsDisplayed()
        compose.onNodeWithContentDescription("games played: 0").assertIsDisplayed()
        compose.onNodeWithContentDescription("stars: 0").assertIsDisplayed()
    }

    @Test
    fun `Leaderboards, Achievements and Back`() {
        show()

        compose.onNodeWithText("Leaderboards").performClick()
        compose.onNodeWithText("Achievements").performClick()
        compose.onNodeWithContentDescription("Back to the map").performClick()
        assertEquals(listOf("leaderboards", "achievements", "back"), tapped)
    }

    @Test
    fun `Stats, dark`() {
        show(dark = true)
        capture("stats_dark")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun `Stats on a small phone keeps its buttons in reach`() {
        show()
        compose.onNodeWithText("Leaderboards").assertIsDisplayed()
        compose.onNodeWithText("Achievements").assertIsDisplayed()
        capture("stats_small_phone")
    }
}
