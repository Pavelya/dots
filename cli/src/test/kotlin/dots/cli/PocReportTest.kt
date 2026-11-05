package dots.cli

import dots.engine.Action
import dots.engine.Game
import dots.engine.GameRecord
import dots.engine.GameSettings
import dots.engine.Player
import dots.log.GameLogEntry
import dots.log.Outcome
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PocReportTest {
    private val day = 86_400_000L

    /** A logged game with chosen numbers; its record is a placeholder (numbers() does not replay). */
    private fun game(
        opponent: String,
        red: Int,
        blue: Int,
        minutes: Double,
        undos: Int = 0,
        threats: Boolean = false,
        onDay: Int = 0,
        outcome: Outcome = Outcome.FINISHED,
    ) = GameLogEntry(
        appVersion = "0.1-poc",
        startedAt = 1_762_300_000_000 + onDay * day + red * 1_000L + blue,
        updatedAt = 0,
        playMillis = (minutes * 60_000).toLong(),
        opponent = opponent,
        threatHighlight = threats,
        undos = undos,
        outcome = outcome,
        red = red,
        blue = blue,
        record = GameRecord(GameSettings.POC.copy(seed = onDay.toLong()), emptyList()),
    )

    private val ann = PocReport.Tester(
        "ann",
        listOf(
            game("easy", red = 3, blue = 1, minutes = 5.0, undos = 1, onDay = 0),
            game("easy", red = 0, blue = 2, minutes = 6.0, onDay = 1),
            game("easy", red = 4, blue = 0, minutes = 7.0, onDay = 2),
        ),
    )
    private val bobGames = listOf(
        game(GameLogEntry.HUMAN, red = 2, blue = 2, minutes = 20.0, threats = true),
        game("normal", red = 0, blue = 0, minutes = 1.0, undos = 2, outcome = Outcome.ABANDONED),
    )

    // Bob shared his log twice: the games are counted once.
    private val bob = PocReport.Tester("bob", bobGames + bobGames)

    @Test
    fun `each metric of the test protocol comes out as worked out by hand`() {
        assertEquals(3, ann.daysPlayed)
        assertEquals(1, bob.daysPlayed)
        assertEquals(2, bob.games.size)

        val all = PocReport.numbers(ann.games + bob.games)
        assertEquals(5, all.games)
        assertEquals(4, all.finished)
        assertEquals(1, all.abandoned)
        assertEquals(6.5, all.medianMinutes)
        assertEquals(0.75, all.winnerShare)
        assertEquals(mapOf("easy" to PocReport.Record(wins = 2, losses = 1, draws = 0)), all.vsComputer.toMap())
        assertEquals(0.75, all.undosPerGame)
        assertEquals(4.0, all.capturesWithThreats)
        assertEquals(10.0 / 3, all.capturesWithoutThreats!!, 1e-9)
    }

    @Test
    fun `the hypotheses are marked from the same numbers`() {
        val marks = PocReport.verdicts(listOf(ann, bob)).associate { it.hypothesis.take(2) to it.mark }
        assertEquals(
            mapOf("H1" to "PASS", "H3" to "FAIL", "H4" to "FAIL", "H5" to "PASS", "H6" to "PASS"),
            marks,
        )
    }

    @Test
    fun `no games gives no data, not a crash`() {
        val marks = PocReport.verdicts(emptyList()).map { it.mark }.toSet()
        assertEquals(setOf("no data"), marks)
        assertTrue(PocReport.format(emptyList()).contains("Everyone"))
    }

    @Test
    fun `real logs replay cleanly and the tester is named after the file`() {
        val random = Random(3)
        val lines = (0 until 3).map { seed ->
            val game = Game(GameSettings.POC.copy(seed = seed.toLong()))
            while (!game.state.isOver) {
                val moves = game.state.legalMoves()
                game.apply(Action.Place(moves[random.nextInt(moves.size)]))
            }
            GameLogEntry.start(game.settings, "hard", threatHighlight = false, now = seed * day)
                .copy(outcome = Outcome.FINISHED, red = game.state.score(Player.RED), blue = game.state.score(Player.BLUE), record = game.record)
                .toJson()
        }
        val dir = kotlin.io.path.createTempDirectory("poc-report").toFile().apply { deleteOnExit() }
        val file = File(dir, "carol.txt").apply { writeText(lines.joinToString("\n")) }
        assertEquals(0, PocReport.run(listOf(file.path)))
    }
}
