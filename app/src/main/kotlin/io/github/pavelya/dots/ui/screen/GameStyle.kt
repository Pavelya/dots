package io.github.pavelya.dots.ui.screen

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import dots.engine.Player
import io.github.pavelya.dots.ui.board.BoardStyle

/**
 * The game screen's inks, by the roles the canvas game frames give them (docs/design/canvas):
 * [red] and [blue] for each side's name, piece, tally and score, [bluePale] under the computer's
 * face, [pencil] for text, [graphite] for labels and empty slots, [paper] behind it all, [card]
 * for notes, the photo and buttons, [tape] for the tape on them, [scrim] over the board behind
 * Pause. The POC uses [plain]; the Notebook theme (V-02, V-03) gives the canvas's own colours.
 */
data class GameStyle(
    val paper: Color,
    val card: Color,
    val red: Color,
    val blue: Color,
    val bluePale: Color,
    val pencil: Color,
    val graphite: Color,
    val tape: Color,
    val scrim: Color,
) {
    fun ink(player: Player): Color = if (player == Player.RED) red else blue

    companion object {
        fun plain(colors: ColorScheme, dark: Boolean): GameStyle {
            val board = BoardStyle.plain(colors, dark)
            return GameStyle(
                paper = colors.background,
                card = colors.surfaceContainerLowest,
                red = board.red,
                blue = board.blue,
                bluePale = board.blue.copy(alpha = 0.3f),
                pencil = colors.onSurface,
                graphite = colors.onSurfaceVariant,
                tape = colors.tertiaryContainer,
                scrim = Color.Black.copy(alpha = if (dark) 0.55f else 0.4f),
            )
        }
    }
}
