package dots.campaign

import dots.engine.Action
import dots.engine.Booster
import dots.engine.GameState
import dots.engine.Notation
import dots.engine.Pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Star targets and the stars a level game earns. The human is red and moves first; moves alternate. */
class StarsTest {

    /** Red's d3 encloses both blue dots, c3 and e3, at once. */
    private val twoAtOnce = listOf(
        ".......",
        "..R.R..",
        ".RB.BR.",
        "..R.R..",
        ".......",
    )

    /** Blue's d3 captures the red dot at c3; the red dot at a5 is on the border and safe. */
    private val oneInDanger = listOf(
        ".....",
        "..B..",
        ".BR..",
        "..B..",
        "R....",
    )

    private fun level(
        mission: Mission,
        dots: Int,
        map: List<String>,
        stars: Stars?,
        boosters: Map<Booster, Int> = emptyMap(),
    ) = Level(1, 1, "Test", Strength.EASY, dots, LevelMap(map), mission, boosters, stars = stars)

    private fun GameState.then(vararg moves: String): GameState =
        moves.fold(this) { state, move -> state.play(Notation.parse(move)!!).state }

    private fun pos(name: String): Pos = Notation.parse(name)!!

    @Test
    fun `passing earns one star, the two-star score two and the three-star score three`() {
        val stars = Stars(StarMeasure.MARGIN, two = 3, three = 5)

        assertEquals(listOf(1, 1, 1, 2, 2, 3, 3), listOf(-1, 0, 2, 3, 4, 5, 9).map(stars::earned))
    }

    @Test
    fun `margin counts the player's score minus the computer's at the end`() {
        val level = level(Mission.Win, dots = 2, twoAtOnce, Stars(StarMeasure.MARGIN, two = 2, three = 3))

        val won = level.result(level.newGame().then("d3", "a1", "a5", "g5"))
        assertTrue(won.isPassed)
        assertEquals(2, won.starScore)
        assertEquals(2, won.stars)

        // Blue takes d3 first, so nothing is captured: a draw fails "win" and earns no star.
        val drawn = level.result(level.newGame().then("a1", "d3", "a5", "g5"))
        assertEquals(Outcome.FAILED, drawn.status.outcome)
        assertEquals(0, drawn.starScore)
        assertEquals(0, drawn.stars)
    }

    @Test
    fun `a level that ends the moment it passes counts the margin then`() {
        val level = level(Mission.Capture(dots = 1, moves = 2), dots = 3, twoAtOnce, Stars(StarMeasure.MARGIN, two = 1, three = 2))
        val result = level.result(level.newGame().then("d3"))

        assertTrue(result.isPassed)
        assertEquals(2, result.starScore)
        assertEquals(3, result.stars)
    }

    @Test
    fun `moves counts the moves left of the mission's limit`() {
        val level = level(Mission.Capture(dots = 2, moves = 4), dots = 4, twoAtOnce, Stars(StarMeasure.MOVES, two = 1, three = 3))
        fun stars(vararg moves: String) = level.result(level.newGame().then(*moves)).let { it.starScore to it.stars }

        assertEquals(3 to 3, stars("d3"))
        assertEquals(2 to 2, stars("a1", "g5", "d3"))
        assertEquals(1 to 2, stars("a1", "g5", "a5", "g1", "d3"))
        assertEquals(0 to 1, stars("a1", "g5", "a5", "g1", "b1", "f1", "d3"))
    }

    @Test
    fun `moves counts the dots the player has not placed, in a target mission`() {
        val level = level(Mission.Target(pos("c3")), dots = 5, twoAtOnce, Stars(StarMeasure.MOVES, two = 2, three = 4))
        val result = level.result(level.newGame().then("a1", "g5", "d3"))

        assertTrue(result.isPassed)
        assertEquals(3, result.starScore)
        assertEquals(2, result.stars)
    }

    @Test
    fun `saved counts the player's starting dots not captured`() {
        val level = level(Mission.Win, dots = 3, oneInDanger, Stars(StarMeasure.SAVED, two = 1, three = 2))

        assertEquals(2, level.starScore(level.newGame()))
        assertEquals(1, level.starScore(level.newGame().then("e1", "d3")))
        assertEquals(2, level.starScore(level.newGame().then("d3", "e1")))
    }

    @Test
    fun `boosters counts the boosters left, all kinds together`() {
        val level = level(
            Mission.Win, dots = 3, twoAtOnce, Stars(StarMeasure.BOOSTERS, two = 1, three = 2),
            boosters = mapOf(Booster.WALL to 1, Booster.HINT to 1),
        )
        val start = level.newGame()

        assertEquals(2, level.starScore(start))
        assertEquals(1, level.starScore(start.apply(Action.Wall(pos("a1"))).state))
    }

    @Test
    fun `a level without star targets earns one star when passed`() {
        val level = level(Mission.Capture(dots = 1, moves = 2), dots = 3, twoAtOnce, stars = null)
        val passed = level.result(level.newGame().then("d3"))
        val failed = level.result(level.newGame().then("a1", "d3", "a5", "g5"))

        assertNull(passed.starScore)
        assertEquals(1, passed.stars)
        assertEquals(Outcome.FAILED, failed.status.outcome)
        assertEquals(0, failed.stars)
    }

    @Test
    fun `star targets must fit the mission`() {
        val cases = listOf(
            Triple(Mission.Win, Stars(StarMeasure.MARGIN, 1, 3), "every pass of 'win' already has a margin of 1"),
            Triple(Mission.WinBy(3), Stars(StarMeasure.MARGIN, 3, 5), "every pass of 'win by 3' already has a margin of 3"),
            Triple(Mission.Win, Stars(StarMeasure.MOVES, 1, 2), "stars for 'moves' fit only capture, rescue and target"),
            Triple(Mission.Survive(2), Stars(StarMeasure.MOVES, 1, 2), "stars for 'moves' fit only capture, rescue and target"),
            Triple(Mission.Capture(1, 3), Stars(StarMeasure.MOVES, 1, 3), "at most 2 moves can be left to spare, fewer than the 3"),
            Triple(Mission.Target(pos("c3")), Stars(StarMeasure.MOVES, 1, 4), "at most 3 moves can be left to spare, fewer than the 4"),
            Triple(Mission.Survive(2), Stars(StarMeasure.SAVED, 1, 2), "losing a dot fails it, so every pass saves them all"),
            Triple(Mission.Win, Stars(StarMeasure.SAVED, 6, 7), "the map has 6 of the player's dots, fewer than the 7"),
            Triple(Mission.Win, Stars(StarMeasure.BOOSTERS, 1, 2), "the level allows 0 boosters, fewer than the 2"),
        )
        for ((mission, stars, expected) in cases) {
            val error = assertFailsWith<IllegalArgumentException> { level(mission, dots = 4, twoAtOnce, stars) }
            assertTrue(expected in error.message!!, "expected '$expected', got '${error.message}'")
        }
        // The largest targets that fit.
        level(Mission.WinBy(3), dots = 4, twoAtOnce, Stars(StarMeasure.MARGIN, 4, 6))
        level(Mission.Capture(1, 3), dots = 4, twoAtOnce, Stars(StarMeasure.MOVES, 1, 2))
        level(Mission.Win, dots = 4, twoAtOnce, Stars(StarMeasure.SAVED, 5, 6))
    }

    @Test
    fun `every campaign level has star targets`() {
        for (level in Campaign.levels) assertNotNull(level.stars, "level ${level.code} has 'stars: none'")
    }
}
