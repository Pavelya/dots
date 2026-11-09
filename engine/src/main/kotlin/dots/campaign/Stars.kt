package dots.campaign

import dots.engine.Booster
import dots.engine.GameState

/**
 * What a level's stars count, when the level ends. Higher is better for each. [Stars.problem]
 * says which missions each one fits.
 */
enum class StarMeasure(val text: String) {
    /** Margin of victory: the player's score minus the computer's, a dot on a bonus point counting 2. */
    MARGIN("margin"),

    /**
     * Moves to spare: in "capture N in M moves", the moves left of M; in the other missions, the
     * dots the player has not placed. For missions that end the moment they pass.
     */
    MOVES("moves"),

    /** Dots saved: the player's dots on the map at the start (`R`) that are not captured. */
    SAVED("saved"),

    /** Boosters unused: the level's boosters the player has left, all kinds together. */
    BOOSTERS("boosters"),
}

/**
 * A level's star targets. Passing the level earns one star; a [measure] count of [two] or more
 * earns two, and of [three] or more earns three.
 */
data class Stars(val measure: StarMeasure, val two: Int, val three: Int) {
    init {
        require(two >= 1 && three > two) { "the three-star score must be higher than the two-star score, both 1 or more" }
    }

    /** The words a level file uses for it. */
    val text: String get() = "${measure.text} $two $three"

    /** The stars earned by passing a level with a count of [score]. */
    fun earned(score: Int): Int = when {
        score >= three -> MAX
        score >= two -> 2
        else -> 1
    }

    /**
     * Why these targets do not fit a level with [mission], [map], [dotsPerPlayer] and [boosters],
     * or null if they fit: the measure must vary between passes, two stars must ask for more than
     * every pass gets, and three stars must be within reach.
     */
    fun problem(mission: Mission, map: LevelMap, dotsPerPlayer: Int, boosters: Map<Booster, Int>): String? {
        val what = "stars for '${measure.text}'"
        return when (measure) {
            StarMeasure.MARGIN -> {
                val least = when (mission) {
                    Mission.Win, is Mission.WinWithFewerDots -> 1
                    is Mission.WinBy -> mission.margin
                    else -> null
                }
                if (least != null && two <= least) {
                    "every pass of '${mission.text}' already has a margin of $least, so $what must ask for more than $least"
                } else {
                    null
                }
            }
            StarMeasure.MOVES -> {
                val most = when (mission) {
                    is Mission.Capture -> mission.moves - 1
                    is Mission.Rescue, is Mission.Target -> dotsPerPlayer - 1
                    else -> return "$what fit only capture, rescue and target, the missions that end the moment they pass"
                }
                if (three > most) "at most $most moves can be left to spare, fewer than the $three that $what ask for" else null
            }
            StarMeasure.SAVED -> {
                val dots = map.points(LevelMap.HUMAN).size
                when {
                    mission is Mission.Survive -> "$what do not fit '${mission.text}': losing a dot fails it, so every pass saves them all"
                    three > dots -> "the map has $dots of the player's dots, fewer than the $three that $what ask for"
                    else -> null
                }
            }
            StarMeasure.BOOSTERS -> {
                val total = boosters.values.sum()
                if (three > total) "the level allows $total boosters, fewer than the $three that $what ask for" else null
            }
        }
    }

    companion object {
        /** The most stars a level gives. */
        const val MAX = 3
    }
}

/**
 * How a level game ended: where its mission stands, the [stars] earned (0 when the mission is not
 * passed) and the [starScore] they were judged on, which is null when the level has no star
 * targets.
 */
data class LevelResult(val status: MissionStatus, val stars: Int, val starScore: Int?) {
    val isPassed: Boolean get() = status.outcome == Outcome.PASSED
}

/** What [stars]'s measure counts for [level] in [state]. */
internal fun count(stars: Stars, level: Level, state: GameState): Int = when (stars.measure) {
    StarMeasure.MARGIN -> state.score(level.human) - state.score(level.computer)
    StarMeasure.MOVES -> when (val mission = level.mission) {
        is Mission.Capture -> (mission.moves - state.dotsPlaced(level.human)).coerceAtLeast(0)
        else -> state.dotsLeft(level.human) ?: 0
    }
    StarMeasure.SAVED -> level.map.points(LevelMap.HUMAN).count { !state.isCaptured(it) }
    StarMeasure.BOOSTERS -> level.boosters.keys.sumOf { state.boostersLeft(level.human, it) }
}
