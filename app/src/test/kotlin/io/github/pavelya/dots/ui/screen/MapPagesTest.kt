package io.github.pavelya.dots.ui.screen

import androidx.compose.ui.unit.dp
import dots.campaign.Campaign
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The map pages' doodles stay where MapScreen can draw them whole. */
class MapPagesTest {
    private val worlds = Campaign.levels.map { it.world }.distinct()

    @Test
    fun `every world has its map page`() {
        for (world in worlds) assertNotNull("World $world has no map page", MapPages.of(world))
    }

    @Test
    fun `every doodle hangs from inside the row of a level of its world, or of its start`() {
        for (world in worlds) {
            val page = MapPages.of(world) ?: continue
            val levels = Campaign.levels.filter { it.world == world }
            for (doodle in page.doodles) {
                val height = when {
                    doodle.level == 0 -> START_HEIGHT
                    else -> {
                        val level = levels.firstOrNull { it.number == doodle.level }
                        assertNotNull("World $world has no level ${doodle.level}", level)
                        if (level!!.rule != null) BOSS_HEIGHT else STOP_HEIGHT
                    }
                }
                assertTrue("$doodle: its top is outside its row", doodle.top >= 0.dp && doodle.top < height)
            }
        }
    }

    @Test
    fun `every doodle stays between the margin and the right edge, from 320 to 412 dp wide`() {
        for (world in worlds) {
            val page = MapPages.of(world) ?: continue
            for (doodle in page.doodles) {
                for (width in listOf(320f, 360f, 412f)) {
                    val left = width * doodle.x - doodle.width.value / 2
                    val right = width * doodle.x + doodle.width.value / 2
                    assertTrue("$doodle at $width dp: crosses the margin", left >= 58f)
                    assertTrue("$doodle at $width dp: off the right edge", right <= width - 4f)
                }
            }
        }
    }
}
