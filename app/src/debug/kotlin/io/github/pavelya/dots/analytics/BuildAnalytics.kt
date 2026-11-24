package io.github.pavelya.dots.analytics

import android.content.Context
import android.util.Log

/**
 * Debug builds (CI test builds, POC test builds, the tests) contain no Firebase and send nothing
 * (decision 0033). Each event only goes to the phone's log, to check events on a test build:
 * `adb logcat -s Analytics`.
 */
@Suppress("UNUSED_PARAMETER") // the release build's version needs the context
fun buildAnalytics(context: Context): Analytics = Analytics { event -> Log.d("Analytics", "${event.name} ${event.params}") }

/** Debug builds collect nothing, so the consent form's answer changes nothing here. */
@Suppress("UNUSED_PARAMETER") // the release build's version needs the context
fun allowAnalytics(context: Context) = Unit
