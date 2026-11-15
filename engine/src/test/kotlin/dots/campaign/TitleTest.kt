package dots.campaign

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TitleTest {
    @Test
    fun `the title grows with the stars, from Novice to Grandmaster`() {
        assertEquals(Title.NOVICE, Title.of(0))
        assertEquals(Title.NOVICE, Title.of(9))
        assertEquals(Title.APPRENTICE, Title.of(10))
        assertEquals(Title.APPRENTICE, Title.of(14), "the canvas map: Apprentice at 14 stars")
        assertEquals(Title.STRATEGIST, Title.of(45))
        assertEquals(Title.MASTER, Title.of(100))
        assertEquals(Title.GRANDMASTER, Title.of(180))
        assertEquals(Title.GRANDMASTER, Title.of(Campaign.levels.size * Stars.MAX))
    }

    @Test
    fun `every title can be reached in the campaign, and each needs more stars than the last`() {
        for ((before, after) in Title.entries.zipWithNext()) {
            assertTrue(after.stars > before.stars, after.name)
            assertEquals(after, before.next)
        }
        assertNull(Title.GRANDMASTER.next)
        assertTrue(Title.GRANDMASTER.stars <= Campaign.levels.size * Stars.MAX)
    }

    @Test
    fun `the map and the progress show the same title`() {
        val progress = Progress(Campaign.levels.take(7).associate { it.code to 2 })
        assertEquals(Title.APPRENTICE, progress.title)
        assertEquals(Title.APPRENTICE, Campaign.map(progress).title)
    }
}
