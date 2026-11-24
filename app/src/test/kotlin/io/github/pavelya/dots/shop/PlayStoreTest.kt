package io.github.pavelya.dots.shop

import android.app.Activity
import com.android.billingclient.api.BillingClient.BillingResponseCode
import dots.campaign.Cosmetic
import dots.campaign.Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * The Shop's Google Play (docs/design/money.md, "Owning an item" and "Prices by country"), against
 * a Google Play that answers with the billing library's response codes: buying, acknowledging,
 * restoring, refunds, an item also earned, and where Google Play can't sell.
 */
@RunWith(RobolectricTestRunner::class)
class PlayStoreTest {
    /** A Google Play that answers at once with what the test sets. */
    private class FakePlay : BillingGateway {
        var setup = BillingResponseCode.OK
        var country: String? = "PL"
        var pricesCode = BillingResponseCode.OK
        var prices = mapOf("star_chart" to "11,99 zł", "neon_arcade" to "11,99 zł", "lasso" to "3,99 zł")
        var purchasesCode = BillingResponseCode.OK
        var purchases = emptyList<PlayPurchase>()
        var launchCode = BillingResponseCode.OK
        val acknowledged = mutableListOf<String>()
        val launched = mutableListOf<String>()
        private var listener: (Int, List<PlayPurchase>) -> Unit = { _, _ -> }

        override fun listen(listener: (code: Int, purchases: List<PlayPurchase>) -> Unit) {
            this.listener = listener
        }

        override suspend fun connect() = setup
        override suspend fun country() = country
        override suspend fun prices(productIds: List<String>) = Answer(pricesCode, prices.filterKeys { it in productIds }.takeIf { pricesCode == BillingResponseCode.OK }.orEmpty())
        override suspend fun purchases() = Answer(purchasesCode, purchases)
        override suspend fun acknowledge(token: String): Int {
            acknowledged += token
            return BillingResponseCode.OK
        }

        override fun launch(activity: Activity, productId: String): Int {
            launched += productId
            return launchCode
        }

        /** Google Play's purchase screen closes with [code] and the [purchases] it brought. */
        fun closes(code: Int, purchases: List<PlayPurchase> = emptyList()) = listener(code, purchases)
    }

    /** The purchases kept on the phone. */
    private class Memory(var saved: Set<Cosmetic> = emptySet()) : PurchaseCache {
        override suspend fun read() = saved
        override suspend fun save(bought: Set<Cosmetic>) {
            saved = bought
        }
    }

    private val play = FakePlay()
    private val cache = Memory()

    /** Every answer comes at once, so each call has finished when it returns. */
    private fun store(cached: Memory = cache) = PlayStore(play, cached, CoroutineScope(Dispatchers.Unconfined))

    private val activity: Activity get() = Robolectric.buildActivity(Activity::class.java).get()

    private fun paid(id: String, acknowledged: Boolean = false) = PlayPurchase(listOf(id), paid = true, pending = false, token = "token-$id", acknowledged = acknowledged)

    private fun pending(id: String) = PlayPurchase(listOf(id), paid = false, pending = true, token = "token-$id", acknowledged = false)

    @Test
    fun `where Google Play sells, the Shop has its prices in the player's currency`() {
        val store = store().apply { refresh() }
        assertEquals(Selling.YES, store.state.value.selling)
        assertEquals("11,99 zł", store.state.value.price(Cosmetic.STAR_CHART))
        assertEquals("3,99 zł", store.state.value.price(Cosmetic.LASSO))
        assertNull("not found, so not for sale", store.state.value.price(Cosmetic.RUBBER_STAMP))
    }

    @Test
    fun `every cosmetic is a product, with underscores for its hyphens`() {
        assertEquals(listOf("hearts_and_clubs", "lasso", "flame_and_frost", "rubber_stamp", "star_chart", "neon_arcade"), PlayStore.PRODUCTS)
        for (cosmetic in Cosmetic.entries) assertEquals(cosmetic, cosmeticOfProduct(cosmetic.productId))
    }

