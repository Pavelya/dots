package io.github.pavelya.dots.ui.theme

import androidx.compose.runtime.Immutable

/**
 * A theme's board sizes (docs/design/theme-notebook.md, "The board"), each a [Measure] of the
 * spacing between two points. Widths are line widths; "across" is a diameter.
 */
@Immutable
data class BoardLook(
    /** A grid line. */
    val grid: Measure,
    /** The board's outermost grid lines. */
    val edge: Measure,
    /** The dot size: a round piece across. Other pieces are sized from it ([Piece.across]). */
    val dot: Measure,
    /** Red's piece. */
    val red: Piece,
    /** Blue's piece. */
    val blue: Piece,
    /** How much ink a captured piece keeps: 1 is full ink. */
    val capturedAlpha: Float,
    /** The ring round a captured area, through the centres of the capturing dots, in the captor's ink. */
    val ring: Measure,
    /** How far, in dp, each side of the ring may bow to either side of the straight line, like a pen stroke. */
    val ringBow: Float,
    /** How much the ring's width may vary along each side, as a share of it (0.15: up to 15% either way). */
    val ringWobble: Float,
    /** A hatch line across a captured area, in the captor's pale ink. No hatching when it is 0. */
    val hatch: Measure,
    /** The gap between two hatch lines, in dp, the same at every spacing. */
    val hatchGap: Float,
    /** True when the board always draws the links between touching dots of one side. */
    val links: Boolean,
    /** A link between two touching dots of one side, in the side's pale ink. */
    val link: Measure,
    /** The threat ring around each dot of a group close to capture, across. */
    val threatRing: Measure,
    /** The threat ring's line when the group is one move from capture. */
    val threatNear: Measure,
    /** The threat ring's line when the group is two moves from capture. */
    val threatFar: Measure,
    /** The closing point: where the attacker would close the ring, across. */
    val closingPoint: Measure,
    /** The closing point's line. */
    val closingLine: Measure,
    /** The preview's ring around the piece about to be placed, across. */
    val previewRing: Measure,
    /** The preview ring's line. */
    val previewLine: Measure,
    /** The ring marking the last move, across. */
    val lastMoveRing: Measure,
    /** The last-move ring's line. */
    val lastMoveLine: Measure,
    /** Drag's guides: the preview's row and column, from its ring to the board's edges (decision 0024). */
    val guide: Measure,
    /** How much of the ink of the side to move the guides show. */
    val guideAlpha: Float,
    /** The guides' dashes, in dp; solid lines when it is 0. */
    val guideDash: Float,
    /** The gaps between the guides' dashes, in dp. */
    val guideGap: Float,
)

/**
 * A length on the board: [ofSpacing] times the spacing between two points, plus [dp], kept
 * between [min] and [max] dp. "Half the spacing, at least 9 dp, at most 20 dp" is
 * `Measure(ofSpacing = 0.5f, min = 9f, max = 20f)`; "always 1 dp" is `Measure(dp = 1f)`.
 */
@Immutable
data class Measure(
    val ofSpacing: Float = 0f,
    val dp: Float = 0f,
    val min: Float = 0f,
    val max: Float = Float.POSITIVE_INFINITY,
) {
    /** This length in pixels, for points [spacing] pixels apart at [density] pixels per dp. */
    fun px(spacing: Float, density: Float): Float = (spacing * ofSpacing + dp * density).coerceIn(min * density, max * density)
}

/** A side's piece: its [shape], [across] times the dot size (docs/design/pieces.md). */
@Immutable
data class Piece(val shape: PieceShape, val across: Float = 1f)

enum class PieceShape {
    /** A round dot. */
    DOT,

    /** A square turned 45°, its corners pointing along the grid lines. */
    DIAMOND,
}
