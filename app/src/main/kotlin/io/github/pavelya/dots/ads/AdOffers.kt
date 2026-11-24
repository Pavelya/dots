package io.github.pavelya.dots.ads

import android.app.Activity
import dots.campaign.Chest
import dots.campaign.Level
import dots.engine.Booster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The two places a rewarded ad is offered (docs/design/money.md, "Rewarded ads"), and nowhere else:
 * never during a game, in the daily puzzle, free play, two players or online.
 */
object AdPlaces {
    /**
     * The booster offered for the retry on the Result of a failed attempt at [level]: the first
     * kind the level allows ([Level.adBooster]). Null for no offer: in World 1, where players learn
     * the rules; in a practice game; and on a level that allows no booster but Undo.
     */
    fun retry(level: Level, practice: Boolean): Booster? = level.adBooster?.takeIf { level.world > 1 && !practice }

    /** True when [chest] offers to keep both of its choices: a chest of two, from World 2. */
    fun chest(chest: Chest): Boolean = !chest.isSingle && chest.after.world > 1
}

/**
 * What the phone remembers for the ad limits: how many ads were [watched] on [day] (a day number,
 * [dots.daily.Day.epochDay], local time), and when the player last paid for an item ([paidAt], in
 * milliseconds since 1970), if ever.
 */
data class AdRecord(val day: Long = -1, val watched: Int = 0, val paidAt: Long? = null) {
    /** The ads watched on [today]: none yet on a new day. */
    fun watchedOn(today: Long): Int = if (day == today) watched else 0
}

/** Where [AdRecord] is kept. [StoredAdLog] keeps it on the phone; the tests keep it in memory. */
interface AdLog {
    /** The record, or null until it has been read. */
    val record: StateFlow<AdRecord?>

    /** Counts one more ad watched on [today]. */
    suspend fun watched(today: Long)

    /** Remembers that the player paid for an item at [at]. */
    suspend fun paid(at: Long)
}

/** The limits on rewarded ads (money.md, "Rewarded ads", Limits). */
object AdRules {
    /** At most this many rewarded ads a day, counted together across both places. */
    const val PER_DAY = 3

    /** No ad offers for this long after a purchase: 24 hours. */
    const val PAUSE_AFTER_PURCHASE = 24 * 60 * 60 * 1000L

    /**
     * True when the limits let an offer show at [now], on day [today]: Show ad offers is [on],
     * fewer than [PER_DAY] ads have been watched today, and the last purchase was 24 hours ago or
     * more. A purchase "in the future" (the phone's clock was turned back) pauses nothing.
     */
    fun allowed(record: AdRecord, today: Long, now: Long, on: Boolean): Boolean {
        val paused = record.paidAt?.let { now - it in 0 until PAUSE_AFTER_PURCHASE } ?: false
        return on && !paused && record.watchedOn(today) < PER_DAY
    }
}

/** What an offer depends on, besides the time: the [ads], the [record] and the Show ad offers switch ([on]). */
data class AdInputs(val ads: AdsState, val record: AdRecord?, val on: Boolean?)

/**
 * Whether a rewarded ad may be offered now, and watching one (money.md, "Rewarded ads"). An offer
 * shows only when ads are allowed (Google's consent form), an ad is ready, the [log] is within
 * [AdRules], and the Show ad offers switch ([on], from Settings) is on. Each ad shown counts
 * toward the day's limit; its reward is given once, only after the player has watched it.
 *
 * @param ads the ads, read each time, so the tests can replace them
 * @param now the time now, in milliseconds since 1970
 * @param today today's day number, local time, as the daily puzzle counts days
 */
class AdOffers(
    private val ads: () -> Ads,
    private val log: AdLog,
    adsState: StateFlow<AdsState>,
    private val on: StateFlow<Boolean?>,
    private val now: () -> Long,
    private val today: () -> Long,
    private val scope: CoroutineScope,
) {
    /** Changes when anything but the time that an offer depends on changes; screens then ask [canOffer] again. */
    val inputs: StateFlow<AdInputs> = combine(adsState, log.record, on, ::AdInputs)
        .stateIn(scope, SharingStarted.Eagerly, AdInputs(adsState.value, log.record.value, on.value))

    /** True when an offer may show now, where [AdPlaces] allows one. */
    fun canOffer(): Boolean {
        val ready = ads().state.value
        val record = log.record.value ?: return false
        return ready.allowed && ready.ready && AdRules.allowed(record, today(), now(), on.value ?: false)
    }

    /**
     * Gets an ad ready for an offer that may come soon: the Result of a level from World 2, or a
     * chest of two. Nothing is loaded when the limits rule out an offer today.
     */
    fun prepare() {
        val record = log.record.value ?: return
        if (AdRules.allowed(record, today(), now(), on.value ?: false)) ads().load()
    }

    /**
     * Shows the ready ad over [activity]. [reward] runs once, only when the player has watched it
     * to its reward; [closed] runs when the ad has gone, with whether the reward was given. Closing
     * the ad early gives nothing, and the offer stays where it was.
     */
    fun watch(activity: Activity, reward: () -> Unit, closed: (rewarded: Boolean) -> Unit = {}) {
        if (!canOffer()) return closed(false)
        var given = false
        var counted = false
        var gone = false
        ads().show(
            activity,
            shown = {
                if (!counted) {
                    counted = true
                    val day = today()
                    scope.launch { log.watched(day) }
                }
            },
            earned = {
                if (!given && !gone) {
                    given = true
                    reward()
                }
            },
            closed = {
                if (!gone) {
                    gone = true
                    closed(given)
                    prepare() // the next offer's ad, if today allows one
                }
            },
        )
    }
}
