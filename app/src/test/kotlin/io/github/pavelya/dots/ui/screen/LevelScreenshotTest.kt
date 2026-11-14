package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.ai.Ai
import dots.campaign.Campaign
import dots.campaign.CampaignMap
import dots.campaign.Level
import dots.campaign.LevelResult
import dots.campaign.Progress
import dots.engine.Action
import dots.engine.GameSettings
import dots.engine.GameState
import dots.engine.Opening
import dots.engine.Pos
import io.github.pavelya.dots.data.CampaignJournal
import io.github.pavelya.dots.data.LevelEnd
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.gameViewModel
import io.github.pavelya.dots.game.levelGame
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.GameTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Level card over the map (canvas frame `level-card`; the wireframes' Level card and Boss
 * card) and Result over the final board (canvas frame `result-win`; the wireframes' Win and Loss),
 * shown next to their frames in the PR. Reference images in app/src/test/screenshots; they are
 * recorded in CI (CLAUDE.md, "Recording screenshots").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class LevelScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /** Draws [content] as the phone would: 24 dp of status bar at the top, 12 dp of gesture bar at the bottom. */
    private fun show(theme: GameTheme = GameTheme.NOTEBOOK, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            DotsTheme(theme, dark) {
                Surface { Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp)) { content() } }
            }
        }
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")

    /** The card of [level] over the map, with every level before it passed, the map dimmed behind it. */
    private fun card(level: Level, saved: Boolean = false, dark: Boolean = false) {
        val before = Campaign.levels.takeWhile { it != level }
        val map = Campaign.map(Progress(before.withIndex().associate { (i, it) -> it.code to listOf(3, 2, 1)[i % 3] }))
        val actions = MapActions(onLevel = {}, onDaily = {}, onPlay = {}, onRules = {}, onStats = {}, onShop = {}, onSettings = {})
        show(dark = dark) {
            Box {
                MapScreen(map, actions)
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                LevelCard(level, saved, onStart = {}, onBooster = {}, onClose = {})
            }
        }
    }

    @Test
    fun levelCard() {
        card(Campaign.level(1, 7))
        capture("level_card")
    }

    @Test
    fun levelCardDark() {
        card(Campaign.level(1, 7), dark = true)
        capture("level_card_dark")
    }

    @Test
    fun levelCardWithBoosters() {
        card(Campaign.level(2, 1), saved = true)
        capture("level_card_boosters_resume")
    }

    @Test
    fun bossCard() {
        card(Campaign.level(1, 15))
        capture("level_card_boss")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun levelCardOnASmallPhone() {
        card(Campaign.level(1, 7))
        capture("level_card_small_phone")
    }

    /** The computer places on the first free point, the top row, far from the ring of Level 1-1. */
    private val firstFreePoint = object : Ai {
        override fun choose(state: GameState): Action = Action.Place(state.legalMoves().first())
    }

    private fun level11(journal: CampaignJournal) = Campaign.level(1, 1).let { level ->
        GameViewModel(
            start = levelGame(level, threatHighlight = false, now = 0),
            nextGame = { levelGame(level, threatHighlight = false, now = 0) },
            computerFor = { firstFreePoint },
            campaign = journal,
            computerDispatcher = Dispatchers.Unconfined,
            replyDelayMillis = 0,
        )
    }

    /** Plays [point] (choose, then place), and waits for the computer's reply if the game goes on. */
    private fun play(vm: GameViewModel, point: Pos) {
        compose.runOnUiThread { repeat(2) { vm.onTap(point) } }
        compose.waitUntil(timeoutMillis = 5_000) { !vm.ui.value.thinking }
    }

    private fun waitFor(text: String) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    /** Level 1-1 passed with a move to spare, in [theme]: two stars of three, and Level 1-2 opens. */
    private fun win(name: String, theme: GameTheme = GameTheme.NOTEBOOK, dark: Boolean = false) {
        val journal = CampaignJournal { _: Level, result: LevelResult -> LevelEnd(result, CampaignMap.Opened(emptyList(), listOf(Campaign.level(1, 2))), 0, Campaign.level(1, 2)) }
        val vm = level11(journal)
        show(theme, dark) { GameScreen(vm, onToMap = {}, onRules = {}) }
        play(vm, Pos(0, 6))
        play(vm, Pos(3, 4))
        waitFor("You won!")
        capture(name)
    }

    @Test
    fun resultWin() = win("result_level_win")

    @Test
    fun resultWinDark() = win("result_level_win_dark", dark = true)

    @Test
    fun resultLossAfterTheThirdFailure() {
        val journal = CampaignJournal { _: Level, result: LevelResult -> LevelEnd(result, CampaignMap.Opened.NOTHING, LevelEnd.HELP_AFTER_FAILURES, null) }
        val vm = level11(journal)
        show { GameScreen(vm, onToMap = {}, onRules = {}) }
        play(vm, Pos(0, 6))
        play(vm, Pos(1, 6))
        play(vm, Pos(2, 6))
        waitFor("So close!")
        capture("result_level_loss")
    }

    @Test
    fun resultFreePlay() {
        val vm = gameViewModel(
            newSettings = { GameSettings(width = 5, height = 5, dotsPerPlayer = 1, opening = Opening.EMPTY) },
            newComputer = { firstFreePoint },
            computerDispatcher = Dispatchers.Unconfined,
            replyDelayMillis = 0,
        )
        show { GameScreen(vm, onToMap = {}, onRules = {}) }
        play(vm, Pos(2, 2))
        waitFor("A draw")
        capture("result_free_play")
    }
}
