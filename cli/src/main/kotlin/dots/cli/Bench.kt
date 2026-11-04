package dots.cli

import dots.ai.Ai
import dots.ai.AiConfig
import dots.ai.Match
import dots.ai.SearchAi
import dots.campaign.Campaign
import dots.engine.Action
import dots.engine.GameState
import dots.engine.Openings
import dots.engine.Player
import kotlin.random.Random

/** Self-play harness: measures whether harder tiers really win, and how hard each level is. */
internal object Bench {
    private const val DEFAULT_GAMES = 10

    fun run(args: List<String>) {
        val games = args.getOrNull(1)?.toIntOrNull() ?: DEFAULT_GAMES
        when (args.firstOrNull()) {
            "tiers" -> tiers(games, args.getOrNull(2)?.toIntOrNull() ?: 10, args.getOrNull(3)?.toIntOrNull() ?: 16)
            "levels" -> levels(games, args.getOrNull(2)?.uppercase() ?: "NORMAL")
            "duel" -> duel(args.drop(1))
            "variants" -> Variants.run(games)
            else -> exitWith(
                "usage: bench tiers [games] [size] [dots each] | bench levels [games] [tier] | bench variants [games] |\n" +
                    "       bench duel games size dots depthA breadthA attackA dangerA depthB breadthB attackB dangerB",
            )
        }
    }

    private class Tally(var wins: Int = 0, var losses: Int = 0, var draws: Int = 0, var margin: Int = 0) {
        val games get() = wins + losses + draws

        /**
         * Two-sided sign test on the decisive games: the chance of a split at least this lopsided
         * if both sides were equally strong. Small values (say below 0.05) mean a real difference.
         */
        val pValue: Double
            get() {
                val n = wins + losses
                if (n == 0) return 1.0
                var term = Math.pow(0.5, n.toDouble())
                var tail = 0.0
                for (k in 0..minOf(wins, losses)) {
                    tail += term
                    term = term * (n - k) / (k + 1)
                }
                return minOf(1.0, 2 * tail)
            }

        fun add(own: Int, other: Int) {
            margin += own - other
            when {
                own > other -> wins++
                own < other -> losses++
                else -> draws++
            }
        }

        override fun toString() = "%2d-%2d-%2d (win %3d%%, avg margin %+.1f, sign test p=%.3f)".format(
            wins, losses, draws, if (games == 0) 0 else 100 * wins / games,
            if (games == 0) 0.0 else margin.toDouble() / games, pValue,
        )
    }

    /**
     * Plays [games] games between [a] and [b] from the cross opening, in pairs: both games of a pair
     * use the same random seeds for the red and blue seats, with the configurations swapped. Luck
     * then cancels out between the two sides, so two identical configurations always come out even.
     */
    private fun pairedGames(a: AiConfig, b: AiConfig, games: Int, size: Int, dots: Int): Tally {
        val tally = Tally()
        for (g in 0 until games) {
            val aIsRed = g % 2 == 0
            val pair = g / 2
            val end = Match.playOut(
                GameState.start(size, size, dots, dots = Openings.cross(size, size)),
                SearchAi(if (aIsRed) a else b, Random(1000 + pair)),
                SearchAi(if (aIsRed) b else a, Random(2000 + pair)),
            )
            val aSide = if (aIsRed) Player.RED else Player.BLUE
            tally.add(end.score(aSide), end.score(aSide.opponent))
        }
        return tally
    }

    /** Every pair of tiers plays [games] paired games (see [pairedGames]) on a [size] board. */
    private fun tiers(games: Int, size: Int, dots: Int) {
        val names = TIERS.keys.toList()
        println("Tier round robin, $games games per pair, ${size}x$size from a cross, $dots dots each (W-L-D from the first tier's side)")
        for (i in names.indices) {
            for (j in i + 1 until names.size) {
                val started = System.nanoTime()
                val tally = pairedGames(TIERS.getValue(names[i]), TIERS.getValue(names[j]), games, size, dots)
                println("%-7s vs %-7s %s  [%d s]".format(names[i], names[j], tally, (System.nanoTime() - started) / 1_000_000_000))
            }
        }
    }

    /** Two hand-made configurations play each other; for tuning. */
    private fun duel(args: List<String>) {
        val n = args.map { it.toIntOrNull() ?: exitWith("not a number: $it") }
        if (n.size != 11) {
            exitWith("usage: bench duel games size dots depthA breadthA attackA dangerA depthB breadthB attackB dangerB")
        }
        val (games, size, dots) = n
        val a = AiConfig(depth = n[3], breadth = n[4], attackReach = n[5], dangerReach = n[6])
        val b = AiConfig(depth = n[7], breadth = n[8], attackReach = n[9], dangerReach = n[10])
        val started = System.nanoTime()
        val tally = pairedGames(a, b, games, size, dots)
        println("A=$a\nB=$b\n$games games, ${size}x$size, $dots dots: A $tally  [${(System.nanoTime() - started) / 1_000_000_000} s]")
    }

    /**
     * A stand-in human plays each level as red against the level's own computer: a computer tier,
     * or BORDER, which only ever plays on the border (where dots can never be captured).
     */
    private fun levels(games: Int, standInName: String) {
        val config = TIERS[standInName]
        if (config == null && standInName != "BORDER") {
            exitWith("unknown stand-in $standInName (${TIERS.keys.joinToString()}, BORDER)")
        }
        fun standIn(random: Random): Ai = if (config == null) BorderOnly(random) else SearchAi(config, random)
        println("Stand-in human: $standInName, $games games per level (W-L-D from the human's side)")
        for (level in Campaign.levels) {
            val tally = Tally()
            val started = System.nanoTime()
            for (g in 0 until games) {
                val end = Match.playOut(
                    level.newGame(),
                    standIn(Random(3000 + g)),
                    SearchAi(level.ai, Random(4000 + g)),
                )
                tally.add(end.score(level.human), end.score(level.computer))
            }
            println("%2d %-12s %s  [%d s]".format(level.id, level.name, tally, (System.nanoTime() - started) / 1_000_000_000))
        }
    }

    /** Places every dot on a random free border cell while there is one. */
    private class BorderOnly(private val random: Random) : Ai {
        override fun choose(state: GameState): Action {
            val moves = state.legalMoves()
            val border = moves.filter { it.x == 0 || it.y == 0 || it.x == state.width - 1 || it.y == state.height - 1 }
            val pick = border.ifEmpty { moves }
            return if (pick.isEmpty()) Action.Pass else Action.Place(pick[random.nextInt(pick.size)])
        }
    }
}
