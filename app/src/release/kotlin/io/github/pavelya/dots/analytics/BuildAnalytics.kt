package io.github.pavelya.dots.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Store builds send each event to Google Analytics for Firebase (decision 0033). What Firebase may
 * collect besides (no advertising ID, no Android ID, no ads consent) is set in this build's
 * `AndroidManifest.xml`; the EU and UK consent form (C-03) decides whether it collects at all there.
 */
fun buildAnalytics(context: Context): Analytics {
    val firebase = FirebaseAnalytics.getInstance(context)
    return Analytics { event ->
        val params = Bundle()
        event.params.forEach { (key, value) ->
            when (value) {
                is Long -> params.putLong(key, value)
                else -> params.putString(key, value.toString())
            }
        }
        firebase.logEvent(event.name, params)
    }
}
