package dots.cli

import dots.ai.Ai
import dots.ai.AiConfig
import dots.ai.SearchAi
import dots.campaign.Campaign
import dots.campaign.Level
import dots.campaign.LevelResult
import dots.campaign.LevelFormat
import dots.campaign.LevelFormatException
import dots.campaign.Mission
import dots.campaign.MissionStatus
import dots.campaign.Reason
import dots.campaign.SpecialRule
import dots.campaign.StarMeasure
import dots.campaign.Stars
import dots.engine.Action
import dots.engine.GameState
import dots.engine.MoveResult
import dots.engine.Notation
import dots.engine.Openings
import dots.engine.Player
import dots.engine.Pos
import java.io.File
import kotlin.system.exitProcess

private const val USAGE = """Dots - terminal prototype

  levels                                list the campaign levels
  play [level | file]                   campaign level (e.g. 1-3) or level file against the computer;
                                        you are red and move first
  vs [tier] [width height] [opening]    classic open-ended game against the computer
  pvp [width height] [opening] [dots]   two players on one keyboard (traditional size: 39 32)
  watch [tierA] [tierB] [size] [dots]   two computers play each other (A is red)
  bench tiers [games] [size] [dots]     round-robin of the AI difficulty tiers
  bench steps [games] [size] [dots]     each strength step against the next, from four crosses
  bench levels [games] [level...]       the difficulty curve: weak, average and strong stand-ins play
                                        every level (or the given ones), then the curve's checks
  bench levels [games] stand-in [level...]  one stand-in (weak, average, strong, a tier, or border)
  bench duel ...                        two custom configurations play each other (run it for help)
  bench variants [games]                how much happens in each game format (draws, captures, ...)
  bench variety [tier] [margin]         how often a tier answers the same position differently
  check-levels [file...] [--seconds N]  solve every puzzle level (or the files); fails on one that is
                                        unsolvable, undecided in N seconds, or has over 3 winning first moves
  log-check <file>...                   replay the games in logs exported from the app, check their results
  poc-report <file>...                  the POC test week's numbers, one shared log per tester

  tier: beginner, easy, steady, normal, hard, expert   opening: cross (default), double, crosses, empty
  boards are 3 to 99 in each direction; columns after z are aa, ab, ...

Moves are a column letter and a row number, e.g. e5. Other commands:
  stop   declare that all the dots you want to keep are grounded; the opponent may then capture
         whatever is left with as many moves as they like
  done   after your opponent has stopped: you have finished capturing, end the game
  pass, quit"""

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "levels" -> listLevels()
        "play" -> playLevel(levelFrom(args.getOrNull(1)))
        "vs" -> versus(args.drop(1))
        "pvp" -> pvp(args.drop(1))
        "watch" -> watch(args.drop(1))
        "bench" -> Bench.run(args.drop(1))
        "check-levels" -> if (CheckLevels.run(args.drop(1)) > 0) exitProcess(1)
        "log-check" -> if (LogCheck.run(args.drop(1)) > 0) exitProcess(1)
        "poc-report" -> if (PocReport.run(args.drop(1)) > 0) exitProcess(1)
        else -> println(USAGE)
    }
}

private fun listLevels() {
    for (level in Campaign.levels) {
        println(
            "%-5s %-12s %2dx%-2d  %2d dots each  computer head start: %2d  %-6s  %s"
                .format(
                    level.code, level.name, level.map.width, level.map.height, level.dotsPerPlayer, level.headStart,
                    level.strength.text, level.mission.text,
                ),
        )
    }
}

/** A campaign level by world and number (`1-3`), or a level file by its path. */
private fun levelFrom(arg: String?): Level {
    if (arg == null) return Campaign.levels.first()
    val file = File(arg)
    if (file.isFile) {
        return try {
            LevelFormat.parse(file.readText(), file.path)
        } catch (e: LevelFormatException) {
            exitWith(e.message!!)
        }
    }
    val (world, number) = Regex("(\\d+)-(\\d+)").matchEntire(arg)?.destructured
        ?: exitWith("'$arg' is neither a level like 1-3 nor a level file (see: cli levels)")
    return Campaign.levels.firstOrNull { it.world == world.toInt() && it.number == number.toInt() }
        ?: exitWith("No level $arg in the campaign (see: cli levels)")
}

private fun checkSize(width: Int, height: Int) {
    if (width !in 3..99 || height !in 3..99) exitWith("Boards are 3 to 99 in each direction, not ${width}x$height")
}

internal val TIERS = linkedMapOf(
    "BEGINNER" to AiConfig.BEGINNER,
    "EASY" to AiConfig.EASY,
    "STEADY" to AiConfig.STEADY,
    "NORMAL" to AiConfig.NORMAL,
    "HARD" to AiConfig.HARD,
    "EXPERT" to AiConfig.EXPERT,
)

