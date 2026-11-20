package io.github.pavelya.dots.analytics

import dots.campaign.Campaign
import dots.campaign.Level
import dots.campaign.LevelResult
import dots.daily.DailyPuzzle
import dots.engine.Booster

/**
 * Where the game reports what players do, so we see where they get stuck or leave (decision 0033).
 * Store builds send it to Google Analytics for Firebase; debug builds and the tests send nothing.
 */
fun interface Analytics {
    fun log(event: AnalyticsEvent)

    companion object {
        /** Reports nothing. */
        val NONE = Analytics { }
    }
}

/**
 * One event: its [name] and [params], each a text or a whole number. Nothing in them points to a
 * person: no name, no account, no device ID, no free text the player typed.
 */
data class AnalyticsEvent(val name: String, val params: Map<String, Any> = emptyMap()) {
    init {
        require(NAME.matches(name) && name.length <= MAX_NAME) { "bad event name: $name" }
        require(params.size <= MAX_PARAMS) { "too many params in $name" }
        params.forEach { (key, value) ->
            require(NAME.matches(key) && key.length <= MAX_NAME) { "bad param name in $name: $key" }
            require(value is String && value.length <= MAX_TEXT || value is Long) { "param $key of $name must be a short text or a Long" }
        }
    }

    companion object {
        /** Google Analytics' limits: names of letters, digits and underscores, starting with a letter. */
        private val NAME = Regex("[a-zA-Z][a-zA-Z0-9_]*")
        private const val MAX_NAME = 40
        private const val MAX_PARAMS = 25
        private const val MAX_TEXT = 100
    }
}

/**
 * The game's events, the whole list. `first_open` (the install) and sessions come from Firebase
 * by itself. Level events use Google Analytics' own names (`level_start`, `level_end`), so its
 * reports know them. Practice games, such as a replay of the tutorial, are not reported.
 */
object Events {
    /** A new game of a campaign level begins: from the map, Play again or Retry. */
    fun levelStart(level: Level) = AnalyticsEvent("level_start", levelParams(level))

    /** A campaign level's game ended: passed (`success` 1) with its stars, or failed (`success` 0). */
    fun levelEnd(level: Level, result: LevelResult) = AnalyticsEvent(
        "level_end",
        levelParams(level) + mapOf("success" to flag(result.isPassed), "stars" to result.stars.toLong()),
    )

    /** The player used [booster] in [level]. */
    fun boosterUsed(level: Level, booster: Booster) = AnalyticsEvent("booster_used", levelParams(level) + ("booster" to booster.text))

    /** An attempt at a daily puzzle ended, solved (`success` 1) or not. */
    fun dailyPuzzleDone(puzzle: DailyPuzzle, solved: Boolean) =
        AnalyticsEvent("daily_puzzle_done", mapOf("puzzle_number" to puzzle.number.toLong(), "success" to flag(solved)))

    /**
     * `level_name` is the code players see (`1-4`); `level_number` counts every level from the
     * first (1-1 is 1), so a funnel can ask for "level 10" across worlds.
     */
    private fun levelParams(level: Level): Map<String, Any> = mapOf(
        "level_name" to level.code,
        "level_number" to number(level).toLong(),
        "world" to level.world.toLong(),
    )

    private fun number(level: Level): Int = Campaign.levels.indexOfFirst { it.world == level.world && it.number == level.number } + 1

    /** Google Analytics' `success` is 1 or 0. */
    private fun flag(yes: Boolean): Long = if (yes) 1L else 0L
}
