package io.github.pavelya.dots.ui.theme

import io.github.pavelya.dots.audio.Effect

/**
 * The Neon arcade board (theme-neon-arcade.md, "The board" and "The pieces"): Notebook's sizes,
 * with magenta discs and cyan triangles, each live piece in a 1.5 dp band of glow, rings as straight
 * tubes with a glow to either side and a bright core, and hatching one line every 6 dp.
 */
private val NeonArcadeBoard = NotebookBoard.copy(
    red = Piece(PieceShape.DOT),
    blue = Piece(PieceShape.TRIANGLE, across = 2 * 0.66f),
    ringBow = 0f,
    ringWobble = 0f,
    hatchGap = 6f,
    glow = Measure(dp = 1.5f),
    ringGlow = Measure(dp = 1.5f),
    ringCore = 1f / 3,
    capture = CaptureMotion.NEON,
)

/**
 * Neon light drawn in solid colours (decision 0028): magenta discs against cyan triangles, tubes for
 * rings, and a band of glow round every live piece and tube; a neon sign by day in the light
 * version. Magenta is Red's side and Cyan is Blue's. Every value comes from
 * docs/design/theme-neon-arcade.md; the fonts are Notebook's.
 */
internal val NeonArcadeTheme: GameTheme = GameTheme(
    name = "Neon arcade",
    light = inkPalette(
        dark = false,
        paper = 0xFFF6F4FB, card = 0xFFFFFFFF, grid = 0xFFDCD6EB,
        red = 0xFFC8106E, blue = 0xFF00778E,
        redWash = 0xFFFADDEB, blueWash = 0xFFD8F1F5,
        redPale = 0xFFF5B9D7, bluePale = 0xFFA3DDE7,
        highlighter = 0xFF9C5E00, highlighterFaint = 0xFFEBCB8E,
        pencil = 0xFF1B1730, graphite = 0xFF5A5574,
        // No margin on the game screen; the tape is Magenta glow; errors stay in Magenta ink.
        margin = 0xFFDCD6EB, tape = 0xFFF5B9D7,
        redCore = 0xFFF77DB7, blueCore = 0xFF5FC9DB,
    ),
    dark = inkPalette(
        dark = true,
        paper = 0xFF0A0A12, card = 0xFF1A1730, grid = 0xFF2A2546,
        red = 0xFFFF3D8B, blue = 0xFF26E5FF,
        redWash = 0xFF3A1630, blueWash = 0xFF0D3440,
        redPale = 0xFF6A1D4A, bluePale = 0xFF0A4450,
        highlighter = 0xFFF5F23A, highlighterFaint = 0xFF5C5A1E,
        pencil = 0xFFF3F3FF, graphite = 0xFFA8A4CA,
        margin = 0xFF2A2546, tape = 0xFF6A1D4A,
        redCore = 0xFFFFC2DA, blueCore = 0xFFC4F8FF,
    ),
    board = NeonArcadeBoard,
    highContrastBoard = NeonArcadeBoard.highContrast(),
    type = NotebookType,
    // The board's sounds of sound.md, "Neon arcade"; the rest are Notebook's.
    sounds = Effect.ofTheme("neon_arcade"),
    faceInks = FaceInks.NEON_ARCADE,
)
