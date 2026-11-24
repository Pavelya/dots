package io.github.pavelya.dots.ui.screen

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dots.campaign.Campaign
import dots.campaign.Cosmetic
import dots.campaign.Progress
import io.github.pavelya.dots.data.LookPart
import io.github.pavelya.dots.data.PieceStyleId
import io.github.pavelya.dots.data.ThemeId
import io.github.pavelya.dots.shop.BuyOutcome
import io.github.pavelya.dots.shop.Owned
import io.github.pavelya.dots.shop.Selling
import io.github.pavelya.dots.shop.StoreState
import io.github.pavelya.dots.ui.theme.DotsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Shop and the Cosmetic preview (docs/design/flows.md, "Shop and rewarded ads"; money.md): the
 * words of each state, where Google Play can't sell, and what each button does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h900dp")
class ShopScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val selling = StoreState(selling = Selling.YES, prices = mapOf(Cosmetic.STAR_CHART to "$2.99", Cosmetic.NEON_ARCADE to "$2.99", Cosmetic.FLAME_AND_FROST to "$0.99"))
    private val tabs = mutableListOf<ShopTab>()
    private val items = mutableListOf<LookPart>()
    private var restores = 0
    private var buys = 0
    private var uses = 0

    private fun shop(view: ShopView, start: ShopTab = ShopTab.THEMES) {
        compose.setContent {
            var tab by remember { mutableStateOf(start) }
            DotsTheme(dark = false) {
                ShopScreen(view, tab, onTab = { tabs += it; tab = it }, onItem = { items += it }, onRestore = { restores++ }, onBack = {})
            }
        }
    }

    private var shown: MutableState<Pair<LookPart, ShopView>>? = null

    /** Shows the preview of [part]; a second call changes what the same screen shows, as the app's state would. */
    private fun preview(part: LookPart, view: ShopView) {
        val now = shown
        if (now == null) {
            val state = mutableStateOf(part to view)
            shown = state
            compose.setContent {
                val (item, seen) = state.value
                DotsTheme(dark = false) { CosmeticPreviewScreen(item, seen, onBuy = { buys++ }, onUse = { uses++ }, onBack = {}) }
            }
        } else {
            compose.runOnUiThread {
                now.value = part to view
                Snapshot.sendApplyNotifications()
            }
            compose.waitForIdle()
        }
    }

    @Test
    fun `a tile shows the price and the stars that earn the item, and opens its preview`() {
        shop(ShopView(selling, Owned(emptySet(), setOf(Cosmetic.NEON_ARCADE)), stars = 112))
        compose.onNodeWithText("In use").assertExists()
        compose.onNodeWithText("$2.99").assertExists()
        compose.onNodeWithText("Or earn: 112 / 150 ★").assertExists()
        compose.onNodeWithText("Owned").assertExists()
        compose.onNodeWithText("Every item here is earned with stars from the campaign.").assertDoesNotExist()

        compose.onNode(hasText("Star chart")).performClick()
        assertEquals(listOf<LookPart>(ThemeId.STAR_CHART), items)
        compose.onNodeWithText("Restore purchases").performClick()
        assertEquals(1, restores)
    }

    @Test
    fun `the tabs show the themes, the piece styles and the ring effects, each default first`() {
        shop(ShopView(selling, Owned.NONE, stars = 30))
        compose.onNode(hasText("Themes")).assertIsSelected()
        compose.onNode(hasText("Pieces")).performClick()
        compose.onNode(hasText("Pieces")).assertIsSelected()
        compose.onNodeWithText("Hearts and clubs").assertExists()
        compose.onNodeWithText("Or earn: 30 / 100 ★").assertExists()
        compose.onNode(hasText("Rings")).performClick()
        compose.onNodeWithText("Rubber stamp").assertExists()
        assertEquals(listOf(ShopTab.PIECES, ShopTab.RINGS), tabs)
    }

    @Test
    fun `an earned item says Earned, marked New until its preview is opened`() {
        shop(ShopView(selling, Owned(Cosmetic.earnedAt(152), emptySet()), stars = 152, fresh = setOf(Cosmetic.STAR_CHART)))
        compose.onNodeWithText("Earned").assertExists()
        compose.onNodeWithText("New").assertExists()
        compose.onNodeWithText("Or earn: 152 / 200 ★").assertExists()
    }

    @Test
    fun `with billing unavailable, items show only how to earn them`() {
        val earnOnly = ShopView(StoreState(selling = Selling.EARN_ONLY, prices = selling.prices), Owned.NONE, stars = 112)
        shop(earnOnly)
        compose.onNodeWithText("Every item here is earned with stars from the campaign.").assertExists()
        compose.onNodeWithText("Earn: 112 / 150 ★").assertExists()
        compose.onNodeWithText("Earn: 112 / 200 ★").assertExists()
        compose.onNodeWithText("$2.99").assertDoesNotExist()
        compose.onNodeWithText("Restore purchases").assertDoesNotExist()
    }

    @Test
    fun `with billing unavailable, a preview shows only how to earn the item`() {
        val earnOnly = ShopView(StoreState(selling = Selling.EARN_ONLY, prices = selling.prices), Owned.NONE, stars = 112)
        preview(ThemeId.STAR_CHART, earnOnly)
        compose.onNodeWithText("Earn it with 150 stars from the campaign. You have 112: 38 to go.").assertExists()
        compose.onNodeWithText("112 of 150 stars").assertExists()
        compose.onNode(hasText("Buy", substring = true)).assertDoesNotExist()
        compose.onNodeWithText("Use it").assertDoesNotExist()
    }

    @Test
    fun `a preview for sale offers Buy, and the earning`() {
        preview(ThemeId.STAR_CHART, ShopView(selling, Owned.NONE, stars = 112))
        compose.onNodeWithText("Gold stars and ice-blue comets on a chart of the sky.").assertExists()
        compose.onNodeWithText("Or earn it with 150 stars. You have 112.").assertExists()
        compose.onNodeWithContentDescription("A sample board with Star chart").assertExists()
        compose.onNodeWithText("Buy for $2.99").performClick()
        assertEquals(1, buys)
    }

    @Test
    fun `an item owned can be put in use, and one in use says so`() {
        preview(ThemeId.NEON_ARCADE, ShopView(selling, Owned(emptySet(), setOf(Cosmetic.NEON_ARCADE)), stars = 112))
        compose.onNodeWithText("Owned").assertExists()
        compose.onNodeWithText("Use it").performClick()
        assertEquals(1, uses)

        preview(ThemeId.NEON_ARCADE, ShopView(selling, Owned(emptySet(), setOf(Cosmetic.NEON_ARCADE)), stars = 112, theme = ThemeId.NEON_ARCADE))
        compose.onNodeWithText("Use it").assertDoesNotExist()
        compose.onNode(hasText("Buy", substring = true)).assertDoesNotExist()
    }

    @Test
    fun `an earned item's preview says how it was earned`() {
        preview(ThemeId.STAR_CHART, ShopView(selling, Owned(Cosmetic.earnedAt(152), emptySet()), stars = 152, fresh = setOf(Cosmetic.STAR_CHART)))
        compose.onNodeWithText("Earned with 150 stars").assertExists()
        compose.onNodeWithText("New").assertExists()
        compose.onNodeWithText("Use it").assertExists()
    }

    @Test
    fun `the Standard pieces are always owned`() {
        preview(PieceStyleId.STANDARD, ShopView(selling, Owned.NONE, stars = 0, pieces = PieceStyleId.HEARTS_AND_CLUBS))
        compose.onNodeWithText("Owned").assertExists()
        compose.onNodeWithText("Use it").assertExists()
    }

    @Test
    fun `after a Buy the preview says whether it is pending, cancelled or failed, and a failure offers Retry`() {
        fun after(outcome: BuyOutcome) = preview(ThemeId.STAR_CHART, ShopView(selling.copy(outcomes = mapOf(Cosmetic.STAR_CHART to outcome)), Owned.NONE, stars = 112))

        after(BuyOutcome.PENDING)
        compose.onNodeWithText("Payment pending").assertExists()
        compose.onNodeWithText("Google Play is waiting for the payment. The item is yours once it goes through.").assertExists()
        compose.onNode(hasText("Buy", substring = true)).assertDoesNotExist()

        after(BuyOutcome.CANCELLED)
        compose.onNodeWithText("Purchase cancelled.").assertExists()
        compose.onNodeWithText("Buy for $2.99").assertExists()

        after(BuyOutcome.FAILED)
        compose.onNodeWithText("The purchase didn’t go through.").assertExists()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(1, buys)
    }

    @Test
    fun `the map's Shop icon says something new, for TalkBack too`() {
        val noActions = MapActions(onLevel = {}, onDaily = {}, onPlay = {}, onRules = {}, onStats = {}, onShop = {}, onSettings = {})
        compose.setContent { DotsTheme(dark = false) { MapScreen(Campaign.map(Progress(mapOf("1-1" to 3))), noActions, shopNew = true) } }
        compose.onNodeWithContentDescription("Shop, something new").assertExists()
    }
}
