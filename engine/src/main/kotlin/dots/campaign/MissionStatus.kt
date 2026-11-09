package dots.campaign

import dots.engine.GameState
import dots.engine.Grounding
import dots.engine.Player
import dots.engine.Pos
import kotlin.math.min

/** Whether a mission is still open, passed or failed. */
enum class Outcome { PLAYING, PASSED, FAILED }

/** What decided a mission. */
enum class Reason {
    /** Capture: the player captured enough dots. */
    CAPTURED_ENOUGH,

    /** Capture: the player has placed all the dots the mission allows. */
    OUT_OF_MOVES,

    /** Capture, win, win by N, win with fewer dots: even capturing every computer dot that can still be captured would not be enough. */
    OUT_OF_REACH,

    /** Survive: the computer has placed its dots, or can capture nothing more, and the player lost none. */
    SURVIVED,

    /** Survive: the computer captured one of the player's dots. */
    LOST_A_DOT,

    /** Rescue: the marked dot is joined to the border and can never be captured. */
    DOT_SAFE,

    /** Rescue: the marked dot was captured. */
    DOT_CAPTURED,

    /** Target: the player captured the target dot. */
    TARGET_CAPTURED,

    /** Target: the target dot is joined to the border and can never be captured. */
    TARGET_SAFE,

    /** The game ended, and its result decided the mission. */
    GAME_OVER,
}

/**
 * Where a level's mission stands in a position: the outcome and what decided it, and the numbers
 * the player follows on the way. The app turns these into words in each language.
 */
data class MissionStatus(
    val outcome: Outcome,
    /** What decided the mission; null while it is [Outcome.PLAYING]. */
    val reason: Reason?,
    /** The player's score: computer dots captured, 2 for each on a bonus point. */
    val captured: Int,
    /** The computer's score: the player's dots captured, 2 for each on a bonus point. */
    val lost: Int,
    /**
     * For missions with a move limit: in "capture N in M moves", the player's moves left; in
     * "survive M moves", the computer's. Null for the other missions.
     */
    val movesLeft: Int?,
) {
    val isDecided: Boolean get() = outcome != Outcome.PLAYING
}

/**
 * Judges [mission] in [state] for [human]. A mission is decided as soon as no later move can
 * change it; see [Mission] for what each one asks.
 */
internal fun judge(mission: Mission, state: GameState, human: Player): MissionStatus {
    val computer = human.opponent
    val captured = state.score(human)
    val lost = state.score(computer)
    fun status(outcome: Outcome, reason: Reason?, movesLeft: Int? = null) =
        MissionStatus(outcome, reason, captured, lost, movesLeft)

    fun byMargin(margin: Int): MissionStatus {
        val reach = stillCapturable(state, human)
        return when {
            state.isOver -> status(if (captured - lost >= margin) Outcome.PASSED else Outcome.FAILED, Reason.GAME_OVER)
            reach != null && captured + reach - lost < margin -> status(Outcome.FAILED, Reason.OUT_OF_REACH)
            else -> status(Outcome.PLAYING, null)
        }
    }

    return when (mission) {
        Mission.Win -> byMargin(1)
        is Mission.WinBy -> byMargin(mission.margin)
        is Mission.WinWithFewerDots -> byMargin(1)
        is Mission.Capture -> {
            val left = (mission.moves - state.dotsPlaced(human)).coerceAtLeast(0)
            val reach = stillCapturable(state, human)
            when {
                captured >= mission.dots -> status(Outcome.PASSED, Reason.CAPTURED_ENOUGH, left)
                left == 0 -> status(Outcome.FAILED, Reason.OUT_OF_MOVES, left)
                state.isOver -> status(Outcome.FAILED, Reason.GAME_OVER, left)
                reach != null && captured + reach < mission.dots -> status(Outcome.FAILED, Reason.OUT_OF_REACH, left)
                else -> status(Outcome.PLAYING, null, left)
            }
        }
        is Mission.Survive -> {
            val left = (mission.moves - state.dotsPlaced(computer)).coerceAtLeast(0)
            when {
                lost > 0 -> status(Outcome.FAILED, Reason.LOST_A_DOT, left)
                left == 0 || stillCapturable(state, computer) == 0 -> status(Outcome.PASSED, Reason.SURVIVED, left)
                state.isOver -> status(Outcome.PASSED, Reason.GAME_OVER, left)
                else -> status(Outcome.PLAYING, null, left)
            }
        }
        is Mission.Rescue -> when {
            state.isCaptured(mission.dot) -> status(Outcome.FAILED, Reason.DOT_CAPTURED)
            mission.dot in Grounding.safe(state, human) -> status(Outcome.PASSED, Reason.DOT_SAFE)
            state.isOver -> status(Outcome.PASSED, Reason.GAME_OVER)
            else -> status(Outcome.PLAYING, null)
        }
        is Mission.Target -> when {
            state.isCaptured(mission.dot) -> status(Outcome.PASSED, Reason.TARGET_CAPTURED)
            mission.dot in Grounding.safe(state, computer) -> status(Outcome.FAILED, Reason.TARGET_SAFE)
            state.isOver -> status(Outcome.FAILED, Reason.GAME_OVER)
            else -> status(Outcome.PLAYING, null)
        }
    }
}

/**
 * The most [player] can still add to their score from here on: the opponent's live dots that are
 * not safe, plus those the opponent may still place, each counted 2 if it is or may be on a bonus
 * point. Null when the opponent has no dot limit.
 */
private fun stillCapturable(state: GameState, player: Player): Int? {
    val enemy = player.opponent
    if (state.isOver) return 0
    val safe = Grounding.safe(state, enemy)
    var exposed = 0
    var freeBonus = 0
    for (y in 0 until state.height) {
        for (x in 0 until state.width) {
            val owner = state.ownerAt(x, y)
            if (owner == enemy && !state.isCaptured(x, y) && Pos(x, y) !in safe) exposed += state.worth(x, y)
            if (owner == null && !state.isDead(x, y) && state.isBonus(x, y)) freeBonus++
        }
    }
    return when {
        // After a stop only the collecting player moves, and the other places no more dots.
        state.isCleanup -> if (state.cleaner == player) exposed else 0
        else -> {
            val left = state.dotsLeft(enemy) ?: return null
            exposed + left + min(left, freeBonus)
        }
    }
}
