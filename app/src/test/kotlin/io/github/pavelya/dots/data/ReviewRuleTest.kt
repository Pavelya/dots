package io.github.pavelya.dots.data

import dots.campaign.Campaign
import dots.campaign.LevelResult
import dots.campaign.MissionStatus
import dots.campaign.Outcome
import dots.campaign.Reason
import dots.campaign.Stars
import dots.campaign.Tutorial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When Google Play's review prompt may follow a level (A-12). */
class ReviewRuleTest {
    private val level = Campaign.level(2, 1)

    private fun passed(stars: Int) = LevelResult(MissionStatus(Outcome.PASSED, Reason.GAME_OVER, 3, 0, null), stars, starScore = stars)

    private fun failed(stars: Int = 0) = LevelResult(MissionStatus(Outcome.FAILED, Reason.GAME_OVER, 0, 3, null), stars, starScore = 0)

    private val day = 24L * 60 * 60 * 1000

    @Test
    fun `a level won with three stars earns the prompt`() {
        assertTrue(ReviewRule.earns(level, passed(Stars.MAX), practice = false))
    }

    @Test
    fun `a win with fewer stars does not`() {
        for (stars in 0 until Stars.MAX) assertFalse("$stars stars", ReviewRule.earns(level, passed(stars), practice = false))
    }

    @Test
    fun `a loss never does, whatever its stars say`() {
        assertFalse(ReviewRule.earns(level, failed(), practice = false))
        assertFalse(ReviewRule.earns(level, failed(stars = Stars.MAX), practice = false))
        assertFalse(ReviewRule.earns(level, failed(), practice = true))
    }

    @Test
    fun `a practice game and the tutorial's levels do not`() {
        assertFalse(ReviewRule.earns(level, passed(Stars.MAX), practice = true))
        for (lesson in Tutorial.levels) {
            assertFalse(lesson.code, ReviewRule.earns(lesson, passed(Stars.MAX), practice = false))
        }
        // The first level after the tutorial does.
        assertTrue(ReviewRule.earns(Campaign.level(1, Tutorial.LESSONS + 1), passed(Stars.MAX), practice = false))
    }

    @Test
    fun `the prompt is asked at most once a month`() {
        assertEquals(30, ReviewRule.GAP_DAYS)
        val asked = 1_800_000_000_000L
        assertTrue("never asked", ReviewRule.isDue(lastAsked = null, now = asked))
        assertFalse("the same moment", ReviewRule.isDue(asked, asked))
        assertFalse("a day later", ReviewRule.isDue(asked, asked + day))
        assertFalse("just short of 30 days", ReviewRule.isDue(asked, asked + 30 * day - 1))
        assertTrue("30 days later", ReviewRule.isDue(asked, asked + 30 * day))
        assertTrue("a year later", ReviewRule.isDue(asked, asked + 365 * day))
    }

    @Test
    fun `a clock set back waits`() {
        val asked = 1_800_000_000_000L
        assertFalse(ReviewRule.isDue(asked, asked - day))
        assertFalse(ReviewRule.isDue(asked, asked - 60 * day))
    }
}
