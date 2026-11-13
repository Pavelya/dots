package io.github.pavelya.dots.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import dots.engine.Player
import io.github.pavelya.dots.ui.theme.BoardLook
import io.github.pavelya.dots.ui.theme.GameTheme
import io.github.pavelya.dots.ui.theme.Palette
import io.github.pavelya.dots.ui.theme.Piece
import io.github.pavelya.dots.ui.theme.PieceShape

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
        /** The board's style in [theme], in its dark version when [dark]. */
        fun of(theme: GameTheme, dark: Boolean): BoardStyle = BoardStyle(theme.palette(dark), theme.board)

        /** The board's style in the theme and the light or dark version the screen is drawn in. */
        val current: BoardStyle
            @Composable @ReadOnlyComposable
            get() = BoardStyle(Palette.current, GameTheme.current.board)
    }
}

/** [piece] in [color], centred on [center], for a dot size of [dotSize] pixels. */
fun DrawScope.drawPiece(piece: Piece, center: Offset, dotSize: Float, color: Color) {
    val half = dotSize * piece.across / 2
    when (piece.shape) {
        PieceShape.DOT -> drawCircle(color, half, center)
        PieceShape.DIAMOND -> drawPath(
            Path().apply {
                moveTo(center.x, center.y - half)
                lineTo(center.x + half, center.y)
                lineTo(center.x, center.y + half)
                lineTo(center.x - half, center.y)
                close()
            },
            color,
        )
    }
}
