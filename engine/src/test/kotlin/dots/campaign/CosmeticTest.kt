package dots.campaign

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CosmeticTest {
    /** Progress with exactly [total] stars, 3 to a level and the rest on the next. */
    private fun withStars(total: Int): Progress {
        val levels = Campaign.levels.take((total + Stars.MAX - 1) / Stars.MAX)
        val stars = levels.mapIndexed { i, level -> level.code to minOf(Stars.MAX, total - i * Stars.MAX) }.toMap()
        return Progress(stars).also { assertEquals(total, it.total) }
    }

    @Test
    fun `the themes are earned at 150 and 200 stars`() {
        assertEquals(emptySet(), withStars(149).earned)
        assertEquals(setOf(Cosmetic.STAR_CHART), withStars(150).earned)
        assertEquals(setOf(Cosmetic.STAR_CHART), withStars(199).earned)
        assertEquals(setOf(Cosmetic.STAR_CHART, Cosmetic.NEON_ARCADE), withStars(200).earned)

        assertFalse(withStars(149).hasEarned(Cosmetic.STAR_CHART))
        assertTrue(withStars(150).hasEarned(Cosmetic.STAR_CHART))
        assertFalse(withStars(150).hasEarned(Cosmetic.NEON_ARCADE))
        assertTrue(withStars(200).hasEarned(Cosmetic.NEON_ARCADE))
    }

    @Test
    fun `a chest's cosmetic counts as earned at any star count`() {
        for (total in listOf(0, 1, 149, 150)) {
            val progress = withStars(total).copy(cosmetics = setOf(Cosmetic.NEON_ARCADE.id))
            assertTrue(progress.hasEarned(Cosmetic.NEON_ARCADE), "$total stars")
            assertTrue(Cosmetic.NEON_ARCADE in progress.earned, "$total stars")
        }
    }

    @Test
    fun `a stored name that is no cosmetic earns nothing`() {
        val progress = Progress(cosmetics = setOf("ink-splash"))
        assertEquals(emptySet(), progress.earned)
        assertNull(Cosmetic.of("ink-splash"))
    }

    @Test
    fun `every cosmetic can be earned in the campaign`() {
        val most = Campaign.levels.size * Stars.MAX
        for (cosmetic in Cosmetic.entries) {
            assertTrue(cosmetic.stars in 1..most, "${cosmetic.id} needs ${cosmetic.stars} stars; the campaign has $most")
            assertTrue(withStars(most).hasEarned(cosmetic), cosmetic.id)
        }
        assertEquals(Cosmetic.entries.toSet(), withStars(most).earned)
    }

    @Test
    fun `each id is one that progress stores and reads back`() {
        assertEquals(Cosmetic.entries.size, Cosmetic.entries.map { it.id }.toSet().size, "ids are unique")
        for (cosmetic in Cosmetic.entries) {
            val stored = Progress(cosmetics = setOf(cosmetic.id))
            assertEquals(stored, Progress.parse("", cosmetics = stored.cosmeticsText), cosmetic.id)
            assertEquals(cosmetic, Cosmetic.of(cosmetic.id))
        }
    }
}
