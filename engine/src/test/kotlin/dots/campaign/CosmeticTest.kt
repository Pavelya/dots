package dots.campaign

import java.io.File
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

    /** The themes earned with [total] stars. */
    private fun themesAt(total: Int): Set<Cosmetic> = withStars(total).earned.filterTo(mutableSetOf()) { it.kind == Cosmetic.Kind.THEME }

    @Test
    fun `the themes are earned at 150 and 200 stars`() {
        assertEquals(emptySet(), themesAt(149))
        assertEquals(setOf(Cosmetic.STAR_CHART), themesAt(150))
        assertEquals(setOf(Cosmetic.STAR_CHART), themesAt(199))
        assertEquals(setOf(Cosmetic.STAR_CHART, Cosmetic.NEON_ARCADE), themesAt(200))

        assertFalse(withStars(149).hasEarned(Cosmetic.STAR_CHART))
        assertTrue(withStars(150).hasEarned(Cosmetic.STAR_CHART))
        assertFalse(withStars(150).hasEarned(Cosmetic.NEON_ARCADE))
        assertTrue(withStars(200).hasEarned(Cosmetic.NEON_ARCADE))
    }

    @Test
    fun `the piece styles and ring effects are earned at 40, 70, 100 and 125 stars`() {
        assertEquals(emptySet(), withStars(39).earned)
        assertEquals(setOf(Cosmetic.HEARTS_AND_CLUBS), withStars(40).earned)
        assertEquals(setOf(Cosmetic.HEARTS_AND_CLUBS, Cosmetic.LASSO), withStars(70).earned)
        assertEquals(setOf(Cosmetic.HEARTS_AND_CLUBS, Cosmetic.LASSO, Cosmetic.FLAME_AND_FROST), withStars(124).earned)
        assertEquals(
            setOf(Cosmetic.HEARTS_AND_CLUBS, Cosmetic.LASSO, Cosmetic.FLAME_AND_FROST, Cosmetic.RUBBER_STAMP),
            withStars(149).earned,
        )
        assertEquals(Cosmetic.Kind.PIECE_STYLE, Cosmetic.HEARTS_AND_CLUBS.kind)
        assertEquals(Cosmetic.Kind.PIECE_STYLE, Cosmetic.FLAME_AND_FROST.kind)
        assertEquals(Cosmetic.Kind.RING_EFFECT, Cosmetic.LASSO.kind)
        assertEquals(Cosmetic.Kind.RING_EFFECT, Cosmetic.RUBBER_STAMP.kind)
    }

    /** money.md's "The catalogue at launch" and cosmetics.md's table name the same items, kinds, ids and star counts as the table here. */
    @Test
    fun `the table is the catalogue of money_md and cosmetics_md`() {
        fun rows(file: String, heading: String): List<List<String>> =
            File("../docs/design/$file").readText().substringAfter(heading).lineSequence()
                .dropWhile { !it.startsWith("|") }.takeWhile { it.startsWith("|") }.drop(2)
                .map { row -> row.trim('|').split("|").map { it.trim() } }.toList()
        val catalogue = rows("money.md", "## The catalogue at launch").filter { it[4].endsWith(" stars") }
        assertEquals(Cosmetic.entries.size, catalogue.size)
        for ((name, kind, _, _, earned) in catalogue) {
            val cosmetic = Cosmetic.of(name.lowercase().replace(' ', '-'))
            assertTrue(cosmetic != null, "money.md names $name, which the table lacks")
            assertEquals(kind, cosmetic.kind.name.lowercase().replace('_', ' '), name)
            assertEquals(earned, "${cosmetic.stars} stars", name)
        }
        val spec = rows("cosmetics.md", "# Piece styles and ring effects")
        assertEquals(4, spec.size)
        for (row in spec) {
            val cosmetic = Cosmetic.of(row[5].trim('`'))
            assertTrue(cosmetic != null, "cosmetics.md names ${row[5]}, which the table lacks")
            assertEquals(row[4], "${cosmetic.stars} stars", row[0])
        }
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
