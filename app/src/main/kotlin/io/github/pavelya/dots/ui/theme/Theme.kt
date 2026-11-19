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
import io.github.pavelya.dots.audio.Effect
import io.github.pavelya.dots.data.ThemeId

/**
 * A look for the whole game, as data: every ink, board size, piece, text style and sound the
 * screens draw with, in a [light] and a [dark] version. A new look is a new value of this class,
 * not new drawing code. The worlds' map pages are not part of it: they belong to the worlds
 * ([io.github.pavelya.dots.ui.screen.MapPages], decision 0028).
 *
 * [NOTEBOOK] is the game's look, with the values of docs/design/theme-notebook.md and pieces.md.
 * [STAR_CHART] and [NEON_ARCADE] are the cosmetic themes of docs/design/theme-star-chart.md and
 * theme-neon-arcade.md (decision 0028). [PLAIN] is the POC's look, kept to show that a look is data.
 */
@Immutable
data class GameTheme(
    val name: String,
    val light: Palette,
    val dark: Palette,
    val board: BoardLook,
    val type: TypeScale,
    /** The board with the player's "High-contrast pieces" setting on (pieces.md, "High contrast"). */
    val highContrastBoard: BoardLook = board,
    /**
     * The board's sounds this look plays in place of Notebook's (docs/design/sound.md, "Board
     * sounds of the other themes"): for each [Effect] it replaces, the name of its own file in
     * `assets/audio/`. Effects it leaves out play Notebook's; [sound] picks.
     */
    val sounds: Map<Effect, String> = emptyMap(),
    /**
     * The inks of the opponent's faces on the game screen (docs/design/illustrations.md, "Other
     * themes"). The map and its notes, the level card among them, stay Notebook's (decision 0028).
     */
    val faceInks: FaceInks = FaceInks.NOTEBOOK,
) {
    fun palette(dark: Boolean): Palette = if (dark) this.dark else light

    /** The board's look, with high-contrast pieces when [highContrast]. */
    fun board(highContrast: Boolean): BoardLook = if (highContrast) highContrastBoard else board

    /** The file this look plays for [effect]: its own, if it has one, or Notebook's. */
    fun sound(effect: Effect): String = sounds[effect] ?: effect.file

    companion object {
        val PLAIN: GameTheme get() = PlainTheme
        val NOTEBOOK: GameTheme get() = NotebookTheme
        val STAR_CHART: GameTheme get() = StarChartTheme
        val NEON_ARCADE: GameTheme get() = NeonArcadeTheme

        /**
         * The themes a player can have: Notebook, the default, then the cosmetic ones. Which of them a
         * player may choose is not the theme's business (decision 0029).
         */
        val ALL: List<GameTheme> get() = listOf(NotebookTheme, StarChartTheme, NeonArcadeTheme)

        /** The theme saved as [id]. */
        fun of(id: ThemeId): GameTheme = when (id) {
            ThemeId.NOTEBOOK -> NotebookTheme
            ThemeId.STAR_CHART -> StarChartTheme
            ThemeId.NEON_ARCADE -> NeonArcadeTheme
        }

        /** The theme the screen is drawn in. */
        val current: GameTheme
            @Composable @ReadOnlyComposable
            get() = LocalGameTheme.current

        /** Whether the player turned on high-contrast pieces; [DotsTheme] passes it on. */
        val highContrast: Boolean
            @Composable @ReadOnlyComposable
            get() = LocalHighContrast.current

        /** Whether the screen is drawn in the theme's dark version; [DotsTheme] passes it on. */
        val isDark: Boolean
            @Composable @ReadOnlyComposable
            get() = LocalDark.current
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
    /** The small point at each grid crossing, in a theme whose board has them ([BoardLook.chartPoint]). */
    val chartPoint: Color = grid,
    /** The bright line along the middle of Red's ring, in a theme whose rings have one ([BoardLook.tubeCore]). */
    val redCore: Color = red,
    /** The bright line along the middle of Blue's ring. */
    val blueCore: Color = blue,
) {
    fun ink(player: Player): Color = if (player == Player.RED) red else blue

    fun wash(player: Player): Color = if (player == Player.RED) redWash else blueWash

    fun pale(player: Player): Color = if (player == Player.RED) redPale else bluePale

    fun core(player: Player): Color = if (player == Player.RED) redCore else blueCore

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
private val LocalHighContrast = staticCompositionLocalOf { false }
private val LocalDark = staticCompositionLocalOf { false }

/**
 * Draws [content] in [theme], in its dark version when [dark] (by default, when the phone's dark
 * theme is on). The Material parts of the screens take the theme's [Palette.material] and
 * [TypeScale.material]. Every board draws high-contrast pieces when [highContrast].
 */
@Composable
fun DotsTheme(
    theme: GameTheme = GameTheme.NOTEBOOK,
    dark: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = theme.palette(dark)
    CompositionLocalProvider(
        LocalGameTheme provides theme,
        LocalPalette provides palette,
        LocalHighContrast provides highContrast,
        LocalDark provides dark,
    ) {
        MaterialTheme(colorScheme = palette.material, typography = theme.type.material, content = content)
    }
}

/**
 * The inks a theme draws the opponent's faces in: one set of pictures per value, recoloured from
 * Notebook's by docs/design/illustrations/make_pictures.py with the inks of the theme's page
 * ("The faces" on theme-star-chart.md and theme-neon-arcade.md).
 */
enum class FaceInks { NOTEBOOK, STAR_CHART, NEON_ARCADE }
