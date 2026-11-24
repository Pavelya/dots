package io.github.pavelya.dots.shop

import android.app.Activity
import dots.campaign.Cosmetic
import dots.campaign.Progress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Where the Shop sells (docs/design/money.md): Google Play, or nowhere. The game reads what was
 * bought from it and asks it to sell; it never writes a purchase into the saved progress.
 */
interface Store {
    /** What Google Play has said so far. */
    val state: StateFlow<StoreState>

    /**
     * Asks Google Play again for the prices and the purchases: when the app comes back on screen,
     * when the Shop opens, and from Restore purchases. A refund shows up here, as a purchase that
     * is no longer listed.
     */
    fun refresh()

    /** Opens Google Play's purchase screen for [cosmetic] over [activity]; the outcome comes in [state]. */
    fun buy(activity: Activity, cosmetic: Cosmetic)
}

/** Whether Google Play sells here (money.md, "Prices by country"). */
enum class Selling {
    /** Not known yet: Google Play has not answered, or can't be reached now (offline). */
    UNKNOWN,

    /** Google Play sells, and [StoreState.prices] holds the prices. */
    YES,

    /**
     * Google Play can't sell here: Russia and Belarus, a phone without Google Play, or billing
     * unavailable to this account. Every item shows only how to earn it.
     */
    EARN_ONLY,
}

/** How the last Buy of an item ended, while it is not owned (flows.md, "Shop and rewarded ads"). */
enum class BuyOutcome {
    /** Google Play waits for the payment (cash at a shop, say); the item is owned once it is paid. */
    PENDING,

    /** The player closed Google Play's purchase screen. */
    CANCELLED,

    /** Google Play could not take the payment; the preview offers Retry. */
    FAILED,
}

/**
 * What Google Play has said: whether it [sells] here, each item's price in the player's own
 * currency as Google Play writes it ([prices]), the purchases it confirms ([bought]), and how the
 * latest Buy of each item not yet owned ended ([outcomes]).
 */
data class StoreState(
    val selling: Selling = Selling.UNKNOWN,
    val prices: Map<Cosmetic, String> = emptyMap(),
    val bought: Set<Cosmetic> = emptySet(),
    val outcomes: Map<Cosmetic, BuyOutcome> = emptyMap(),
) {
    /** The price of [cosmetic], when Google Play sells it here. */
    fun price(cosmetic: Cosmetic): String? = prices[cosmetic].takeIf { selling == Selling.YES }
}

/**
 * The cosmetics the player owns (money.md, "Owning an item"): those [earned] by playing (stars and
 * chests, from the saved progress) and those [bought] on Google Play. A refund takes away only the
 * purchase, so an item also earned stays owned.
 */
data class Owned(val earned: Set<Cosmetic>, val bought: Set<Cosmetic>) {
    fun owns(cosmetic: Cosmetic): Boolean = cosmetic in earned || cosmetic in bought

    companion object {
        val NONE = Owned(emptySet(), emptySet())

        fun of(progress: Progress, store: StoreState): Owned = Owned(progress.earned, store.bought)
    }
}

/**
 * The cosmetic's product ID in Play Console. Google Play allows lowercase letters, digits,
 * underscores and dots, so the cosmetic's name has underscores for its hyphens: `star_chart`.
 */
val Cosmetic.productId: String get() = id.replace('-', '_')

/** The cosmetic whose product ID is [productId], or null if there is none. */
fun cosmeticOfProduct(productId: String): Cosmetic? = Cosmetic.entries.firstOrNull { it.productId == productId }

/**
 * A phone without Google Play (the Play Store app), and the tests: nothing can be bought, and
 * nothing was. Every item shows how to earn it.
 */
object NoStore : Store {
    override val state: StateFlow<StoreState> = MutableStateFlow(StoreState(selling = Selling.EARN_ONLY)).asStateFlow()

    override fun refresh() = Unit

    override fun buy(activity: Activity, cosmetic: Cosmetic) = Unit
}