    @Test
    fun `a purchase is owned at once, acknowledged, and remembered for offline`() {
        val store = store().apply { refresh() }
        store.buy(activity, Cosmetic.STAR_CHART)
        assertEquals(listOf("star_chart"), play.launched)
        play.closes(BillingResponseCode.OK, listOf(paid("star_chart")))

        assertEquals(setOf(Cosmetic.STAR_CHART), store.state.value.bought)
        assertEquals(listOf("token-star_chart"), play.acknowledged)
        assertEquals(setOf(Cosmetic.STAR_CHART), cache.saved)

        // Offline on the next start: Google Play can't be reached, the purchase still counts.
        play.setup = BillingResponseCode.SERVICE_UNAVAILABLE
        val offline = store().apply { refresh() }
        assertEquals(setOf(Cosmetic.STAR_CHART), offline.state.value.bought)
        assertEquals(Selling.UNKNOWN, offline.state.value.selling)
    }

    @Test
    fun `restoring on a new phone brings the purchases back, and acknowledges only those not acknowledged`() {
        play.purchases = listOf(paid("lasso", acknowledged = true), paid("neon_arcade"))
        val store = store(Memory()).apply { refresh() }
        assertEquals(setOf(Cosmetic.LASSO, Cosmetic.NEON_ARCADE), store.state.value.bought)
        assertEquals(listOf("token-neon_arcade"), play.acknowledged)
    }

    @Test
    fun `a refund takes the purchase away, and an item also earned stays owned`() {
        play.purchases = listOf(paid("star_chart", acknowledged = true), paid("lasso", acknowledged = true))
        val store = store().apply { refresh() }
        val earned = Progress(mapOf("1-1" to 3)).copy(cosmetics = setOf(Cosmetic.STAR_CHART.id))
        assertTrue(Owned.of(earned, store.state.value).owns(Cosmetic.LASSO))

        // Both refunded: Google Play no longer lists them.
        play.purchases = emptyList()
        store.refresh()
        assertEquals(emptySet<Cosmetic>(), store.state.value.bought)
        assertEquals("the phone forgets them too", emptySet<Cosmetic>(), cache.saved)
        val owned = Owned.of(earned, store.state.value)
        assertTrue("earned from a chest, so still owned", owned.owns(Cosmetic.STAR_CHART))
        assertFalse("only bought, so gone", owned.owns(Cosmetic.LASSO))
    }

    @Test
    fun `an item earned with stars stays owned after a refund of the same item`() {
        play.purchases = listOf(paid("hearts_and_clubs", acknowledged = true))
        val store = store().apply { refresh() }
        // 45 stars earn Hearts and clubs (40).
        val progress = Progress((1..15).associate { "1-$it" to 3 })
        assertTrue(Cosmetic.HEARTS_AND_CLUBS in progress.earned)

        play.purchases = emptyList()
        store.refresh()
        assertTrue(Owned.of(progress, store.state.value).owns(Cosmetic.HEARTS_AND_CLUBS))
    }

    @Test
    fun `with billing unavailable, the Shop only shows how to earn`() {
        play.setup = BillingResponseCode.BILLING_UNAVAILABLE
        val store = store().apply { refresh() }
        assertEquals(Selling.EARN_ONLY, store.state.value.selling)
        assertNull(store.state.value.price(Cosmetic.STAR_CHART))
    }

    @Test
    fun `in Russia and Belarus the Shop only shows how to earn, and purchases made elsewhere still count`() {
        for (country in listOf("RU", "by")) {
            play.country = country
            play.purchases = listOf(paid("star_chart", acknowledged = true))
            val store = store(Memory()).apply { refresh() }
            assertEquals(country, Selling.EARN_ONLY, store.state.value.selling)
            assertNull(country, store.state.value.price(Cosmetic.STAR_CHART))
            assertEquals(country, setOf(Cosmetic.STAR_CHART), store.state.value.bought)
        }
    }

    @Test
    fun `where Google Play has no product on sale, the Shop only shows how to earn`() {
        play.prices = emptyMap()
        val store = store().apply { refresh() }
        assertEquals(Selling.EARN_ONLY, store.state.value.selling)
    }

