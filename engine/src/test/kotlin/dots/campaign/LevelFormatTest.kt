package dots.campaign

import dots.engine.Booster
import dots.engine.Pos
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LevelFormatTest {

    private val sample = """
        world: 2
        level: 7
        name: Long Night
        computer: normal
        dots: 12
        mission: win
        solutions: few
        boosters: none
        rule: none
        stars: none
        map:
        .....
        .R...
        ..RB.
        ..B..
        .....
    """.trimIndent() + "\n"

    @Test
    fun `every level file is written back exactly as it is`() {
        val files = File("src/main/resources/${Campaign.FOLDER}").listFiles()!!.sorted()

        assertTrue(files.isNotEmpty())
        for (file in files) {
            val text = file.readText()
            assertEquals(text, LevelFormat.write(LevelFormat.parse(text, file.name)), file.name)
        }
    }

    @Test
    fun `every field is read back as it was written`() {
        val level = Level(
            world = 3,
            number = 12,
            name = "The Fortress: part 2",
            strength = Strength.EXPERT,
            dotsPerPlayer = 30,
            map = LevelMap(listOf("-.....-", ".R.#...", "..RB+..", "...B...", "-.....-")),
            mission = Mission.Capture(dots = 5, moves = 8),
            boosters = mapOf(Booster.WALL to 2, Booster.HINT to 1, Booster.BOMB to 3, Booster.DOUBLE_DOT to 1),
            rule = SpecialRule.DoubleMove(every = 5),
            stars = Stars(two = 3, three = 6),
            solutions = Solutions.OPEN,
        )
        val text = LevelFormat.write(level)

        assertEquals(level, LevelFormat.parse(text))
        assertTrue("boosters: wall 2, hint 1, bomb 3, double-dot 1\n" in text, text)
        assertTrue("map:\n-.....-\n.R.#...\n" in text, text)
    }

    @Test
    fun `every mission is read back as it was written`() {
        val base = LevelFormat.parse(sample)
        val missions = listOf(
            Mission.Win,
            Mission.WinBy(3),
            Mission.WinWithFewerDots(1),
            Mission.WinWithFewerDots(8),
            Mission.Capture(dots = 1, moves = 1),
            Mission.Capture(dots = 4, moves = 9),
            Mission.Survive(1),
            Mission.Survive(6),
            Mission.Rescue(Pos(2, 2)),
            Mission.Target(Pos(3, 2)),
        )
        for (mission in missions) {
            val level = base.copy(mission = mission)
            val text = LevelFormat.write(level)
            assertEquals(level, LevelFormat.parse(text), mission.text)
            assertEquals(text, LevelFormat.write(LevelFormat.parse(text)), mission.text)
        }
        assertEquals("rescue c3", Mission.Rescue(Pos(2, 2)).text)
        assertEquals("capture 1 in 1 move", Mission.Capture(1, 1).text)
        assertEquals("win with 1 dot", Mission.WinWithFewerDots(1).text)
    }

    @Test
    fun `blank lines, extra spaces and Windows line ends are accepted`() {
        val loose = "\r\n" + sample.replace("\n", "\r\n").replace("dots: 12", "dots:    12  ").replace("map:", "\r\nmap:") + "\r\n\r\n"

        assertEquals(LevelFormat.parse(sample), LevelFormat.parse(loose))
    }

    @Test
    fun `fields can come in any order before the map`() {
        val lines = sample.lines()
        val shuffled = (lines.take(10).reversed() + lines.drop(10)).joinToString("\n")

        assertEquals(LevelFormat.parse(sample), LevelFormat.parse(shuffled))
    }

    @Test
    fun `a bad file names the line and the problem`() {
        val cases = listOf(
            Triple("computer: normal", "computer: medium", "line 4: unknown computer strength 'medium' (easy, normal, hard, expert)"),
            Triple("dots: 12", "dots: twelve", "line 5: 'twelve' is not a whole number"),
            Triple("dots: 12", "dots: 0", "line 5: 0 is not between 1 and 9999"),
            Triple("level: 7", "level: 100", "line 2: 100 is not between 1 and 99"),
            Triple("name: Long Night", "name:", "line 3: the name is empty"),
            Triple("mission: win", "mission: win big", "line 6: unknown mission 'win big'"),
            Triple("mission: win", "mission: rescue d3", "line 6: d3 must hold a human dot ('R' on the map)"),
            Triple("mission: win", "mission: target z9", "line 6: z9 must hold a computer dot ('B' on the map)"),
            Triple("mission: win", "mission: target 9", "line 6: '9' is not a point like e5"),
            Triple("mission: win", "mission: win with fewer dots", "line 6: 'fewer' is not a whole number"),
            Triple(
                "mission: win", "mission: win with 12 dots",
                "line 6: the mission 'win with 12 dots' must give the player fewer dots than the computer's 12",
            ),
            Triple(
                "mission: win", "mission: capture 1 in 13 moves",
                "line 6: the mission 'capture 1 in 13 moves' has more moves than the 12 dots the player may place",
            ),
            Triple(
                "mission: win", "mission: survive 13 moves",
                "line 6: the mission 'survive 13 moves' has more moves than the 12 dots the computer may place",
            ),
            Triple(
                "mission: win", "mission: capture 15 in 12 moves",
                "line 6: the mission 'capture 15 in 12 moves' is already decided before the first move",
            ),
            Triple("solutions: few", "solutions: many", "line 7: unknown solutions 'many' (few, open)"),
            Triple("boosters: none", "boosters: wall 2, shield 1", "line 8: unknown booster 'shield' (wall, hint, bomb, double-dot)"),
            Triple("boosters: none", "boosters: wall", "line 8: write each booster as a name and a count, like 'wall 2', not 'wall'"),
            Triple("boosters: none", "boosters: hint 1, hint 2", "line 8: 'hint' appears twice"),
            Triple("rule: none", "rule: double move every 1 turns", "line 9: 1 is not between 2 and 999"),
            Triple("rule: none", "rule: fog", "line 9: unknown special rule 'fog'"),
            Triple("stars: none", "stars: 6 3", "line 10: the three-star score (3) must be higher than the two-star score (6)"),
            Triple("stars: none", "stars: 3", "line 10: stars are 'none' or two scores"),
            Triple("rule: none", "rules: none", "line 9: unknown field 'rules'"),
            Triple("rule: none", "rule none", "line 9: expected 'field: value' or 'map:', found 'rule none'"),
            Triple("stars: none", "stars: none\ndots: 3", "line 11: 'dots' appears twice (first on line 5)"),
            Triple(".R...\n", ".R..\n", "line 13: this row has 4 points, the first row has 5"),
            Triple("..B..\n", "..B.X\n", "line 15: unknown map symbol 'X' in column 5"),
            Triple("..B..\n.....\n", "..B..\n.....\nstars: 3 6\n", "line 17: fields must come before 'map:'"),
            Triple("map:\n", "map: .....\n", "line 11: the map's rows start on the line after 'map:'"),
            Triple("..RB.\n..B..\n.....\n", "", "line 11: the board is 5x2; boards are 3 to 99 points in each direction"),
            Triple("..RB.\n..B..\n.....\n", "..RB.\n.RBR.\n..R..\n", "line 11: The starting position already encloses an enemy dot"),
            Triple("dots: 12\n", "", "level: missing 'dots:'"),
        )
        for ((find, replace, expected) in cases) {
            val text = sample.replaceFirst(find, replace)
            assertTrue(text != sample, "test case does not change the sample: $find")
            val error = assertFailsWith<LevelFormatException>(expected) { LevelFormat.parse(text, "bad.level") }
            val message = error.message!!
            assertTrue(message.startsWith("bad.level") && expected.removePrefix("level") in message, "expected '$expected', got '$message'")
        }
    }

    @Test
    fun `a file without a map says so`() {
        val error = assertFailsWith<LevelFormatException> { LevelFormat.parse(sample.substringBefore("map:"), "1-01.level") }

        assertEquals("1-01.level: missing 'map:' and the board's rows", error.message)
        assertNull(error.line)
    }

    @Test
    fun `levels with special points the engine does not play refuse to start`() {
        val level = LevelFormat.parse(sample)
        val unplayable = listOf(
            level.copy(map = LevelMap(level.map.rows.map { it.replace('.', '+') })) to "bonus points",
        )

        assertNull(level.unplayable)
        level.copy(boosters = mapOf(Booster.HINT to 1), rule = SpecialRule.DoubleMove(4), stars = Stars(1, 2)).newGame()
        for ((bad, reason) in unplayable) {
            val error = assertFailsWith<UnsupportedOperationException> { bad.newGame() }
            assertTrue(reason in error.message!!, error.message)
        }
    }

    @Test
    fun `the head start counts the computer's extra dots on the map`() {
        assertEquals(0, LevelFormat.parse(sample).headStart)
        assertEquals(2, LevelFormat.parse(sample.replace(".....\n.R...", "B....\n.R..B")).headStart)
    }
}
