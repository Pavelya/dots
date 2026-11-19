package io.github.pavelya.dots.ui.theme

import io.github.pavelya.dots.audio.Effect

/**
 * The Star chart board (theme-star-chart.md, "The board" and "The pieces"): Notebook's sizes, with
 * gold stars and ice comets, a chart point at every crossing, straight rings of even width, and
 * hatching one line every 6 dp.
 */
private val StarChartBoard = NotebookBoard.copy(
    red = Piece(PieceShape.STAR, across = 1.4f),
    blue = Piece(PieceShape.COMET, across = 0.75f),
    ringBow = 0f,
    ringWobble = 0f,
    hatchGap = 6f,
    chartPoint = Measure(dp = 2f),
    capture = CaptureMotion.CONSTELLATION,
)

/**
 * A printed star chart (decision 0028): gold stars against ice-blue comets, constellation lines for
 * rings and a faint hatched haze in captured areas; nothing glows. Gold is Red's side and Ice is
 * Blue's. Every value comes from docs/design/theme-star-chart.md; the fonts are Notebook's.
 */
internal val StarChartTheme: GameTheme = GameTheme(
    name = "Star chart",
    light = inkPalette(
        dark = false,
        paper = 0xFFF3F5FA, card = 0xFFFFFFFF, grid = 0xFFD2DAEA,
        red = 0xFF9A5B00, blue = 0xFF0E5C85,
        redWash = 0xFFF1E7D9, blueWash = 0xFFDCE7F0,
        redPale = 0xFFE2CDAD, bluePale = 0xFFB5CDE0,
        highlighter = 0xFFB8326E, highlighterFaint = 0xFFE6B3C9,
        pencil = 0xFF1A2240, graphite = 0xFF525C78,
        // No margin on the game screen; the tape is Gold pale; errors are in Nova.
        margin = 0xFFD2DAEA, tape = 0xFFE2CDAD, error = 0xFFB8326E,
        chartPoint = 0xFF8F9CBF,
    ),
    dark = inkPalette(
        dark = true,
        paper = 0xFF0F1733, card = 0xFF1A2448, grid = 0xFF26315C,
        red = 0xFFF6CB6E, blue = 0xFF6DBAE6,
        redWash = 0xFF2C2A3F, blueWash = 0xFF152A4C,
        redPale = 0xFF6B5B45, bluePale = 0xFF2D5678,
        highlighter = 0xFFFF8DB5, highlighterFaint = 0xFF6A3559,
        pencil = 0xFFE6EAF7, graphite = 0xFF9CA8CA,
        margin = 0xFF26315C, tape = 0xFF6B5B45, error = 0xFFFF8DB5,
        chartPoint = 0xFF4A5A8E,
    ),
    board = StarChartBoard,
    highContrastBoard = StarChartBoard.highContrast(),
    type = NotebookType,
    // The board's sounds of sound.md, "Star chart"; the rest are Notebook's.
    sounds = Effect.ofTheme("star_chart"),
    faceInks = FaceInks.STAR_CHART,
)
