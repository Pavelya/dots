package dots.cli

import dots.ai.Ai
import dots.ai.AiConfig
import dots.ai.Hints
import dots.ai.SearchAi
import dots.engine.Action
import dots.engine.GameState
import dots.engine.Openings
import dots.engine.Player
import dots.engine.Pos
import kotlin.random.Random

/**
 * How often each tier attacks, defends or does neither, in the format the testers played
 * (15x15, four random crosses, 33 dots each), against a fixed opponent that stands in for a person.
 *
 * Every move of the tier is sorted by what it does to groups that are at most [REACH] dots from
 * capture:
 * - **attack**: it captures, or brings an enemy group closer to capture;
 * - **defend**: it moves one of its own threatened groups further from capture, or grounds it;
 * - **both**, or **neither**.
 *
 * It also counts:
 * - **threatened**: moves made while one of the tier's own groups was at most two dots from
 *   capture, and the share of those that defended;
 * - **en prise**: moves that attack, but do not defend, while one of the tier's own groups is one
 *   dot from capture;
 * - **lost after attack**: dots the tier loses on the opponent's very next move after an attack
 *   that was not also a defence; and all the dots it **lost**.
 */
internal object Balance {
    private const val SIZE = 15
    private const val DOTS = 33
    private const val REACH = 3

    /**
     * The opponent: a computer that thinks two moves ahead, looks at the whole board and plays a
     * random nearby move a quarter of the time.
     * Spelled out rather than built from NORMAL, so retuning a tier keeps the opponent.
     */
    private val person = AiConfig(
        depth = 2, breadth = 10, mistakeRate = 0.25, variety = 10, attackReach = 2, dangerReach = 1,
        attackWeights = listOf(3000, 600, 120, 30), dangerWeights = listOf(2500, 400, 80, 20),
    )

    private class Counts {
        var games = 0
        var moves = 0
        var attack = 0
        var defend = 0
        var both = 0
        var neither = 0
        var threatened = 0
        var answered = 0
        var enPrise = 0
        var lostAfterAttack = 0
        var lost = 0

        private fun share(n: Int) = if (moves == 0) 0.0 else 100.0 * n / moves
        private fun perGame(n: Int) = if (games == 0) 0.0 else n.toDouble() / games

        fun row() = "%5.1f%%  %5.1f%%  %5.1f%%  %5.1f%%    %5.1f%%      %5.1f%%    %5.2f     %5.2f          %5.2f".format(
            share(attack), share(defend), share(both), share(neither),
            share(threatened), if (threatened == 0) 0.0 else 100.0 * answered / threatened,
            perGame(enPrise), perGame(lostAfterAttack), perGame(lost),
        )
    }

    /**
     * Plays [games] games per tier, the tier as red in half of them. [attack] and [danger], if
     * given, replace every tier's [AiConfig.attackWeights] and [AiConfig.dangerWeights].
     */
    fun run(games: Int, attack: List<Int>?, danger: List<Int>?) {
        println("Attack and defence, $games games per tier against a casual stand-in, ${SIZE}x$SIZE, four crosses, $DOTS dots each")
        if (attack != null) println("attack weights $attack for every tier")
        if (danger != null) println("danger weights $danger for every tier")
        println("                                            threatened  of which             lost after")
        println("tier      attack  defend  both    neither   (moves)     defended  en prise  attack         lost")
        println("          (share of the tier's moves)                              (per game)")
        for ((name, tier) in TIERS) {
            val config = tier.copy(
                attackWeights = attack ?: tier.attackWeights,
                dangerWeights = danger ?: tier.dangerWeights,
            )
            val counts = Counts()
            val started = System.nanoTime()
            for (g in 0 until games) play(config, g, counts)
            println("%-8s  %s   [%d s]".format(name, counts.row(), (System.nanoTime() - started) / 1_000_000_000))
        }
    }

    private fun play(config: AiConfig, game: Int, counts: Counts) {
        val tierSide = if (game % 2 == 0) Player.RED else Player.BLUE
        val pair = game / 2
        val tier = SearchAi(config, Random(8000 + pair))
        val other: Ai = SearchAi(person, Random(9000 + pair))
        var state = GameState.start(SIZE, SIZE, DOTS, dots = Openings.randomCrosses(SIZE, SIZE, random = Random(pair)))
        var attackedOnly = false
        counts.games++
        while (!state.isOver) {
            val mover = state.toMove
            val action = (if (mover == tierSide) tier else other).choose(state)
            if (mover == tierSide && action is Action.Place) {
                val result = state.play(action.pos)
                val attacked = result.captures.isNotEmpty() ||
                    nearer(costs(result.state, mover.opponent), than = costs(state, mover.opponent))
                val ownBefore = costs(state, mover)
                val defended = nearer(ownBefore, than = costs(result.state, mover))
                counts.moves++
                if (ownBefore.values.any { it <= 2 }) {
                    counts.threatened++
                    if (defended) counts.answered++
                }
                when {
                    attacked && defended -> counts.both++
                    attacked -> counts.attack++
                    defended -> counts.defend++
                    else -> counts.neither++
                }
                attackedOnly = attacked && !defended
                if (attackedOnly && ownBefore.values.any { it == 1 }) counts.enPrise++
                state = result.state
                continue
            }
            val before = state.score(mover)
            state = state.apply(action).state
            if (mover != tierSide) {
                val lost = state.score(mover) - before
                counts.lost += lost
                if (attackedOnly) counts.lostAfterAttack += lost
                attackedOnly = false
            }
        }
    }

    /** For every dot of [player] that is at most [REACH] dots from capture: how many dots away. */
    private fun costs(state: GameState, player: Player): Map<Pos, Int> {
        val result = HashMap<Pos, Int>()
        for (danger in Hints.dangers(state, player, REACH)) {
            for (dot in danger.group) result[dot] = danger.dotsNeeded
        }
        return result
    }

    /**
     * True if some dot is nearer to capture in [costs] than in [than]: it needs fewer dots, or it
     * is not threatened at all in [than].
     */
    private fun nearer(costs: Map<Pos, Int>, than: Map<Pos, Int>): Boolean =
        costs.any { (dot, cost) -> cost < (than[dot] ?: Int.MAX_VALUE) }
}