private fun openingDots(name: String, width: Int, height: Int): Map<Pos, Player> = when (name) {
    "cross" -> Openings.cross(width, height)
    "double" -> Openings.doubleCross(width, height)
    "crosses" -> Openings.randomCrosses(width, height, 4)
    "empty" -> emptyMap()
    else -> exitWith("unknown opening '$name' (cross, double, crosses, empty)")
}

private fun playLevel(level: Level) {
    println("Level ${level.code}: ${level.name} - computer is ${level.strength.text}, head start ${level.headStart}")
    println("Mission: ${goal(level)}")
    level.stars?.let { println("Stars: ${targets(it)}") }
    level.rule?.let { println("Boss rule: ${announce(it)}") }
    val computer = SearchAi(level.ai)
    val seats = mapOf(level.human to null, level.computer to computer)
    val end = loop(level.newGame(), seats, level::isOver) { state ->
        listOfNotNull(progress(level.mission, level.status(state)), countdown(level, state)).joinToString("\n    ")
    } ?: return
    val result = level.result(end)
    val verdict = if (result.isPassed) "Level passed! ${earned(level, result)}" else "Try again."
    println("${outcome(level, result.status)} $verdict")
}

/** What earns two and three stars, as the level opens. */
private fun targets(stars: Stars): String = when (stars.measure) {
    StarMeasure.MARGIN -> "two for a margin of ${stars.two} (your captures minus the computer's), three for ${stars.three}."
    StarMeasure.MOVES -> "two with ${moves(stars.two)} to spare, three with ${moves(stars.three)}."
    StarMeasure.SAVED -> "two for saving ${stars.two} of your starting dots, three for ${stars.three}."
    StarMeasure.BOOSTERS -> "two for leaving ${stars.two} boosters unused, three for ${stars.three}."
}

/** The stars a passed level earned, and the count they were judged on. */
private fun earned(level: Level, result: LevelResult): String {
    val stars = if (result.stars == 1) "1 star" else "${result.stars} stars"
    val measure = level.stars?.measure ?: return "$stars."
    val score = result.starScore!!
    val why = when (measure) {
        StarMeasure.MARGIN -> "margin $score"
        StarMeasure.MOVES -> "${moves(score)} to spare"
        StarMeasure.SAVED -> "${dots(score)} saved"
        StarMeasure.BOOSTERS -> if (score == 1) "1 booster unused" else "$score boosters unused"
    }
    return "$stars of ${Stars.MAX} ($why)."
}

/** The mission in a sentence, as the level opens. */
private fun goal(level: Level): String = when (val mission = level.mission) {
    Mission.Win -> "capture more dots than the computer."
    is Mission.WinBy -> "capture at least ${mission.margin} more dots than the computer."
    is Mission.WinWithFewerDots ->
        "capture more dots than the computer, with ${dots(mission.dots)} against its ${level.dotsPerPlayer}."
    is Mission.Capture -> "capture ${dots(mission.dots)} in your first ${moves(mission.moves)}."
    is Mission.Survive -> "lose no dot while the computer places its next ${dots(mission.moves)}."
    is Mission.Rescue ->
        "keep your dot at ${name(mission.dot)} from being captured. Join it to the edge in a straight line to make it safe."
    is Mission.Target -> "capture the computer's dot at ${name(mission.dot)} before it reaches the edge."
}

/** A boss level's rule in a sentence, as the level opens. */
private fun announce(rule: SpecialRule): String = when (rule) {
    is SpecialRule.DoubleMove ->
        "the computer places two dots on its turns ${rule.every}, ${2 * rule.every}, ${3 * rule.every} and so on."
    is SpecialRule.Fortress -> "the computer starts with a fortress of ${dots(rule.dots)}."
    SpecialRule.TwoGroups -> "every turn, the computer places two dots: one on the left half of the board, one on the right."
}

/** The countdown to the computer's next double move, for the double-move rule. */
private fun countdown(level: Level, state: GameState): String? {
    if (level.rule !is SpecialRule.DoubleMove) return null
    return when (val turns = state.doubleMoveIn(level.computer)) {
        null -> null
        0 -> "Double move: now"
        1 -> "Double move: on the computer's next turn"
        else -> "Double move: in $turns of the computer's turns"
    }
}

