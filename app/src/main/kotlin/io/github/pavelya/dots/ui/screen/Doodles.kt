package io.github.pavelya.dots.ui.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The icons and doodles of the map, the game screen, the Level card and Result, with the same
 * outlines as the canvas (docs/design/canvas/draw.py: `ICONS` and `STAR`) and the wireframes (sound
 * and undo), drawn in code. The opponent's faces are pictures ([Faces]).
 */
object Doodles {
    const val PAUSE = "M12 2.5 q9.5 .3 9.5 9.5 q-.3 9.5 -9.5 9.5 q-9.5 -.3 -9.5 -9.5 q.3 -9.3 9.5 -9.5 M9.5 8 q-.3 4 0 8 M14.5 8 q.3 4 0 8"
    const val SOUND_ON = "M4 9.5h4l5-4v13l-5-4H4z M16.5 9a4 4 0 0 1 0 6 M19 6.5a7.5 7.5 0 0 1 0 11"
    const val SOUND_OFF = "M4 9.5h4l5-4v13l-5-4H4z M16.5 9.5l5 5 M21.5 9.5l-5 5"
    const val UNDO = "M9 13.5 L4.5 9 L9 4.5 M4.5 9 h10 a5.5 5.5 0 0 1 0 11 h-4"
    const val SHOP = "M5 8h14l-1.5 12h-11z M9 8a3 3 0 0 1 6 0"
    const val STATS = "M5 20V12M10 20V6M15 20v-9M20 20V9M3 20h18"
    const val HELP = "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M9.6 9.3a2.5 2.5 0 1 1 3.5 2.3c-.7.3-1.1.9-1.1 1.6M12 16.6h.01"
    const val SETTINGS = "M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4z M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3" +
        "M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M5.3 18.7l2.1-2.1M16.6 7.4l2.1-2.1"
    const val CALENDAR = "M3.5 5h17v15h-17z M3.5 10h17M8 3v4M16 3v4"

    /** The streak's flame, as the wireframes' Today's puzzle draws it. */
    const val FLAME = "M12 22c4 0 7-3 7-7 0-4-3-6-4-10-2 2-2 4-2 5-1-1-3-3-3-6-3 3-5 6-5 10 0 5 3 8 7 8z"
    const val STAR = "M12 2.8l2.7 5.6 6.1.8-4.5 4.2 1.1 6.1L12 16.6l-5.4 2.9 1.1-6.1-4.5-4.2 6.1-.8z"
    const val CLOSE = "M6 6l12 12M18 6L6 18"
    const val BACK = "M15 5l-7 7 7 7"
    const val CHEVRON = "M9 5l7 7-7 7"
    const val FLAG = "M6 21.5V3 M6 4h11.5l-2.5 4.5 2.5 4.5H6"
    const val TICK = "M4.5 12.5l5 5L19.5 6.5"
    const val RETRY = "M20 12a8 8 0 1 1-2.3-5.7 M20 4v5h-5"
    const val NEXT = "M3 12h16M13 6l6 6-6 6"
    const val TARGET = "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M12 16.5a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9z M12 12.5v-1"
    const val MAP = "M3 6l6-2 6 2 6-2v14l-6 2-6-2-6 2z M9 4v14 M15 6v14"
    const val WALL = "M3 7.5h18v11H3z M3 13h18 M9 7.5v5.5 M15 13v5.5 M6 13v5.5 M18 7.5v5.5"
    const val HINT = "M9 18h6M10 21h4M12 3a6 6 0 0 0-4 10.5c.8.8 1 1.5 1 2.5h6c0-1 .2-1.7 1-2.5A6 6 0 0 0 12 3z M12 0.5v0 M3 4l1 1 M21 4l-1 1"
    /** The Undo booster (D-14, draw.py `ICONS["undo"]`): a pen stroke from a dot, looping back to an arrowhead; see [undoBooster]. */
    const val UNDO_BOOSTER = "M10.6 19.6 q2.4 -.2 4.5 -.1 q5.3 .1 5.4 -5.5 q-.1 -5.4 -5.5 -5.5 q-4.7 .1 -9.4 0 " +
        "M8.9 4.7 q-2 2 -3.7 3.8 q1.7 1.8 3.8 3.8"
    const val BOMB = "M11 21a7 7 0 1 0 0-14 7 7 0 0 0 0 14z M15 8l3-3 M18 5l1.5-1.5 M19.5 7.5h2 M17 2.5V1 M8 12.5a3 3 0 0 1 3-2.5"

