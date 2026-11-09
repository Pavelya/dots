package dots.cli

import dots.ai.AiConfig
import dots.ai.SearchAi
import dots.engine.GameState
import dots.engine.Openings
import dots.engine.Player
import kotlin.random.Random

/**
 * Plays candidate game formats (board size, dot budget, opening) between stand-in players and
 * measures how much happens in a game: draws, captures, lead changes, comebacks and how long it
 * takes before the first capture. It cannot measure fun, only whether there is action to enjoy.
 */
internal object Variants {
    private class Format(val size: Int, val share: Int, val opening: String) {
        /** Dots each player may place: [share] percent of the board's cells. */
        val dots get() = size * size * share / 100
        val label get() = "${size}x$size  ${share}% (${dots} each)  ${opening.padEnd(7)}"
    }

    private class Stats {
        var games = 0
        var draws = 0
        var redWins = 0
        var captures = 0
        var dotsCaptured = 0
        var leadChanges = 0
        var comebacks = 0
        var moves = 0
        val firstCapture = ArrayList<Int>()

        fun row(seconds: Long) = "%4.0f%%  %5.1f  %5.1f  %5.1f  %4.0f%%  %6s  %4.0f%%  %4.0f   [%d s]".format(
            100.0 * draws / games, captures.toDouble() / games, dotsCaptured.toDouble() / games,
            leadChanges.toDouble() / games, 100.0 * comebacks / games,
            firstCapture.sorted().let { if (it.size * 2 <= games) "never" else it[games / 2].toString() },
            100.0 * redWins / maxOf(1, games - draws), moves.toDouble() / games, seconds,
        )
    }

    fun run(games: Int) {
        val players = linkedMapOf(
            // Spelled out rather than built from a tier, so retuning a tier keeps the stand-in.
            "casual" to AiConfig(depth = 2, breadth = 10, mistakeRate = 0.25, variety = 10, attackReach = 2, dangerReach = 1),
            "strong" to AiConfig.HARD,
        )
        val formats = buildList {
            for (size in listOf(9, 12, 15)) for (share in listOf(15, 30)) for (opening in listOf("empty", "cross", "crosses")) {
                add(Format(size, share, opening))
            }
            add(Format(20, 15, "cross"))
        }
        println("$games games per row; both sides are the same stand-in")
        println("casual = two moves ahead, whole board, a random nearby move 25% of the time; strong = HARD")
        println("columns: draws, captures/game, dots captured/game, lead changes/game, games won from behind,")
        println("         median move of the first capture, first player's share of wins, moves/game")
        for ((name, config) in players) {
            println("\n== $name vs $name")
            for (format in formats) {
                val started = System.nanoTime()
                val stats = Stats()
                repeat(games) { g -> play(format, config, g, stats) }
                println("${format.label}  ${stats.row((System.nanoTime() - started) / 1_000_000_000)}")
            }
        }
    }

    private fun play(format: Format, config: AiConfig, g: Int, stats: Stats) {
        val n = format.size
        val dots = when (format.opening) {
            "cross" -> Openings.cross(n, n)
            "crosses" -> Openings.randomCrosses(n, n, 4, Random(5000 + g))
            else -> emptyMap()
        }
        var state = GameState.start(n, n, format.dots, dots = dots)
        val red = SearchAi(config, Random(1000 + g))
        val blue = SearchAi(config, Random(2000 + g))
        var leader = 0
        var redTrailed = false
        var blueTrailed = false
        var first: Int? = null
        while (!state.isOver) {
            val ai = if (state.toMove == Player.RED) red else blue
            val result = state.apply(ai.choose(state))
            state = result.state
            for (capture in result.captures) {
                stats.captures++
                stats.dotsCaptured += capture.dots.size
                if (first == null) first = state.moveCount
            }
            val diff = state.score(Player.RED) - state.score(Player.BLUE)
            val now = Integer.signum(diff)
            if (now != 0 && leader != 0 && now != leader) stats.leadChanges++
            if (now != 0) leader = now
            if (diff < 0) redTrailed = true
            if (diff > 0) blueTrailed = true
        }
        stats.games++
        stats.moves += state.moveCount
        first?.let { stats.firstCapture += it }
        when (state.winner) {
            null -> stats.draws++
            Player.RED -> { stats.redWins++; if (redTrailed) stats.comebacks++ }
            Player.BLUE -> if (blueTrailed) stats.comebacks++
        }
    }
}
