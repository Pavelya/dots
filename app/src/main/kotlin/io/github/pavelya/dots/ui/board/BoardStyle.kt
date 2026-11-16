package io.github.pavelya.dots.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import dots.engine.Player
import io.github.pavelya.dots.ui.theme.BoardLook
import io.github.pavelya.dots.ui.theme.GameTheme
import io.github.pavelya.dots.ui.theme.Palette
import io.github.pavelya.dots.ui.theme.Piece
import io.github.pavelya.dots.ui.theme.PieceShape
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
        /** The board's style in [theme], in its dark version when [dark], with high-contrast pieces when [highContrast]. */
        fun of(theme: GameTheme, dark: Boolean, highContrast: Boolean = false): BoardStyle =
            BoardStyle(theme.palette(dark), theme.board(highContrast))

        /** The board's style in the theme, the light or dark version and the piece contrast the screen is drawn in. */
        val current: BoardStyle
            @Composable @ReadOnlyComposable
            get() = BoardStyle(Palette.current, GameTheme.current.board(GameTheme.highContrast))
    }
}

/**
 * [piece] in [color], centred on [center], for a dot size of [dotSize] pixels: filled, or as an
 * outline [outline] pixels wide when it is above 0.
 */
fun DrawScope.drawPiece(piece: Piece, center: Offset, dotSize: Float, color: Color, outline: Float = 0f) {
    val half = dotSize * piece.across / 2
    val style = if (outline > 0f) Stroke(outline) else Fill
    when (piece.shape) {
        PieceShape.DOT -> drawCircle(color, half, center, style = style)
        PieceShape.DIAMOND -> drawPath(
            Path().apply {
                moveTo(center.x, center.y - half)
                lineTo(center.x + half, center.y)
                lineTo(center.x, center.y + half)
                lineTo(center.x - half, center.y)
                close()
            },
            color,
            style = style,
        )
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
