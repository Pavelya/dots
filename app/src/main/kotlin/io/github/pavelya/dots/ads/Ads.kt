package io.github.pavelya.dots.ads

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What Google's rewarded ads can do now. */
data class AdsState(
    /**
     * True once ads may be asked for: Google's consent form has the player's answer, or Google has
     * said that no answer is needed where the player is.
     */
    val allowed: Boolean = false,

    /** True while an ad has loaded and can be shown at once. An offer shows only then. */
    val ready: Boolean = false,

    /**
     * True where the player can change their consent answer (the EU, the EEA, the UK): Privacy
     * then shows Ad choices.
     */
    val choices: Boolean = false,
)

/**
 * Google's rewarded ads and consent form, as the game uses them (docs/design/money.md, "Rewarded
 * ads"). [AdMobAds] is the real one; [NoAds] stands for a phone without Google Play services, and
 * the tests use fakes.
 */
interface Ads {
    val state: StateFlow<AdsState>

    /**
     * Each time the app opens: asks Google whether this player must answer the consent form (the
     * EU, the EEA, the UK), shows it over [activity] if so, then lets ads be asked for. [answered]
     * runs once Google has the player's answer, or has said none is needed. It does not run when
     * Google can't be reached; an answer from an earlier time still holds then.
     */
    fun start(activity: Activity, answered: () -> Unit)

    /** Loads an ad for an offer that may come, if ads are allowed and none is loaded or loading. */
    fun load()

    /**
     * Shows the loaded ad over [activity]. [shown] runs when it is on the screen; [earned] when the
     * player has watched it to its reward; [closed] when it has gone, after the reward or before
     * it, or when it could not be shown.
     */
    fun show(activity: Activity, shown: () -> Unit, earned: () -> Unit, closed: () -> Unit)

    /** Opens Google's form over [activity], where the player changes their consent answer (Privacy, Ad choices). */
    fun choices(activity: Activity)

    /** Forgets the consent answer, so the form shows again on the next start: the developer menu only. */
    fun forgetConsent()
}

/** A phone without Google Play services, and the tests' phone: no ads, no offers. */
object NoAds : Ads {
    override val state: StateFlow<AdsState> = MutableStateFlow(AdsState()).asStateFlow()

    override fun start(activity: Activity, answered: () -> Unit) = Unit

    override fun load() = Unit

    override fun show(activity: Activity, shown: () -> Unit, earned: () -> Unit, closed: () -> Unit) = closed()

    override fun choices(activity: Activity) = Unit

    override fun forgetConsent() = Unit
}
