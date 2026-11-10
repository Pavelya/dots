package io.github.pavelya.dots.ui.screen

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import io.github.pavelya.dots.ui.board.BoardStyle

/**
 * The world map's inks, by the roles the canvas gives them (docs/design/canvas, the map frame):
 * [done] for passed levels and the inked path, [next] for the next level, stars and flags,
 * [pencil] for text and the castle, [graphite] for locked levels, the dotted path and labels,
 * [paper] behind it all, [card] for notes, [tape] for the tape on them. The POC uses [plain]; the
 * Notebook theme (V-02, V-03) gives the canvas's own colours.
 */
data class MapStyle(
    val paper: Color,
    val card: Color,
    val done: Color,
    val onDone: Color,
    val next: Color,
    val pencil: Color,
    val graphite: Color,
    val tape: Color,
) {
    companion object {
        fun plain(colors: ColorScheme, dark: Boolean): MapStyle {
            val board = BoardStyle.plain(colors, dark)
            return MapStyle(
                paper = colors.background,
                card = colors.surfaceContainerLowest,
                done = board.blue,
                onDone = colors.background,
                next = board.red,
                pencil = colors.onSurface,
                graphite = colors.onSurfaceVariant,
                tape = colors.tertiaryContainer,
            )
        }
    }
}
