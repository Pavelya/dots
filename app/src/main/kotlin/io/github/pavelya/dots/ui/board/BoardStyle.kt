package io.github.pavelya.dots.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import dots.engine.Player
import io.github.pavelya.dots.data.PieceStyleId
import io.github.pavelya.dots.data.RingEffectId
import io.github.pavelya.dots.ui.theme.BoardLook
import io.github.pavelya.dots.ui.theme.GameTheme
import io.github.pavelya.dots.ui.theme.Palette
import io.github.pavelya.dots.ui.theme.Piece
import io.github.pavelya.dots.ui.theme.PieceShape
import io.github.pavelya.dots.ui.theme.dressed
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Everything [BoardView] draws with: the inks of the theme's light or dark [palette], and its
 * board sizes ([look]). It comes from the theme ([current]); nothing about the board's look is
 * written in the drawing code.
 */
@Immutable
data class BoardStyle(val palette: Palette, val look: BoardLook) {
    val paper: Color get() = palette.paper
    val grid: Color get() = palette.grid
    val pencil: Color get() = palette.pencil
    val graphite: Color get() = palette.graphite

    fun color(player: Player): Color = palette.ink(player)

    fun piece(player: Player): Piece = if (player == Player.RED) look.red else look.blue

    companion object {
        /**
         * The board's style in [theme], in its dark version when [dark], with high-contrast pieces
         * when [highContrast], and with the piece style [pieces] and the ring effect [rings].
         */
        fun of(
            theme: GameTheme,
            dark: Boolean,
            highContrast: Boolean = false,
            pieces: PieceStyleId = PieceStyleId.DEFAULT,
            rings: RingEffectId = RingEffectId.DEFAULT,
        ): BoardStyle = BoardStyle(theme.palette(dark), theme.board(highContrast).dressed(pieces, rings))

        /**
         * The board's style in the theme, the light or dark version, the piece contrast, the piece
         * style and the ring effect the screen is drawn in.
         */
        val current: BoardStyle
            @Composable @ReadOnlyComposable
            get() = BoardStyle(
                Palette.current,
                GameTheme.current.board(GameTheme.highContrast).dressed(GameTheme.pieceStyle, GameTheme.ringEffect),
            )
    }
}

/**
 * [piece] in [color], centred on [center], for a dot size of [dotSize] pixels: filled, or as an
 * outline [outline] pixels wide when it is above 0, with round corners.
 */
fun DrawScope.drawPiece(piece: Piece, center: Offset, dotSize: Float, color: Color, outline: Float = 0f) {
    val half = dotSize * piece.across / 2
    val style = if (outline > 0f) Stroke(outline, join = StrokeJoin.Round) else Fill
    StyleShapes.outline(piece.shape)?.let { shape ->
        val k = dotSize * piece.across
        return drawStyleShape(shape, center, k, color, if (outline > 0f) Stroke(outline / k, join = StrokeJoin.Round) else Fill)
    }
    when (piece.shape) {
        PieceShape.DOT -> drawCircle(color, half, center, style = if (outline > 0f) Stroke(outline) else Fill)
        PieceShape.DIAMOND -> drawPath(
            Path().apply {
                moveTo(center.x, center.y - half)
                lineTo(center.x + half, center.y)
                lineTo(center.x, center.y + half)
                lineTo(center.x - half, center.y)
                close()
            },
            color,
            style = if (outline > 0f) Stroke(outline) else Fill,
        )
        else -> drawPath(piecePath(piece, center, dotSize), color, style = style)
    }
}

/**
 * A band [band] pixels wide round [piece]'s outline, in [color], with the piece inside it: a glow or
 * a high-contrast band. A dot's or a diamond's band is the piece grown ([grown]); every other
 * shape's follows its outline with round corners (theme-star-chart.md, theme-neon-arcade.md and,
 * for the piece styles' shapes, cosmetics.md).
 */
fun DrawScope.drawPieceBand(piece: Piece, center: Offset, dotSize: Float, color: Color, band: Float) {
    if (band <= 0f) return
    StyleShapes.outline(piece.shape)?.let { shape ->
        val k = dotSize * piece.across
        drawStyleShape(shape, center, k, color, Fill)
        return drawStyleShape(shape, center, k, color, Stroke(2 * band / k, join = StrokeJoin.Round))
    }
    when (piece.shape) {
        PieceShape.DOT, PieceShape.DIAMOND -> drawPiece(piece.grown(band, dotSize), center, dotSize, color)
        else -> {
            val path = piecePath(piece, center, dotSize)
            drawPath(path, color)
            drawPath(path, color, style = Stroke(2 * band, join = StrokeJoin.Round))
        }
    }
}

/**
 * [piece] grown by [by] pixels all round, for a dot size of [dotSize] pixels: a diamond's corners
 * move out by the square root of 2 times as much as its sides.
 */
fun Piece.grown(by: Float, dotSize: Float): Piece {
    val corner = if (shape == PieceShape.DIAMOND) by * sqrt(2f) else by
    return copy(across = across + 2 * corner / dotSize)
}

