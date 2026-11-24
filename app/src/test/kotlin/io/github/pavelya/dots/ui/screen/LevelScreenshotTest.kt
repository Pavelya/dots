package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.ai.Ai
import dots.campaign.Campaign
import dots.campaign.CampaignMap
import dots.campaign.Cosmetic
import dots.campaign.Level
import dots.campaign.LevelMap
import dots.campaign.LevelRecord
import dots.campaign.Mission
import dots.campaign.StarMeasure
import dots.campaign.Stars
import dots.campaign.Strength
import dots.campaign.LevelResult
import dots.campaign.Progress
import dots.engine.Action
import dots.engine.Booster
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
import dots.log.GameLogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    /**
     * The card of [level] over the map, with every level before it passed, the map dimmed behind it
     * by the card. [boosters] are what a new game of the level has.
     */
    private fun card(level: Level, saved: Boolean = false, dark: Boolean = false, boosters: Map<Booster, Int> = level.boosters) {
        val before = Campaign.levels.takeWhile { it != level }
        val map = Campaign.map(Progress(before.withIndex().associate { (i, it) -> it.code to listOf(3, 2, 1)[i % 3] }))
        val actions = MapActions(onLevel = {}, onDaily = {}, onPlay = {}, onRules = {}, onStats = {}, onShop = {}, onSettings = {})
        show(dark = dark) {
            Box {
                MapScreen(map, actions)
                LevelCard(level, saved, onStart = {}, onBooster = {}, onClose = {}, boosters = boosters)
            }
        }
    }

    @Test
    fun levelCard() {
        card(Campaign.level(1, 7))
        compose.onNodeWithText("Holes", substring = true).assertDoesNotExist() // a board without holes says nothing of them
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

    /** 2-1 (wall 2, undo 1) after an earlier attempt used a Wall and the Undo (decision 0040). */
    private val spentOn21: Progress get() = Progress.NONE.spendOn(Campaign.level(2, 1), mapOf(Booster.WALL to 1, Booster.UNDO to 1))

    /** The card counts what is left for the level: one wall, and Undo ×0 in grey. */
    @Test
    fun levelCardAfterBoostersSpent() {
        val level = Campaign.level(2, 1)
        card(level, boosters = spentOn21.boostersFor(level))
        compose.onNodeWithContentDescription("Wall, 1. What it does").assertExists()
        compose.onNodeWithContentDescription("Undo, 0. What it does").assertExists()
        capture("level_card_boosters_spent")
    }

    /** The next game of 2-1 starts with what is left: the tool bar shows one wall, and Undo dashed with 0. */
    @Test
    fun toolBarAfterBoostersSpent() {
        val level = Campaign.level(2, 1)
        val vm = GameViewModel(
            start = levelGame(level, threatHighlight = false, now = 0, progress = spentOn21),
            nextGame = { levelGame(level, threatHighlight = false, now = 0, progress = spentOn21) },
            computerFor = { firstFreePoint },
            computerDispatcher = Dispatchers.Unconfined,
            replyDelayMillis = 0,
        )
        show { GameScreen(vm, onToMap = {}, onRules = {}) }
        compose.onNodeWithContentDescription("Wall, 1 left").assertExists()
        compose.onNodeWithContentDescription("Undo, none left").assertExists()
        capture("game_boosters_spent")
    }

    @Test
    fun bossCard() {
        card(Campaign.level(1, 15))
        capture("level_card_boss")
    }

    /** Full Moon (3-6), a round board whose corner holes count as border: the card says holes are safe (A-15). */
    @Test
    fun cardWithSafeHoles() {
        card(Campaign.level(3, 6))
        compose.onNodeWithText("Holes are safe: a dot next to a hole can never be captured.").assertExists()
        capture("level_card_holes_safe")
    }

    /** Reef (4-18): open holes, rocks and pearls; the card says holes are open. */
    @Test
    fun cardWithOpenHoles() {
        card(Campaign.level(4, 18))
        compose.onNodeWithText("Holes are open: a ring can go round a hole, so a dot next to one can be captured.").assertExists()
        capture("level_card_holes_open")
    }

    @Test
    fun cardWithOpenHolesDark() {
        card(Campaign.level(4, 18), dark = true)
        capture("level_card_holes_open_dark")
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

    private fun level11(journal: CampaignJournal, level11: Level = Campaign.level(1, 1)) = level11.let { level ->
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
    private fun win(name: String, theme: GameTheme = GameTheme.NOTEBOOK, dark: Boolean = false, earned: List<Cosmetic> = emptyList()) {
        val journal = CampaignJournal { _: Level, result: LevelResult ->
            LevelEnd(result, CampaignMap.Opened(emptyList(), listOf(Campaign.level(1, 2))), 0, Campaign.level(1, 2), earned = earned)
        }
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

    /** The wireframes' Result: win, a theme earned (D-28, MoneyWinEarned): the new stars passed 150. */
    @Test
    fun resultWinEarned() = win("result_level_win_earned", earned = listOf(Cosmetic.STAR_CHART))

    @Test
    fun resultWinEarnedDark() = win("result_level_win_earned_dark", dark = true, earned = listOf(Cosmetic.STAR_CHART))

    /** A piece style earned, drawn in the theme in use. */
    @Test
    fun resultWinEarnedPiecesNeonArcade() = win("result_level_win_earned_pieces_neon_arcade", GameTheme.NEON_ARCADE, earned = listOf(Cosmetic.HEARTS_AND_CLUBS))

    /** Result over the final board in the other themes (canvas frames `result-win-star-chart` and `result-win-neon-arcade`). */
    @Test
    fun resultWinStarChart() = win("result_level_win_star_chart", GameTheme.STAR_CHART)

    @Test
    fun resultWinStarChartDark() = win("result_level_win_star_chart_dark", GameTheme.STAR_CHART, dark = true)

    @Test
    fun resultWinNeonArcade() = win("result_level_win_neon_arcade", GameTheme.NEON_ARCADE)

    @Test
    fun resultWinNeonArcadeDark() = win("result_level_win_neon_arcade_dark", GameTheme.NEON_ARCADE, dark = true)

    /**
     * Level 1-1 lost by one capture, in [theme], after the third failure in a row ([failed]): Retry
     * with a free Hint, and a rewarded ad's offer of [adOffer] below it (D-28). With [watched], the
     * ad has been watched: 1-1 here allows a Wall, so the Wall waits for the retry.
     */
    private fun loss(
        name: String,
        theme: GameTheme = GameTheme.NOTEBOOK,
        failed: Int = LevelEnd.HELP_AFTER_FAILURES,
        adOffer: Booster? = null,
        watched: Boolean = false,
    ) {
        val journal = CampaignJournal { _: Level, result: LevelResult -> LevelEnd(result, CampaignMap.Opened.NOTHING, failed, null) }
        val level = Campaign.level(1, 1).let { if (watched) it.copy(boosters = it.boosters + (Booster.WALL to 1)) else it }
        val vm = level11(journal, level)
        var watches = 0
        show(theme) { GameScreen(vm, onToMap = {}, onRules = {}, adOffer = adOffer, onWatchAd = { watches++ }) }
        play(vm, Pos(0, 6))
        play(vm, Pos(1, 6))
        play(vm, Pos(2, 6))
        waitFor("So close!")
        if (adOffer != null && !watched) {
            compose.onNodeWithText("Watch an ad for one Wall on your retry").performSemanticsAction(SemanticsActions.OnClick)
            assertEquals(1, watches)
        }
        if (watched) {
            compose.runOnUiThread { vm.adWatched() }
            waitFor("One Wall ready for your retry")
            assertEquals(Booster.WALL, vm.ui.value.adForRetry)
        }
        capture(name)
    }

    @Test
    fun resultLossAfterTheThirdFailure() = loss("result_level_loss")

    @Test
    fun resultLossStarChart() = loss("result_level_loss_star_chart", GameTheme.STAR_CHART)

    @Test
    fun resultLossNeonArcade() = loss("result_level_loss_neon_arcade", GameTheme.NEON_ARCADE)

    /** The wireframes' Result: loss, ad offer (D-28): from World 2, one booster for the retry. */
    @Test
    fun resultLossAdOffer() = loss("result_level_loss_ad_offer", failed = 1, adOffer = Booster.WALL)

    /** The wireframes' Result: loss, third try and ad offer: the free Hint above the ad's offer. */
    @Test
    fun resultLossHintAndAdOffer() = loss("result_level_loss_hint_and_ad_offer", adOffer = Booster.WALL)

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun resultLossHintAndAdOfferSmallPhone() = loss("result_level_loss_hint_and_ad_offer_small_phone", adOffer = Booster.WALL)

    /** The wireframes' Result: loss, after the ad: "One Wall ready for your retry" in its place. */
    @Test
    fun resultLossAfterTheAd() = loss("result_level_loss_after_the_ad", failed = 1, adOffer = Booster.WALL, watched = true)

    /**
     * A level passed with the ad's Wall used (G-29): 1 star, its margin, and no "Two stars need"
     * line, as more stars can't come from a game with the ad's booster. A test level in World 2: red's d3
     * encloses both blue dots, after a wall on each of two turns, the second the ad's.
     */
    @Test
    fun resultWinWithTheAdsBooster() {
        val map = LevelMap(listOf(".......", "..R.R..", ".RB.BR.", "..R.R..", "......."))
        val level = Level(2, 1, "Test", Strength.EASY, 2, map, Mission.Win, mapOf(Booster.WALL to 1, Booster.UNDO to 1), stars = Stars(StarMeasure.MARGIN, two = 2, three = 3))
        val record = LevelRecord(level, "wall:g1 b1 a1 wall:a5 d3 g5".split(' ').map(Action::parse), ad = Booster.WALL)
        assertEquals("the same game without the ad earns 2", 2, level.result(record.replay()).stars)
        val vm = GameViewModel(
            start = GameLogEntry.start(record, level.strength.text, threatHighlight = false, now = 0).copy(record = record),
            nextGame = { levelGame(level, threatHighlight = false, now = 0) },
            computerFor = { firstFreePoint },
            campaign = CampaignJournal { _: Level, result: LevelResult -> LevelEnd(result, CampaignMap.Opened.NOTHING, 0, null) },
            computerDispatcher = Dispatchers.Unconfined,
            replyDelayMillis = 0,
        )
        show { GameScreen(vm, onToMap = {}, onRules = {}) }
        waitFor("You won!")
        assertEquals(1, vm.ui.value.levelEnd?.result?.stars)
        compose.onNodeWithContentDescription("1 of 3 stars").assertExists()
        assertTrue(compose.onAllNodes(hasText("Two stars need", substring = true)).fetchSemanticsNodes().isEmpty())
        capture("result_level_win_ad_capped")
    }

    private fun freePlay(name: String, theme: GameTheme = GameTheme.NOTEBOOK) {
        val vm = gameViewModel(
            newSettings = { GameSettings(width = 5, height = 5, dotsPerPlayer = 1, opening = Opening.EMPTY) },
            newComputer = { firstFreePoint },
            computerDispatcher = Dispatchers.Unconfined,
            replyDelayMillis = 0,
        )
        show(theme) { GameScreen(vm, onToMap = {}, onRules = {}) }
        play(vm, Pos(2, 2))
        waitFor("A draw")
        capture(name)
    }

    @Test
    fun resultFreePlay() = freePlay("result_free_play")

    @Test
    fun resultFreePlayStarChart() = freePlay("result_free_play_star_chart", GameTheme.STAR_CHART)

    @Test
    fun resultFreePlayNeonArcade() = freePlay("result_free_play_neon_arcade", GameTheme.NEON_ARCADE)
}
