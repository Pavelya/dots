package io.github.pavelya.dots.game

import dots.ai.Ai
import dots.ai.AiConfig
import dots.ai.SearchAi
import dots.campaign.Level
import dots.campaign.LevelRecord
import dots.campaign.Tutorial
import dots.engine.Booster
import dots.log.GameLogEntry
import io.github.pavelya.dots.data.Difficulty
import kotlin.random.Random

/** The computer never thinks longer than this, even on a slow phone. */
const val COMPUTER_TIME_LIMIT_MILLIS = 1_500L

/**
 * The computer a game is played against, or null for two players on one phone. In a campaign level
 * it plays at the level's strength; in free play, at the test difficulty of the game's settings.
 */
fun computerFor(game: GameLogEntry): Ai? {
    val record = game.record
    if (record is LevelRecord) return SearchAi(record.level.ai.copy(timeLimitMillis = COMPUTER_TIME_LIMIT_MILLIS))
    return Difficulty.entries.firstOrNull { it.name.lowercase() == game.opponent }?.computer()
}

/** The computer opponent for a test difficulty (see decision 0003: levels replace this later). */
fun Difficulty.computer(random: Random = Random.Default): Ai {
    val config = when (this) {
        Difficulty.EASY -> AiConfig.EASY
        Difficulty.NORMAL -> AiConfig.NORMAL
        Difficulty.HARD -> AiConfig.HARD
    }
    return SearchAi(config.copy(timeLimitMillis = COMPUTER_TIME_LIMIT_MILLIS), random)
}

/**
 * A new game of [level], before its first move, against the computer at the level's strength.
 * [gift] gives the player boosters on top of the level's own (the free Hint after three failures).
 * Threats show when [threatHighlight] is on, and always in the tutorial's levels (A-06). A
 * [practice] game, such as a replay of the tutorial, changes no campaign progress.
 */
fun levelGame(
    level: Level,
    threatHighlight: Boolean,
    now: Long,
    gift: Map<Booster, Int> = emptyMap(),
    practice: Boolean = false,
): GameLogEntry = GameLogEntry.start(
    LevelRecord(level, emptyList(), gift),
    level.strength.text,
    threatHighlight = threatHighlight || Tutorial.lesson(level) != null,
    now = now,
).copy(practice = practice)
