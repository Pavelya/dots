package io.github.pavelya.dots

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

/** Shows Google Play's review prompt over [Activity]; tests replace it with one that only counts. */
fun interface ReviewPrompt {
    fun show(activity: Activity)
}

/**
 * Google Play's in-app review prompt. Google draws it and decides whether it shows at all; it
 * never shows in a build that was not installed from Google Play. Nothing is reported back, so a
 * failure is ignored.
 */
class PlayReviewPrompt : ReviewPrompt {
    override fun show(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful && !activity.isFinishing && !activity.isDestroyed) {
                manager.launchReviewFlow(activity, request.result)
            }
        }
    }
}
