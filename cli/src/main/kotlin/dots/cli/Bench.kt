package dots.cli

import dots.ai.Ai
import dots.ai.AiConfig
import dots.ai.Match
import dots.ai.SearchAi
import dots.campaign.Campaign
import dots.campaign.Level
import dots.campaign.LevelResult
import dots.campaign.Stars
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
            "           [attackWeightsA=3000,600,120,30] [dangerWeightsA=...] [attackWeightsB=...] [dangerWeightsB=...] [focusA=0] [focusB=0]\n" +
            "           [opening=cross|crosses]"
    private const val POSITIONS = 20
    private const val RUNS = 10
    private val LEVEL_CODE = Regex("\\d+-\\d+")

    fun run(args: List<String>) {
        val games = args.getOrNull(1)?.toIntOrNull() ?: DEFAULT_GAMES
        when (args.firstOrNull()) {
            "tiers" -> tiers(games, args.getOrNull(2)?.toIntOrNull() ?: 10, args.getOrNull(3)?.toIntOrNull() ?: 16)
            "steps" -> steps(games, args.getOrNull(2)?.toIntOrNull() ?: 15, args.getOrNull(3)?.toIntOrNull() ?: 33)
            "levels" -> {
                val (codes, names) = args.drop(2).partition { LEVEL_CODE.matches(it) }
                val levels = pick(codes)
                if (names.isEmpty()) Curve.run(games, levels) else levels(games, (names.singleOrNull() ?: exitWith("bench levels takes one stand-in at most")).uppercase(), levels)
            }
            "stars" -> {
                val (codes, names) = args.drop(2).partition { LEVEL_CODE.matches(it) }
                stars(games, names.map { it.uppercase() }.ifEmpty { listOf("EASY", "NORMAL", "HARD", "EXPERT") }, codes)
            }
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
                "usage: bench tiers [games] [size] [dots each] | bench steps [games] [size] [dots each] |\n" +
                    "       bench levels [games] [stand-in] [level...] | bench stars [games] [stand-in...] [level...] | bench variants [games] |\n" +
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
        // Games run in parallel; each has its own computers and seeds, so the result is the same.
        val scores = (0 until games).toList().parallelStream().map { g ->
            val aIsRed = g % 2 == 0
            val pair = g / 2
            val opening = if (crosses) Openings.randomCrosses(size, size, random = Random(pair)) else Openings.cross(size, size)
            val end = Match.playOut(
                GameState.start(size, size, dots, dots = opening),
                SearchAi(if (aIsRed) a else b, Random(1000 + pair)),
                SearchAi(if (aIsRed) b else a, Random(2000 + pair)),
            )
            val aSide = if (aIsRed) Player.RED else Player.BLUE
            end.score(aSide) to end.score(aSide.opponent)
        }.toList()
        val tally = Tally()
        for ((own, other) in scores) tally.add(own, other)
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
     * Each strength step ([AiConfig.STEPS]) plays the next stronger one, [games] paired games (see
     * [pairedGames]) on a [size] board from four random crosses, the format the testers played. The
     * steps are distinct if the stronger side wins every pairing with a sign test below 0.05.
     */
    private fun steps(games: Int, size: Int, dots: Int) {
        val names = AiConfig.STEPS.map { step -> TIERS.entries.first { it.value == step }.key }
        println("Strength steps, each against the next, $games games per pair, ${size}x$size from four crosses, $dots dots each")
        println("(W-L-D from the weaker step's side)")
        for (i in 0 until names.size - 1) {
            val started = System.nanoTime()
            val tally = pairedGames(AiConfig.STEPS[i], AiConfig.STEPS[i + 1], games, size, dots, crosses = true)
            println("%d %-8s vs %d %-8s %s  [%d s]".format(i + 1, names[i], i + 2, names[i + 1], tally, (System.nanoTime() - started) / 1_000_000_000))
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
        val unknown = settings.keys - setOf("attackWeightsA", "dangerWeightsA", "attackWeightsB", "dangerWeightsB", "focusA", "focusB", "opening")
        if (unknown.isNotEmpty()) exitWith("unknown setting ${unknown.first()}\nusage: $DUEL_USAGE")
        val n = positional.map { it.toIntOrNull() ?: exitWith("not a number: $it") }
        if (n.size != 11 && n.size != 13) exitWith("usage: $DUEL_USAGE")
        val (games, size, dots) = n
        fun weights(name: String) = settings[name]?.let(::weights) ?: AiConfig.BALANCED
        fun focus(name: String) = settings[name]?.let { it.toIntOrNull()?.takeIf { f -> f >= 0 } ?: exitWith("not a focus: $it") } ?: 0
        val a = AiConfig(
            depth = n[3], breadth = n[4], attackReach = n[5], dangerReach = n[6], variety = n.getOrElse(11) { 0 },
            attackWeights = weights("attackWeightsA"), dangerWeights = weights("dangerWeightsA"), focus = focus("focusA"),
        )
        val b = AiConfig(
            depth = n[7], breadth = n[8], attackReach = n[9], dangerReach = n[10], variety = n.getOrElse(12) { 0 },
            attackWeights = weights("attackWeightsB"), dangerWeights = weights("dangerWeightsB"), focus = focus("focusB"),
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
     * Mid-game positions for [variety]: two casual players (a two-move computer that plays a
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
     * A stand-in human plays each level as red against the level's own computer: a person of the
     * difficulty curve ([Person]), a computer tier, or BORDER, which only ever plays on the border
     * (where dots can never be captured).
     */
    private fun levels(games: Int, standInName: String, levels: List<Level>) {
        val standIn = standIn(standInName)
        println("Stand-in human: $standInName, $games games per level (captures W-L-D from the human's side; levels passed)")
        for (level in levels) {
            val started = System.nanoTime()
            val results = playLevel(level, games, standIn)
            val tally = Tally()
            for (result in results) tally.add(result.status.captured, result.status.lost)
            println(
                "%-5s %-12s %-22s %s  passed %d of %d  [%d s]".format(
                    level.code, level.name, level.mission.text, tally, results.count { it.isPassed }, games,
                    (System.nanoTime() - started) / 1_000_000_000,
                ),
            )
        }
    }

    /**
     * Each stand-in (see [levels]) plays each level [games] times; how many games earned 0, 1, 2
     * and 3 stars, and the star scores of the passed games. A level is "ok" when, over all the
     * stand-ins together, three stars are reachable but rarer than one. [codes] (like `1-4`) picks
     * levels; without them, every level plays.
     */
    private fun stars(games: Int, standInNames: List<String>, codes: List<String>) {
        val standIns = standInNames.associateWith(::standIn)
        val levels = pick(codes)
        println("Stars: $games games per level for each stand-in; games with 0/1/2/3 stars, then the star scores of passes (score x games)")
        for (level in levels) {
            val started = System.nanoTime()
            val all = IntArray(Stars.MAX + 1)
            val columns = standIns.map { (name, standIn) ->
                val results = playLevel(level, games, standIn)
                val counts = IntArray(Stars.MAX + 1)
                for (result in results) counts[result.stars]++
                for (n in counts.indices) all[n] += counts[n]
                val scores = results.filter { it.isPassed }.mapNotNull { it.starScore }.groupingBy { it }.eachCount().toSortedMap()
                "%s %s  %s".format(name, counts.joinToString("/"), scores.entries.joinToString(" ") { "${it.key}x${it.value}" })
            }
            val verdict = when {
                level.stars == null -> "no star targets"
                all[3] == 0 -> "3 stars never reached"
                all[3] >= all[1] -> "3 stars not rarer than 1"
                else -> "ok"
            }
            println("%-5s %-12s %-12s %s".format(level.code, level.name, level.stars?.text ?: "none", verdict))
            for (column in columns) println("      $column")
            println("      all ${all.joinToString("/")}  [${(System.nanoTime() - started) / 1_000_000_000} s]")
        }
    }

    /** The campaign levels with the given [codes] (like `1-4`); every level when there are none. */
    private fun pick(codes: List<String>): List<Level> = if (codes.isEmpty()) Campaign.levels else codes.map { code ->
        Campaign.levels.firstOrNull { it.code == code } ?: exitWith("No level $code in the campaign")
    }

    /** A stand-in human by name: a [Person], a computer tier, or BORDER (see [BorderOnly]). */
    private fun standIn(name: String): StandIn {
        Person.entries.firstOrNull { it.name == name }?.let { return it::player }
        val config = TIERS[name]
        if (config == null && name != "BORDER") {
            exitWith("unknown stand-in $name (${(Person.entries.map { it.name } + TIERS.keys).joinToString()}, BORDER)")
        }
        return { _, random -> if (config == null) BorderOnly(random) else SearchAi(config, random) }
    }

    /**
     * [games] games of [level] between [standIn] as red and the level's computer, each stopping
     * when the mission is decided. Games run in parallel; each has its own seeds, so the results
     * are the same every run.
     */
    fun playLevel(level: Level, games: Int, standIn: StandIn): List<LevelResult> =
        (0 until games).toList().parallelStream().map { g ->
            val end = Match.playOut(level.newGame(), standIn(level, Random(3000 + g)), SearchAi(level.ai, Random(4000 + g)), level::isOver)
            level.result(end)
        }.toList()

    /** Places every dot on a random free border cell while there is one. */
    private class BorderOnly(private val random: Random) : Ai {
        override fun choose(state: GameState): Action {
            val moves = state.legalMoves()
            val border = moves.filter { state.isBorder(it) }
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
