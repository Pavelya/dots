package io.github.pavelya.dots.ui

import android.app.Activity
import androidx.activity.ComponentActivity
import dots.campaign.Campaign
import dots.campaign.CampaignMap
import dots.campaign.Level
import dots.campaign.LevelResult
import dots.campaign.MissionStatus
import dots.campaign.Outcome
import dots.campaign.Reason
import dots.campaign.Stars
import dots.campaign.Tutorial
import io.github.pavelya.dots.DotsApplication
import io.github.pavelya.dots.ReviewPrompt
import io.github.pavelya.dots.data.LevelEnd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * The app's review prompt after a level (A-12): the rule, the stored time of the last ask and the
 * prompt together, with Google's prompt replaced by one that counts.
 */
@RunWith(RobolectricTestRunner::class)
class ReviewPromptTest {
    private val app get() = RuntimeEnvironment.getApplication() as DotsApplication
    private val shown = mutableListOf<Activity>()
    private lateinit var activity: Activity

    private val level = Campaign.level(2, 1)
    private val day = 24L * 60 * 60 * 1000

    /** In the year 2100, so no ask stored by another test is within a month of it. */
    private val start = 4_102_444_800_000L

    @Before
    fun `count the prompts`() {
        app.reviews = ReviewPrompt { shown += it }
        activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    }

    private fun end(passed: Boolean, stars: Int): LevelEnd {
        val status = MissionStatus(if (passed) Outcome.PASSED else Outcome.FAILED, Reason.GAME_OVER, 3, 0, null)
        return LevelEnd(LevelResult(status, stars, starScore = stars), CampaignMap.Opened.NOTHING, failed = if (passed) 0 else 1, next = null)
    }

    private fun ask(end: LevelEnd, at: Long, practice: Boolean = false, played: Level = level): Boolean =
        runBlocking(Dispatchers.IO) { askForReview(app, activity, played, end, practice, at) }

    @Test
    fun `a three-star level asks for the prompt once a month, and a loss never does`() {
        // A loss, fewer stars, a practice game and a tutorial level ask nothing, and leave the month's ask unspent.
        assertEquals(false, ask(end(passed = false, stars = 0), start))
        assertEquals(false, ask(end(passed = true, stars = Stars.MAX - 1), start))
        assertEquals(false, ask(end(passed = true, stars = Stars.MAX), start, practice = true))
        assertEquals(false, ask(end(passed = true, stars = Stars.MAX), start, played = Tutorial.levels.first()))
        assertEquals(emptyList<Activity>(), shown)

        // Three stars: the prompt, over the game's screen.
        assertEquals(true, ask(end(passed = true, stars = Stars.MAX), start))
        assertEquals(listOf(activity), shown)

        // Not again within the month, however the levels end.
        assertEquals(false, ask(end(passed = true, stars = Stars.MAX), start + day))
        assertEquals(false, ask(end(passed = false, stars = 0), start + 29 * day))
        assertEquals(false, ask(end(passed = true, stars = Stars.MAX), start + 30 * day - 1))
        assertEquals(1, shown.size)

        // A month on, a loss still asks nothing; the next three-star level does.
        assertEquals(false, ask(end(passed = false, stars = 0), start + 30 * day))
        assertEquals(true, ask(end(passed = true, stars = Stars.MAX), start + 30 * day))
        assertEquals(2, shown.size)
    }
}
