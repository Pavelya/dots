package io.github.pavelya.dots.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import dots.engine.Player

/**
 * A look for the whole game, as data: every ink, board size, piece, text style, sound and world
 * background the screens draw with, in a [light] and a [dark] version. A new look is a new value
 * of this class, not new drawing code.
 *
 * [NOTEBOOK] is the game's look, with the values of docs/design/theme-notebook.md and pieces.md.
 * [PLAIN] is the POC's look, kept to show that a look is data.
 */
@Immutable
data class GameTheme(
    val name: String,
    val light: Palette,
    val dark: Palette,
    val board: BoardLook,
    val type: TypeScale,
    /**
     * Each sound effect, by its name in docs/design/sound.md (`sfx_place_1`, `sfx_ring_draw` and
     * so on), as a raw resource. Empty until the sounds come into the game (V-08, V-10).
     */
    val sounds: Map<String, Int> = emptyMap(),
    /** Each world's background picture, by world number, as a drawable resource. Empty until V-11 draws them. */
    val worldBackgrounds: Map<Int, Int> = emptyMap(),
) {
    fun palette(dark: Boolean): Palette = if (dark) this.dark else light

    companion object {
        val PLAIN: GameTheme get() = PlainTheme
        val NOTEBOOK: GameTheme get() = NotebookTheme

        /** The theme the screen is drawn in. */
        val current: GameTheme
            @Composable @ReadOnlyComposable
            get() = LocalGameTheme.current
    }
}

/**
 * A theme's inks in one version, light or dark, by the names of docs/design/theme-notebook.md
 * ("Colours"), and the [material] colour scheme the plain Material parts of the screens use.
 */
@Immutable
data class Palette(
    /** The board and the screen background. */
    val paper: Color,
    /** Cards, notes, dialogs and sheets on the paper. */
    val card: Color,
    /** Grid lines on the board and the menu screens. */
    val grid: Color,
    /** Red's pieces, rings, name and score. */
    val red: Color,
    /** Blue's pieces, rings, name and score; main buttons. */
    val blue: Color,
    /** Fill of an area Red has captured. */
    val redWash: Color,
    /** Fill of an area Blue has captured. */
    val blueWash: Color,
    /** Links between red dots; Red's preview piece. */
    val redPale: Color,
    /** Links between blue dots; Blue's preview piece; behind the computer's face. */
    val bluePale: Color,
    /** The ring around a group one move from capture. */
    val highlighter: Color,
    /** The ring around a group two moves from capture. */
    val highlighterFaint: Color,
    /** Main text and icons. */
    val pencil: Color,
    /** Secondary text, hints, labels and empty slots. */
    val graphite: Color,
    /** The notebook margin line on menu screens. */
    val margin: Color,
    /** The strip of tape on notes and cards. */
    val tape: Color,
    /** Over the board behind Pause. */
    val scrim: Color,
    val material: ColorScheme,
) {
    fun ink(player: Player): Color = if (player == Player.RED) red else blue

    fun wash(player: Player): Color = if (player == Player.RED) redWash else blueWash

    fun pale(player: Player): Color = if (player == Player.RED) redPale else bluePale

    companion object {
        /** The inks of the theme and version the screen is drawn in. */
        val current: Palette
            @Composable @ReadOnlyComposable
            get() = LocalPalette.current
    }
}

/**
 * A theme's text styles (docs/design/theme-notebook.md, "Type"), without colour. [material] gives
 * the plain Material parts of the screens the same fonts.
 */
@Immutable
data class TypeScale(
    /** The result headline; level and world names. */
    val display: TextStyle,
    /** Screen titles. */
    val headline: TextStyle,
    /** Card and dialog titles; the one big button. */
    val title: TextStyle,
    /** Scores, counters, move numbers. */
    val score: TextStyle,
    /** Running text, level goals. */
    val body: TextStyle,
    /** Buttons, tabs, chips. */
    val label: TextStyle,
    /** Captions; the smallest text in the game. */
    val small: TextStyle,
) {
    /**
     * Material's text styles in this theme: its display and headline styles are [display] and
     * [headline]; the large title, body and label are [title], [body] and [label]; the small body
     * and label are [small]. The other styles keep Material's sizes in the [body] font.
     */
    val material: Typography by lazy {
        val base = Typography()
        val font = body.fontFamily
        Typography(
            displayLarge = display,
            displayMedium = display,
            displaySmall = display,
            headlineLarge = headline,
            headlineMedium = headline,
            headlineSmall = headline,
            titleLarge = title,
            titleMedium = base.titleMedium.copy(fontFamily = font),
            titleSmall = base.titleSmall.copy(fontFamily = font),
            bodyLarge = body,
            bodyMedium = base.bodyMedium.copy(fontFamily = font),
            bodySmall = small,
            labelLarge = label,
            labelMedium = base.labelMedium.copy(fontFamily = font),
            labelSmall = small,
        )
    }
}

private val LocalGameTheme = staticCompositionLocalOf { NotebookTheme }
private val LocalPalette = staticCompositionLocalOf { NotebookTheme.light }

/**
 * Draws [content] in [theme], in its dark version when [dark] (by default, when the phone's dark
 * theme is on). The Material parts of the screens take the theme's [Palette.material] and
 * [TypeScale.material].
 */
@Composable
fun DotsTheme(
    theme: GameTheme = GameTheme.NOTEBOOK,
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = theme.palette(dark)
    CompositionLocalProvider(LocalGameTheme provides theme, LocalPalette provides palette) {
        MaterialTheme(colorScheme = palette.material, typography = theme.type.material, content = content)
    }
}
