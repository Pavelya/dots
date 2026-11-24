package io.github.pavelya.dots.shop

import android.app.Activity
import com.android.billingclient.api.BillingClient.BillingResponseCode
import dots.campaign.Cosmetic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One purchase as Google Play lists it: its [products], whether it is [paid] or [pending], its [token]. */
data class PlayPurchase(
    val products: List<String>,
    val paid: Boolean,
    val pending: Boolean,
    val token: String,
    val acknowledged: Boolean,
)

/** A [BillingResponseCode] and what came with it. */
data class Answer<T>(val code: Int, val value: T)

/**
 * Google Play Billing as [PlayStore] uses it. [PlayBilling] is the real one; the tests use a fake
 * one that answers with the billing library's response codes.
 */
interface BillingGateway {
    /** [listener] hears how each Buy ends: a response code and the purchases it brought. */
    fun listen(listener: (code: Int, purchases: List<PlayPurchase>) -> Unit)

    /** Connects to Google Play, if not connected yet; the setup's response code. */
    suspend fun connect(): Int

    /** The two-letter country of the player's Google Play account, or null if it is not known. */
    suspend fun country(): String?

    /** The price of each of [productIds] that Google Play sells here, by product ID, as Google Play writes it. */
    suspend fun prices(productIds: List<String>): Answer<Map<String, String>>

    /** The one-time purchases the player's Google Play account holds: paid and pending, not refunded. */
    suspend fun purchases(): Answer<List<PlayPurchase>>

    /** Tells Google Play the game has given the purchase of [token]; unacknowledged ones are refunded after three days. */
    suspend fun acknowledge(token: String): Int

    /** Opens Google Play's purchase screen for [productId], one [prices] found. Runs on the main thread. */
    fun launch(activity: Activity, productId: String): Int
}

/**
 * The Shop's Google Play (docs/design/money.md, "Owning an item"; flows.md, "Shop and rewarded
 * ads"). What Google Play confirms as bought is kept in [cache], so owned items work offline, and
 * replaced by each new answer, so a refunded item drops out. Purchases are acknowledged as soon as
 * they are seen. Where Google Play can't sell ([Selling.EARN_ONLY]: no Google Play billing for the
 * account, a country without sales, or no product on sale), the purchases Google Play still lists
 * keep counting as bought.
 */
