package dots.campaign

import dots.engine.Booster
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChestTest {
    private val levels = Campaign.levels

    @Test
    fun `a chest follows every 10th level of the campaign`() {
        assertEquals(listOf("1-10", "2-5", "2-15", "3-5", "3-15", "4-5", "4-15"), Campaign.chests.map { it.after.code })
        assertEquals(Campaign.chests.first(), Campaign.chestAfter(Campaign.level(1, 10)))
        assertNull(Campaign.chestAfter(Campaign.level(1, 11)))
    }

    @Test
    fun `each chest offers two different boosters, never undo, that later levels allow`() {
        for (chest in Campaign.chests) {
            val later = levels.drop(levels.indexOf(chest.after) + 1)
            assertEquals(2, chest.choices.map { it.booster }.distinct().size, chest.after.code)
            for (reward in chest.choices) {
                assertTrue(reward.booster != Booster.UNDO, "the chest after ${chest.after.code} offers undo")
                assertTrue(later.any { reward.booster in it.boosters }, "no level after ${chest.after.code} allows ${reward.booster.text}")
            }
        }
    }

    @Test
    fun `a new chest without choices fails the tests`() {
        val longer = levels + levels.take(Chest.CHOICES.size * Chest.EVERY + 1)
        assertFailsWith<IllegalStateException> { Chest.of(longer) }
    }
}
