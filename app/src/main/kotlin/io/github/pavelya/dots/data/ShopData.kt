package io.github.pavelya.dots.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dots.campaign.Cosmetic
import io.github.pavelya.dots.shop.PurchaseCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The purchases Google Play last confirmed (docs/design/money.md, "Owning an item": owned items
 * work offline). They live in a store of their own, `files/datastore/purchases.preferences_pb`,
 * never in the saved progress, and Android's backup leaves it out: on a new phone Google Play
 * brings the purchases back by itself.
 */
class StoredPurchases(private val store: DataStore<Preferences>) : PurchaseCache {
    override suspend fun read(): Set<Cosmetic> = store.data.first()[BOUGHT].orEmpty().mapNotNullTo(mutableSetOf(), Cosmetic::of)

    override suspend fun save(bought: Set<Cosmetic>) {
        store.edit { it[BOUGHT] = bought.mapTo(mutableSetOf()) { item -> item.id } }
    }

    companion object {
        /** The store's name. */
        const val NAME = "purchases"

        /** The bought cosmetics, by [Cosmetic.id]. */
        private val BOUGHT = stringSetPreferencesKey("bought")
    }
}

/**
 * What the Shop has shown of the items the player earned (flows.md, Campaign): an earned item is
 * marked New in the Shop until its preview is opened ([seen]), and a red dot sits on the map's Shop
 * icon until the Shop opens after it was earned ([shown]). Kept in the progress store, beside the
 * stars that earn the items, so a backup restore doesn't mark them New again.
 */
class ShopMarks(private val store: DataStore<Preferences>) {
    /** The earned items whose preview the player has opened. */
    val seen: Flow<Set<Cosmetic>> = store.data.map { it[SEEN].cosmetics() }

    /** The earned items the Shop had when the player last opened it. */
    val shown: Flow<Set<Cosmetic>> = store.data.map { it[SHOWN].cosmetics() }

    /** The Shop opened with [earned] earned: the map's red dot goes. */
    suspend fun shopOpened(earned: Set<Cosmetic>) = add(SHOWN, earned)

    /** The preview of [cosmetic] opened: its New mark goes. */
    suspend fun previewOpened(cosmetic: Cosmetic) = add(SEEN, setOf(cosmetic))

    private suspend fun add(key: Preferences.Key<Set<String>>, items: Set<Cosmetic>) {
        if (items.isEmpty()) return
        store.edit { prefs -> prefs[key] = prefs[key].orEmpty() + items.map { it.id } }
    }

    private fun Set<String>?.cosmetics(): Set<Cosmetic> = orEmpty().mapNotNullTo(mutableSetOf(), Cosmetic::of)

    private companion object {
        /** The earned items whose preview was opened, by [Cosmetic.id]. */
        val SEEN = stringSetPreferencesKey("shop_seen")

        /** The earned items the Shop has shown, by [Cosmetic.id]. */
        val SHOWN = stringSetPreferencesKey("shop_shown")
    }
}