    /** A 24 x 24 line icon in one [ink]. */
    fun icon(path: String, ink: Color, width: Float = 1.8f): ImageVector = vector(24f, 24f) {
        stroke(path, ink, width)
    }

    /** A star: [ink] filled, or only its outline in [ink]. */
    fun star(ink: Color, filled: Boolean): ImageVector = vector(24f, 24f) {
        if (filled) fill(STAR, ink, stroke = 1.6f) else stroke(STAR, ink, 1.6f)
    }

    /** Two people, one in each player's ink: Play a game. */
    fun people(red: Color, blue: Color): ImageVector = vector(28f, 22f) {
        stroke("M5 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0 M2 21 q7 -10 14 0", red, 1.8f)
        stroke("M15 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0 M12 21 q7 -10 14 0", blue, 1.8f)
    }

    /** A plain smiling face in outline, 48 x 48: the computer on Play a game's card. */
    fun smile(ink: Color): ImageVector = vector(48f, 48f) {
        stroke("M7 24a17 17 0 1 0 34 0a17 17 0 1 0 -34 0 M17 30 q7 5 14 0", ink, 2f)
        fill(ellipse(18f, 22f, 1.8f) + " " + ellipse(30f, 22f, 1.8f), ink)
    }

    /** A play triangle, 16 x 16, on Play a game's cards. */
    fun play(ink: Color): ImageVector = vector(16f, 16f) {
        fill("M4 2.5 L13 8 L4 13.5 Z", ink)
    }

    /** The double dot booster: a dot and a diamond side by side, 30 x 16 (the canvas's `toolbar`). */
    fun doubleDot(ink: Color): ImageVector = vector(30f, 16f) {
        fill(ellipse(7f, 8f, 6f), ink)
        fill("M23 1 L30 8 L23 15 L16 8 Z", ink)
    }

    /** The Undo booster's icon: [UNDO_BOOSTER] and the dot it starts from. Free play's Undo is the plain [UNDO] arrow. */
    fun undoBooster(ink: Color, width: Float = 1.8f): ImageVector = vector(24f, 24f) {
        stroke(UNDO_BOOSTER, ink, width)
        fill(ellipse(7f, 19.6f, 2.4f), ink)
    }

    /** The arrow from "next!" to the next level, 34 x 20, pointing left. */
    fun arrow(ink: Color): ImageVector = vector(34f, 20f) {
        stroke("M32 12 q-14 6 -28 -2 M4 10 l7 -6 M4 10 l8 3", ink, 2f)
    }

    /** An ellipse around ([x], [y]) as path data; a circle when [ry] is left out. */
    private fun ellipse(x: Float, y: Float, rx: Float, ry: Float = rx): String =
        "M${x - rx} $y a$rx $ry 0 1 0 ${2 * rx} 0 a$rx $ry 0 1 0 ${-2 * rx} 0 z"

    private class Builder(val builder: ImageVector.Builder) {
        fun stroke(path: String, ink: Color, width: Float) {
            builder.addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(ink),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }

        fun fill(path: String, ink: Color, stroke: Float = 0f, strokeInk: Color = ink) {
            builder.addPath(
                pathData = addPathNodes(path),
                fill = SolidColor(ink),
                stroke = if (stroke > 0f) SolidColor(strokeInk) else null,
                strokeLineWidth = stroke,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }

    private fun vector(width: Float, height: Float, draw: Builder.() -> Unit): ImageVector {
        val builder = ImageVector.Builder(defaultWidth = width.dp, defaultHeight = height.dp, viewportWidth = width, viewportHeight = height)
        Builder(builder).draw()
        return builder.build()
    }
}
