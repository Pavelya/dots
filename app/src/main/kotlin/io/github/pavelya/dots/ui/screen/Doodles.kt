package io.github.pavelya.dots.ui.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import io.github.pavelya.dots.game.Mood

/**
 * The icons and doodles of the map, the game screen, the Level card and Result, with the same
 * outlines as the canvas (docs/design/canvas/draw.py: `ICONS`, `STAR`, the boss castle and the
 * computer's `face`) and the
 * wireframes (sound and undo), drawn in code until the hand-drawn icons of V-03 and the faces of
 * V-12 arrive.
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
    const val STAR = "M12 2.8l2.7 5.6 6.1.8-4.5 4.2 1.1 6.1L12 16.6l-5.4 2.9 1.1-6.1-4.5-4.2 6.1-.8z"
    const val CLOSE = "M6 6l12 12M18 6L6 18"
    const val FLAG = "M6 21.5V3 M6 4h11.5l-2.5 4.5 2.5 4.5H6"
    const val TICK = "M4.5 12.5l5 5L19.5 6.5"
    const val RETRY = "M20 12a8 8 0 1 1-2.3-5.7 M20 4v5h-5"
    const val NEXT = "M3 12h16M13 6l6 6-6 6"
    const val TARGET = "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M12 16.5a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9z M12 12.5v-1"
    const val MAP = "M3 6l6-2 6 2 6-2v14l-6 2-6-2-6 2z M9 4v14 M15 6v14"
    const val WALL = "M3 7.5h18v11H3z M3 13h18 M9 7.5v5.5 M15 13v5.5 M6 13v5.5 M18 7.5v5.5"
    const val HINT = "M9 18h6M10 21h4M12 3a6 6 0 0 0-4 10.5c.8.8 1 1.5 1 2.5h6c0-1 .2-1.7 1-2.5A6 6 0 0 0 12 3z M12 0.5v0 M3 4l1 1 M21 4l-1 1"
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

    /**
     * The computer as the canvas draws it: a round blot of [ink] with a [shadow] under it, and a
     * face in the [mood] drawn in [line] (the paper's colour). The eyes keep their own colours in
     * both themes. 100 x 100.
     */
    fun face(mood: Mood, ink: Color, shadow: Color, line: Color): ImageVector = vector(100f, 100f) {
        fill(ellipse(50f, 93f, 30f, 4f), shadow)
        fill(BLOB, ink)
        fill(ellipse(88f, 26f, 3.5f), ink)
        fill(ellipse(14f, 82f, 2.5f), ink)
        fill(ellipse(93f, 38f, 2f), ink)
        val (dx, dy, height) = when (mood) {
            Mood.HAPPY -> Triple(0f, 0f, 9f)
            Mood.SMUG -> Triple(3f, 1f, 9f)
            Mood.UPSET -> Triple(0f, 2f, 7f)
            Mood.DETERMINED -> Triple(-2f, 0f, 8f)
            Mood.SULKY -> Triple(-4f, 2f, 8f)
        }
        for (x in listOf(37f, 63f)) {
            fill(ellipse(x, 46f, 8f, height), EYE_WHITE)
            fill(ellipse(x + dx, 47f + dy, 3.6f), PUPIL)
            fill(ellipse(x + dx + 1.3f, 45.5f + dy, 1.1f), EYE_WHITE)
        }
        when (mood) {
            Mood.SMUG -> { // heavy lids
                fill("M28 44 Q37 36 46 44 Z M54 44 Q63 36 72 44 Z", ink)
                stroke("M28 44 H46 M54 44 H72", line, 2f)
            }
            Mood.SULKY -> fill("M28 42 Q37 38 46 42 L46 40 Q37 33 28 40Z M54 42 Q63 38 72 42 L72 40 Q63 33 54 40Z", ink)
            else -> Unit
        }
        val (brows, mouth) = when (mood) {
            Mood.HAPPY -> "M30 32 Q37 27 44 31 M56 31 Q63 27 70 32" to "M36 64 Q50 78 64 64"
            Mood.SMUG -> "M29 35 L44 35 M56 31 Q63 25 71 30" to "M39 67 Q52 72 63 61"
            Mood.UPSET -> "M30 33 L43 37 M70 33 L57 37" to "M38 73 Q50 63 62 73"
            Mood.DETERMINED -> "M30 32 L44 37 M70 32 L56 37" to "M40 68 H60"
            Mood.SULKY -> "M30 34 Q37 31 44 34 M56 34 Q63 31 70 34" to "M41 69 Q45 66 50 69 T59 69"
        }
        stroke("$brows $mouth", line, 2.6f)
        if (mood == Mood.UPSET) fill("M74 52 q4 7 0 9 q-4 -2 0 -9z", EYE_WHITE) // a tear
    }

    private const val BLOB = "M50 18 C68 16 84 30 85 50 C87 60 84 70 78 77 C80 84 78 90 74 90 C70 90 70 84 68 82 " +
        "C60 87 48 88 38 85 C26 82 16 72 15 57 C13 36 30 19 50 18 Z"
    private val EYE_WHITE = Color(0xFFFBF8F1)
    private val PUPIL = Color(0xFF2E2E2E)

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
