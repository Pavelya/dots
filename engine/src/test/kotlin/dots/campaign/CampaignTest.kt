package dots.campaign

import dots.ai.AiConfig
import dots.ai.Match
import dots.ai.SearchAi
import dots.engine.GameState
import dots.engine.Grounding
import dots.engine.Player
import dots.engine.Pos
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CampaignTest {

    @Test
    fun `levels are numbered from one without gaps`() {
        assertEquals((1..Campaign.levels.size).toList(), Campaign.levels.map { it.id })
    }

    @Test
    fun `every level opens on a playable position with the human to move`() {
        for (level in Campaign.levels) {
            val game = level.newGame()

            assertFalse(game.isOver, level.name)
            assertEquals(Player.RED, game.toMove, level.name)
            assertEquals(level.dotsPerPlayer, game.dotsLeft(Player.RED), level.name)
            assertEquals(0, game.score(Player.RED) + game.score(Player.BLUE), level.name)
            assertTrue(game.legalMoves().isNotEmpty(), level.name)
        }
    }

    @Test
    fun `the computer never gets weaker or the game shorter as the levels go on`() {
        for ((easier, harder) in Campaign.levels.zipWithNext()) {
            assertTrue(harder.ai.depth >= easier.ai.depth, "${harder.name} has a weaker computer than ${easier.name}")
            assertTrue(harder.dotsPerPlayer >= easier.dotsPerPlayer, "${harder.name} is shorter than ${easier.name}")
        }
    }

    @Test
    fun `the head start grows as the levels get harder`() {
        val headStarts = Campaign.levels.map { it.headStart }

        assertEquals(0, headStarts.first())
        assertTrue(headStarts.last() > headStarts.first())
    }

    @Test
    fun `every level can be played to the end`() {
        for (level in Campaign.levels) {
            val easy = AiConfig.EASY
            val end = Match.playOut(level.newGame(), SearchAi(easy, Random(level.id)), SearchAi(easy, Random(level.id + 50)))

            assertTrue(end.isOver, level.name)
            assertEquals(level.dotsPerPlayer * 2, end.moveCount, level.name)
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
        val error = runCatching { Campaign.level(99) }.exceptionOrNull()

        assertTrue(error is NoSuchElementException)
    }
}
