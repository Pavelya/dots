package dots.cli

import dots.ai.Ai
import dots.ai.AiConfig
import dots.ai.SearchAi
import dots.campaign.Campaign
import dots.campaign.Level
import dots.engine.GameState
import dots.engine.MoveResult
import dots.engine.Player
import dots.engine.Pos
import kotlin.system.exitProcess

private const val USAGE = """Dots - terminal prototype

  levels                      list the campaign levels
  play [level]                play a campaign level against the computer (you are red, you move first)
  pvp [width height dots]     two players on one keyboard (default 12 12 20)
  bench tiers [games]         round-robin of the AI difficulty tiers
  bench levels [games] [tier] a stand-in human (default NORMAL) plays every level

Moves are a column letter and a row number, e.g. e5. Type "pass" or "quit" at any prompt."""

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "levels" -> listLevels()
        "play" -> playLevel(Campaign.level(args.getOrNull(1)?.toIntOrNull() ?: 1))
        "pvp" -> {
            val (w, h, d) = (1..3).map { args.getOrNull(it)?.toIntOrNull() }
            pvp(w ?: 12, h ?: 12, d ?: 20)
        }
        "bench" -> Bench.run(args.drop(1))
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

private fun tierName(config: AiConfig): String = TIERS.entries.firstOrNull { it.value == config }?.key ?: "custom"

internal val TIERS = linkedMapOf(
    "EASY" to AiConfig.EASY,
    "NORMAL" to AiConfig.NORMAL,
    "HARD" to AiConfig.HARD,
    "EXPERT" to AiConfig.EXPERT,
)

private sealed interface Command {
    data class Move(val pos: Pos) : Command
    data object Pass : Command
    data object Quit : Command
}

private fun playLevel(level: Level) {
    println("Level ${level.id}: ${level.name} - computer is ${tierName(level.ai)}, head start ${level.headStart}")
    val computer = SearchAi(level.ai)
    val end = loop(level.newGame(), mapOf(level.human to null, level.computer to computer)) ?: return
    println(
        when (end.winner) {
            level.human -> "You win ${end.score(level.human)} to ${end.score(level.computer)}!"
            level.computer -> "The computer wins ${end.score(level.computer)} to ${end.score(level.human)}."
            else -> "A draw at ${end.score(level.human)} each."
        },
    )
}

private fun pvp(width: Int, height: Int, dots: Int) {
    val end = loop(GameState.start(width, height, dots), mapOf(Player.RED to null, Player.BLUE to null)) ?: return
    val winner = end.winner
    println(
        if (winner == null) "A draw at ${end.score(Player.RED)} each."
        else "$winner wins ${end.score(winner)} to ${end.score(winner.opponent)}.",
    )
}

/** Runs a game; a null entry in [seats] is a human at the keyboard. Returns null if the player quit. */
private fun loop(start: GameState, seats: Map<Player, Ai?>): GameState? {
    var state = start
    var last: Pos? = null
    while (!state.isOver) {
        print(render(state, last))
        val mover = state.toMove
        val ai = seats.getValue(mover)
        if (ai == null) {
            when (val command = ask(state)) {
                Command.Quit -> return null
                Command.Pass -> {
                    state = state.pass()
                    println("$mover passes")
                }
                is Command.Move -> {
                    val result = state.play(command.pos)
                    announce(mover, command.pos, result)
                    state = result.state
                    last = command.pos
                }
            }
        } else {
            val started = System.nanoTime()
            val move = ai.chooseMove(state)
            if (move == null) {
                state = state.pass()
                println("$mover passes")
            } else {
                val result = state.play(move)
                println("($mover thought for ${(System.nanoTime() - started) / 1_000_000} ms)")
                announce(mover, move, result)
                state = result.state
                last = move
            }
        }
    }
    print(render(state, last))
    return state
}

private fun announce(mover: Player, pos: Pos, result: MoveResult) {
    val captured = result.captures.sumOf { it.dots.size }
    val who = result.captures.firstOrNull()?.by
    val note = if (captured == 0) "" else ": ${who} captures $captured dot${if (captured == 1) "" else "s"}!"
    println("$mover plays ${name(pos)}$note")
}

private fun name(pos: Pos) = "${'a' + pos.x}${pos.y + 1}"

private fun ask(state: GameState): Command {
    while (true) {
        print("${state.toMove}> ")
        val line = readlnOrNull()?.trim()?.lowercase() ?: return Command.Quit
        when {
            line == "quit" || line == "q" -> return Command.Quit
            line == "pass" || line == "p" -> return Command.Pass
            else -> {
                val pos = parse(line, state)
                when {
                    pos == null -> println("Type a move like e5 (columns a-${'a' + state.width - 1}, rows 1-${state.height}).")
                    !state.isLegal(pos) -> println("${name(pos)} is not playable.")
                    else -> return Command.Move(pos)
                }
            }
        }
    }
}

private fun parse(text: String, state: GameState): Pos? {
    val match = Regex("([a-z])\\s*(\\d{1,2})").matchEntire(text) ?: return null
    val x = match.groupValues[1][0] - 'a'
    val y = match.groupValues[2].toInt() - 1
    return Pos(x, y).takeIf { state.inBounds(x, y) }
}

private val color = System.console() != null && System.getenv("NO_COLOR") == null

private fun paint(text: String, player: Player?, dim: Boolean = false, highlight: Boolean = false): String {
    if (!color || player == null) return text
    val hue = if (player == Player.RED) 31 else 34
    val style = (if (dim) "2" else "1") + (if (highlight) ";7" else "")
    return "\u001B[$style;${hue}m$text\u001B[0m"
}

private fun render(state: GameState, last: Pos?): String = buildString {
    append("\n    ")
    for (x in 0 until state.width) append('a' + x).append(' ')
    append('\n')
    for (y in 0 until state.height) {
        append("%2d  ".format(y + 1))
        for (x in 0 until state.width) {
            val owner = state.ownerAt(x, y)
            val cell = when {
                owner == null -> if (state.isDead(x, y)) "," else "."
                state.isCaptured(x, y) -> paint(owner.symbol.lowercase(), owner, dim = true)
                else -> paint(owner.symbol.toString(), owner, highlight = Pos(x, y) == last)
            }
            append(cell).append(' ')
        }
        append('\n')
    }
    val left = Player.entries.joinToString("   ") { p ->
        "${paint(p.name, p)} ${state.score(p)} captured" + (state.dotsLeft(p)?.let { ", $it dots left" } ?: "")
    }
    append("\n    $left\n")
    append("    (r/b = captured, ',' = out of play)\n\n")
}

internal fun exitWith(message: String): Nothing {
    System.err.println(message)
    exitProcess(2)
}
