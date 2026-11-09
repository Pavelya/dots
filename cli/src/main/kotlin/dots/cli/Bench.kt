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
    private const val DUEL_USAGE =
        "bench duel games size dots depthA breadthA attackA dangerA depthB breadthB attackB dangerB [varietyA varietyB]\n" +
            "           [attackWeightsA=3000,600,120,30] [dangerWeightsA=...] [attackWeightsB=...] [dangerWeightsB=...] [opening=cross|crosses]"
    private const val POSITIONS = 20
    private const val RUNS = 10

    fun run(args: List<String>) {
        val games = args.getOrNull(1)?.toIntOrNull() ?: DEFAULT_GAMES
        when (args.firstOrNull()) {
            "tiers" -> tiers(games, args.getOrNull(2)?.toIntOrNull() ?: 10, args.getOrNull(3)?.toIntOrNull() ?: 16)
            "levels" -> levels(games, args.getOrNull(2)?.uppercase() ?: "NORMAL")
            "duel" -> duel(args.drop(1))
            "variety" -> variety(
                args.getOrNull(1)?.uppercase() ?: "HARD",
                args.getOrNull(2)?.let { it.toIntOrNull() ?: exitWith("not a number: $it") },
            )
            "variants" -> Variants.run(games)
            "balance" -> Balance.run(
                args.getOrNull(1)?.let { it.toIntOrNull() ?: exitWith("not a number: $it") } ?: 20,
                args.getOrNull(2)?.let(::weights),
                args.getOrNull(3)?.let(::weights),
            )
            else -> exitWith(
                "usage: bench tiers [games] [size] [dots each] | bench levels [games] [tier] | bench variants [games] |\n" +
                    "       bench variety [tier] [margin] | bench balance [games] [attack weights] [danger weights] |\n" +
                    "       $DUEL_USAGE",
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
     * Plays [games] games between [a] and [b] in pairs: both games of a pair start from the same
     * position and use the same random seeds for the red and blue seats, with the configurations
     * swapped. Luck then cancels out between the two sides, so two identical configurations always
     * come out even. The games start from one cross, or with [crosses] from four random crosses.
     */
    private fun pairedGames(a: AiConfig, b: AiConfig, games: Int, size: Int, dots: Int, crosses: Boolean = false): Tally {
        val tally = Tally()
        for (g in 0 until games) {
            val aIsRed = g % 2 == 0
            val pair = g / 2
            val opening = if (crosses) Openings.randomCrosses(size, size, random = Random(pair)) else Openings.cross(size, size)
            val end = Match.playOut(
                GameState.start(size, size, dots, dots = opening),
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

    /**
     * Two hand-made configurations play each other; for tuning. The optional last two numbers are
     * each side's [AiConfig.variety] (default 0). Settings written as name=value may follow: each
     * side's threat weights ([AiConfig.attackWeights], [AiConfig.dangerWeights]; default
     * [AiConfig.BALANCED]) and the opening (one cross by default).
     */
    private fun duel(args: List<String>) {
        val (named, positional) = args.partition { '=' in it }
        val settings = named.associate { it.substringBefore('=') to it.substringAfter('=') }
        val unknown = settings.keys - setOf("attackWeightsA", "dangerWeightsA", "attackWeightsB", "dangerWeightsB", "opening")
        if (unknown.isNotEmpty()) exitWith("unknown setting ${unknown.first()}\nusage: $DUEL_USAGE")
        val n = positional.map { it.toIntOrNull() ?: exitWith("not a number: $it") }
        if (n.size != 11 && n.size != 13) exitWith("usage: $DUEL_USAGE")
        val (games, size, dots) = n
        fun weights(name: String) = settings[name]?.let(::weights) ?: AiConfig.BALANCED
        val a = AiConfig(
            depth = n[3], breadth = n[4], attackReach = n[5], dangerReach = n[6], variety = n.getOrElse(11) { 0 },
            attackWeights = weights("attackWeightsA"), dangerWeights = weights("dangerWeightsA"),
        )
        val b = AiConfig(
            depth = n[7], breadth = n[8], attackReach = n[9], dangerReach = n[10], variety = n.getOrElse(12) { 0 },
            attackWeights = weights("attackWeightsB"), dangerWeights = weights("dangerWeightsB"),
        )
        val crosses = when (val opening = settings["opening"] ?: "cross") {
            "cross" -> false
            "crosses" -> true
            else -> exitWith("unknown opening $opening (cross, crosses)")
        }
        val started = System.nanoTime()
        val tally = pairedGames(a, b, games, size, dots, crosses)
        val from = if (crosses) "four crosses" else "a cross"
        println("A=$a\nB=$b\n$games games, ${size}x$size from $from, $dots dots: A $tally  [${(System.nanoTime() - started) / 1_000_000_000} s]")
    }

    /**
     * How often a tier answers the same position differently: [POSITIONS] fixed mid-game positions,
     * each answered [RUNS] times with a different seed. [margin], if given, replaces the tier's
     * own [AiConfig.variety].
     */
    private fun variety(tierName: String, margin: Int?) {
        val tier = TIERS[tierName] ?: exitWith("unknown tier $tierName (${TIERS.keys.joinToString()})")
        val config = if (margin == null) tier else tier.copy(variety = margin)
        println("$tierName, variety ${config.variety}: different moves in each of $POSITIONS positions, $RUNS runs each")
        val counts = varietyPositions().map { state ->
            (0 until RUNS).map { run -> SearchAi(config, Random(5000 + run)).chooseMove(state) }.toSet().size
        }
        println(counts.joinToString(" "))
        println("positions with at least 2 different moves: ${counts.count { it >= 2 }} of $POSITIONS")
    }

    /**
     * Mid-game positions for [variety]: two casual players (a NORMAL-like computer that plays a
     * random nearby move a quarter of the time) play from a cross on 10x10, 24 dots each, for 8 to
     * 20 moves. Spelled out rather than built from NORMAL, so retuning a tier keeps the positions.
     */
    private fun varietyPositions(): List<GameState> {
        val casual = AiConfig(depth = 2, breadth = 10, mistakeRate = 0.25, attackReach = 2, dangerReach = 1)
        return (0 until POSITIONS).map { i ->
            val red = SearchAi(casual, Random(6000 + i))
            val blue = SearchAi(casual, Random(7000 + i))
            var state = GameState.start(10, 10, 24, dots = Openings.cross(10, 10))
            repeat(8 + i % 5 * 3) {
                if (state.isOver) return@repeat
                val ai = if (state.toMove == Player.RED) red else blue
                state = state.apply(ai.choose(state)).state
            }
            state
        }
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
        println("Stand-in human: $standInName, $games games per level (captures W-L-D from the human's side; levels passed)")
        for (level in Campaign.levels) {
            val tally = Tally()
            var passed = 0
            val started = System.nanoTime()
            for (g in 0 until games) {
                val end = Match.playOut(
                    level.newGame(),
                    standIn(Random(3000 + g)),
                    SearchAi(level.ai, Random(4000 + g)),
                    level::isOver,
                )
                tally.add(end.score(level.human), end.score(level.computer))
                if (level.isPassed(end)) passed++
            }
            println(
                "%-5s %-12s %-22s %s  passed %d of %d  [%d s]".format(
                    level.code, level.name, level.mission.text, tally, passed, games,
                    (System.nanoTime() - started) / 1_000_000_000,
                ),
            )
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

    /** Threat weights written as numbers separated by commas, e.g. 3000,600,120,30. */
    private fun weights(text: String): List<Int> {
        val numbers = text.split(',').map { it.trim().toIntOrNull() ?: exitWith("not a list of weights: $text") }
        if (numbers.isEmpty() || numbers.any { it < 0 }) exitWith("weights must be numbers of 0 or more: $text")
        return numbers
    }
}
