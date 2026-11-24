package io.github.pavelya.dots.ads

import android.app.Activity
import dots.campaign.Campaign
import dots.campaign.Chest
import dots.campaign.Reward
import dots.engine.Booster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * The rewarded ads' rules (docs/design/money.md, "Rewarded ads"): the two places, at most 3 ads a
 * day, none in World 1, none for 24 hours after a purchase, none with Show ad offers off or with no
 * ad ready, and the reward given once, only after the ad has been watched.
 */
@RunWith(RobolectricTestRunner::class)
class AdOffersTest {
    /** Google's ads as the test sets them: the test plays each ad's events itself. */
    private class FakeAds : Ads {
        val shown = MutableStateFlow(AdsState(allowed = true, ready = true))
        override val state: StateFlow<AdsState> = shown.asStateFlow()
        var loads = 0
        var shows = 0
        private var events: Triple<() -> Unit, () -> Unit, () -> Unit>? = null

        override fun start(activity: Activity, answered: () -> Unit) = Unit

        override fun load() {
            loads++
        }

        override fun show(activity: Activity, shown: () -> Unit, earned: () -> Unit, closed: () -> Unit) {
            shows++
            events = Triple(shown, earned, closed)
        }

        override fun choices(activity: Activity) = Unit

        override fun forgetConsent() = Unit

        fun onScreen() = events!!.first()

        fun rewarded() = events!!.second()

        fun gone() = events!!.third()
    }

    /** The record kept in memory. */
    private class Memory : AdLog {
        private val kept = MutableStateFlow<AdRecord?>(AdRecord())
        override val record: StateFlow<AdRecord?> = kept.asStateFlow()

        override suspend fun watched(today: Long) {
            val before = kept.value ?: AdRecord()
            kept.value = before.copy(day = today, watched = before.watchedOn(today) + 1)
        }

        override suspend fun paid(at: Long) {
            kept.value = (kept.value ?: AdRecord()).copy(paidAt = at)
        }
    }

    private val ads = FakeAds()
    private val log = Memory()
    private val on = MutableStateFlow<Boolean?>(true)
    private var now = 1_800_000_000_000L
    private var today = 20_000L
    private val offers = AdOffers({ ads }, log, ads.state, on, { now }, { today }, CoroutineScope(Dispatchers.Unconfined))
    private val activity: Activity get() = Robolectric.buildActivity(Activity::class.java).get()

    /** Watches one ad to its end: on the screen, the reward, closed. Returns the rewards given. */
    private fun watchOne(): Int {
        var rewards = 0
        offers.watch(activity, reward = { rewards++ })
        ads.onScreen()
        ads.rewarded()
        ads.gone()
        return rewards
    }

    @Test
    fun `at most 3 ads a day across both places, then none until the next day`() {
        repeat(AdRules.PER_DAY) {
            assertTrue("ad ${it + 1} of the day", offers.canOffer())
            assertEquals(1, watchOne())
        }
        assertFalse("the fourth ad of the day", offers.canOffer())
        val shows = ads.shows
        offers.watch(activity, reward = { error("no reward past the limit") })
        assertEquals("not even shown", shows, ads.shows)

        today += 1
        assertTrue("a new day from local midnight", offers.canOffer())
    }

    @Test
    fun `no offers in World 1, in practice, or on a level with no booster but Undo`() {
        val world1 = Campaign.levels.filter { it.world == 1 }
        assertTrue(world1.isNotEmpty())
        for (level in world1) assertNull(level.code, AdPlaces.retry(level, practice = false))

        val level21 = Campaign.level(2, 1)
        assertEquals("the level's first booster, never Undo", level21.adBooster, AdPlaces.retry(level21, practice = false))
        assertEquals(Booster.WALL, AdPlaces.retry(level21, practice = false))
        assertNull("a practice game", AdPlaces.retry(level21, practice = true))
        for (level in Campaign.levels.filter { it.boosters.keys.all { kind -> kind == Booster.UNDO } }) {
            assertNull(level.code, AdPlaces.retry(level, practice = false))
        }
    }

    @Test
    fun `only a chest of two boosters from World 2 offers to keep both`() {
        for (chest in Campaign.chests) assertEquals(chest.after.code, !chest.isSingle && chest.after.world > 1, AdPlaces.chest(chest))
        assertTrue("the chest after 2-15", AdPlaces.chest(Campaign.chests.first { !it.isSingle }))
        val worldOne = Chest(Campaign.level(1, 10), listOf(Reward(Booster.WALL, 2), Reward(Booster.BOMB, 2)))
        assertFalse("never in World 1", AdPlaces.chest(worldOne))
    }

    @Test
    fun `no offers for 24 hours after a purchase`() {
        runBlocking { log.paid(now) }
        assertFalse(offers.canOffer())
        now += AdRules.PAUSE_AFTER_PURCHASE - 1
        assertFalse("a minute before the 24 hours are up", offers.canOffer())
        now += 1
        assertTrue(offers.canOffer())
    }

    @Test
    fun `no offers with Show ad offers off`() {
        on.value = false
        assertFalse(offers.canOffer())
        offers.prepare()
        assertEquals("no ad loaded for an offer that can't show", 0, ads.loads)
        on.value = true
        assertTrue(offers.canOffer())
    }

    @Test
    fun `no offer until an ad is ready, and none before Google's consent form allows ads`() {
        ads.shown.value = AdsState(allowed = true, ready = false)
        assertFalse(offers.canOffer())
        ads.shown.value = AdsState(allowed = false, ready = true)
        assertFalse(offers.canOffer())
        ads.shown.value = AdsState(allowed = true, ready = true)
        assertTrue(offers.canOffer())
    }

    @Test
    fun `the reward is given once, and only after the ad has been watched to its reward`() {
        var rewards = 0
        var closedWith: Boolean? = null
        offers.watch(activity, reward = { rewards++ }, closed = { closedWith = it })
        ads.onScreen()
        assertEquals("nothing while the ad plays", 0, rewards)
        ads.rewarded()
        ads.rewarded()
        assertEquals("once", 1, rewards)
        ads.gone()
        ads.gone()
        assertEquals(true, closedWith)
        assertEquals(1, rewards)
    }

    @Test
    fun `an ad closed early gives nothing, still counts toward the day, and the offer stays`() {
        var rewards = 0
        var closedWith: Boolean? = null
        offers.watch(activity, reward = { rewards++ }, closed = { closedWith = it })
        ads.onScreen()
        ads.gone()
        ads.rewarded() // too late: the ad has gone
        assertEquals(0, rewards)
        assertEquals(false, closedWith)
        assertEquals(1, log.record.value?.watchedOn(today))
        assertTrue("the offer is still there", offers.canOffer())
    }

    @Test
    fun `an ad is made ready only when today allows an offer, and the next one after each ad`() {
        offers.prepare()
        assertEquals(1, ads.loads)
        watchOne()
        assertEquals("the next ad, after the first", 2, ads.loads)
        watchOne()
        watchOne()
        assertEquals("none after the third of the day", 3, ads.loads)
        offers.prepare()
        assertEquals(3, ads.loads)
    }
}
