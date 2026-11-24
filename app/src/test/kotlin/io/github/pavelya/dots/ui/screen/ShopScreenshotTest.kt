package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dots.campaign.Campaign
import dots.campaign.Cosmetic
import dots.campaign.Progress
import io.github.pavelya.dots.data.LookPart
import io.github.pavelya.dots.data.PieceStyleId
import io.github.pavelya.dots.data.RingEffectId
import io.github.pavelya.dots.data.ThemeId
import io.github.pavelya.dots.shop.BuyOutcome
import io.github.pavelya.dots.shop.Owned
import io.github.pavelya.dots.shop.Selling
import io.github.pavelya.dots.shop.StoreState
import io.github.pavelya.dots.ui.theme.DotsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Shop, the Cosmetic preview and the map's red dot (the wireframes' "Ad offers and earning"
 * row, D-28: MoneyShop, MoneyShopEarned, MoneyShopEarnOnly, MoneyPreviewSale, MoneyPreviewEarned,
 * MoneyPreviewEarnOnly, MoneyMapNew; and the D-29 Shop tabs `shop-pieces` and `shop-rings`),
 * shown next to their frames in the PR. As on the frames, the player has 112 stars (152 once a
 * theme is earned; 30 on the Pieces and Rings tabs, so their items are for sale) and Notebook in use. Reference images in app/src/test/screenshots; they are
 * recorded in CI (CLAUDE.md, "Recording screenshots").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class ShopScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /** Draws [content] as the phone would: 24 dp of status bar at the top, 12 dp of gesture bar at the bottom. */
    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            DotsTheme(dark = dark) {
                Surface { Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp)) { content() } }
            }
        }
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")

    /** Google Play selling at the base prices (money.md). */
    private val selling = StoreState(
        selling = Selling.YES,
        prices = Cosmetic.entries.associateWith { if (it.kind == Cosmetic.Kind.THEME) "$2.99" else "$0.99" },
    )

    /** The D-28 Shop "for sale": 112 stars (which earn Hearts and clubs, Lasso and Flame and frost) and Neon arcade bought. */
    private val forSale = ShopView(
        store = selling,
        owned = Owned(earned = Cosmetic.earnedAt(112), bought = setOf(Cosmetic.NEON_ARCADE)),
        stars = 112,
    )

    /**
     * The D-29 Shop tabs: 30 stars, too few for any piece style or ring effect; Hearts and clubs
     * and Rubber stamp bought, the others for sale.
     */
    private val fewStars = ShopView(
        store = selling,
        owned = Owned(earned = emptySet(), bought = setOf(Cosmetic.HEARTS_AND_CLUBS, Cosmetic.RUBBER_STAMP)),
        stars = 30,
    )

    /** 152 stars: Star chart just earned, its preview not opened yet. Hearts and clubs, Lasso and Flame and frost earned and seen. */
    private val themeEarned = ShopView(
        store = selling,
        owned = Owned(earned = Cosmetic.earnedAt(152), bought = emptySet()),
        stars = 152,
        fresh = setOf(Cosmetic.STAR_CHART),
    )

    /** Where Google Play can't sell. */
    private val earnOnly = ShopView(store = StoreState(selling = Selling.EARN_ONLY), owned = Owned.NONE, stars = 112)

    private fun shop(view: ShopView, tab: ShopTab = ShopTab.THEMES, dark: Boolean = false) = show(dark) {
        ShopScreen(view, tab, onTab = {}, onItem = {}, onRestore = {}, onBack = {})
    }

    private fun preview(part: LookPart, view: ShopView, dark: Boolean = false) = show(dark) {
        CosmeticPreviewScreen(part, view, onBuy = {}, onUse = {}, onBack = {})
    }

    @Test
    fun `shop, for sale`() {
        shop(forSale)
        capture("shop_for_sale")
    }

    @Test
    fun `shop, for sale, dark`() {
        shop(forSale, dark = true)
        capture("shop_for_sale_dark")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun `shop on a small phone`() {
        shop(forSale)
        capture("shop_small_phone")
    }

    @Test
    fun `shop, a theme earned`() {
        shop(themeEarned)
        capture("shop_theme_earned")
    }

    @Test
    fun `shop, earn only`() {
        shop(earnOnly)
        capture("shop_earn_only")
    }

    @Test
    fun `shop, pieces`() {
        shop(fewStars, ShopTab.PIECES)
        capture("shop_pieces")
    }

    @Test
    fun `shop, rings`() {
        shop(fewStars, ShopTab.RINGS)
        capture("shop_rings")
    }

    /** The piece styles drawn in the theme in use: Star chart's inks. */
    @Test
    fun `shop, pieces in Star chart`() {
        shop(fewStars.copy(owned = fewStars.owned.copy(bought = fewStars.owned.bought + Cosmetic.STAR_CHART), theme = ThemeId.STAR_CHART), ShopTab.PIECES)
        capture("shop_pieces_star_chart")
    }

    @Test
    fun `preview, for sale`() {
        preview(ThemeId.STAR_CHART, forSale)
        capture("preview_for_sale")
    }

    @Test
    fun `preview, for sale, dark`() {
        preview(ThemeId.STAR_CHART, forSale, dark = true)
        capture("preview_for_sale_dark")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun `preview on a small phone`() {
        preview(ThemeId.STAR_CHART, forSale)
        capture("preview_small_phone")
    }

    @Test
    fun `preview, owned`() {
        preview(ThemeId.NEON_ARCADE, forSale)
        capture("preview_owned")
    }

    @Test
    fun `preview, in use`() {
        preview(ThemeId.NEON_ARCADE, forSale.copy(theme = ThemeId.NEON_ARCADE))
        capture("preview_in_use")
    }

    @Test
    fun `preview, earned`() {
        preview(ThemeId.STAR_CHART, themeEarned)
        capture("preview_earned")
    }

    @Test
    fun `preview, earn only`() {
        preview(ThemeId.STAR_CHART, earnOnly)
        capture("preview_earn_only")
    }

    @Test
    fun `preview, a piece style for sale`() {
        preview(PieceStyleId.FLAME_AND_FROST, fewStars)
        capture("preview_pieces_for_sale")
    }

    @Test
    fun `preview, a ring effect for sale`() {
        preview(RingEffectId.LASSO, fewStars)
        capture("preview_rings_for_sale")
    }

    @Test
    fun `preview, payment pending`() {
        preview(ThemeId.STAR_CHART, forSale.copy(store = selling.copy(outcomes = mapOf(Cosmetic.STAR_CHART to BuyOutcome.PENDING))))
        capture("preview_pending")
    }

    @Test
    fun `preview, purchase failed`() {
        preview(ThemeId.STAR_CHART, forSale.copy(store = selling.copy(outcomes = mapOf(Cosmetic.STAR_CHART to BuyOutcome.FAILED))))
        capture("preview_failed")
    }

    @Test
    fun `preview, purchase cancelled`() {
        preview(ThemeId.STAR_CHART, forSale.copy(store = selling.copy(outcomes = mapOf(Cosmetic.STAR_CHART to BuyOutcome.CANCELLED))))
        capture("preview_cancelled")
    }

    /** The D-28 frame MoneyMapNew: an item earned, so a red dot on Shop. */
    @Test
    fun `map, something new in the Shop`() {
        val progress = Progress(Campaign.levels.take(15).associate { it.code to 3 })
        val noActions = MapActions(onLevel = {}, onDaily = {}, onPlay = {}, onRules = {}, onStats = {}, onShop = {}, onSettings = {})
        show { MapScreen(Campaign.map(progress), noActions, shopNew = true) }
        capture("map_shop_new")
    }
}
