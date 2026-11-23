package dots.campaign

import dots.engine.Action
import dots.engine.Booster
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** A level's own boosters are handed out once: one used is gone for every later attempt (decision 0040). */
class SpentBoosterTest {

    private fun actions(text: String) = text.split(' ').map(Action::parse)

    /** 2-1: survive 2 moves; wall 2, undo 1; stars for 2 and 3 boosters unused. */
    private val survive = Campaign.level(2, 1)

    /** 2-4: wall 1, undo 1. */
    private val wallOne = Campaign.level(2, 4)

    @Test
    fun `a used wall is gone from every later game of the level, an unused one stays`() {
        val wall = Booster.WALL
        assertEquals(mapOf(wall to 1, Booster.UNDO to 1), wallOne.boosters)
        val fresh = Progress.NONE
        assertEquals(1, fresh.game(wallOne).start().boostersLeft(wallOne.human, wall))

        val spent = fresh.spendOn(wallOne, mapOf(wall to 1))
        assertEquals(mapOf(wall to 1), spent.spent(wallOne))
        // A loss, a retry, a restart and a return from the map all start from the same progress.
        val next = spent.game(wallOne)
        assertEquals(0, next.start().boostersLeft(wallOne.human, wall), "the wall is gone")
        assertEquals(1, next.start().boostersLeft(wallOne.human, Booster.UNDO), "the unused undo stays")
        assertEquals(mapOf(wall to 0, Booster.UNDO to 1), spent.boostersFor(wallOne))
        // A replay after passing it too.
        val passed = spent.record(wallOne, 2)
        assertEquals(0, passed.game(wallOne).start().boostersLeft(wallOne.human, wall))
        // Other levels keep theirs.
        assertEquals(mapOf(wall to 2, Booster.UNDO to 1), spent.boostersFor(survive))
    }

    @Test
    fun `spending stays between none and what the level allows, and an undo gives back`() {
        val wall = Booster.WALL
        val once = Progress.NONE.spendOn(survive, mapOf(wall to 1))
        assertEquals(mapOf(wall to 1), once.spent(survive))
        assertEquals(mapOf(wall to 2), once.spendOn(survive, mapOf(wall to 5)).spent(survive), "never more than the level allows")
        assertEquals(Progress.NONE, once.spendOn(survive, mapOf(wall to -1)), "an undo gives it back")
        assertEquals(Progress.NONE, once.spendOn(survive, mapOf(wall to -3)), "never below none")
        assertEquals(once, once.spendOn(survive, mapOf(Booster.BOMB to 1)), "a kind the level does not allow")
        assertEquals(once, once.spendOn(survive, emptyMap()))
    }

    @Test
    fun `the player's own boosters add to what is left of the level's`() {
        val progress = Progress(boosters = mapOf(Booster.WALL to 2, Booster.BOMB to 1)).spendOn(wallOne, mapOf(Booster.WALL to 1))
        assertEquals(mapOf(Booster.WALL to 2, Booster.UNDO to 1), progress.boostersFor(wallOne))
        val record = progress.game(wallOne)
        assertEquals(mapOf(Booster.WALL to 2), record.owned)
        assertEquals(mapOf(Booster.WALL to 1), record.spent)
        assertEquals(2, record.start().boostersLeft(wallOne.human, Booster.WALL))
    }

    @Test
    fun `the free hint and the ad's booster are for one game`() {
        val progress = Progress.NONE.spendOn(survive, mapOf(Booster.WALL to 2))
        val withAd = progress.game(survive, ad = Booster.WALL)
        assertEquals(1, withAd.start().boostersLeft(survive.human, Booster.WALL))
        val withHint = progress.game(survive, gift = mapOf(Booster.HINT to 1))
        assertEquals(1, withHint.start().boostersLeft(survive.human, Booster.HINT))

        val after = progress.game(survive)
        assertEquals(0, after.start().boostersLeft(survive.human, Booster.WALL))
        assertEquals(0, after.start().boostersLeft(survive.human, Booster.HINT))
        // Neither one, used, is the level's own.
        val adUsed = LevelRecord(survive, actions("wall:d5 d6 a7"), ad = Booster.WALL, spent = mapOf(Booster.WALL to 2))
        assertEquals(emptyMap(), adUsed.levelUsed(adUsed.replay()))
    }

    @Test
    fun `a game counts the level's own boosters it used, after the player's own and the gift`() {
        val wall = Booster.WALL
        val two = LevelRecord(survive, actions("wall:d5 d6 a7 wall:g1 a1"))
        assertEquals(mapOf(wall to 2), two.levelUsed(two.replay()))

        val owned = two.copy(owned = mapOf(wall to 1))
        assertEquals(mapOf(wall to 1), owned.levelUsed(owned.replay()))
        assertEquals(mapOf(wall to 1), owned.ownedUsed(owned.replay()))

        val gift = two.copy(gift = mapOf(wall to 2))
        assertEquals(emptyMap(), gift.levelUsed(gift.replay()))

        // After one wall spent before, the game has one wall: using it spends the level's last.
        val second = LevelRecord(survive, actions("wall:d5 d6 a7"), spent = mapOf(wall to 1))
        assertEquals(mapOf(wall to 1), second.levelUsed(second.replay()))
        assertFailsWith<IllegalArgumentException>("no second wall") {
            LevelRecord(survive, actions("wall:d5 d6 a7 wall:g1 a1"), spent = mapOf(wall to 1)).replay()
        }
    }

