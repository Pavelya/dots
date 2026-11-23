package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.campaign.Campaign
import dots.campaign.Chest
import dots.campaign.Progress
import dots.campaign.Reward
import dots.engine.Booster
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.GameTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What A-04 adds to the screens: the Chest over the map (the wireframes' Chest), the player's title
 * (on Stats, StatsScreenTest), and the Level card with the player's own boosters and its
 * question before a saved game ends. Reference images in app/src/test/screenshots; they are
 * recorded in CI (CLAUDE.md, "Recording screenshots").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class ProgressScreensTest {
    @get:Rule
    val compose = createComposeRule()

    /** A chest of two boosters: Wall or Bomb, after 2-15. */
    private val chest = Campaign.chests.single { it.after.code == "2-15" }

    /** The chest after 1-10, whose one reward is two walls (decision 0038). */
    private val single = Campaign.chests.first()

    /**
     * Level 1-10 just passed: the map behind either chest. The note hides which level it follows, so
     * the two-booster chest keeps the screenshots it had when the chest after 1-10 held two.
     */
    private val passedTen = Progress(Campaign.levels.take(10).withIndex().associate { (i, it) -> it.code to listOf(3, 2, 1)[i % 3] })

    /** Draws [content] as the phone would: 24 dp of status bar at the top, 12 dp of gesture bar at the bottom. */
    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            DotsTheme(GameTheme.NOTEBOOK, dark) {
                Surface { Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp)) { content() } }
            }
        }
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")

    private val noActions = MapActions(onLevel = {}, onDaily = {}, onPlay = {}, onRules = {}, onStats = {}, onShop = {}, onSettings = {})

    private fun chestOverMap(
        dark: Boolean = false,
        picked: MutableList<Reward> = mutableListOf(),
        closed: () -> Unit = {},
        shown: Chest = chest,
    ) = show(dark) {
        Box {
            MapScreen(Campaign.map(passedTen), noActions)
            ChestNote(shown, onPick = { picked += it }, onClose = closed)
        }
    }

    @Test
    fun `the chest offers its two boosters, and a tap keeps one`() {
        val picked = mutableListOf<Reward>()
        chestOverMap(picked = picked)

        compose.onNodeWithText("Ten levels done!").assertIsDisplayed()
        compose.onNodeWithText("Pick one to keep.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Wall, 2 boosters. Keep it.").assertIsDisplayed()
        capture("chest")

        compose.onNodeWithContentDescription("Bomb, 2 boosters. Keep it.").performClick()
        assertEquals(listOf(Reward(Booster.BOMB, 2)), picked)
    }

    @Test
    fun `a tap on the map around the chest closes it unopened`() {
        var closed = 0
        val picked = mutableListOf<Reward>()
        chestOverMap(picked = picked, closed = { closed++ })

        // The top of the map, above the note.
        compose.onRoot().performTouchInput { click(Offset(centerX, 120f)) }
        assertEquals(1, closed)
        assertTrue(picked.isEmpty())
    }

    @Test
    fun `the chest, dark`() {
        chestOverMap(dark = true)
        capture("chest_dark")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun `the chest on a small phone`() {
        chestOverMap()
        compose.onNodeWithContentDescription("Bomb, 2 boosters. Keep it.").assertIsDisplayed()
        capture("chest_small_phone")
    }

    @Test
    fun `a chest of one reward shows it with Keep it, and Keep it keeps it`() {
        val picked = mutableListOf<Reward>()
        chestOverMap(picked = picked, shown = single)

        compose.onNodeWithText("Ten levels done!").assertIsDisplayed()
        compose.onNodeWithText("For the levels ahead.").assertIsDisplayed()
        compose.onNodeWithText("Pick one to keep.").assertDoesNotExist()
        // The reward is shown, not a button: Keep it is the only way to keep it.
        compose.onNodeWithContentDescription("Wall, 2 boosters.").assertIsDisplayed().assertHasNoClickAction()
        capture("chest_single")

        compose.onNodeWithText("Keep it").performClick()
        assertEquals(listOf(Reward(Booster.WALL, 2)), picked)
    }

    @Test
    fun `the chest of one reward, dark`() {
        chestOverMap(dark = true, shown = single)
        capture("chest_single_dark")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun `the chest of one reward on a small phone`() {
        chestOverMap(shown = single)
        compose.onNodeWithText("Keep it").assertIsDisplayed()
        capture("chest_single_small_phone")
    }

    @Test
    fun `at the highest title the bar is full`() {
        show { TitleCard(225) }

        compose.onNodeWithContentDescription("Your title: Grandmaster, the highest.").assertIsDisplayed()
    }

    @Test
    fun `the Level card adds the player's own boosters to the level's, and asks before Start ends another level's game`() {
        val level = Campaign.level(2, 1)
        var started = 0
        show {
            Box {
                MapScreen(Campaign.map(Progress(Campaign.worlds.first().levels.associate { it.code to 3 })), noActions)
                LevelCard(
                    level, saved = false, onStart = { started++ }, onBooster = {}, onClose = {},
                    owned = mapOf(Booster.WALL to 2), replaces = "1-7",
                )
            }
        }

        // Level 2-1 gives 2 walls, and the player brings 2 of their own.
        compose.onNodeWithContentDescription("Wall, 4. What it does").assertExists()
        // The card is turned a little and scrolls: its buttons are pressed through their click
        // actions, without scrolling it.
        compose.onNodeWithText("Start").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Replace your saved game?").assertExists()
        compose.onNodeWithText("Your unfinished game of level 1-7 ends, and this level takes its place.").assertExists()
        assertEquals(0, started)
        capture("level_card_replace")

        compose.onNodeWithText("Keep it").performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(compose.onAllNodes(hasText("Replace your saved game?")).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText("Start").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Start").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(1, started)
    }
}
