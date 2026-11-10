package io.github.pavelya.dots.ui.screen

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

/** The game screen's inks for the current colour scheme, light or dark. */
@Composable
internal fun rememberGameStyle(): GameStyle {
    val colors = MaterialTheme.colorScheme
    return remember(colors) { GameStyle.plain(colors, colors.background.luminance() < 0.5f) }
}

/**
 * The canvas's text sizes (docs/design/canvas/draw.py: `DISPLAY`, `TITLE`, `BODY`, `LABEL`,
 * `SMALL`) for the notes and cards. [display] is handwriting, in italics until the Notebook theme
 * brings Caveat (V-03).
 */
internal object NoteText {
    val display = TextStyle(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
    val title = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
    val body = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
    val label = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
    val small = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
}
