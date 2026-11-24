package io.github.pavelya.dots.game

import dots.ai.Ai
import dots.ai.SearchAi
import dots.campaign.Level
import dots.campaign.LevelRecord
import dots.campaign.Progress
import dots.campaign.Strength
import dots.campaign.Tutorial
import dots.daily.DailyPuzzle
import dots.daily.DailyRecord
import dots.engine.Booster
import dots.engine.GameRecord
import dots.log.GameLogEntry
import io.github.pavelya.dots.data.AppSettings
import kotlin.random.Random

/** The computer never thinks longer than this, even on a slow phone. */
const val COMPUTER_TIME_LIMIT_MILLIS = 1_500L

/**
 * The computer a game is played against, or null for two players on one phone. In a campaign level
 * it plays at the level's strength, for the level's mission ([dots.campaign.Level.computer]), and in
 * the daily puzzle likewise ([dots.daily.DailyPuzzle.computer]); in free play, at the strength the game was started with, the
 * one the player had reached in the campaign (decision 0003). Games saved before that name the
 * POC's test difficulty ("easy", "normal", "hard"), which are strengths of the same names.
 */
fun computerFor(game: GameLogEntry): Ai? {
    val record = game.record
    if (record is LevelRecord) {
        val level = record.level
        return level.computer(config = level.ai.copy(timeLimitMillis = COMPUTER_TIME_LIMIT_MILLIS))
    }
    if (record is DailyRecord) {
        val puzzle = record.puzzle
        return puzzle.computer(config = puzzle.ai.copy(timeLimitMillis = COMPUTER_TIME_LIMIT_MILLIS))
    }
    return Strength.entries.firstOrNull { it.text == game.opponent }?.computer()
}

/** The computer opponent at this strength, thinking no longer than [COMPUTER_TIME_LIMIT_MILLIS]. */
fun Strength.computer(random: Random = Random.Default): Ai =
    SearchAi(config.copy(timeLimitMillis = COMPUTER_TIME_LIMIT_MILLIS), random)

/**
 * A new free game in [settings]' board size and game length, from fresh crosses: against the
 * computer at [strength], or between two people on one phone when [strength] is null.
 */
fun freeGame(settings: AppSettings, strength: Strength?, now: Long, seed: Long = Random.nextLong()): GameLogEntry = GameLogEntry.start(
    settings = settings.gameSettings(seed),
    opponent = strength?.text ?: GameLogEntry.HUMAN,
    threatHighlight = settings.threatHighlight,
    now = now,
)

/**
 * The next free game after [game]: the same opponent, board size and game length (Play again keeps
 * the same setup), from fresh crosses. Threats show as [threatHighlight] says now.
 */
fun freeGameLike(game: GameLogEntry, threatHighlight: Boolean, now: Long, seed: Long = Random.nextLong()): GameLogEntry {
    val record = game.record as GameRecord
    return GameLogEntry.start(record.settings.copy(seed = seed), game.opponent, threatHighlight = threatHighlight, now = now)
}

/**
 * A new game of [level], before its first move, against the computer at the level's strength.
 * The player's [progress] gives its boosters ([Progress.game]): the level's own left after earlier
 * attempts (decision 0040) and the player's own (decision 0027). [gift] gives boosters for this
 * game only (the free Hint after three failures), and [ad] the one booster a rewarded ad gave for
 * this retry (decision 0030). Every way into a level comes here: Start, Retry, Next and the free
 * Hint's retry.
 * Threats show when [threatHighlight] is on, and always in the tutorial's levels (A-06). A
 * [practice] game, such as a replay of the tutorial, changes no campaign progress: it has the
 * level's full allowance and none of the player's own.
 */
fun levelGame(
    level: Level,
    threatHighlight: Boolean,
    now: Long,
    progress: Progress = Progress.NONE,
    gift: Map<Booster, Int> = emptyMap(),
    practice: Boolean = false,
    ad: Booster? = null,
): GameLogEntry = GameLogEntry.start(
    (if (practice) Progress.NONE else progress).game(level, gift, ad),
    level.strength.text,
    threatHighlight = threatHighlight || Tutorial.lesson(level) != null,
    now = now,
).copy(practice = practice)

/**
 * A new attempt at the daily [puzzle], before its first move: no boosters and no undo (decision
 * 0023). Threats show when [threatHighlight] is on. The attempt belongs to the puzzle's day,
 * whenever it ends.
 */
fun dailyGame(puzzle: DailyPuzzle, threatHighlight: Boolean, now: Long): GameLogEntry =
    GameLogEntry.start(DailyRecord(puzzle, emptyList()), puzzle.strength.text, threatHighlight = threatHighlight, now = now)