    @Test
    fun `an undo gives back the wall of the turn it takes back, and stays used itself`() {
        val record = LevelRecord(survive, actions("wall:d5 d6 a7 undo"))
        val state = record.replay()
        assertEquals(2, state.boostersLeft(survive.human, Booster.WALL))
        assertEquals(mapOf(Booster.UNDO to 1), record.levelUsed(state))

        val next = Progress.NONE.spendOn(survive, record.levelUsed(state)).game(survive)
        assertEquals(0, next.start().boostersLeft(survive.human, Booster.UNDO))
        assertEquals(2, next.start().boostersLeft(survive.human, Booster.WALL))
    }

    @Test
    fun `stars count only the boosters used in this attempt`() {
        assertTrue(survive.mission is Mission.Survive && survive.stars?.measure == StarMeasure.BOOSTERS)
        val all = mapOf(Booster.WALL to 2, Booster.UNDO to 1)
        val plays = mapOf("no wall" to "d5 a7 a1 g7", "one wall" to "wall:d5 d6 a7 a1 g7")
        val first = plays.mapValues { (_, play) -> LevelRecord(survive, actions(play)).let { it.result(it.replay()) } }
        assertEquals(3, first.getValue("no wall").stars)
        assertEquals(2, first.getValue("one wall").stars)

        // An attempt that uses none after earlier attempts used them all earns the same as a first one.
        val none = LevelRecord(survive, actions(plays.getValue("no wall")), spent = all)
        assertEquals(first.getValue("no wall"), none.result(none.replay()))
        // One wall used costs the same on any attempt.
        for (spent in listOf(emptyMap(), mapOf(Booster.WALL to 1), mapOf(Booster.UNDO to 1))) {
            val one = LevelRecord(survive, actions(plays.getValue("one wall")), spent = spent)
            assertEquals(first.getValue("one wall"), one.result(one.replay()), "after $spent")
        }
        // The ad's wall, with the level's walls spent before, costs the same as one of the level's own.
        val ad = LevelRecord(survive, actions(plays.getValue("one wall")), ad = Booster.WALL, spent = all)
        assertEquals(first.getValue("one wall"), ad.result(ad.replay()))
        val adUnused = LevelRecord(survive, actions(plays.getValue("no wall")), ad = Booster.WALL, spent = all)
        assertEquals(first.getValue("no wall"), adUnused.result(adUnused.replay()))
    }

    @Test
    fun `every boosters level earns the same stars whatever was spent before`() {
        val levels = Campaign.levels.filter { it.stars?.measure == StarMeasure.BOOSTERS }
        assertEquals(18, levels.size)
        for (level in levels) {
            val start = level.newGame()
            val all = level.boosters
            assertEquals(level.starScore(start), level.starScore(level.newGame(spent = all), spent = all), level.code)
            assertEquals(level.boosters.values.sum(), level.starScore(start), level.code)
        }
    }

    @Test
    fun `a record says what its game started without, and older records replay with everything`() {
        val record = LevelRecord(survive, actions("wall:d5 d6 a7"), gift = mapOf(Booster.HINT to 1), spent = mapOf(Booster.WALL to 1, Booster.UNDO to 1))
        val text = record.toText()
        assertEquals("level/2 2-1 -wall -undo +hint | wall:d5 d6 a7", text)
        assertEquals(record, LevelRecord.parse(text))
        assertEquals(0, LevelRecord.parse(text).replay().boostersLeft(survive.human, Booster.WALL))

        val older = LevelRecord.parse("level/2 2-1 | wall:d5 d6 a7 wall:g1 a1")
        assertEquals(emptyMap(), older.spent)
        assertEquals(0, older.replay().boostersLeft(survive.human, Booster.WALL))
        assertEquals(1, older.replay().boostersLeft(survive.human, Booster.UNDO))
    }

    @Test
    fun `spent boosters are stored as one line and read back, damaged entries left out`() {
        val progress = Progress(stars = mapOf("2-1" to 2))
            .spendOn(survive, mapOf(Booster.WALL to 1, Booster.UNDO to 1))
            .spendOn(Campaign.level(1, 2), mapOf(Booster.UNDO to 1))
        assertEquals("1-2:undo:1 2-1:wall:1 2-1:undo:1", progress.spentText)
        assertEquals(progress, Progress.parse(progress.text, spent = progress.spentText))
        assertEquals(Progress.NONE, Progress.parse("", spent = ""), "progress saved before spent boosters existed")
        assertEquals(
            mapOf("2-1" to mapOf(Booster.WALL to 1)),
            Progress.parse("", spent = "2-1:wall:1 2-1:moon:1 x:wall:1 2-1:undo:0 2-1:undo 1-1:hint:x").spent,
        )
    }
}