/**
 * The box round [piece] for a dot size of [dotSize], with its centre at (0, 0): a comet reaches
 * further up and to the left, a star and a triangle further up than down, a heart further down.
 */
fun Piece.bounds(dotSize: Float): Rect {
    val half = dotSize * across / 2
    StyleShapes.bounds(shape)?.let { box ->
        val k = dotSize * across
        return Rect(box.left * k, box.top * k, box.right * k, box.bottom * k)
    }
    return when (shape) {
        PieceShape.DOT, PieceShape.DIAMOND -> Rect(-half, -half, half, half)
        PieceShape.COMET -> {
            // The head's arc runs from the tail's lower side round by the bottom and the right.
            val points = cometPoints(this, Offset.Zero, dotSize) + listOf(Offset(half, 0f), Offset(0f, half))
            Rect(points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y })
        }
        else -> {
            val points = cornerPoints(this, Offset.Zero, dotSize)
            Rect(points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y })
        }
    }
}

/** A piece style's [shape], in dot sizes, drawn [k] pixels to the dot size round [center]; a stroke's width is in dot sizes too. */
private fun DrawScope.drawStyleShape(shape: Path, center: Offset, k: Float, color: Color, style: DrawStyle) {
    withTransform({
        translate(center.x, center.y)
        scale(k, k, pivot = Offset.Zero)
    }) {
        drawPath(shape, color, style = style)
    }
}

/** The outline of a star, a comet or a triangle, as a path. */
private fun piecePath(piece: Piece, center: Offset, dotSize: Float): Path = Path().apply {
    if (piece.shape == PieceShape.COMET) {
        val points = cometPoints(piece, center, dotSize)
        moveTo(points[0].x, points[0].y)
        for (point in points.drop(1)) lineTo(point.x, point.y)
        // Back to the first point round the head, the long way: by the bottom and the right.
        val head = dotSize * piece.across / 2
        arcTo(Rect(center, head), COMET_TAIL_ANGLE - COMET_TAIL_LEAVES, -(360f - 2 * COMET_TAIL_LEAVES), false)
    } else {
        val points = cornerPoints(piece, center, dotSize)
        moveTo(points[0].x, points[0].y)
        for (point in points.drop(1)) lineTo(point.x, point.y)
    }
    close()
}

/** A star's ten corners or a triangle's three, from the top point clockwise. */
private fun cornerPoints(piece: Piece, center: Offset, dotSize: Float): List<Offset> {
    val reach = dotSize * piece.across / 2
    return if (piece.shape == PieceShape.STAR) {
        List(10) { k ->
            val r = if (k % 2 == 0) reach else reach * STAR_INNER
            val a = Math.toRadians(-90.0 + 36 * k)
            Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
        }
    } else {
        listOf(-90.0, 30.0, 150.0).map { d ->
            val a = Math.toRadians(d)
            Offset(center.x + reach * cos(a).toFloat(), center.y + reach * sin(a).toFloat())
        }
    }
}

/**
 * The comet's outline without its head's arc (theme-star-chart.md, "The pieces"): where the tail
 * leaves the head, a side streak's tip, a notch, the middle streak's tip, a notch, the other side
 * streak's tip, and where the tail meets the head again. Lengths are in the theme page's dot sizes,
 * the head being 0.75 of one.
 */
private fun cometPoints(piece: Piece, center: Offset, dotSize: Float): List<Offset> {
    val head = dotSize * piece.across / 2
    val d = dotSize * piece.across / COMET_HEAD
    val tail = Math.toRadians(COMET_TAIL_ANGLE.toDouble())
    val ux = cos(tail).toFloat()
    val uy = sin(tail).toFloat()
    // Across the tail: a quarter turn clockwise from along it.
    val nx = -uy
    val ny = ux
    fun at(along: Float, across: Float) = Offset(center.x + (ux * along + nx * across) * d, center.y + (uy * along + ny * across) * d)
    val leaves = Math.toRadians(COMET_TAIL_LEAVES.toDouble())
    val c = cos(leaves).toFloat()
    val s = sin(leaves).toFloat()
    return listOf(
        Offset(center.x + (ux * c + nx * s) * head, center.y + (uy * c + ny * s) * head),
        at(1.05f, 0.3f),
        at(0.65f, 0.11f),
        at(1.4f, 0f),
        at(0.65f, -0.11f),
        at(1.05f, -0.3f),
        Offset(center.x + (ux * c - nx * s) * head, center.y + (uy * c - ny * s) * head),
    )
}

/** A star's inner corners, as a share of its points' radius. */
private const val STAR_INNER = 0.55f

/** The comet's head, in the theme page's dot sizes. */
private const val COMET_HEAD = 0.75f

/** The tail's direction, in degrees from the right, clockwise on the screen: up and to the left. */
private const val COMET_TAIL_ANGLE = -135f

/** The tail leaves the head this many degrees either side of its line. */
private const val COMET_TAIL_LEAVES = 70f
