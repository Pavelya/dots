package io.github.pavelya.dots.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.github.takahirom.roborazzi.captureRoboImage
import dots.campaign.Campaign
import dots.campaign.Level
import dots.campaign.Progress
import io.github.pavelya.dots.data.SampleProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The world map at different points of the campaign. Reference images in
 * app/src/test/screenshots; they are recorded in CI (CLAUDE.md, "Recording screenshots").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class MapScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val opened = mutableListOf<Level>()

    private fun show(progress: Progress) {
        val map = Campaign.map(progress)
        val actions = MapActions(
            onLevel = { opened += it },
            onDaily = {}, onPlay = {}, onRules = {}, onStats = {}, onShop = {}, onSettings = {},
        )
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface { MapScreen(map, actions) }
            }
        }
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")

    @Test
    fun `a new player sees level 1-1 as next, near the thumb`() {
        show(Progress.NONE)

        compose.onNodeWithContentDescription("Level 1-1, next").assertIsDisplayed()
        compose.onNodeWithText("World 1 · Notebook").assertIsDisplayed()
        capture("map_new_player")
    }

    @Test
    fun `mid-world, the map opens at the next level with the stars earned below it`() {
        show(SampleProgress.midWorld)

        compose.onNodeWithContentDescription("Level 2-10, next").assertIsDisplayed()
        compose.onNodeWithContentDescription("Level 2-9, 1 star").assertIsDisplayed()
        capture("map_mid_world")
    }

    @Test
    fun `a completed world opens the next one`() {
        show(SampleProgress.worldComplete)

        compose.onNodeWithContentDescription("Level 2-1, next").assertIsDisplayed()
        compose.onNodeWithText("World 2 · Garden").assertIsDisplayed()
        capture("map_world_complete")
    }

    @Test
    fun `a locked world says the stars it needs`() {
        show(SampleProgress.worldNeedsStars)

        compose.onNodeWithText("Stars needed: 20. You have 15.").assertIsDisplayed()
        capture("map_world_needs_stars")

        compose.onNodeWithText("World 2 · Garden").performClick()
        compose.onAllNodes(hasText("Stars needed: 20. You have 15.")).fetchSemanticsNodes().let { assertEquals(2, it.size) }
        assertTrue(opened.isEmpty())
    }

    @Test
    fun `an open or passed level opens its card, a locked one does not`() {
        show(SampleProgress.midWorld)

        compose.onNodeWithContentDescription("Level 2-10, next").performClick()
        compose.onNodeWithContentDescription("Level 2-9", substring = true).performClick()
        compose.onNodeWithContentDescription("Level 2-11, locked").performClick()

        assertEquals(listOf("2-10", "2-9"), opened.map { it.code })
        compose.onNodeWithText("Pass level 2-10 to open this one.").assertIsDisplayed()
    }

    @Test
    fun `all 75 levels are on one path that scrolls, and only the visible ones are drawn`() {
        show(SampleProgress.everything)
        assertEquals(75, Campaign.levels.size)

        // With nothing left to play, the map opens at the end of the path, above the last boss.
        compose.onNodeWithText("More worlds are coming.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Level 4-20, Kraken, 3 stars").assertIsDisplayed()
        val drawn = compose.onAllNodes(hasContentDescription("Level ", substring = true)).fetchSemanticsNodes().size
        assertTrue("$drawn levels drawn at once", drawn in 1..12)

        for (code in listOf("1-1", "3-1", "4-19")) {
            compose.onNodeWithTag(MAP_PATH_TAG).performScrollToNode(hasContentDescription("Level $code,", substring = true))
            compose.onNodeWithContentDescription("Level $code,", substring = true).assertIsDisplayed()
        }
    }
}
