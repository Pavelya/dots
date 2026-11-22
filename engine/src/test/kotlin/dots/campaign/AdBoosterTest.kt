package dots.campaign

import dots.ai.SearchAi
import dots.engine.Action
import dots.engine.Booster
import dots.engine.Game
import dots.engine.GameState
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The booster a rewarded ad gives for one retry of a level (decision 0030). */
class AdBoosterTest {

    private fun actions(text: String) = text.split(' ').map(Action::parse)

    @Test
    fun `a level offers the first kind it allows, never undo`() {
        assertEquals(Booster.WALL, Campaign.level(2, 1).adBooster, "wall 2, undo 1")
        assertEquals(Booster.HINT, Campaign.level(4, 8).adBooster, "hint 1, double-dot 1, undo 1")
        assertEquals(Booster.WALL, Campaign.level(4, 6).adBooster, "all five kinds")
        assertNull(Campaign.level(1, 1).adBooster, "undo only")
        assertNull(Campaign.level(1, 1).copy(boosters = emptyMap()).adBooster, "no boosters")
        for (level in Campaign.levels) {
            val offered = level.adBooster ?: continue
            assertTrue(offered != Booster.UNDO && offered in level.boosters, "${level.code} offers ${offered.text}")
        }
    }

    @Test
    fun `the ad's booster is one more of its kind, for this game only`() {
        val level = Campaign.level(2, 1)
        val record = LevelRecord(level, emptyList(), ad = Booster.WALL)
        val start = record.start()

        assertEquals(3, start.boostersLeft(level.human, Booster.WALL))
        assertEquals(1, start.boostersLeft(level.human, Booster.UNDO))
        assertEquals(2, level.newGame().boostersLeft(level.human, Booster.WALL), "the next game has the level's own")
        assertFailsWith<IllegalArgumentException>("never undo") { LevelRecord(level, emptyList(), ad = Booster.UNDO) }
        assertFailsWith<IllegalArgumentException>("a kind the level allows") { LevelRecord(level, emptyList(), ad = Booster.BOMB) }
    }

    @Test
    fun `the ad's booster does not join the player's own`() {
        val level = Campaign.level(2, 1)
        val adOnly = LevelRecord(level, actions("wall:d5 d6 a7 wall:g1 a1"), ad = Booster.WALL)
        assertEquals(emptyMap(), adOnly.ownedUsed(adOnly.replay()), "nothing to take from the player's own")

        // The player's own are used first, then the ad's and the level's: two walls use one of theirs.
        val both = LevelRecord(level, actions("wall:d5 d6 a7 wall:g1 a1"), owned = mapOf(Booster.WALL to 1), ad = Booster.WALL)
        assertEquals(4, both.start().boostersLeft(level.human, Booster.WALL))
        assertEquals(mapOf(Booster.WALL to 1), both.ownedUsed(both.replay()))
    }

    @Test
    fun `a survive level played with the ad's booster earns no more stars than with a level booster used`() {
        val level = Campaign.level(2, 1) // survive 2 moves; wall 2, undo 1; stars for 2 and 3 boosters unused
        assertTrue(level.mission is Mission.Survive && level.stars?.measure == StarMeasure.BOOSTERS)
        val plays = mapOf(
            // Fills the gap below d4 with a dot: no booster used.
            "no wall" to "d5 a7 a1 g7",
            // Walls the gap: one wall used.
            "one wall" to "wall:d5 d6 a7 a1 g7",
            // Walls the gap, then walls another point: two walls used, with an ad as many as the level has.
            "two walls" to "wall:d5 d6 a7 wall:g1 a1 g7",
        )
        val expected = mapOf("no wall" to 3, "one wall" to 2, "two walls" to 1)
        for ((name, play) in plays) {
            val without = LevelRecord(level, actions(play))
            val with = LevelRecord(level, actions(play), ad = Booster.WALL)
            val end = without.replay()
            assertEquals(end.toAscii(), with.replay().toAscii(), name)
            assertTrue(level.isOver(end) && level.isPassed(end), "$name passes:\n${end.toAscii()}")

            assertEquals(expected.getValue(name), without.result(end).stars, name)
            assertEquals(without.result(end), with.result(with.replay()), "$name: the ad adds no star")
        }
        // Counted as a gift, the ad's wall would have left one of the level's walls, for 2 stars.
        val twoWalls = LevelRecord(level, actions(plays.getValue("two walls")), ad = Booster.WALL)
        assertEquals(2, level.result(twoWalls.replay()).stars)
    }

