package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import dots.ai.Ai
import dots.campaign.Campaign
import dots.campaign.CampaignMap
import dots.campaign.Level
import dots.campaign.LevelMap
import dots.campaign.LevelResult
import dots.campaign.Mission
import dots.campaign.StarMeasure
import dots.campaign.Stars
import dots.campaign.Strength
import dots.engine.Action
import dots.engine.Booster
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import io.github.pavelya.dots.data.CampaignJournal
import io.github.pavelya.dots.data.LevelEnd
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.levelGame
import io.github.pavelya.dots.ui.board.BoardGeometry
import io.github.pavelya.dots.ui.theme.DotsTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Level card, the level opening, the mission note and Result in the campaign. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class LevelScreensTest {
    @get:Rule
    val compose = createComposeRule()

    /** The ring around the computer's dot needs one more red dot, at (3, 4). */
    private val map = LevelMap(
        listOf(
            ".......",
            ".......",
            "...R...",
            "..RBR..",
            ".......",
            ".......",
            ".......",
        ),
    )

    /** Capture 1 dot in 3 moves; two stars for a move to spare, three for two. */
    private val level = Level(1, 7, "Test Ring", Strength.BEGINNER, 3, map, Mission.Capture(1, 3), stars = Stars(StarMeasure.MOVES, 1, 2))

    /** The computer places on the first free point, the top row, far from the ring. */
    private val firstFreePoint = object : Ai {
        override fun choose(state: GameState): Action = Action.Place(state.legalMoves().first())
    }

    /** Remembers failures in a row as the progress store does. */
    private class Journal(var failures: Int = 0) : CampaignJournal {
        override suspend fun finished(level: Level, result: LevelResult): LevelEnd {
            failures = if (result.isPassed) 0 else failures + 1
            return LevelEnd(result, CampaignMap.Opened.NOTHING, failures, next = null)
        }
    }

    private fun levelViewModel(journal: CampaignJournal = Journal()) = GameViewModel(
        start = levelGame(level, threatHighlight = false, now = 0),
        nextGame = { levelGame(level, threatHighlight = false, now = 0) },
        computerFor = { firstFreePoint },
        campaign = journal,
        computerDispatcher = Dispatchers.Default,
        replyDelayMillis = 0,
        clock = { 0L },
    )

    private fun place(x: Int, y: Int) {
        repeat(2) {
            compose.onNodeWithTag(BOARD_TAG).performTouchInput {
                click(BoardGeometry(map.width, map.height, width.toFloat(), height.toFloat()).center(x, y))
            }
        }
    }

    private fun waitFor(matcher: SemanticsMatcher) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    private fun waitFor(text: String) = waitFor(hasText(text))

    private fun yourTurn(dotsLeft: Int) {
        waitFor(hasContentDescription("You, score 0, to move"))
        waitFor(hasContentDescription(if (dotsLeft == 1) "1 dot left" else "$dotsLeft dots left"))
    }

    @Test
    fun `a level opens with its mission, which then sits in the corner note`() {
        compose.mainClock.autoAdvance = false
        compose.setContent { DotsTheme { GameScreen(levelViewModel(), onToMap = {}, onRules = {}) } }
        compose.mainClock.advanceTimeByFrame()

        compose.onNodeWithText("World 1 · Level 7").assertIsDisplayed()
        compose.onNodeWithText("The computer starts 2 dots behind.").assertIsDisplayed()
        compose.onNodeWithText("Level 7").assertIsDisplayed() // the top row
        val note = "Mission. Capture 1 dot in 3 moves. Captured 0 of 1. 3 moves left"
        compose.onNodeWithContentDescription(note).assertDoesNotExist()

        compose.mainClock.advanceTimeBy(OPENING_MILLIS + 100)
        compose.onNodeWithText("World 1 · Level 7").assertDoesNotExist()
        compose.onNodeWithContentDescription(note).assertIsDisplayed()
    }

    @Test
    fun `a pass shows the stars, the score and the next target, and Retry plays the level again`() {
        val vm = levelViewModel()
        var next = 0
        compose.setContent { DotsTheme { GameScreen(vm, onToMap = {}, onRules = {}, onNext = { next++ }) } }
        yourTurn(3)
        place(0, 6)
        yourTurn(2)
        place(3, 4)

        waitFor("You won!")
        compose.onNodeWithContentDescription("2 of 3 stars").assertIsDisplayed()
        compose.onNodeWithText("1 captured · 1 move to spare").assertIsDisplayed()
        compose.onNodeWithText("Three stars need 2 moves to spare").assertIsDisplayed()
        compose.onNodeWithContentDescription("Mission done. Capture 1 dot in 3 moves. Captured 1 of 1").assertExists()
        compose.onNodeWithContentDescription("Pause").assertDoesNotExist()

        compose.onNodeWithText("Next").performClick()
        assertEquals(1, next)
        compose.onNodeWithContentDescription("Retry").performClick()
        yourTurn(3)
        compose.onNodeWithText("You won!").assertDoesNotExist()
    }

    @Test
    fun `a failure says how close it was, Retry is the main button, and the third offers a free Hint`() {
        val vm = levelViewModel()
        var toMap = 0
        compose.setContent { DotsTheme { GameScreen(vm, onToMap = { toMap++ }, onRules = {}) } }
        for (attempt in 1..3) {
            yourTurn(3)
            place(0, 6)
            yourTurn(2)
            place(1, 6)
            yourTurn(1)
            place(2, 6)

            waitFor("So close!")
            compose.onNodeWithText("Missed by 1 capture").assertIsDisplayed()
            compose.onNodeWithContentDescription("Captured 0 of 1").assertIsDisplayed()
            if (attempt < 3) {
                compose.onNodeWithText("Retry with a free Hint").assertDoesNotExist()
                compose.onNodeWithText("Retry").performClick()
            }
        }
        compose.onNodeWithContentDescription("Map").performClick()
        assertEquals(1, toMap)

        compose.onNodeWithText("Retry with a free Hint").performClick()
        yourTurn(3)
        assertEquals(1, vm.ui.value.state.boostersLeft(Player.RED, Booster.HINT))
    }

    @Test
    fun `the Level card shows the mission, the computer, the boosters and the star targets, with one button`() {
        val level = Campaign.level(2, 1)
        var started = 0
        var closed = 0
        val boosters = mutableListOf<Booster>()
        compose.setContent {
            DotsTheme { LevelCard(level, saved = false, onStart = { started++ }, onBooster = { boosters += it }, onClose = { closed++ }) }
        }

        compose.onNodeWithText("World 2 · Level 1").assertIsDisplayed()
        compose.onNodeWithText(level.name).assertIsDisplayed()
        compose.onNodeWithText("Keep all your dots for 2 moves").assertIsDisplayed()
        compose.onNodeWithContentDescription("The board at the start").assertIsDisplayed()
        compose.onNode(hasContentDescription("Computer, plays steady", substring = true)).assertIsDisplayed()
        compose.onNodeWithText("You have 2 dots. Boosters:").assertIsDisplayed()
        compose.onNodeWithContentDescription("1 of 3 stars: Keep all your dots for 2 moves").assertExists()
        compose.onNodeWithContentDescription("3 of 3 stars: With 3 boosters unused").assertExists()
        compose.onNodeWithContentDescription("Wall, 2. What it does").performClick()
        compose.onNodeWithContentDescription("Undo, 1. What it does").performClick()
        assertEquals(listOf(Booster.WALL, Booster.UNDO), boosters)

        compose.onNodeWithText("Start").performClick()
        assertEquals(1, started)
        compose.onNodeWithContentDescription("Close").performClick()
        assertEquals(1, closed)
    }

    @Test
    fun `a saved level's card says Resume, and a boss's card shows its rule`() {
        val boss = Campaign.level(1, 15)
        compose.setContent { DotsTheme { LevelCard(boss, saved = true, onStart = {}, onBooster = {}, onClose = {}) } }

        compose.onNodeWithText("Resume").assertIsDisplayed()
        compose.onNodeWithText("Start").assertDoesNotExist()
        compose.onNodeWithText("BOSS LEVEL").assertIsDisplayed()
        compose.onNode(hasText("Its rule: Every 5 turns it places two dots.", substring = true)).assertIsDisplayed()
        compose.onNode(hasContentDescription("${boss.name}, plays steady", substring = true)).assertIsDisplayed()
    }

    @Test
    fun `every mission type has its line`() {
        val missions = listOf(
            Mission.Win to "Win",
            Mission.WinBy(3) to "Win by 3 dots",
            Mission.WinWithFewerDots(20) to "Win with only 20 dots",
            Mission.Capture(2, 5) to "Capture 2 dots in 5 moves",
            Mission.Capture(1, 1) to "Capture 1 dot in 1 move",
            Mission.Survive(4) to "Keep all your dots for 4 moves",
            Mission.Rescue(Pos(3, 3)) to "Save the circled dot",
            Mission.Target(Pos(3, 3)) to "Capture the circled dot",
        )
        compose.setContent { DotsTheme { Column { for ((mission, _) in missions) Text(missionText(mission)) } } }

        for ((_, line) in missions) compose.onNodeWithText(line).assertExists()
    }

    @Test
    fun `every campaign level has a mission line, star targets and, for a boss, its rule`() {
        val lines = mutableListOf<String>()
        compose.setContent {
            DotsTheme {
                Column {
                    for (level in Campaign.levels) {
                        val texts = listOf(missionText(level.mission), starTargetText(level, 2), starTargetText(level, 3), headStartText(level)) +
                            listOfNotNull(level.rule?.let { ruleText(it) })
                        lines += texts.map { "${level.code}: $it" }
                    }
                }
            }
        }
        compose.waitForIdle()

        assertTrue(lines.size >= Campaign.levels.size * 4)
        assertTrue(lines.filter { it.substringAfter(": ").isBlank() }.toString(), lines.none { it.substringAfter(": ").isBlank() })
        assertTrue("1-1: With 2 moves to spare" in lines)
        assertTrue("1-15: Every 5 turns it places two dots. The counter above the board shows when." in lines)
    }
}
