package dots.cli

import dots.ai.Ai
import dots.ai.AiConfig
import dots.ai.SearchAi
import dots.campaign.Campaign
import dots.campaign.Level
import dots.engine.Action
import dots.engine.GameState
import dots.engine.MoveResult
import dots.engine.Notation
import dots.engine.Openings
import dots.engine.Player
import dots.engine.Pos
import kotlin.system.exitProcess

private const val USAGE = """Dots - terminal prototype

  levels                                list the campaign levels
  play [level]                          campaign level against the computer (you are red and move first)
  vs [tier] [width height] [opening]    classic open-ended game against the computer
  pvp [width height] [opening] [dots]   two players on one keyboard (traditional size: 39 32)
  watch [tierA] [tierB] [size] [dots]   two computers play each other (A is red)
  bench tiers [games] [size] [dots]     round-robin of the AI difficulty tiers
  bench levels [games] [stand-in]       a stand-in player (a tier, or border) plays every level
  bench duel ...                        two custom configurations play each other (run it for help)
  bench variants [games]                how much happens in each game format (draws, captures, ...)
  log-check <file>...                   replay the games in logs exported from the app, check their results
  poc-report <file>...                  the POC test week's numbers, one shared log per tester

  tier: easy, normal, hard, expert        opening: cross (default), double, crosses, empty
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
        "log-check" -> if (LogCheck.run(args.drop(1)) > 0) exitProcess(1)
        "poc-report" -> if (PocReport.run(args.drop(1)) > 0) exitProcess(1)
        else -> println(USAGE)
    }
}

private fun listLevels() {
    for (level in Campaign.levels) {
        val size = level.map.lines().first { it.isNotBlank() }.trim().length
        println(
            "%2d  %-12s %2dx%-2d  %2d dots each  computer head start: %2d  %s"
                .format(level.id, level.name, size, size, level.dotsPerPlayer, level.headStart, tierName(level.ai)),
        )
    }
}

private fun levelFrom(arg: String?): Level {
    if (arg == null) return Campaign.levels.first()
    val id = arg.toIntOrNull()
    return Campaign.levels.firstOrNull { it.id == id }
        ?: exitWith("No level '$arg': choose 1 to ${Campaign.levels.size} (see: cli levels)")
}

private fun checkSize(width: Int, height: Int) {
    if (width !in 3..99 || height !in 3..99) exitWith("Boards are 3 to 99 in each direction, not ${width}x$height")
}

private fun tierName(config: AiConfig): String = TIERS.entries.firstOrNull { it.value == config }?.key ?: "custom"

internal val TIERS = linkedMapOf(
    "EASY" to AiConfig.EASY,
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
    println("Level ${level.id}: ${level.name} - computer is ${tierName(level.ai)}, head start ${level.headStart}")
    val computer = SearchAi(level.ai)
    val end = loop(level.newGame(), mapOf(level.human to null, level.computer to computer)) ?: return
    println(
        when (end.winner) {
            level.human -> "You win ${end.score(level.human)} to ${end.score(level.computer)} - level passed!"
            level.computer -> "The computer wins ${end.score(level.computer)} to ${end.score(level.human)}. Try again."
            else -> "A draw at ${end.score(level.human)} each - not enough: you need more captures than the computer."
        },
    )
}

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

/** Runs a game; a null entry in [seats] is a human at the keyboard. Returns null if the player quit. */
private fun loop(start: GameState, seats: Map<Player, Ai?>): GameState? {
    var state = start
    var last: Pos? = null
    while (!state.isOver) {
        print(render(state, last))
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
        val note = if (captured == 0) "" else ": ${result.captures.first().by} captures $captured dot${if (captured == 1) "" else "s"}!"
        "$mover plays ${name(action.pos)}$note"
    }
    Action.Pass -> "$mover passes"
    Action.Stop -> "$mover declares its remaining dots grounded - ${mover.opponent} may now capture whatever it can"
    Action.Finish -> "$mover is done capturing"
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
                owner == null -> if (state.isDead(x, y)) "," else "."
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
    append("    (r/b = captured, ',' = out of play)\n\n")
}

internal fun exitWith(message: String): Nothing {
    System.err.println(message)
    exitProcess(2)
}