class PlayStore(
    private val play: BillingGateway,
    private val cache: PurchaseCache,
    private val scope: CoroutineScope,
) : Store {
    private val shown = MutableStateFlow(StoreState())
    override val state: StateFlow<StoreState> = shown.asStateFlow()

    /** One talk with Google Play at a time, so answers are applied in order. */
    private val talking = Mutex()
    private var cacheRead = false

    /** The item of the latest Buy, which a cancel or a failure belongs to. */
    @Volatile
    private var buying: Cosmetic? = null

    init {
        play.listen { code, purchases -> scope.launch { talking.withLock { bought(code, purchases) } } }
        scope.launch { talking.withLock { readCache() } }
    }

    override fun refresh() {
        scope.launch {
            talking.withLock {
                readCache()
                sync()
            }
        }
    }

    override fun buy(activity: Activity, cosmetic: Cosmetic) {
        buying = cosmetic
        shown.update { it.copy(outcomes = it.outcomes - cosmetic) }
        when (play.launch(activity, cosmetic.productId)) {
            BillingResponseCode.OK -> Unit
            BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            BillingResponseCode.USER_CANCELED -> outcome(cosmetic, BuyOutcome.CANCELLED)
            else -> outcome(cosmetic, BuyOutcome.FAILED)
        }
    }

    private suspend fun readCache() {
        if (cacheRead) return
        cacheRead = true
        val saved = cache.read()
        shown.update { it.copy(bought = it.bought + saved) }
    }

    /** Asks for the prices and the purchases again. An answer that doesn't come keeps what was known. */
    private suspend fun sync() {
        when (play.connect()) {
            BillingResponseCode.OK -> Unit
            BillingResponseCode.BILLING_UNAVAILABLE -> {
                shown.update { it.copy(selling = Selling.EARN_ONLY, prices = emptyMap()) }
                return
            }
            else -> return
        }
        val country = play.country()?.uppercase()
        val prices = play.prices(PRODUCTS)
        val found = prices.value.mapNotNull { (id, price) -> cosmeticOfProduct(id)?.let { it to price } }.toMap()
        val selling = when {
            country in NO_SALES -> Selling.EARN_ONLY
            prices.code == BillingResponseCode.BILLING_UNAVAILABLE -> Selling.EARN_ONLY
            prices.code != BillingResponseCode.OK -> null
            found.isEmpty() -> Selling.EARN_ONLY
            else -> Selling.YES
        }
        shown.update { it.copy(selling = selling ?: it.selling, prices = if (prices.code == BillingResponseCode.OK) found else it.prices) }
        val listed = play.purchases()
        if (listed.code == BillingResponseCode.OK) listedNow(listed.value)
    }

    /** Google Play's whole list of purchases: what it lists as paid is all that is bought. */
    private suspend fun listedNow(purchases: List<PlayPurchase>) {
        val paid = purchases.filter { it.paid }
        val bought = paid.cosmetics()
        val pending = purchases.filter { it.pending }.cosmetics() - bought
        cache.save(bought)
        shown.update { state ->
            // A pending purchase no longer listed was declined: its item is for sale again, with no message.
            val kept = state.outcomes.filter { (item, outcome) -> item !in bought && outcome != BuyOutcome.PENDING }
            state.copy(bought = bought, outcomes = kept + pending.associateWith { BuyOutcome.PENDING })
        }
        acknowledge(paid)
    }

    /** How a Buy ended. */
    private suspend fun bought(code: Int, purchases: List<PlayPurchase>) {
        when (code) {
            BillingResponseCode.OK -> {
                val paid = purchases.filter { it.paid }
                val added = paid.cosmetics()
                val pending = purchases.filter { it.pending }.cosmetics() - added
                val all = shown.value.bought + added
                if (added.isNotEmpty()) cache.save(all)
                shown.update { it.copy(bought = all, outcomes = it.outcomes - added + pending.associateWith { BuyOutcome.PENDING }) }
                acknowledge(paid)
            }
            BillingResponseCode.USER_CANCELED -> buying?.let { outcome(it, BuyOutcome.CANCELLED) }
            // Bought before, on this account: Google Play's list says so.
            BillingResponseCode.ITEM_ALREADY_OWNED -> sync()
            else -> buying?.let { outcome(it, BuyOutcome.FAILED) }
        }
    }

    private fun outcome(cosmetic: Cosmetic, outcome: BuyOutcome) {
        shown.update { if (cosmetic in it.bought) it else it.copy(outcomes = it.outcomes + (cosmetic to outcome)) }
    }

    /** Acknowledges each of [paid] not acknowledged yet; one that fails is tried again on the next refresh. */
    private suspend fun acknowledge(paid: List<PlayPurchase>) {
        for (purchase in paid) if (!purchase.acknowledged) play.acknowledge(purchase.token)
    }

    private fun List<PlayPurchase>.cosmetics(): Set<Cosmetic> = flatMap { it.products }.mapNotNullTo(mutableSetOf(), ::cosmeticOfProduct)

    companion object {
        /** Every item the Shop sells, by product ID. */
        val PRODUCTS: List<String> = Cosmetic.entries.map { it.productId }

        /** Where Google Play takes no payments (money.md, "Prices by country"): Russia and Belarus. */
        val NO_SALES = setOf("RU", "BY")
    }
}

/** The purchases Google Play last confirmed, kept on the phone so owned items work offline. */
interface PurchaseCache {
    suspend fun read(): Set<Cosmetic>

    suspend fun save(bought: Set<Cosmetic>)
}
