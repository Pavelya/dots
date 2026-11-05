package io.github.pavelya.dots.game

import dots.ai.Ai
import dots.ai.AiConfig
import dots.ai.SearchAi
import dots.log.GameLogEntry
import io.github.pavelya.dots.data.Difficulty
import kotlin.random.Random

/** The computer never thinks longer than this, even on a slow phone. */
const val COMPUTER_TIME_LIMIT_MILLIS = 1_500L

/** The computer a game is played against, or null for two players on one phone. */
fun computerFor(game: GameLogEntry): Ai? = Difficulty.entries.firstOrNull { it.name.lowercase() == game.opponent }?.computer()

/** The computer opponent for a test difficulty (see decision 0003: levels replace this later). */
fun Difficulty.computer(random: Random = Random.Default): Ai {
    val config = when (this) {
        Difficulty.EASY -> AiConfig.EASY
        Difficulty.NORMAL -> AiConfig.NORMAL
        Difficulty.HARD -> AiConfig.HARD
    }
    return SearchAi(config.copy(timeLimitMillis = COMPUTER_TIME_LIMIT_MILLIS), random)
}
