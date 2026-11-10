package io.github.pavelya.dots.ui.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The map's icons and doodles, with the same outlines as the canvas (docs/design/canvas/draw.py,
 * `ICONS`, `STAR` and the boss castle), drawn in code until the hand-drawn icons of V-03 arrive.
 */
object Doodles {
    const val SHOP = "M5 8h14l-1.5 12h-11z M9 8a3 3 0 0 1 6 0"
    const val STATS = "M5 20V12M10 20V6M15 20v-9M20 20V9M3 20h18"
    const val HELP = "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M9.6 9.3a2.5 2.5 0 1 1 3.5 2.3c-.7.3-1.1.9-1.1 1.6M12 16.6h.01"
    const val SETTINGS = "M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4z M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3" +
        "M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M5.3 18.7l2.1-2.1M16.6 7.4l2.1-2.1"
    const val CALENDAR = "M3.5 5h17v15h-17z M3.5 10h17M8 3v4M16 3v4"
    const val STAR = "M12 2.8l2.7 5.6 6.1.8-4.5 4.2 1.1 6.1L12 16.6l-5.4 2.9 1.1-6.1-4.5-4.2 6.1-.8z"

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

    /** The arrow from "next!" to the next level, 34 x 20, pointing left. */
    fun arrow(ink: Color): ImageVector = vector(34f, 20f) {
        stroke("M32 12 q-14 6 -28 -2 M4 10 l7 -6 M4 10 l8 3", ink, 2f)
    }

    /** The boss castle, 68 x 76, in [ink] on [paper], with a [flag]. */
    fun castle(ink: Color, paper: Color, flag: Color): ImageVector = vector(68f, 76f) {
        fill("M8 70 V34 h9 v-7 h7 v7 h7 v-7 h7 v7 h7 v-7 h7 v7 h9 V70 z", paper, stroke = 2f, strokeInk = ink)
        stroke("M27 70 v-13 a7 7 0 0 1 14 0 v13 M34 27 V8", ink, 2f)
        fill("M34 8 l13 5 -13 5 z", flag)
        stroke("M2 70 h64", ink, 2f)
    }

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
