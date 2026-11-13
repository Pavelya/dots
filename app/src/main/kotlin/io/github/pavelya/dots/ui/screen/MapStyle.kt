package io.github.pavelya.dots.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import io.github.pavelya.dots.ui.theme.Palette

/**
 * The world map's inks, by the roles the canvas gives them (docs/design/canvas, the map frame):
 * [done] for passed levels and the inked path, [next] for the next level, stars and flags,
 * [pencil] for text and the castle, [graphite] for locked levels, the dotted path and labels,
 * [paper] behind it all, [card] for notes, [tape] for the tape on them. Each is one of the
 * theme's inks ([of]).
 */
@Immutable
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
        fun of(palette: Palette): MapStyle = MapStyle(
            paper = palette.paper,
            card = palette.card,
            done = palette.blue,
            onDone = palette.paper,
            next = palette.red,
            pencil = palette.pencil,
            graphite = palette.graphite,
            tape = palette.tape,
        )

        /** The map's inks in the theme the screen is drawn in. */
        val current: MapStyle
            @Composable @ReadOnlyComposable
            get() = of(Palette.current)
    }
}
