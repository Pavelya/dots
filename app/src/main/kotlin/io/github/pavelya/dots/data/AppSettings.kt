package io.github.pavelya.dots.data

import dots.engine.GameSettings
import dots.engine.Opening

enum class BoardSize(val points: Int) { SMALL(12), STANDARD(15), LARGE(20) }

/**
 * How a free game ends (decision 0025): a dot budget, as a share of the board's points for each
 * player ([percent]), or [OPEN_ENDED], as on paper, with no limit: a player stops and the
 * opponent ends the game with done.
 */
enum class GameLength(val percent: Int?) { SHORT(15), LONG(30), OPEN_ENDED(null) }

/**
 * How the player places a dot, chosen in Settings (decision 0024). [key] names the method in the
 * game log, where `cli placing-report` reads it.
 */
enum class PlacingMethod(val key: String) {
    /** Tap a point to see the preview, tap it again to place the dot. The default. */
    TWO_TAPS("two-taps"),

    /** Press, drag to the point and lift; the point's row and column show around the finger. */
    DRAG("drag"),
}

/**
 * What the player chose: in Settings, and the board size and game length of free play and two
 * players, chosen on Play a game and remembered for the next game.
 */
data class AppSettings(
    val boardSize: BoardSize = BoardSize.STANDARD,
    val length: GameLength = GameLength.SHORT,
    val threatHighlight: Boolean = false,
    /**
     * Sound on or off, from the icon in Pause. Off silences music and sound effects whatever their
     * volumes; the sounds themselves come later (V-08, V-09).
     */
    val sound: Boolean = true,
    /** The music's volume in Settings, from 0 (silent) to [MAX_VOLUME]. */
    val musicVolume: Int = DEFAULT_VOLUME,
    /** The sound effects' volume in Settings, from 0 (silent) to [MAX_VOLUME]. */
    val effectsVolume: Int = DEFAULT_VOLUME,
    /** Vibration when a dot lands and a ring closes (motion.md, rule 6); V-07 makes the phone vibrate. */
    val vibration: Boolean = true,
    /** High-contrast pieces (pieces.md); V-04 draws them. */
    val highContrast: Boolean = false,
    /** How a dot is placed (decision 0024). */
    val placing: PlacingMethod = PlacingMethod.TWO_TAPS,
    /** True once the player skipped the tutorial's guided steps; replaying the tutorial shows them again (A-06). */
    val tutorialSkipped: Boolean = false,
) {
    /** Each player's dots, or null for an open-ended game. */
    val dotsPerPlayer: Int? get() = dotsFor(boardSize, length)

    /** A new free game in these settings, starting from four random crosses placed by [seed]. */
    fun gameSettings(seed: Long): GameSettings = GameSettings(
        width = boardSize.points,
        height = boardSize.points,
        dotsPerPlayer = dotsPerPlayer,
        opening = Opening.CROSSES,
        seed = seed,
    )

    companion object {
        const val MAX_VOLUME = 100

        /** Both volumes start here, as the Settings wireframe draws them. */
        const val DEFAULT_VOLUME = 70

        /** Each player's dots on a board of [size] for a game of [length]; null when it is open-ended. */
        fun dotsFor(size: BoardSize, length: GameLength): Int? = length.percent?.let { size.points * size.points * it / 100 }
    }
}