    @Test
    fun `an answer that doesn't come keeps what was known`() {
        val store = store().apply { refresh() }
        play.pricesCode = BillingResponseCode.NETWORK_ERROR
        play.purchasesCode = BillingResponseCode.NETWORK_ERROR
        play.purchases = emptyList()
        store.buy(activity, Cosmetic.LASSO)
        play.closes(BillingResponseCode.OK, listOf(paid("lasso", acknowledged = true)))
        store.refresh()
        assertEquals(Selling.YES, store.state.value.selling)
        assertEquals("11,99 zł", store.state.value.price(Cosmetic.STAR_CHART))
        assertEquals("not dropped while Google Play can't list purchases", setOf(Cosmetic.LASSO), store.state.value.bought)
    }

    @Test
    fun `a cancelled or failed Buy says so, and a failure can be tried again`() {
        val store = store().apply { refresh() }
        store.buy(activity, Cosmetic.NEON_ARCADE)
        play.closes(BillingResponseCode.USER_CANCELED)
        assertEquals(BuyOutcome.CANCELLED, store.state.value.outcomes[Cosmetic.NEON_ARCADE])

        store.buy(activity, Cosmetic.NEON_ARCADE)
        assertNull("a new Buy clears the last outcome", store.state.value.outcomes[Cosmetic.NEON_ARCADE])
        play.closes(BillingResponseCode.ERROR)
        assertEquals(BuyOutcome.FAILED, store.state.value.outcomes[Cosmetic.NEON_ARCADE])

        play.launchCode = BillingResponseCode.SERVICE_UNAVAILABLE
        store.buy(activity, Cosmetic.LASSO)
        assertEquals("the purchase screen didn't open", BuyOutcome.FAILED, store.state.value.outcomes[Cosmetic.LASSO])

        play.launchCode = BillingResponseCode.OK
        store.buy(activity, Cosmetic.NEON_ARCADE)
        play.closes(BillingResponseCode.OK, listOf(paid("neon_arcade")))
        assertEquals(setOf(Cosmetic.NEON_ARCADE), store.state.value.bought)
        assertNull(store.state.value.outcomes[Cosmetic.NEON_ARCADE])
    }

    @Test
    fun `a pending payment is not owned until it is paid, and one declined goes back on sale`() {
        val store = store().apply { refresh() }
        store.buy(activity, Cosmetic.STAR_CHART)
        play.closes(BillingResponseCode.OK, listOf(pending("star_chart")))
        assertEquals(BuyOutcome.PENDING, store.state.value.outcomes[Cosmetic.STAR_CHART])
        assertFalse(Cosmetic.STAR_CHART in store.state.value.bought)
        assertTrue("nothing to acknowledge yet", play.acknowledged.isEmpty())

        // Still pending at the next start; then paid.
        play.purchases = listOf(pending("star_chart"))
        store.refresh()
        assertEquals(BuyOutcome.PENDING, store.state.value.outcomes[Cosmetic.STAR_CHART])
        play.purchases = listOf(paid("star_chart"))
        store.refresh()
        assertEquals(setOf(Cosmetic.STAR_CHART), store.state.value.bought)
        assertNull(store.state.value.outcomes[Cosmetic.STAR_CHART])
        assertEquals(listOf("token-star_chart"), play.acknowledged)

        // Another one pending, then declined: Google Play no longer lists it.
        play.purchases = listOf(paid("star_chart", acknowledged = true), pending("lasso"))
        store.refresh()
        assertEquals(BuyOutcome.PENDING, store.state.value.outcomes[Cosmetic.LASSO])
        play.purchases = listOf(paid("star_chart", acknowledged = true))
        store.refresh()
        assertNull(store.state.value.outcomes[Cosmetic.LASSO])
        assertFalse(Cosmetic.LASSO in store.state.value.bought)
    }

    @Test
    fun `an item already owned on the account comes back instead of an error`() {
        val store = store().apply { refresh() }
        play.purchases = listOf(paid("neon_arcade", acknowledged = true))
        store.buy(activity, Cosmetic.NEON_ARCADE)
        play.closes(BillingResponseCode.ITEM_ALREADY_OWNED)
        assertEquals(setOf(Cosmetic.NEON_ARCADE), store.state.value.bought)
        assertNull(store.state.value.outcomes[Cosmetic.NEON_ARCADE])
    }
}
