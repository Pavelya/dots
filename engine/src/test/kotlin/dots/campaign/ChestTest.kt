package dots.campaign

import dots.engine.Booster
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChestTest {
    private val levels = Campaign.levels

    /** The levels a player meets between [chest] and the next chest: up to the level before it, or to the end. */
    private fun levelsUntilNext(chest: Chest): List<Level> {
        val from = levels.indexOf(chest.after) + 1
        val next = Campaign.chests.getOrNull(Campaign.chests.indexOf(chest) + 1)
        val until = next?.let { levels.indexOf(it.after) + 1 } ?: levels.size
        return levels.subList(from, until)
    }

    private fun kinds(chest: Chest) = chest.choices.map { it.booster }

    @Test
    fun `a chest follows every 10th level of the campaign`() {
        assertEquals(listOf("1-10", "2-5", "2-15", "3-5", "3-15", "4-5", "4-15"), Campaign.chests.map { it.after.code })
        assertEquals(Campaign.chests.first(), Campaign.chestAfter(Campaign.level(1, 10)))
        assertNull(Campaign.chestAfter(Campaign.level(1, 11)))
    }

    /** Reads every level file: a change to the levels that breaks the rule fails here (decision 0038). */
    @Test
    fun `each chest offers only kinds that a level before the next chest allows, never undo`() {
        for (chest in Campaign.chests) {
            val until = levelsUntilNext(chest)
            assertEquals(kinds(chest).distinct(), kinds(chest), chest.after.code)
            for (reward in chest.choices) {
                assertTrue(reward.booster != Booster.UNDO, "the chest after ${chest.after.code} offers undo")
                assertTrue(
                    until.any { reward.booster in it.boosters },
                    "no level from ${until.first().code} to ${until.last().code}, after the chest after ${chest.after.code}, allows ${reward.booster.text}",
                )
            }
        }
    }

    @Test
    fun `where the levels up to the next chest allow one kind, the chest holds just that one`() {
        for (chest in Campaign.chests) {
            val allowed = levelsUntilNext(chest).flatMap { it.boosters.keys }.toSet() - Booster.UNDO
            if (allowed.size == 1) assertEquals(listOf(Reward(allowed.single(), Chest.COUNT)), chest.choices, chest.after.code)
        }
    }

    @Test
    fun `the chests after 1-10 and 2-5 give two walls, with no pick`() {
        for (code in listOf("1-10", "2-5")) {
            val chest = Campaign.chests.single { it.after.code == code }
            assertTrue(chest.isSingle, code)
            assertEquals(listOf(Reward(Booster.WALL, 2)), chest.choices, code)
        }
    }

    @Test
    fun `the other five chests keep their two choices`() {
        val pairs = Campaign.chests.filterNot { it.isSingle }.associate { it.after.code to kinds(it) }
        assertEquals(
            mapOf(
                "2-15" to listOf(Booster.WALL, Booster.BOMB),
                "3-5" to listOf(Booster.BOMB, Booster.DOUBLE_DOT),
                "3-15" to listOf(Booster.DOUBLE_DOT, Booster.HINT),
                "4-5" to listOf(Booster.HINT, Booster.BOMB),
                "4-15" to listOf(Booster.WALL, Booster.HINT),
            ),
            pairs,
        )
        assertTrue(Campaign.chests.flatMap { it.choices }.all { it.count == Chest.COUNT })
    }

    @Test
    fun `a chest holds one or two different boosters`() {
        val level = Campaign.level(1, 10)
        assertFailsWith<IllegalArgumentException> { Chest(level, emptyList()) }
        assertFailsWith<IllegalArgumentException> { Chest(level, listOf(Reward(Booster.WALL, 2), Reward(Booster.WALL, 2))) }
        assertFailsWith<IllegalArgumentException> {
            Chest(level, listOf(Reward(Booster.WALL, 2), Reward(Booster.BOMB, 2), Reward(Booster.HINT, 2)))
        }
        assertFalse(Chest(level, listOf(Reward(Booster.WALL, 2), Reward(Booster.BOMB, 2))).isSingle)
    }

    @Test
    fun `a new chest without choices fails the tests`() {
        val longer = levels + levels.take(Chest.CHOICES.size * Chest.EVERY + 1)
        assertFailsWith<IllegalStateException> { Chest.of(longer) }
    }

    @Test
    fun `each chest reward is first usable on these levels`() {
        val firstUse = Campaign.chests.associate { chest ->
            chest.after.code to chest.choices.associate { it.booster to Campaign.firstUse(chest, it.booster)?.code }
        }
        assertEquals(
            mapOf(
                "1-10" to mapOf(Booster.WALL to "2-1"),
                "2-5" to mapOf(Booster.WALL to "2-6"),
                "2-15" to mapOf(Booster.WALL to "2-16", Booster.BOMB to "3-1"),
                "3-5" to mapOf(Booster.BOMB to "3-6", Booster.DOUBLE_DOT to "3-6"),
                "3-15" to mapOf(Booster.DOUBLE_DOT to "3-16", Booster.HINT to "4-1"),
                "4-5" to mapOf(Booster.HINT to "4-6", Booster.BOMB to "4-6"),
                "4-15" to mapOf(Booster.WALL to "4-16", Booster.HINT to "4-17"),
            ),
            firstUse,
        )
    }

    /**
     * Reads the `boosters:` line of each level file itself, not the parsed level: the first use of
     * every chest reward is the first file after the chest that lists its kind.
     */
    @Test
    fun `the first use of every chest reward matches the level files`() {
        val listed = levels.associate { level ->
            val text = Campaign::class.java.getResourceAsStream("/${Campaign.FOLDER}/${Campaign.fileName(level.world, level.number)}")!!
                .use { it.readBytes().toString(Charsets.UTF_8) }
            val line = text.lines().single { it.trim().startsWith("boosters:") }
            level.code to line.substringAfter(':').split(',').map { it.trim().substringBefore(' ') }.toSet()
        }
        for (chest in Campaign.chests) {
            for (reward in chest.choices) {
                val name = reward.booster.text
                val expected = levels.drop(levels.indexOf(chest.after) + 1).first { name in listed.getValue(it.code) }
                assertEquals(expected, Campaign.firstUse(chest, reward.booster), "$name from the chest after ${chest.after.code}")
            }
        }
    }

    @Test
    fun `the first level that allows a kind counts the level itself`() {
        val bombs = Booster.BOMB
        assertEquals("3-1", Campaign.firstAllowing(bombs, Campaign.level(1, 11))?.code)
        assertEquals("3-1", Campaign.firstAllowing(bombs, Campaign.level(3, 1))?.code, "3-1 allows bombs itself")
        assertEquals("3-3", Campaign.firstAllowing(bombs, Campaign.level(3, 2))?.code)
        assertNull(Campaign.firstAllowing(bombs, Campaign.level(1, 1).copy(world = 9)), "a level not in the campaign")
        assertEquals("2-1", Campaign.after(Campaign.level(1, 15))?.code, "across worlds")
        assertNull(Campaign.after(Campaign.levels.last()))
    }
}
