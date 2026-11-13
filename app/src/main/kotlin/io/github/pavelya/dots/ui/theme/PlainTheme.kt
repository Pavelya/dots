package io.github.pavelya.dots.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The POC's look: Material's own colours, bright inks, pale areas and faded captured dots, sizes
 * in proportion to the spacing, round pieces for both sides, the phone's own font.
 */
internal val PlainTheme: GameTheme = GameTheme(
    name = "Plain",
    light = plainPalette(
        lightColorScheme(),
        red = Color(0xFFD63031),
        blue = Color(0xFF0984E3),
        highlighter = Color(0xFFE08A00),
        scrim = Color.Black.copy(alpha = 0.4f),
    ),
    dark = plainPalette(
        darkColorScheme(),
        red = Color(0xFFFF6B6B),
        blue = Color(0xFF5DADEC),
        highlighter = Color(0xFFFFC14D),
        scrim = Color.Black.copy(alpha = 0.55f),
    ),
    board = BoardLook(
        grid = Measure(ofSpacing = 0.03f),
        edge = Measure(ofSpacing = 0.03f),
        dot = Measure(ofSpacing = 0.56f),
        red = Piece(PieceShape.DOT),
        blue = Piece(PieceShape.DOT),
        capturedAlpha = 0.35f,
        link = Measure(ofSpacing = 0.08f),
        threatRing = Measure(ofSpacing = 0.84f),
        threatNear = Measure(ofSpacing = 0.09f),
        threatFar = Measure(ofSpacing = 0.06f),
        closingPoint = Measure(ofSpacing = 0.32f),
        closingLine = Measure(ofSpacing = 0.06f),
        previewRing = Measure(ofSpacing = 0.9f),
        previewLine = Measure(ofSpacing = 0.08f),
        lastMoveRing = Measure(ofSpacing = 0.84f),
        lastMoveLine = Measure(ofSpacing = 0.06f),
        guide = Measure(ofSpacing = 0.08f, min = 2f),
        guideAlpha = 0.6f,
    ),
    type = TypeScale(
        // Handwriting is italics in the plain look.
        display = TextStyle(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
        headline = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic),
        title = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
        score = TextStyle(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
        body = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
        label = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
        small = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    ),
)

/** The plain inks: Material's roles for paper, card, grid and text; washes and pales are the inks, see-through. */
private fun plainPalette(colors: ColorScheme, red: Color, blue: Color, highlighter: Color, scrim: Color) = Palette(
    paper = colors.background,
    card = colors.surfaceContainerLowest,
    grid = colors.outlineVariant,
    red = red,
    blue = blue,
    redWash = red.copy(alpha = 0.14f),
    blueWash = blue.copy(alpha = 0.14f),
    redPale = red.copy(alpha = 0.45f),
    bluePale = blue.copy(alpha = 0.45f),
    highlighter = highlighter,
    highlighterFaint = highlighter.copy(alpha = 0.45f),
    pencil = colors.onSurface,
    graphite = colors.onSurfaceVariant,
    margin = red.copy(alpha = 0.35f),
    tape = colors.tertiaryContainer,
    scrim = scrim,
    material = colors,
)
