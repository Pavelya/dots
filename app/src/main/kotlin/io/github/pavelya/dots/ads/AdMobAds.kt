package io.github.pavelya.dots.ads

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google's AdMob rewarded ads ([unitId]) and Google's consent form, the User Messaging Platform
 * (UMP), as set up in AdMob (docs/release.md, AdMob). Every call is made on the main thread.
 *
 * - **Consent.** Each start asks Google whether the player must answer the form (the EU, the EEA,
 *   the UK) and shows it if so. Ads are asked for only once Google allows it. UMP also passes the
 *   answer to Google Analytics for Firebase (consent mode).
 * - **No IDs and nothing for adults only.** The app has no advertising ID permission
 *   (AndroidManifest.xml), and every ad is limited to content rated PG: the game's players are
 *   teens and adults (R-04), and no age is asked.
 * - **An ad keeps an hour.** AdMob drops a loaded ad after an hour, so an ad older than
 *   [MAX_AGE_MILLIS] is not offered; the next [load] fetches a new one.
 *
 * [testRegion] (the developer menu, debug builds) makes Google treat the phone as in the EEA, so
 * the form can be checked from anywhere.
 */
class AdMobAds(
    private val app: Application,
    private val unitId: String,
    private val testRegion: () -> Boolean,
) : Ads {
    private val shown = MutableStateFlow(AdsState())
    override val state: StateFlow<AdsState> = shown.asStateFlow()

    private val consent: ConsentInformation = UserMessagingPlatform.getConsentInformation(app)
    private val main = Handler(Looper.getMainLooper())

    /** True once the Mobile Ads SDK has been started. */
    private var begun = false
    private var ad: RewardedAd? = null
    private var loadedAt = 0L
    private var loading = false

    init {
        // An answer from an earlier start lets ads be asked for at once, before Google is asked again.
        if (consent.canRequestAds()) begin()
        update()
    }

    override fun start(activity: Activity, answered: () -> Unit) {
        val parameters = ConsentRequestParameters.Builder().apply {
            if (testRegion()) {
                setConsentDebugSettings(
                    ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .setForceTesting(true)
                        .build(),
                )
            }
        }.build()
        consent.requestConsentInfoUpdate(
            activity,
            parameters,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    update()
                    if (consent.canRequestAds()) begin()
                    val status = consent.consentStatus
                    if (status == ConsentInformation.ConsentStatus.OBTAINED || status == ConsentInformation.ConsentStatus.NOT_REQUIRED) answered()
                }
            },
            { update() }, // offline: an answer from an earlier start still holds
        )
    }

    /** Starts the Mobile Ads SDK once, on a background thread, as Google advises. */
    private fun begin() {
        if (begun) return
        begun = true
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
                .build(),
        )
        Thread({ MobileAds.initialize(app) }, "ads-start").start()
    }

    override fun load() {
        main.post {
            if (!begun || loading || fresh()) return@post
            loading = true
            RewardedAd.load(
                app,
                unitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(loaded: RewardedAd) {
                        loading = false
                        ad = loaded
                        loadedAt = SystemClock.elapsedRealtime()
                        // Once too old, the ad is no longer offered.
                        main.postDelayed({ update() }, MAX_AGE_MILLIS)
                        update()
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loading = false
                        ad = null
                        update()
                    }
                },
            )
        }
    }

    override fun show(activity: Activity, shown: () -> Unit, earned: () -> Unit, closed: () -> Unit) {
        val loaded = ad?.takeIf { fresh() }
        ad = null
        update()
        if (loaded == null) return closed()
        loaded.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() = shown()

            override fun onAdDismissedFullScreenContent() = closed()

            override fun onAdFailedToShowFullScreenContent(error: AdError) = closed()
        }
        loaded.show(activity) { earned() }
    }

    override fun choices(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            update()
            if (consent.canRequestAds()) begin()
        }
    }

    override fun forgetConsent() {
        consent.reset()
        update()
    }

    /** True while the loaded ad is younger than [MAX_AGE_MILLIS]. */
    private fun fresh(): Boolean = ad != null && SystemClock.elapsedRealtime() - loadedAt < MAX_AGE_MILLIS

    private fun update() {
        shown.value = AdsState(
            allowed = consent.canRequestAds(),
            ready = fresh(),
            choices = consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
        )
    }

    private companion object {
        /** AdMob keeps a loaded rewarded ad for an hour; the game offers it for 55 minutes. */
        const val MAX_AGE_MILLIS = 55 * 60 * 1000L
    }
}
