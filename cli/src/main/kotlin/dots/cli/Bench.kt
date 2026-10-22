package dots.cli

import dots.ai.AiConfig
import dots.ai.Match
import dots.ai.SearchAi
import dots.campaign.Campaign
import dots.engine.GameState
import dots.engine.Player
import kotlin.random.Random

/** Self-play harness: measures whether harder tiers really win, and how hard each level is. */
internal object Bench {
    private const val DEFAULT_GAMES = 10

    fun run(args: List<String>) {
        val games = args.getOrNull(1)?.toIntOrNull() ?: DEFAULT_GAMES
        when (args.firstOrNull()) {
            "tiers" -> tiers(games)
            "levels" -> levels(games, args.getOrNull(2)?.uppercase() ?: "NORMAL")
            else -> exitWith("usage: bench tiers [games] | bench levels [games] [tier]")
        }
    }

    private class Tally(var wins: Int = 0, var losses: Int = 0, var draws: Int = 0, var margin: Int = 0) {
        val games get() = wins + losses + draws

        fun add(own: Int, other: Int) {
            margin += own - other
            when {
                own > other -> wins++
                own < other -> losses++
                else -> draws++
            }
        }

        override fun toString() = "%2d-%2d-%2d (win %3d%%, avg margin %+.1f)"
            .format(wins, losses, draws, if (games == 0) 0 else 100 * wins / games, if (games == 0) 0.0 else margin.toDouble() / games)
    }

    /** Each pair plays [games] games on a 10x10 board, 16 dots each, swapping colours every game. */
    private fun tiers(games: Int) {
        val names = TIERS.keys.toList()
        println("Tier round robin, $games games per pair, 10x10, 16 dots each (W-L-D from the first tier's side)")
        for (i in names.indices) {
            for (j in i + 1 until names.size) {
                val a = TIERS.getValue(names[i])
                val b = TIERS.getValue(names[j])
                val tally = Tally()
                val started = System.nanoTime()
                for (g in 0 until games) {
                    val aIsRed = g % 2 == 0
                    val end = Match.playOut(
                        GameState.start(10, 10, 16),
                        SearchAi(if (aIsRed) a else b, Random(1000 + g)),
                        SearchAi(if (aIsRed) b else a, Random(2000 + g)),
                    )
                    val aSide = if (aIsRed) Player.RED else Player.BLUE
                    tally.add(end.score(aSide), end.score(aSide.opponent))
                }
                println("%-7s vs %-7s %s  [%d s]".format(names[i], names[j], tally, (System.nanoTime() - started) / 1_000_000_000))
            }
        }
    }

    /** A stand-in human of the given tier plays each level as red against the level's own computer. */
    private fun levels(games: Int, proxyName: String) {
        val proxy = TIERS[proxyName] ?: exitWith("unknown tier $proxyName (${TIERS.keys.joinToString()})")
        println("Stand-in human: $proxyName, $games games per level (W-L-D from the human's side)")
        for (level in Campaign.levels) {
            val tally = Tally()
            val started = System.nanoTime()
            for (g in 0 until games) {
                val end = Match.playOut(
                    level.newGame(),
                    SearchAi(proxy, Random(3000 + g)),
                    SearchAi(level.ai, Random(4000 + g)),
                )
                tally.add(end.score(level.human), end.score(level.computer))
            }
            println("%2d %-12s %s  [%d s]".format(level.id, level.name, tally, (System.nanoTime() - started) / 1_000_000_000))
        }
    }
}
