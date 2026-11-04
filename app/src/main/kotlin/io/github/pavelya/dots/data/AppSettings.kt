package io.github.pavelya.dots.data

import dots.engine.GameSettings
import dots.engine.Opening

enum class BoardSize(val points: Int) { SMALL(12), STANDARD(15), LARGE(20) }

/** How many dots each player gets, as a share of the board's points. */
enum class GameLength(val percent: Int) { SHORT(15), LONG(30) }

enum class Difficulty { EASY, NORMAL, HARD }

/** What the player chose in Settings. Defaults are the POC's test format (docs/poc/README.md). */
data class AppSettings(
    val boardSize: BoardSize = BoardSize.STANDARD,
    val length: GameLength = GameLength.SHORT,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val threatHighlight: Boolean = false,
) {
    val dotsPerPlayer: Int get() = dotsFor(boardSize, length)

    /** A new game in these settings, starting from four random crosses placed by [seed]. */
    fun gameSettings(seed: Long): GameSettings = GameSettings(
        width = boardSize.points,
        height = boardSize.points,
        dotsPerPlayer = dotsPerPlayer,
        opening = Opening.CROSSES,
        seed = seed,
    )

    companion object {
        fun dotsFor(size: BoardSize, length: GameLength): Int = size.points * size.points * length.percent / 100
    }
}
