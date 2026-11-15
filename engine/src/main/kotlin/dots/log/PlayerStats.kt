package dots.log

import dots.campaign.LevelRecord
import dots.daily.DailyRecord
import dots.engine.Action
import dots.engine.Player

/**
 * The player's totals on Stats: the games they [played] and [won], the dots they [captured], and
 * their [bestCapture], the most dots one ring of theirs took. Only the player's own games count
 * ([GameStats.of]). The app stores it as one line of text ([text]).
 */
data class PlayerStats(val played: Int = 0, val won: Int = 0, val captured: Int = 0, val bestCapture: Int = 0) {

    /** With one more game, [game], counted. */
    operator fun plus(game: GameStats): PlayerStats = PlayerStats(
        played = played + 1,
        won = won + if (game.won) 1 else 0,
        captured = captured + game.captured,
        bestCapture = maxOf(bestCapture, game.bestCapture),
    )

    /** The totals as one line: `played:48 won:31 captured:212 best:9`. */
    val text: String get() = "$PLAYED:$played $WON:$won $CAPTURED:$captured $BEST:$bestCapture"

    companion object {
        val NONE = PlayerStats()

        private const val PLAYED = "played"
        private const val WON = "won"
        private const val CAPTURED = "captured"
        private const val BEST = "best"

        /** The totals of the games of a log, in any order; the games that do not count are left out. */
        fun of(entries: Iterable<GameLogEntry>): PlayerStats =
            entries.fold(NONE) { stats, entry -> GameStats.of(entry)?.let { stats + it } ?: stats }

        /** Reads a line written by [text]; a total that is missing or cannot be read is 0. */
        fun parse(text: String): PlayerStats {
            val totals = text.split(' ').mapNotNull { entry ->
                val count = entry.substringAfter(':', "").toIntOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
                entry.substringBefore(':') to count
            }.toMap()
            return PlayerStats(totals[PLAYED] ?: 0, totals[WON] ?: 0, totals[CAPTURED] ?: 0, totals[BEST] ?: 0)
        }
    }
}

/**
 * What one game adds to [PlayerStats]: whether the player [won] it, the dots they [captured] in
 * it, and the most dots one of their rings took ([bestCapture]).
 */
data class GameStats(val won: Boolean, val captured: Int, val bestCapture: Int) {
    companion object {
        /**
         * What [entry] adds to the player's stats, or null when it does not count. Only the player's
         * own finished games count: campaign levels, daily puzzles and free play, all against the
         * computer, where the player is red. Two players on one phone are left out (neither side is
         * "the player"), and so are a practice game (a replay of the tutorial, which changes nothing)
         * and an abandoned one.
         *
         * A level or a daily puzzle is won when its mission is passed; a free game, when the player
         * ends with more dots captured. Dots count once each, whatever a bonus point makes them
         * worth, and a capture that an Undo took back does not count.
         */
        fun of(entry: GameLogEntry): GameStats? {
            if (entry.outcome != Outcome.FINISHED || !entry.vsComputer || entry.practice) return null
            val record = entry.record
            val human = (record as? LevelRecord)?.level?.human ?: (record as? DailyRecord)?.puzzle?.human ?: Player.RED
            // Each capture of the player's with the move count before the action that made it. An
            // undo goes back to an earlier move count: what was captured from there on is undone.
            val captures = ArrayList<Pair<Int, Int>>()
            var state = record.start()
            for (action in record.actions) {
                val before = state.moveCount
                val result = state.apply(action)
                state = result.state
                if (action == Action.Undo) {
                    captures.removeAll { it.first >= state.moveCount }
                } else {
                    for (capture in result.captures) if (capture.by == human) captures += before to capture.dots.size
                }
            }
            val won = when (record) {
                is LevelRecord -> record.level.isPassed(state)
                is DailyRecord -> record.puzzle.isPassed(state)
                else -> state.winner == human
            }
            return GameStats(won, captures.sumOf { it.second }, captures.maxOfOrNull { it.second } ?: 0)
        }
    }
}
