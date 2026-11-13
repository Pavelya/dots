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

    private val boosters = mapOf(Booster.WALL to 3, Booster.HINT to 2, Booster.BOMB to 2, Booster.DOUBLE_DOT to 2, Booster.UNDO to 2)

    private val level = Level(2, 5, "Test", Strength.EASY, 20, map, boosters = boosters)

    /** A legal booster for the player to move, picked at random, or null if there is none. */
    private fun randomBooster(state: GameState, random: Random): Action.UseBooster? {
        val points = (0 until state.width).flatMap { x -> (0 until state.height).map { y -> dots.engine.Pos(x, y) } }
        val options = buildList {
            addAll(points.map(Action::Wall))
            addAll(points.map(Action::Hint))
            addAll(points.map(Action::Bomb))
            add(Action.DoubleDot)
            add(Action.Undo)
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
        var undos = 0
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
            undos += record.actions.count { it == Action.Undo }
        }
        assertTrue(used > 100, "only $used boosters used")
        assertTrue(undos > 10, "only $undos undos used")
    }

    @Test
    fun `a record with undos replays to the position the undos left`() {
        val actions = "a1 i9 wall:b1 b2 i8 c1 i7 undo undo d1 i8".split(' ').map(Action::parse)
        val record = LevelRecord(level, actions)
        val text = record.toText()
        assertEquals("level/1 2-5 | a1 i9 wall:b1 b2 i8 c1 i7 undo undo d1 i8", text)

        val replayed = LevelRecord.parse(text) { _, _ -> level }.replay()
        val game = dots.engine.Game.from(record)
        // Two undos take back two turns, so this is a1 i9 d1 i8, with both undos used and the wall given back.
        val same = LevelRecord(level, "a1 i9 d1 i8".split(' ').map(Action::parse)).replay()
        for (state in listOf(replayed, game.state)) {
            assertEquals(same.toAscii(), state.toAscii())
            assertEquals(same.toMove, state.toMove)
            assertEquals(same.dotsLeft(level.human), state.dotsLeft(level.human))
            assertEquals(same.dotsPlaced(level.human), state.dotsPlaced(level.human))
            for (booster in Booster.entries) {
                val used = if (booster == Booster.UNDO) 2 else 0
                assertEquals(same.boostersLeft(level.human, booster) - used, state.boostersLeft(level.human, booster), "$booster")
            }
        }
        assertEquals(record, game.record)
    }

    @Test
    fun `no undo once the level's mission is decided`() {
        val survive = level.copy(mission = Mission.Survive(1))
        val game = dots.engine.Game(LevelRecord(survive, emptyList()))
        game.apply(Action.Place(Notation.parse("a1")!!))
        game.apply(Action.Place(Notation.parse("i9")!!)) // the computer's one move, and no capture

        assertTrue(game.isOver)
        assertTrue(game.state.canUse(Booster.UNDO), "the board alone would allow it")
        assertTrue(!game.canUse(Booster.UNDO))
        assertFailsWith<IllegalArgumentException> { game.apply(Action.Undo) }
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
    fun `a gift of boosters is written in the record and replayed`() {
        val record = LevelRecord(level, listOf(Action.Place(Notation.parse("c3")!!)), gift = mapOf(Booster.HINT to 1))

        assertEquals("level/1 2-5 +hint | c3", record.toText())
        assertEquals(record, LevelRecord.parse(record.toText()) { _, _ -> level })
        assertEquals(boosters.getValue(Booster.HINT) + 1, record.start().boostersLeft(level.human, Booster.HINT))
        assertEquals(mapOf(Booster.WALL to 2), LevelRecord.parse("level/1 2-5 +wall +wall |") { _, _ -> level }.gift)
        assertFailsWith<IllegalArgumentException> { LevelRecord.parse("level/1 2-5 +boom |") { _, _ -> level } }
        assertFailsWith<IllegalArgumentException> { LevelRecord.parse("level/1 2-5 hint |") { _, _ -> level } }
    }

    @Test
    fun `a level game ends when its mission is decided, before the board is full`() {
        val capture = level.copy(mission = Mission.Capture(dots = 1, moves = 1))
        val game = dots.engine.Game(LevelRecord(capture, emptyList()))
        assertTrue(!game.isOver)

        game.apply(Action.Place(Notation.parse("a1")!!)) // the one move, and no capture
        assertTrue(game.isOver)
        assertTrue(!game.state.isOver)
        assertEquals(LevelRecord(capture, game.record.actions), game.record)
        assertEquals(game.state.toAscii(), dots.engine.Game.from(game.record).state.toAscii())
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

    @Test
    fun `records on boards with holes, blocked and bonus points replay exactly`() {
        val shaped = LevelMap(
            listOf(
                "--.....--",
                "-.......-",
                "....+....",
                "...B.#...",
                "..+RB....",
                "...BR.+..",
                "....-....",
                "-...#...-",
                "--.....--",
            ),
        )
        val random = Random(9)
        var scored = 0
        for (holes in listOf(Holes.BORDER, Holes.OPEN)) {
            val level = Level(3, 4, "Shapes", Strength.NORMAL, 20, shaped, boosters = boosters, holes = holes)
            repeat(10) { i ->
                val record = LevelRecord(level, play(level, SearchAi(level.ai, Random(i)), random))
                val text = record.toText()
                val parsed = LevelRecord.parse(text) { _, _ -> level }
                val played = record.actions.fold(level.newGame()) { state, action -> state.apply(action).state }
                val replayed = parsed.replay()

                assertEquals(record, parsed, text)
                assertEquals(played.toAscii(), replayed.toAscii(), text)
                for (player in Player.entries) assertEquals(played.score(player), replayed.score(player), text)
                assertEquals(level.status(played), level.status(replayed), text)
                scored += played.score(Player.RED) + played.score(Player.BLUE)
            }
        }
        assertTrue(scored > 0, "no capture in any game, so the replays prove little")
    }
}