    @Test
    fun `using the ad's booster beyond the level's own costs a booster of another kind`() {
        val level = Campaign.level(2, 4).copy(stars = Stars(StarMeasure.BOOSTERS, two = 1, three = 2)) // wall 1, undo 1
        val record = LevelRecord(level, emptyList(), ad = Booster.WALL)
        var state = record.start()
        assertEquals(2, level.starScore(state, Booster.WALL), "the ad's wall unused earns nothing")
        assertEquals(2, level.starScore(level.newGame()))
        state = state.apply(Action.Wall(state.legalMoves().first())).state
        assertEquals(1, level.starScore(state, Booster.WALL))
        state = state.apply(Action.Place(state.legalMoves().first())).state
        state = state.apply(Action.Place(state.legalMoves().first())).state
        state = state.apply(Action.Wall(state.legalMoves().first())).state
        assertEquals(0, state.boostersLeft(level.human, Booster.WALL))
        assertEquals(0, level.starScore(state, Booster.WALL), "two walls cost both of the level's boosters")
    }

    @Test
    fun `games without the ad's booster earn the same stars with it`() {
        val random = Random(28)
        val levels = Campaign.levels.filter { it.adBooster != null }
        for (measure in StarMeasure.entries - StarMeasure.SAVED) {
            assertTrue(levels.count { it.stars?.measure == measure } >= 5, "only a few ${measure.text} levels")
        }
        var boosted = 0
        for (level in levels) {
            repeat(3) { i ->
                val without = LevelRecord(level, play(level, SearchAi(level.ai, Random(i)), random))
                val with = LevelRecord(level, without.actions, ad = level.adBooster)
                assertEquals(without.result(without.replay()), with.result(with.replay()), "${level.code}: ${without.toText()}")
                if (without.actions.any { it is Action.UseBooster && it !is Action.Undo }) boosted++
            }
        }
        assertTrue(boosted >= 10, "only $boosted games used a booster")
    }

    @Test
    fun `a record with the ad's booster round-trips and replays`() {
        val level = Campaign.level(2, 1)
        val record = LevelRecord(level, actions("wall:d5 d6 a7 wall:g1 a1 g7"), gift = mapOf(Booster.HINT to 1), owned = mapOf(Booster.WALL to 1), ad = Booster.WALL)
        val text = record.toText()

        assertEquals("level/1 2-1 +hint *wall !wall | wall:d5 d6 a7 wall:g1 a1 g7", text)
        val read = LevelRecord.parse(text)
        assertEquals(record, read)
        assertEquals(Booster.WALL, read.ad)
        assertEquals(record.replay().toAscii(), read.replay().toAscii())
        assertEquals(record.replay().toAscii(), Game.from(read).state.toAscii())
        assertEquals(record, Game.from(read).record)
        assertNull(LevelRecord.parse("level/1 2-1 *wall | d5").ad, "a record without the word has no ad")
        assertFailsWith<IllegalArgumentException>("one ad, one booster") { LevelRecord.parse("level/1 2-1 !wall !wall |") }
        assertFailsWith<IllegalArgumentException> { LevelRecord.parse("level/1 2-1 !undo |") }
        assertFailsWith<IllegalArgumentException> { LevelRecord.parse("level/1 2-1 !bomb |") }
        assertFailsWith<IllegalArgumentException> { LevelRecord.parse("level/1 2-1 !boom |") }
    }

    /** Red's d3 encloses both blue dots, c3 and e3, at once. Six red dots, none in danger. */
    private val twoAtOnce = LevelMap(
        listOf(
            ".......",
            "..R.R..",
            ".RB.BR.",
            "..R.R..",
            ".......",
        ),
    )

    /** A test level on [twoAtOnce] allowing one wall and one undo, so the ad offers a wall. */
    private fun level(mission: Mission, dots: Int, stars: Stars) =
        Level(1, 1, "Test", Strength.EASY, dots, twoAtOnce, mission, mapOf(Booster.WALL to 1, Booster.UNDO to 1), stars = stars)

