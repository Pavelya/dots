package io.github.pavelya.dots.ui.board

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import dots.engine.Player

/**
 * Everything about how the board looks. The POC uses [plain]; the designed themes of the full
 * plan become further styles, without touching the drawing code's callers.
 */
data class BoardStyle(
    val background: Color,
    val grid: Color,
    val red: Color,
    val blue: Color,
    val danger: Color,
    /** The outline of the tutorial's pointing hand; the hand is filled with [background]. */
    val pencil: Color,
    val dotRadius: Float = 0.28f,
    val capturedAlpha: Float = 0.35f,
    val areaAlpha: Float = 0.14f,
) {
    fun color(player: Player): Color = if (player == Player.RED) red else blue

    companion object {
        fun plain(colors: ColorScheme, dark: Boolean): BoardStyle = BoardStyle(
            background = colors.surface,
            grid = colors.outlineVariant,
            red = if (dark) Color(0xFFFF6B6B) else Color(0xFFD63031),
            blue = if (dark) Color(0xFF5DADEC) else Color(0xFF0984E3),
            danger = if (dark) Color(0xFFFFC14D) else Color(0xFFE08A00),
            pencil = colors.onSurface,
        )
    }
}
