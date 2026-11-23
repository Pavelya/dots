package dots.campaign

import dots.engine.Booster
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressTest {
    private val worlds = Campaign.worlds

    private fun level(code: String): Level {
        val (world, number) = code.split('-').map(String::toInt)
        return Campaign.level(world, number)
    }

    /** Every level from 1-1 to [last], in order, passed with [stars] stars. */
    private fun passedUpTo(last: String, stars: Int = 1): Progress {
        val end = Campaign.levels.indexOf(level(last))
        return Progress(Campaign.levels.take(end + 1).associate { it.code to stars })
    }

    @Test
    fun `every world has a name and the stars it needs, which grow and can be earned before it`() {
        assertEquals(Campaign.levels.map { it.world }.distinct(), worlds.map { it.number })
        assertEquals(0, worlds.first().starsToOpen)
        for ((before, world) in worlds.zipWithNext()) {
            assertTrue(world.starsToOpen > before.starsToOpen, world.name)
            val earlier = worlds.takeWhile { it != world }
            assertTrue(world.starsToOpen <= earlier.sumOf { it.maxStars }, "${world.name} needs more stars than the worlds before it give")
            // Passing every level before it with one star is not enough.
            assertTrue(world.starsToOpen > earlier.sumOf { it.levels.size }, world.name)
        }
    }

    @Test
    fun `every world ends with its boss`() {
        for (world in worlds) {
            assertTrue(world.levels.last().rule != null, world.name)
            assertTrue(world.levels.dropLast(1).none { it.rule != null }, world.name)
        }
    }

    @Test
    fun `a new player has only level 1-1 open`() {
        val map = Campaign.map(Progress.NONE)

        assertEquals(level("1-1"), map.next)
        assertEquals(0, map.totalStars)
        assertEquals(listOf(true, false, false, false), map.worlds.map { it.isOpen })
        val states = map.worlds.flatMap { it.levels }.map { it.state }
        assertEquals(LevelState.OPEN, states.first())
        assertTrue(states.drop(1).all { it == LevelState.LOCKED })
        assertEquals(CampaignMap.Lock.PassFirst(level("1-1")), map.lock(level("1-2")))
        assertNull(map.lock(level("1-1")))
    }

    @Test
    fun `passing a level opens the next one`() {
        val map = Campaign.map(Progress.NONE.record(level("1-1"), 2).record(level("1-2"), 3))

        assertEquals(LevelState.PASSED, map.stop(level("1-1")).state)
        assertEquals(2, map.stop(level("1-1")).stars)
        assertEquals(LevelState.OPEN, map.stop(level("1-3")).state)
        assertEquals(LevelState.LOCKED, map.stop(level("1-4")).state)
        assertEquals(level("1-3"), map.next)
        assertEquals(5, map.totalStars)
        assertEquals(5, map.worlds[0].stars)
    }

    @Test
    fun `a replay keeps the best stars`() {
        val progress = Progress.NONE.record(level("1-1"), 3)

        assertEquals(progress, progress.record(level("1-1"), 1))
        assertEquals(3, progress.stars(level("1-1")))
        assertFalse(progress.isPassed(level("1-2")))
    }

    @Test
    fun `a world without its stars stays locked after the boss, and says how many it needs`() {
        val map = Campaign.map(passedUpTo("1-15", stars = 1))
        val garden = map.worlds[1]

        assertFalse(garden.isOpen)
        assertEquals(5, garden.starsNeeded)
        assertNull(garden.bossNeeded)
        assertNull(map.next, "nothing new to play: the player replays for stars")
        assertEquals(CampaignMap.Lock.WorldClosed(garden), map.lock(level("2-1")))
    }

    @Test
    fun `a world with its stars stays locked until the boss before it is passed`() {
        val map = Campaign.map(passedUpTo("1-14", stars = 3))
        val garden = map.worlds[1]

        assertFalse(garden.isOpen)
        assertEquals(0, garden.starsNeeded)
        assertEquals(level("1-15"), garden.bossNeeded)
        assertEquals(level("1-15"), map.next)
    }

    @Test
    fun `the boss and the stars open the next world`() {
        val map = Campaign.map(passedUpTo("1-15", stars = 2))

        assertTrue(map.worlds[1].isOpen)
        assertEquals(level("2-1"), map.next)
        assertTrue(map.stop(level("1-15")).isBoss)
        assertFalse(map.worlds[2].isOpen)
        assertEquals(45 - 30, map.worlds[2].starsNeeded)
        assertEquals(level("2-20"), map.worlds[2].bossNeeded)
    }

    @Test
    fun `a pass says which levels and worlds it opened`() {
        val before = Campaign.map(passedUpTo("1-6"))
        val after = Campaign.map(passedUpTo("1-6").record(level("1-7"), 2))

        assertEquals(CampaignMap.Opened(emptyList(), listOf(level("1-8"))), after.openedSince(before))
        assertTrue(before.openedSince(before).isEmpty)
        // A replay for more stars opens nothing.
        assertTrue(Campaign.map(passedUpTo("1-7").record(level("1-7"), 3)).openedSince(Campaign.map(passedUpTo("1-7"))).isEmpty)

        val boss = Campaign.map(passedUpTo("1-14", stars = 2))
        val opened = Campaign.map(passedUpTo("1-15", stars = 2)).openedSince(boss)
        assertEquals(listOf(worlds[1]), opened.worlds)
        assertEquals(listOf(level("2-1")), opened.levels)
    }

    @Test
    fun `with every level passed there is no next level`() {
        val map = Campaign.map(passedUpTo(Campaign.levels.last().code, stars = 3))

        assertNull(map.next)
        assertTrue(map.worlds.all { it.isOpen })
        assertEquals(Campaign.levels.size * 3, map.totalStars)
    }

    @Test
    fun `progress is stored as one line and read back`() {
        val progress = Progress(mapOf("2-1" to 1, "1-10" to 3, "1-2" to 2))

        assertEquals("1-2:2 1-10:3 2-1:1", progress.text)
        assertEquals(progress, Progress.parse(progress.text))
        assertEquals(Progress.NONE, Progress.parse(""))
    }

    @Test
    fun `entries that cannot be read are left out`() {
        assertEquals(Progress(mapOf("1-1" to 3)), Progress.parse("1-1:3 1-2:4 x-1:2 1-3 :2 1-4:0"))
    }

    @Test
    fun `owned boosters, opened chests and cosmetics are stored as lines and read back`() {
        val progress = Progress(
            stars = mapOf("1-10" to 3),
            boosters = mapOf(Booster.BOMB to 2, Booster.WALL to 1),
            chests = setOf("2-5", "1-10"),
            cosmetics = setOf("ink-splash"),
        )

        assertEquals("wall:1 bomb:2", progress.boostersText)
        assertEquals("1-10 2-5", progress.chestsText)
        assertEquals("ink-splash", progress.cosmeticsText)
        assertEquals(progress, Progress.parse(progress.text, progress.boostersText, progress.chestsText, progress.cosmeticsText))
    }

    @Test
    fun `progress saved before boosters and chests existed reads with none of them`() {
        val old = Progress.parse("1-1:3 1-2:2")

        assertEquals(mapOf("1-1" to 3, "1-2" to 2), old.stars)
        assertEquals(emptyMap(), old.boosters)
        assertEquals(emptySet(), old.chests)
        assertEquals(emptySet(), old.cosmetics)
    }

    @Test
    fun `damaged boosters, chests and cosmetics lose only the entries that cannot be read`() {
        val progress = Progress.parse(
            stars = "1-1:3",
            boosters = "wall:2 boom:1 bomb:0 hint:-1 double-dot:x bomb",
            chests = "1-10 ten 2- 2-5",
            cosmetics = "ink-splash Bad! -x",
        )

        assertEquals(mapOf(Booster.WALL to 2), progress.boosters)
        assertEquals(setOf("1-10", "2-5"), progress.chests)
        assertEquals(setOf("ink-splash"), progress.cosmetics)
    }

    @Test
    fun `a chest opens once, after the level before it, and its reward is kept`() {
        val chest = Campaign.chests.first()
        val reward = chest.choices.last()
        val before = passedUpTo("1-9")
        assertFalse(before.isReady(chest))

        val passed = before.record(level("1-10"), 2)
        assertTrue(passed.isReady(chest))
        assertEquals(ChestState.READY, Campaign.map(passed).chestAfter(level("1-10"))?.state)

        val opened = passed.open(chest, reward)
        assertEquals(mapOf(reward.booster to reward.count), opened.boosters)
        assertFalse(opened.isReady(chest))
        assertEquals(ChestState.OPENED, Campaign.map(opened).chests.first().state)
        assertEquals(opened, opened.open(chest, chest.choices.first()), "a chest opens once")
        assertFailsWith<IllegalArgumentException> { passed.open(chest, Reward(Booster.UNDO, 1)) }
    }

    @Test
    fun `owned boosters go to the levels that allow them, are spent and given back`() {
        val progress = Progress(boosters = mapOf(Booster.WALL to 2, Booster.BOMB to 1))

        assertEquals(emptyMap(), progress.owned(level("1-11")), "World 1 allows only undo")
        assertEquals(mapOf(Booster.WALL to 2), progress.owned(level("2-1")))
        assertEquals(mapOf(Booster.WALL to 2, Booster.BOMB to 1), progress.owned(level("4-3")))
        // What a level leaves out waits for a later level, and still counts as the player's.
        assertEquals(mapOf(Booster.WALL to 2, Booster.BOMB to 1), progress.waiting(level("1-11")))
        assertEquals(mapOf(Booster.BOMB to 1), progress.waiting(level("2-1")))
        assertEquals(emptyMap(), progress.waiting(level("4-3")))

        val spent = progress.spend(mapOf(Booster.WALL to 1, Booster.BOMB to 1))
        assertEquals(mapOf(Booster.WALL to 1), spent.boosters)
        assertEquals(progress, spent.spend(mapOf(Booster.WALL to -1, Booster.BOMB to -1)))
        assertEquals(emptyMap(), spent.spend(mapOf(Booster.WALL to 5)).boosters, "a count never goes below 0")
    }

    @Test
    fun `stars and chests keep each other's parts`() {
        val chest = Campaign.chests.first()
        val opened = passedUpTo("1-10").open(chest, chest.choices.first())

        val replayed = opened.record(level("1-11"), 3)
        assertEquals(opened.boosters, replayed.boosters)
        assertEquals(opened.chests, replayed.chests)
    }

    @Test
    fun `a passed level has one to three stars`() {
        assertFailsWith<IllegalArgumentException> { Progress(mapOf("1-1" to 0)) }
    }
}
