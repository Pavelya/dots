package dots.campaign

import dots.ai.AiConfig
import dots.ai.Match
import dots.ai.SearchAi
import dots.engine.GameState
import dots.engine.Grounding
import dots.engine.Player
import dots.engine.Pos
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CampaignTest {

    @Test
    fun `worlds and levels are numbered from one without gaps`() {
        for ((world, levels) in Campaign.levels.groupBy { it.world }) {
            assertEquals((1..levels.size).toList(), levels.map { it.number }, "world $world")
        }
        assertEquals((1..Campaign.levels.maxOf { it.world }).toList(), Campaign.levels.map { it.world }.distinct())
    }

    @Test
    fun `every file in the levels folder is part of the campaign`() {
        val files = File("src/main/resources/${Campaign.FOLDER}").list()!!.sorted()

        assertTrue(files.isNotEmpty())
        assertEquals(files, Campaign.levels.map { Campaign.fileName(it.world, it.number) }.sorted())
    }

    @Test
    fun `a file whose name does not match its level is refused`() {
        val text = LevelFormat.write(Campaign.levels.first().copy(number = 2))
        val error = assertFailsWith<LevelFormatException> { Campaign.load { if (it == "1-01.level") text else null } }

        assertEquals("1-01.level: the file says level 1-2, but its name says 1-1", error.message)
    }

    @Test
    fun `every level opens on a playable position with the human to move`() {
        for (level in Campaign.levels) {
            val game = level.newGame()

            assertFalse(game.isOver, level.name)
            assertEquals(Player.RED, game.toMove, level.name)
            assertEquals(level.humanDots, game.dotsLeft(Player.RED), level.name)
            assertEquals(0, game.score(Player.RED) + game.score(Player.BLUE), level.name)
            assertTrue(game.legalMoves().isNotEmpty(), level.name)
        }
    }

    @Test
    fun `the computer never gets weaker as the levels go on`() {
        for ((easier, harder) in Campaign.levels.zipWithNext()) {
            assertTrue(harder.strength >= easier.strength, "${harder.name} has a weaker computer than ${easier.name}")
        }
    }

    // Puzzle missions last a few moves whatever the budget, so length and head start are compared
    // between whole games only.
    private val wholeGames get() = Campaign.levels.filter { it.mission == Mission.Win || it.mission is Mission.WinBy }

    @Test
    fun `whole games never get shorter as the levels go on`() {
        for ((easier, harder) in wholeGames.zipWithNext()) {
            assertTrue(harder.dotsPerPlayer >= easier.dotsPerPlayer, "${harder.name} is shorter than ${easier.name}")
        }
    }

    @Test
    fun `the head start of whole games grows as the levels get harder`() {
        val headStarts = wholeGames.map { it.headStart }

        assertEquals(0, headStarts.first())
        assertTrue(headStarts.last() > headStarts.first())
    }

    @Test
    fun `every level has a design note`() {
        for (level in Campaign.levels) assertTrue(level.note != null, "level ${level.code} has 'note: none'")
    }

    @Test
    fun `every level can be played to the end`() {
        for (level in Campaign.levels) {
            val easy = AiConfig.EASY
            val seed = level.world * 100 + level.number
            val end = Match.playOut(level.newGame(), SearchAi(easy, Random(seed)), SearchAi(easy, Random(seed + 50)))

            assertTrue(end.isOver, level.name)
            // In "win with fewer dots" the player has fewer dots than the computer.
            assertEquals(level.humanDots + level.dotsPerPlayer, end.moveCount, level.name)
        }
    }

    @Test
    fun `every level gives the human dots that can be captured`() {
        // Otherwise playing only on the border would be a safe way to never lose.
        for (level in Campaign.levels) {
            assertTrue(Grounding.ungrounded(level.newGame(), level.human).isNotEmpty(), level.name)
        }
    }

    @Test
    fun `only a win passes a level`() {
        val level = Campaign.levels.first()
        val drawn = level.newGame().stop().finish()
        assertTrue(drawn.isOver)
        assertFalse(level.isPassed(drawn), "a 0-0 draw")
        assertFalse(level.isPassed(level.newGame()), "an unfinished game")

        val won = GameState.fromAscii(
            """
            .....
            ..R..
            .RBR.
            .....
            .....
            """,
            dotsPerPlayer = 1,
        ).play(Pos(2, 3)).state.play(Pos(0, 0)).state
        assertTrue(level.isPassed(won))
    }

    @Test
    fun `asking for a missing level fails clearly`() {
        val error = runCatching { Campaign.level(1, 99) }.exceptionOrNull()

        assertTrue(error is NoSuchElementException)
    }
}
