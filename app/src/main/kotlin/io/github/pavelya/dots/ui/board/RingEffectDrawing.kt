package io.github.pavelya.dots.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import dots.engine.Player
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** Lasso's cord, as a share of the theme's ring width (cosmetics.md, "Lasso"). */
internal const val LASSO_WIDTH = 1.6f

/** Lasso: about this many twists per grid step along each side. */
internal const val LASSO_TWISTS = 4

/** Lasso: each twist leans this many degrees from the cord's line. */
internal const val LASSO_SLANT = 55.0

/** Lasso: a twist's thickness, as a share of the ring's width. */
internal const val LASSO_STRIPE = 0.3f

/** Lasso: a twist reaches this share of the way across the cord. */
internal const val LASSO_REACH = 0.92f

/** Rubber stamp: dp between the ring's inner edge and its inner line. */
internal const val STAMP_GAP = 2.5f

/** Rubber stamp: the inner line is at least this many dp wide. */
internal const val STAMP_LINE_MIN = 1f

/**
 * Lasso's ring (cosmetics.md, "Lasso"): a cord [LASSO_WIDTH] times the ring's width in the captor's
 * ink, with round ends, crossed by slanted twists in its pale ink, a whole number of them evenly
 * along each side. It keeps the ring's path, bow and all, but not its change of width. In a theme
 * whose ring glows (Neon arcade) the cord sits in a glow as wide as the theme's, and has no core.
 * It is drawn from side [from], as far as [drawn] of the way round: thrown round the area, or
 * undrawn by Undo. [width] thickens it at the end of a game.
 */
internal fun DrawScope.drawRope(by: Player, ring: RingStroke, style: BoardStyle, from: Int = 0, drawn: Float = 1f, width: Float = 1f, layer: RingLayer) {
    if (drawn <= 0f || layer == RingLayer.CORE) return
    val cord = LASSO_WIDTH * ring.width * width
    val sides = ring.sides.indices.let { it.drop(from) + it.take(from) }
    val measure = PathMeasure()
    val lengths = sides.map { i -> measure.setPath(ring.sides[i].first, false); measure.length }
    // How much of each side the rope reaches, from 0 to 1.
    var left = lengths.sum() * drawn
    val reach = sides.indices.map { k ->
        val share = if (lengths[k] <= 0f) 1f else (left / lengths[k]).coerceIn(0f, 1f)
        left -= lengths[k]
        share
    }
    val glow = style.look.ringGlow.px(0f, density)
    if (layer == RingLayer.GLOW) {
        if (glow > 0f) along(ring, sides, reach, measure) { part -> drawPath(part, style.palette.pale(by), style = Stroke(cord + 2 * glow, cap = StrokeCap.Round)) }
        return
    }
    along(ring, sides, reach, measure) { part -> drawPath(part, style.color(by), style = Stroke(cord, cap = StrokeCap.Round)) }
    val pale = style.palette.pale(by)
    val stripe = LASSO_STRIPE * ring.width * width
    val slant = Math.toRadians(LASSO_SLANT)
    val k = cord / 2 * LASSO_REACH / sin(slant).toFloat()
    for ((n, i) in sides.withIndex()) {
        if (reach[n] <= 0f) break
        val (a, c, b) = ring.curves[i]
        // As many twists as the side has at its place, so pulling tight never adds one.
        val count = max(1, (hypot(b.x - a.x, b.y - a.y) / ring.scale / (ring.spacing / LASSO_TWISTS)).roundToInt())
        for (j in 0 until count) {
            val s = (j + 0.5f) / count
            if (s > reach[n]) break
            val at = quadratic(a, c, b, s)
            val dx = 2 * (1 - s) * (c.x - a.x) + 2 * s * (b.x - c.x)
            val dy = 2 * (1 - s) * (c.y - a.y) + 2 * s * (b.y - c.y)
            val norm = hypot(dx, dy)
            if (norm <= 0f) continue
            val ux = dx / norm
            val uy = dy / norm
            val vx = ux * cos(slant).toFloat() - uy * sin(slant).toFloat()
            val vy = ux * sin(slant).toFloat() + uy * cos(slant).toFloat()
            drawLine(pale, Offset(at.x - vx * k, at.y - vy * k), Offset(at.x + vx * k, at.y + vy * k), stripe)
        }
    }
}

/** Calls [draw] with each side of [ring] in the order [sides], cut where the rope has [reach]ed. */
private inline fun along(ring: RingStroke, sides: List<Int>, reach: List<Float>, measure: PathMeasure, draw: (Path) -> Unit) {
    for ((n, i) in sides.withIndex()) {
        val share = reach[n]
        if (share <= 0f) break
        val side = ring.sides[i].first
        if (share >= 1f) {
            draw(side)
        } else {
            measure.setPath(side, false)
            draw(Path().also { measure.getSegment(0f, measure.length * share, it, true) })
        }
    }
}

private fun quadratic(a: Offset, c: Offset, b: Offset, s: Float): Offset {
    val u = 1 - s
    return Offset(u * u * a.x + 2 * u * s * c.x + s * s * b.x, u * u * a.y + 2 * u * s * c.y + s * s * b.y)
}

/**
 * Rubber stamp's ring (cosmetics.md, "Rubber stamp"): the theme's own ring, and inside it a second
 * line, half the ring's width and at least [STAMP_LINE_MIN] dp, in the captor's ink, [STAMP_GAP] dp
 * inside the ring's inner edge, following its path with round corners. In Neon arcade the ring
 * keeps its glow and core; the inner line has neither, and clears the glow. [pale] blends both
 * towards the paper as the stamp comes down; as Undo undraws the ring ([drawn] below 1), the inner
 * line fades with it.
 */
internal fun DrawScope.drawStamp(
    by: Player,
    ring: RingStroke,
    style: BoardStyle,
    from: Int = 0,
    drawn: Float = 1f,
    width: Float = 1f,
    layer: RingLayer,
    pale: Float = 0f,
) {
    drawAreaRing(by, ring, style, from, drawn, width, layer, pale)
    if (layer != RingLayer.TUBE) return
    val gone = max(pale, 1f - drawn)
    if (gone >= 1f) return
    drawInnerLine(ring, fade(style.color(by), style.paper, gone))
}

/**
 * The band inside [ring]'s outline from (g − w/2) to (g + w/2) of its middle: a stroke 2g + w wide
 * with a stroke 2g − w wide cleared from it, kept inside the outline.
 */
private fun DrawScope.drawInnerLine(ring: RingStroke, ink: Color) {
    val line = max(STAMP_LINE_MIN.dp.toPx(), ring.width / 2)
    val g = ring.width / 2 + STAMP_GAP.dp.toPx() + line / 2
    val box = ring.outline.getBounds().inflate(g + line)
    drawIntoCanvas { canvas ->
        canvas.saveLayer(box, Paint())
        clipPath(ring.outline) {
            drawPath(ring.outline, ink, style = Stroke(2 * g + line, join = StrokeJoin.Round))
            drawPath(ring.outline, Color.Black, style = Stroke(2 * g - line, join = StrokeJoin.Round), blendMode = BlendMode.Clear)
        }
        canvas.restore()
    }
}
