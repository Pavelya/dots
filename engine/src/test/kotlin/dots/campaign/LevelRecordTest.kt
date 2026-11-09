package dots.campaign

import dots.ai.AiConfig
import dots.ai.SearchAi
import dots.engine.Action
import dots.engine.Booster
import dots.engine.GameState
import dots.engine.Notation
import dots.engine.Player
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LevelRecordTest {

    private val map = LevelMap(
        listOf(
            ".........",
            ".........",
            "...B.....",
            "...RB....",
            "...BR....",
            ".....B...",
            ".........",
            ".........",
            ".........",
        ),
    )

    private val boosters = mapOf(Booster.WALL to 3, Booster.HINT to 2, Booster.BOMB to 2, Booster.DOUBLE_DOT to 2)

    private val level = Level(2, 5, "Test", Strength.EASY, 20, map, boosters = boosters)

    /** A legal booster for the player to move, picked at random, or null if there is none. */
    private fun randomBooster(state: GameState, random: Random): Action.UseBooster? {
        val points = (0 until state.width).flatMap { x -> (0 until state.height).map { y -> dots.engine.Pos(x, y) } }
        val options = buildList {
            addAll(points.map(Action::Wall))
            addAll(points.map(Action::Hint))
            addAll(points.map(Action::Bomb))
            add(Action.DoubleDot)
        }.filter { state.isLegal(it) }
        return options.randomOrNull(random)
    }

    /** The human plays at random and uses boosters often; the computer answers with [ai]. */
    private fun play(level: Level, ai: SearchAi, random: Random): List<Action> {
        val actions = ArrayList<Action>()
        var state = level.newGame()
        while (!level.isOver(state)) {
            val action = if (state.toMove == level.human) {
                val booster = if (random.nextInt(3) == 0) randomBooster(state, random) else null
                val moves = state.legalMoves()
                booster ?: if (moves.isEmpty()) Action.Pass else Action.Place(moves.random(random))
            } else {
                ai.choose(state).also { assertTrue(state.isLegal(it), "the computer chose $it in\n${state.toAscii()}") }
            }
            assertTrue(state.isLegal(action), "$action")
            state = state.apply(action).state
            actions += action
        }
        return actions
    }

    @Test
    fun `records with boosters replay exactly and survive their text form`() {
        val random = Random(4)
        val levels = listOf(level, level.copy(strength = Strength.NORMAL), level.copy(mission = Mission.Capture(3, 12)))
        var used = 0
        repeat(60) { i ->
            val level = levels[i % levels.size]
            val record = LevelRecord(level, play(level, SearchAi(level.ai, Random(i)), random))
            val text = record.toText()
            val parsed = LevelRecord.parse(text) { world, number ->
                assertEquals(level.world to level.number, world to number)
                level
            }

            assertEquals(record, parsed, text)
            val played = record.actions.fold(level.newGame()) { state, action -> state.apply(action).state }
            val replayed = parsed.replay()
            assertEquals(played.toAscii(), replayed.toAscii(), text)
            assertEquals(played.toMove, replayed.toMove, text)
            for (player in Player.entries) {
                assertEquals(played.score(player), replayed.score(player), text)
                assertEquals(played.dotsLeft(player), replayed.dotsLeft(player), text)
                for (booster in Booster.entries) {
                    assertEquals(played.boostersLeft(player, booster), replayed.boostersLeft(player, booster), text)
                }
            }
            assertEquals(level.status(played), level.status(replayed), text)
            used += record.actions.count { it is Action.UseBooster }
        }
        assertTrue(used > 100, "only $used boosters used")
    }

    @Test
    fun `a record names its level and lists the actions`() {
        val record = LevelRecord(
            level,
            listOf(Action.Wall(Notation.parse("e6")!!), Action.Place(Notation.parse("c3")!!), Action.Place(Notation.parse("g7")!!), Action.DoubleDot),
        )

        assertEquals("level/1 2-5 | wall:e6 c3 g7 double-dot", record.toText())
        assertEquals(record, LevelRecord.parse(record.toText()) { _, _ -> level })
        assertEquals(Campaign.level(1, 3), LevelRecord.parse("level/1 1-3 |").level)
        assertFailsWith<IllegalArgumentException> { LevelRecord.parse("level/1 2 | e5") { _, _ -> level } }
        assertFailsWith<IllegalArgumentException> { LevelRecord.parse("dots/1 2-5 | e5") { _, _ -> level } }
        assertFailsWith<IllegalArgumentException> { LevelRecord.parse("level/1 2-5 | boom:e5") { _, _ -> level } }
    }

    @Test
    fun `the level's boosters go to the player and none to the computer`() {
        val start = level.newGame()

        for (booster in Booster.entries) {
            assertEquals(boosters.getValue(booster), start.boostersLeft(level.human, booster))
            assertEquals(0, start.boostersLeft(level.computer, booster))
        }
        val plain = level.copy(boosters = emptyMap()).newGame()
        for (booster in Booster.entries) assertTrue(!plain.canUse(booster), "$booster")
        // The computer never uses them: its search only places dots, passes and stops.
        val reply = SearchAi(AiConfig.HARD, Random(1)).choose(start.play(Notation.parse("a1")!!).state)
        assertTrue(reply !is Action.UseBooster)
    }

    @Test
    fun `a target level cannot allow the bomb`() {
        val target = Mission.Target(Notation.parse("d3")!!)
        Level(1, 1, "Test", Strength.EASY, 20, map, target, mapOf(Booster.WALL to 1))

        val error = assertFailsWith<IllegalArgumentException> {
            Level(1, 1, "Test", Strength.EASY, 20, map, target, mapOf(Booster.BOMB to 1))
        }
        assertEquals("the mission 'target d3' cannot allow the bomb, which would remove the target", error.message)
    }
}