    @Test
    fun `a game that used the ad's booster earns 1 star on margin, moves and saved levels`() {
        // Each game walls a corner on two turns, so the second wall is the ad's, then captures both blue dots with d3.
        val cases = listOf(
            // Win with 2 dots each: 2-0, a margin of 2.
            Triple(level(Mission.Win, dots = 2, Stars(StarMeasure.MARGIN, two = 2, three = 3)), "wall:g1 b1 a1 wall:a5 d3 g5", 2),
            // Capture 2 in 4 moves with 2 dots placed: 2 moves to spare.
            Triple(level(Mission.Capture(dots = 2, moves = 4), dots = 4, Stars(StarMeasure.MOVES, two = 2, three = 3)), "wall:g1 b1 a1 wall:a5 d3", 2),
            // Capture 2 in 4 moves with none of red's 6 dots lost.
            Triple(level(Mission.Capture(dots = 2, moves = 4), dots = 4, Stars(StarMeasure.SAVED, two = 5, three = 6)), "wall:g1 b1 a1 wall:a5 d3", 3),
        )
        for ((level, play, usual) in cases) {
            val name = level.stars!!.measure.text
            assertEquals(Booster.WALL, level.adBooster, name)
            val record = LevelRecord(level, actions(play), ad = Booster.WALL)
            val end = record.replay()
            assertTrue(level.isOver(end) && level.isPassed(end), "$name passes:\n${end.toAscii()}")
            assertTrue(level.adBoosterUsed(end, record.ad), name)
            assertFailsWith<IllegalArgumentException>("$name: the second wall needs the ad's") { LevelRecord(level, record.actions).replay() }

            assertEquals(usual, level.result(end).stars, "$name: the same count without the ad")
            val result = record.result(end)
            assertTrue(result.isPassed, name)
            assertEquals(level.result(end).starScore, result.starScore, "$name: the count is the same")
            assertEquals(1, result.stars, "$name: the ad's booster earns a pass, no more")
        }
    }

    @Test
    fun `a game with the ad's booster unused, or used and undone, earns the usual stars`() {
        val level = level(Mission.Capture(dots = 2, moves = 4), dots = 4, Stars(StarMeasure.MOVES, two = 2, three = 3))
        val plays = mapOf(
            // Never uses a wall: 3 moves to spare.
            "no wall" to ("d3" to 3),
            // Uses the level's wall only: 2 moves to spare.
            "the level's wall" to ("wall:g1 b1 a1 d3" to 2),
            // Uses both walls, takes the second turn back and captures instead: the ad's wall is back.
            "undone" to ("wall:g1 b1 a1 wall:a5 f1 g5 undo d3" to 2),
        )
        for ((name, pair) in plays) {
            val (play, usual) = pair
            val record = LevelRecord(level, actions(play), ad = Booster.WALL)
            val end = record.replay()
            assertTrue(level.isOver(end) && level.isPassed(end), "$name passes:\n${end.toAscii()}")
            assertTrue(end.boostersLeft(level.human, Booster.WALL) >= 1, "$name: a wall left")
            assertFalse(level.adBoosterUsed(end, record.ad), name)
            assertEquals(usual, record.result(end).stars, name)
            assertEquals(level.result(end), record.result(end), "$name: the same as without the ad")
        }
    }

    @Test
    fun `a boosters level keeps counting the ad's booster as one of its own`() {
        val level = Level(
            1, 1, "Test", Strength.EASY, 2, twoAtOnce, Mission.Win,
            mapOf(Booster.WALL to 1, Booster.HINT to 1, Booster.UNDO to 1), stars = Stars(StarMeasure.BOOSTERS, two = 1, three = 2),
        )
        val record = LevelRecord(level, actions("wall:g1 b1 a1 wall:a5 d3 g5"), ad = Booster.WALL)
        val end = record.replay()
        assertTrue(level.isPassed(end) && level.adBoosterUsed(end, record.ad))

        // The two walls cost the level's wall and one more: the hint and the undo left, less one.
        val result = record.result(end)
        assertEquals(1, result.starScore)
        assertEquals(2, result.stars, "G-28's count earns 2 stars, not the 1 of the other measures")
    }

    /** The human plays at random and uses a booster now and then; the computer answers with [ai]. */
    private fun play(level: Level, ai: SearchAi, random: Random): List<Action> {
        val actions = ArrayList<Action>()
        var state = level.newGame()
        while (!level.isOver(state)) {
            val action = if (state.toMove == level.human) {
                val booster = if (random.nextInt(3) == 0) randomBooster(state, random) else null
                val moves = state.legalMoves()
                booster ?: if (moves.isEmpty()) Action.Pass else Action.Place(moves.random(random))
            } else {
                ai.choose(state)
            }
            actions += action
            state = state.apply(action).state
        }
        return actions
    }

    private fun randomBooster(state: GameState, random: Random): Action.UseBooster? {
        val points = state.legalMoves()
        return buildList {
            addAll(points.map(Action::Wall))
            addAll(points.map(Action::Hint))
            addAll(points.map(Action::Bomb))
            add(Action.DoubleDot)
            add(Action.Undo)
        }.filter { state.isLegal(it) }.randomOrNull(random)
    }
}