/** One line under the board while the mission is open. */
private fun progress(mission: Mission, status: MissionStatus): String = "Mission: " + when (mission) {
    Mission.Win, is Mission.WinBy, is Mission.WinWithFewerDots ->
        "${status.captured - status.lost} ahead" + ((mission as? Mission.WinBy)?.let { " (you need ${it.margin})" } ?: "")
    is Mission.Capture -> "${status.captured} of ${mission.dots} captured, ${moves(status.movesLeft!!)} left"
    is Mission.Survive -> "no dot lost, ${dots(status.movesLeft!!)} of the computer's to go"
    is Mission.Rescue -> "${name(mission.dot)} is not safe yet"
    is Mission.Target -> "capture ${name(mission.dot)}"
}

/** What decided the level. */
private fun outcome(level: Level, status: MissionStatus): String {
    val score = "${status.captured} to ${status.lost}"
    return when (status.reason) {
        Reason.CAPTURED_ENOUGH -> "You captured ${dots(status.captured)}."
        Reason.OUT_OF_MOVES -> "Out of moves: you captured ${status.captured} of ${(level.mission as Mission.Capture).dots}."
        Reason.OUT_OF_REACH -> "Too few computer dots are left to capture for the mission (score $score)."
        Reason.SURVIVED -> "You lost no dot."
        Reason.LOST_A_DOT -> "The computer captured your dot."
        Reason.DOT_SAFE -> "Your dot is joined to the edge and safe."
        Reason.DOT_CAPTURED -> "Your dot was captured."
        Reason.TARGET_CAPTURED -> "You captured the target."
        Reason.TARGET_SAFE -> "The target is joined to the edge and can no longer be captured."
        Reason.GAME_OVER, null -> "The game is over, $score."
    }
}

private fun dots(n: Int) = if (n == 1) "1 dot" else "$n dots"

private fun moves(n: Int) = if (n == 1) "1 move" else "$n moves"

private fun versus(args: List<String>) {
    val tier = args.firstOrNull { it.toIntOrNull() == null && it.lowercase() in TIERS.keys.map(String::lowercase) } ?: "normal"
    val opening = args.firstOrNull { it.toIntOrNull() == null && it.lowercase() !in TIERS.keys.map(String::lowercase) } ?: "cross"
    val numbers = args.mapNotNull { it.toIntOrNull() }
    val width = numbers.getOrNull(0) ?: 16
    val height = numbers.getOrNull(1) ?: 14
    checkSize(width, height)
    val computer = SearchAi(TIERS.getValue(tier.uppercase()))
    println("You are RED and move first against a ${tier.uppercase()} computer on ${width}x$height.")
    val start = GameState.start(width, height, dots = openingDots(opening.lowercase(), width, height))
    val end = loop(start, mapOf(Player.RED to null, Player.BLUE to computer)) ?: return
    summarize(end)
}

private fun pvp(args: List<String>) {
    val numbers = args.mapNotNull { it.toIntOrNull() }
    val opening = args.firstOrNull { it.toIntOrNull() == null } ?: "cross"
    val width = numbers.getOrNull(0) ?: 20
    val height = numbers.getOrNull(1) ?: 16
    checkSize(width, height)
    val start = GameState.start(width, height, numbers.getOrNull(2), dots = openingDots(opening.lowercase(), width, height))
    val end = loop(start, mapOf(Player.RED to null, Player.BLUE to null)) ?: return
    summarize(end)
}

private fun watch(args: List<String>) {
    val tiers = args.filter { it.toIntOrNull() == null }.map { it.uppercase() }
    val numbers = args.mapNotNull { it.toIntOrNull() }
    val size = numbers.getOrNull(0) ?: 8
    val dots = numbers.getOrNull(1) ?: 20
    checkSize(size, size)
    val red = TIERS[tiers.getOrElse(0) { "HARD" }] ?: exitWith("unknown tier ${tiers[0]}")
    val blue = TIERS[tiers.getOrElse(1) { "HARD" }] ?: exitWith("unknown tier ${tiers[1]}")
    val start = GameState.start(size, size, dots, dots = Openings.cross(size, size))
    val end = loop(start, mapOf(Player.RED to SearchAi(red), Player.BLUE to SearchAi(blue))) ?: return
    summarize(end)
}

private fun summarize(end: GameState) {
    val winner = end.winner
    println(
        if (winner == null) "A draw at ${end.score(Player.RED)} each."
        else "$winner wins ${end.score(winner)} to ${end.score(winner.opponent)}.",
    )
}

private sealed interface Command {
    data class Act(val action: Action) : Command
    data object Quit : Command
}

/**
 * Runs a game until [isOver]; a null entry in [seats] is a human at the keyboard, and [note] adds a
 * line under the board. Returns null if the player quit.
 */
