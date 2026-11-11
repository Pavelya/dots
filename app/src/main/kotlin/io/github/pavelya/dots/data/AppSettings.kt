package io.github.pavelya.dots.data

import dots.engine.GameSettings
import dots.engine.Opening
import dots.log.GameLogEntry
import kotlin.random.Random

enum class BoardSize(val points: Int) { SMALL(12), STANDARD(15), LARGE(20) }

/** How many dots each player gets, as a share of the board's points. */
enum class GameLength(val percent: Int) { SHORT(15), LONG(30) }

enum class Difficulty { EASY, NORMAL, HARD }

/**
 * The ways of placing a dot that A-13 compares, picked in the developer menu. [key] names the
 * method in the game log, where `cli placing-report` reads it.
 */
enum class PlacingMethod(val key: String) {
    /** Tap a point to see the preview, tap it again to place the dot. */
    TWO_TAPS("two-taps"),

    /** The first tap zooms the board in around the point, to about 48 dp a point; the next tap places. */
    ZOOM("zoom"),

    /** While the finger is down, a magnifier above it shows the point under it; lifting the finger places. */
    MAGNIFIER("magnifier"),

    /** Press, drag to the point and lift; the point's row and column show around the finger. */
    DRAG("drag"),

    /** Hold a point until its row and column are drawn; lifting the finger earlier places nothing. */
    LONG_PRESS("long-press"),
}

/** What the player chose in Settings. Defaults are the POC's test format (docs/poc/README.md). */
data class AppSettings(
    val boardSize: BoardSize = BoardSize.STANDARD,
    val length: GameLength = GameLength.SHORT,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val threatHighlight: Boolean = false,
    /** Sound on or off, from the icon in Pause. A-09 adds it to Settings; the sounds come later (V-08, V-09). */
    val sound: Boolean = true,
    /** How a dot is placed, while A-13 compares the methods. */
    val placing: PlacingMethod = PlacingMethod.TWO_TAPS,
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

    /** A new game in these settings, against the computer or, when [vsComputer] is false, between two people. */
    fun newGame(vsComputer: Boolean, now: Long, seed: Long = Random.nextLong()): GameLogEntry = GameLogEntry.start(
        settings = gameSettings(seed),
        opponent = if (vsComputer) difficulty.name.lowercase() else GameLogEntry.HUMAN,
        threatHighlight = threatHighlight,
        now = now,
    )

    companion object {
        fun dotsFor(size: BoardSize, length: GameLength): Int = size.points * size.points * length.percent / 100
    }
}
