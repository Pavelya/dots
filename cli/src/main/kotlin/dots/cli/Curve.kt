package dots.cli

import dots.ai.Ai
import dots.ai.AiConfig
import dots.ai.SearchAi
import dots.campaign.Campaign
import dots.campaign.Level
import dots.campaign.LevelResult
import dots.campaign.Solver
import dots.engine.Action
import dots.engine.GameState
import kotlin.random.Random

/** A stand-in human for one game of a level: makes the player's moves, from the given seed. */
internal typealias StandIn = (Level, Random) -> Ai

/**
 * The three people of the difficulty curve (`docs/design/difficulty.md`). Each plays like a
 * strength step of the computer, and reads the level's mission [lookAhead] of their own moves
 * ahead ([MissionReader]).
 */
internal enum class Person(val config: AiConfig, val lookAhead: Int) {
    /** Plays like the EASY step: one move ahead, sees a ring two dots from closing. */
    WEAK(AiConfig.EASY, 1),

    /** Plays like the NORMAL step: checks the answer to each move. */
    AVERAGE(AiConfig.NORMAL, 2),

    /** Plays like the HARD step: three moves ahead, the whole board. */
    STRONG(AiConfig.HARD, 3),
    ;

    val label: String get() = name.lowercase()

    fun player(level: Level, random: Random): Ai = MissionReader(level, config, lookAhead, random)
}

/**
 * Someone who reads the mission, unlike a computer step, which only plays for captures. Before
 * each move they look [lookAhead] of their own moves ahead: a sure way to pass the mission is
 * played; in "survive" and "rescue", a move that would let the computer fail the mission within
 * that many moves is swapped for one that does not ([Solver.lookAhead]). Otherwise they play
 * their step's move. In whole games they play their step's move.
 */
internal class MissionReader(private val level: Level, config: AiConfig, private val lookAhead: Int, random: Random) : Ai {
    private val step = SearchAi(config, random)
    private val solver = Solver()

    override fun choose(state: GameState): Action = solver.lookAhead(level, state, lookAhead, step.choose(state))
}

/**
 * `bench levels [games]`: every [Person] plays every level [games] times; the share of games
 * passed and the average stars per level, as Markdown for `docs/design/difficulty.md`, then the
 * checks of the curve.
 */
internal object Curve {
    /** A level the average person passes less often than this is a wall. */
    const val WALL = 0.20

    /** A level note that starts with this marks a deliberate wall. */
    const val WALL_NOTE = "Wall:"

    private val COLOURS = listOf("#E69F00", "#0072B2", "#009E73", "#999999")

    private class Row(val level: Level, val results: Map<Person, List<LevelResult>>) {
        fun passes(person: Person): Int = results.getValue(person).count { it.isPassed }
        fun passed(person: Person): Double = passes(person).toDouble() / results.getValue(person).size
        fun stars(person: Person): Double = results.getValue(person).map { it.stars }.average()
        val isWall: Boolean get() = passed(Person.AVERAGE) < WALL
        val isNoted: Boolean get() = level.note?.startsWith(WALL_NOTE) == true
    }

    fun run(games: Int, levels: List<Level> = Campaign.levels) {
        val started = System.nanoTime()
        val rows = levels.map { level ->
            Row(level, Person.entries.associateWith { person -> Bench.playLevel(level, games, person::player) })
        }
        System.err.println("[${(System.nanoTime() - started) / 1_000_000_000} s]")

        println("$games games per level for each stand-in. Passed: the share of games that passed the mission.")
        println("Stars: the average stars per game, a failed game counting 0.")
        println()
        table(rows)
        for ((world, worldRows) in rows.groupBy { it.level.world }) {
            println()
            println("### World $world")
            println()
            chart("World $world: games passed (%)", worldRows, "passed (%)", 100, withWall = true) { row, p -> percent(row.passed(p)) }
            println()
            chart("World $world: average stars per game", worldRows, "stars", 3, withWall = false) { row, p -> "%.2f".format(row.stars(p)) }
        }
        println()
        checks(rows)
    }

    private fun table(rows: List<Row>) {
        val people = Person.entries
        println(
            "| Level | Name | Mission | Computer | " + people.joinToString(" | ") { "Passed, ${it.label}" } + " | " +
                people.joinToString(" | ") { "Stars, ${it.label}" } + " |",
        )
        println("|" + " --- |".repeat(4) + " ---: |".repeat(2 * people.size))
        for (row in rows) {
            val level = row.level
            println(
                "| ${level.code} | ${level.name} | ${level.mission.text} | ${level.strength.text} | " +
                    people.joinToString(" | ") { "${percent(row.passed(it))}%" } + " | " +
                    people.joinToString(" | ") { "%.1f".format(row.stars(it)) } + " |",
            )
        }
    }

    /** A Mermaid line chart, one line per person, in [COLOURS]; [withWall] adds the [WALL] line. */
    private fun chart(title: String, rows: List<Row>, axis: String, max: Int, withWall: Boolean, value: (Row, Person) -> String) {
        println("```mermaid")
        println("%%{init: {\"themeVariables\": {\"xyChart\": {\"plotColorPalette\": \"${COLOURS.joinToString(", ")}\"}}}}%%")
        println("xychart-beta")
        println("    title \"$title\"")
        println("    x-axis [${rows.joinToString(", ") { "\"${it.level.number}\"" }}]")
        println("    y-axis \"$axis\" 0 --> $max")
        for (person in Person.entries) println("    line [${rows.joinToString(", ") { value(it, person) }}]")
        if (withWall) println("    line [${rows.joinToString(", ") { percent(WALL) }}]")
        println("```")
    }

    /**
     * The checks of the curve: every wall (a level the average person passes under [WALL]) has a
     * note starting with [WALL_NOTE], and no world has two walls in a row.
     */
    private fun checks(rows: List<Row>) {
        val walls = rows.filter { it.isWall }
        println("Walls, levels the average stand-in passes in under ${percent(WALL)}% of games:")
        if (walls.isEmpty()) println("  none")
        for (row in walls) {
            val games = row.results.getValue(Person.AVERAGE).size
            val noted = if (row.isNoted) "noted" else "NOT NOTED: fix the level, or start its note with '$WALL_NOTE'"
            println("  ${row.level.code} ${row.level.name}: ${row.passes(Person.AVERAGE)} of $games, $noted")
        }
        val inARow = rows.zipWithNext().filter { (a, b) -> a.isWall && b.isWall && a.level.world == b.level.world && b.level.number == a.level.number + 1 }
        println("Two walls in a row: " + inARow.joinToString { (a, b) -> "${a.level.code} and ${b.level.code}" }.ifEmpty { "none" })
        val ok = walls.all { it.isNoted } && inARow.isEmpty()
        println(if (ok) "Checks passed." else "Checks FAILED.")
    }

    private fun percent(share: Double): String = Math.round(100 * share).toString()
}
