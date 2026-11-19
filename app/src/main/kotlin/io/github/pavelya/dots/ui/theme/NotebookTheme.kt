package io.github.pavelya.dots.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.pavelya.dots.R

/**
 * Caveat, the handwriting of big headings, in the two weights the theme uses. The font files are
 * fixed-weight copies of the Google Fonts variable font (docs/assets.md), so every Android version
 * draws the right weight.
 */
private val Caveat = FontFamily(
    Font(R.font.caveat_semibold, FontWeight.SemiBold),
    Font(R.font.caveat_bold, FontWeight.Bold),
)

/** Nunito, for everything else, in the three weights the theme uses. */
private val Nunito = FontFamily(
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

/** The Notebook board: theme-notebook.md, "The board", and pieces.md. The other themes start from it. */
internal val NotebookBoard = BoardLook(
    grid = Measure(dp = 1f),
    edge = Measure(dp = 1.5f),
    dot = Measure(ofSpacing = 0.5f, min = 9f, max = 20f),
    red = Piece(PieceShape.DOT),
    blue = Piece(PieceShape.DIAMOND, across = 7f / 6),
    capturedAlpha = 1f,
    ring = Measure(ofSpacing = 1f / 12, min = 1.5f, max = 3f),
    ringBow = 0.4f,
    ringWobble = 0.15f,
    hatch = Measure(dp = 1f),
    hatchGap = 5f,
    link = Measure(dp = 1.5f),
    threatRing = Measure(ofSpacing = 0.84f),
    threatNear = Measure(dp = 2f),
    threatFar = Measure(dp = 1.5f),
    closingPoint = Measure(ofSpacing = 1f / 3),
    closingLine = Measure(dp = 1.5f),
    previewRing = Measure(ofSpacing = 0.9f),
    previewLine = Measure(dp = 2f),
    pinhole = 0.3f,
    lastMoveRing = Measure(),
    lastMoveLine = Measure(),
    cut = 1f / 6,
    cutLength = 1.5f,
    halo = Measure(),
    previewOutline = Measure(),
    guide = Measure(dp = 1.5f),
    guideAlpha = 1f,
    guideDash = 4f,
    guideGap = 3f,
    guideCasing = Measure(),
)

/** Notebook's text styles (theme-notebook.md, "Type"); Star chart and Neon arcade keep them. */
internal val NotebookType = TypeScale(
    display = TextStyle(fontFamily = Caveat, fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold),
    headline = TextStyle(fontFamily = Caveat, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    title = TextStyle(fontFamily = Nunito, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    score = TextStyle(fontFamily = Nunito, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
    body = TextStyle(fontFamily = Nunito, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    label = TextStyle(fontFamily = Nunito, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    small = TextStyle(fontFamily = Nunito, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

/**
 * The launch look (decision 0012): red and blue ink on squared school paper, Caveat for big
 * headings and Nunito for everything else. Every value comes from docs/design/theme-notebook.md
 * and pieces.md; the canvas (docs/design/canvas) adds the tape.
 */
internal val NotebookTheme: GameTheme = GameTheme(
    name = "Notebook",
    light = inkPalette(
        dark = false,
        paper = 0xFFFBF8F1, card = 0xFFFFFFFF, grid = 0xFFD3DFEE,
        red = 0xFFC0392B, blue = 0xFF1F4E9C,
        redWash = 0xFFF6DCD7, blueWash = 0xFFDCE4F4,
        redPale = 0xFFECBEB7, bluePale = 0xFFBCCBEA,
        highlighter = 0xFFA86200, highlighterFaint = 0xFFE9C58F,
        pencil = 0xFF2E2E2E, graphite = 0xFF5E5A53, margin = 0xFFEBB2AA,
    ),
    dark = inkPalette(
        dark = true,
        paper = 0xFF1C1B19, card = 0xFF272522, grid = 0xFF34383E,
        red = 0xFFFF9384, blue = 0xFF6B90E8,
        redWash = 0xFF3A2421, blueWash = 0xFF1F2940,
        redPale = 0xFF5E3834, bluePale = 0xFF2F3F66,
        highlighter = 0xFFF0B04A, highlighterFaint = 0xFF6E5428,
        pencil = 0xFFEDE8DF, graphite = 0xFFA8A399, margin = 0xFF5A3531,
    ),
    board = NotebookBoard,
    highContrastBoard = NotebookBoard.highContrast(),
    type = NotebookType,
)

/**
 * [board] with high-contrast pieces (pieces.md, "High contrast"), as every theme draws them:
 * larger, each in a band of the paper or wash under it; the preview outlined in ink; drag's guides
 * wider, on a strip. The band takes the place of a glow round the pieces.
 */
internal fun BoardLook.highContrast(): BoardLook = copy(
    dot = Measure(ofSpacing = 0.6f, min = 9f, max = 24f),
    halo = Measure(dp = 1.5f),
    previewOutline = Measure(dp = 1.5f),
    guide = Measure(dp = 2f),
    guideCasing = Measure(dp = 1f),
    glow = Measure(),
)



/**
 * A theme's inks in one version, and the Material roles they fill, as Notebook fills them
 * (theme-notebook.md, "Interface colours"). The tape is Highlighter faint, as on the canvas, and
 * errors are in Red ink, unless the theme says otherwise; so are the colours Notebook doesn't have.
 */
internal fun inkPalette(
    dark: Boolean,
    paper: Long, card: Long, grid: Long,
    red: Long, blue: Long,
    redWash: Long, blueWash: Long,
    redPale: Long, bluePale: Long,
    highlighter: Long, highlighterFaint: Long,
    pencil: Long, graphite: Long, margin: Long,
    tape: Long = highlighterFaint,
    error: Long = red,
    chartPoint: Long = grid,
    redCore: Long = red,
    blueCore: Long = blue,
): Palette {
    val p = Color(paper)
    val c = Color(card)
    val material = if (dark) {
        darkColorScheme(
            primary = Color(blue), onPrimary = p, secondary = Color(red), onSecondary = p,
            background = p, onBackground = Color(pencil), surface = p, onSurface = Color(pencil),
            onSurfaceVariant = Color(graphite), outline = Color(graphite), outlineVariant = Color(grid),
            error = Color(error), onError = p,
            surfaceContainerLowest = c, surfaceContainerLow = c, surfaceContainer = c,
            surfaceContainerHigh = c, surfaceContainerHighest = c,
        )
    } else {
        lightColorScheme(
            primary = Color(blue), onPrimary = p, secondary = Color(red), onSecondary = p,
            background = p, onBackground = Color(pencil), surface = p, onSurface = Color(pencil),
            onSurfaceVariant = Color(graphite), outline = Color(graphite), outlineVariant = Color(grid),
            error = Color(error), onError = p,
            surfaceContainerLowest = c, surfaceContainerLow = c, surfaceContainer = c,
            surfaceContainerHigh = c, surfaceContainerHighest = c,
        )
    }
    return Palette(
        paper = p,
        card = c,
        grid = Color(grid),
        red = Color(red),
        blue = Color(blue),
        redWash = Color(redWash),
        blueWash = Color(blueWash),
        redPale = Color(redPale),
        bluePale = Color(bluePale),
        highlighter = Color(highlighter),
        highlighterFaint = Color(highlighterFaint),
        pencil = Color(pencil),
        graphite = Color(graphite),
        margin = Color(margin),
        tape = Color(tape),
        scrim = Color.Black.copy(alpha = if (dark) 0.55f else 0.4f),
        material = material,
        chartPoint = Color(chartPoint),
        redCore = Color(redCore),
        blueCore = Color(blueCore),
    )
}