private fun loop(
    start: GameState,
    seats: Map<Player, Ai?>,
    isOver: (GameState) -> Boolean = { it.isOver },
    note: (GameState) -> String? = { null },
): GameState? {
    var state = start
    var last: Pos? = null
    while (!isOver(state)) {
        print(render(state, last))
        note(state)?.let { println("    $it\n") }
        val mover = state.toMove
        val ai = seats.getValue(mover)
        val action: Action
        var thoughtMs: Long? = null
        if (ai == null) {
            action = when (val command = ask(state)) {
                Command.Quit -> return null
                is Command.Act -> command.action
            }
        } else {
            val started = System.nanoTime()
            action = ai.choose(state)
            thoughtMs = (System.nanoTime() - started) / 1_000_000
        }
        val result = state.apply(action)
        if (thoughtMs != null) println("($mover thought for $thoughtMs ms)")
        println(describe(mover, action, result))
        state = result.state
        if (action is Action.Place) last = action.pos
    }
    print(render(state, last))
    return state
}

private fun describe(mover: Player, action: Action, result: MoveResult): String = when (action) {
    is Action.Place -> {
        val captured = result.captures.sumOf { it.dots.size }
        val points = result.captures.sumOf { it.points }
        val worth = if (points == captured) "" else ", worth $points"
        val note = if (captured == 0) "" else ": ${result.captures.first().by} captures $captured dot${if (captured == 1) "" else "s"}$worth!"
        "$mover plays ${name(action.pos)}$note"
    }
    Action.Pass -> "$mover passes"
    Action.Stop -> "$mover declares its remaining dots grounded - ${mover.opponent} may now capture whatever it can"
    Action.Finish -> "$mover is done capturing"
    is Action.UseBooster -> "$mover uses a booster: ${Action.word(action)}"
}

private fun name(pos: Pos) = Notation.name(pos)

private fun column(x: Int) = Notation.column(x)

private fun ask(state: GameState): Command {
    while (true) {
        print("${state.toMove}${if (state.isCleanup) " (capturing, type done to finish)" else ""}> ")
        val line = readlnOrNull()?.trim()?.lowercase() ?: return Command.Quit
        when (line) {
            "quit", "q" -> return Command.Quit
            "pass", "p" -> if (state.isCleanup) println("Place dots, or type done.") else return Command.Act(Action.Pass)
            "stop", "s" -> if (state.isCleanup) println("Place dots, or type done.") else return Command.Act(Action.Stop)
            "done", "d" -> if (state.isCleanup) return Command.Act(Action.Finish) else println("done only applies after the opponent has stopped.")
            else -> {
                val pos = parse(line, state)
                when {
                    pos == null -> println("Type a move like e5 (columns a-${column(state.width - 1)}, rows 1-${state.height}), or stop / pass / quit.")
                    !state.isLegal(pos) -> println("${name(pos)} is not playable.")
                    else -> return Command.Act(Action.Place(pos))
                }
            }
        }
    }
}

private fun parse(text: String, state: GameState): Pos? = Notation.parse(text)?.takeIf { state.inBounds(it.x, it.y) }

private val color = System.console() != null && System.getenv("NO_COLOR") == null

private fun paint(text: String, player: Player?, dim: Boolean = false, highlight: Boolean = false): String {
    if (!color || player == null) return text
    val hue = if (player == Player.RED) 31 else 34
    val style = (if (dim) "2" else "1") + (if (highlight) ";7" else "")
    return "\u001B[$style;${hue}m$text\u001B[0m"
}

private fun render(state: GameState, last: Pos?): String = buildString {
    val cell = if (state.width > 26) 3 else 2
    append("\n    ")
    for (x in 0 until state.width) append(column(x).padEnd(cell))
    append('\n')
    for (y in 0 until state.height) {
        append("%2d  ".format(y + 1))
        for (x in 0 until state.width) {
            val owner = state.ownerAt(x, y)
            val symbol = when {
                owner == null -> when {
                    state.isHole(x, y) -> " "
                    state.isBlocked(x, y) -> "#"
                    state.isDead(x, y) -> ","
                    state.isBonus(x, y) -> "+"
                    else -> "."
                }
                state.isCaptured(x, y) -> paint(owner.symbol.lowercase(), owner, dim = true)
                else -> paint(owner.symbol.toString(), owner, highlight = Pos(x, y) == last)
            }
            append(symbol).append(" ".repeat(cell - 1))
        }
        append('\n')
    }
    val left = Player.entries.joinToString("   ") { p ->
        "${paint(p.name, p)} ${state.score(p)} captured" + (state.dotsLeft(p)?.let { ", $it dots left" } ?: "")
    }
    append("\n    $left\n")
    if (state.isCleanup) append("    ${state.stopper} has stopped: ${state.cleaner} is collecting\n")
    append("    (r/b = captured, ',' = out of play, '#' = blocked, '+' = bonus point: a dot captured there counts 2)\n\n")
}

internal fun exitWith(message: String): Nothing {
    System.err.println(message)
    exitProcess(2)
}
