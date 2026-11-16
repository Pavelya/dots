package io.github.pavelya.dots.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dots.ai.Danger
import dots.engine.Area
import dots.engine.Capture
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import dots.engine.areas
import io.github.pavelya.dots.game.BoardMoment
import io.github.pavelya.dots.game.CapturedArea
import io.github.pavelya.dots.ui.motion.Announce
import io.github.pavelya.dots.ui.motion.Capturing
import io.github.pavelya.dots.ui.motion.EndOfGame
import io.github.pavelya.dots.ui.motion.Placing
import io.github.pavelya.dots.ui.motion.Threat
import io.github.pavelya.dots.ui.theme.BoardLook
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Draws a position: grid, captured areas (washed and hatched in the captor's colours, inside a ring
 * drawn like a pen stroke), live and captured pieces (a cut across each captured one), the last
 * move's pinhole, and a placement preview. With high-contrast pieces ([BoardLook.halo]) each piece
 * sits in a band of the paper or wash under it. It
 * decides nothing about the game; [PlayableBoard] adds touch. [showLinks] draws the links between
 * neighbouring dots of one colour, which makes chains visible; they are off unless asked for (the
 * player's setting, the tutorial's linking level, How to play), and the board tells tests whether
 * they show ([LinksShown]). [dangers] marks groups close to
 * capture (the threat highlight). [marked] is the dot a level's mission is about (rescue or
 * target), circled twice in the player's ink. [guides] draws the preview's row and column across
 * the board, around a finger that hides the point itself (placing by drag). [pointer] is the point
 * the tutorial asks the player to tap: circled in the player's ink, with a hand pointing at it (the
 * wireframes' "Tutorial level"). Blocked points, a wall booster's among them, carry the wall's mark;
 * [marks] adds the booster preview and the Hint's circle (A-05).
 *
 * Motion (docs/design/motion.md): the [moment] of the dot that has just landed plays as [time]
 * runs from 0 ms: the dot grows and settles, its links grow, the pinhole moves, an announced dot
 * spreads two ripples, a capture draws its ring, spreads its wash and cuts its dots, and new
 * threats draw in and pulse. [edgeMarks] marks a dot's row and column on the board's top and left
 * edges. Drag's guides reach out as far as [guideReach] says. At the end of a game the captured
 * areas light up in turn ([glow]). Time is read while drawing, so only drawing follows it.
 */
@Composable
fun BoardView(
    state: GameState,
    style: BoardStyle,
    description: String,
    modifier: Modifier = Modifier,
    lastMove: Pos? = null,
    preview: Pos? = null,
    showLinks: Boolean = false,
    dangers: List<Danger> = emptyList(),
    marked: Pos? = null,
    guides: Boolean = false,
    pointer: Pos? = null,
    marks: BoardMarks = BoardMarks.NONE,
    moment: BoardMoment? = null,
    time: () -> Float = { Float.MAX_VALUE },
    edgeMarks: Pos? = null,
    guideReach: () -> Float = { 1f },
    glow: AreaGlow? = null,
) {
    val areas = remember(state) { state.areas() }
    val regions = remember(areas) { areas.map { it.region.toHashSet() } }
    Canvas(
        modifier = modifier
            .aspectRatio(state.width.toFloat() / state.height)
            .semantics {
                contentDescription = description
                linksShown = showLinks
            },
    ) {
        val geometry = BoardGeometry(state.width, state.height, size.width, size.height)
        val guided = if (guides) preview ?: marks.point else null
        val frame = Frame.of(moment, if (moment == null) Float.MAX_VALUE else time(), areas, regions, edgeMarks, glow)
        drawPosition(state, areas, geometry, style, lastMove, preview, showLinks, dangers, marked, marks, guided, frame, if (guided != null) guideReach() else 1f)
        pointer?.let { drawPointer(geometry.center(it), geometry, style) }
    }
}

/**
 * The end of a game (motion.md, "End of game"): the captured areas, in the [order] they were
 * captured, light up in turn as [time] runs, at the times of [ending].
 */
class AreaGlow(val order: List<CapturedArea>, val ending: EndOfGame, val time: () -> Float)

/** One area being captured in this frame: by [capture], its ring drawn from the closing dot ([start] on the ring). */
private class AreaCapture(val capture: Capture, val start: Int)

/**
 * Where the moment stands in the frame being drawn, at [t] ms: everything [drawPosition] animates.
 * Without a moment every value is at its end, and the board is drawn as it stands.
 */
private class Frame(
    val moment: BoardMoment?,
    val t: Float,
    /** The captures of the moment's areas, by area index. */
    val capturing: Map<Int, AreaCapture>,
    val timing: Capturing?,
    /** The cut drawn across each dot the moment captured, from 0 to 1; until it starts the dot keeps its full ink. */
    val cuts: Map<Pos, Float>,
    val edgeMarks: Pos?,
    /** How much each area is lit at the end of the game, by area index. */
    val glow: Map<Int, Float>,
) {
    val newDot: Pos? get() = moment?.dot?.takeIf { t < Placing.SETTLED }

    companion object {
        fun of(moment: BoardMoment?, t: Float, areas: List<Area>, regions: List<Set<Pos>>, edgeMarks: Pos?, glow: AreaGlow?): Frame {
            val captures = moment?.captures.orEmpty()
            val timing = if (captures.isEmpty() || moment == null) null else Capturing.of(moment.captured)
            val capturing = HashMap<Int, AreaCapture>()
            val cuts = HashMap<Pos, Float>()
            if (timing != null && t < timing.length) {
                for (capture in captures) {
                    val inside = capture.dots.firstOrNull() ?: capture.region.firstOrNull() ?: continue
                    val index = regions.indexOfFirst { inside in it }
                    if (index >= 0 && index !in capturing) capturing[index] = AreaCapture(capture, ringIndex(areas[index].ring, capture.closing))
                    val nearest = capture.dots.sortedBy { hypot((it.x - capture.closing.x).toFloat(), (it.y - capture.closing.y).toFloat()) }
                    nearest.forEachIndexed { i, dot -> cuts[dot] = timing.cut(t, i) }
                }
            }
            val lit = HashMap<Int, Float>()
            if (glow != null) {
                val now = glow.time()
                glow.order.forEachIndexed { i, area ->
                    val index = regions.indexOfFirst { area.first in it }
                    if (index >= 0) lit[index] = glow.ending.glow(now, i)
                }
            }
            return Frame(moment, t, capturing, timing, cuts, edgeMarks, lit)
        }

        /** Where [ring] reaches the closing dot, or the ring's dot nearest to it. */
        private fun ringIndex(ring: List<Pos>, closing: Pos): Int =
            ring.indexOf(closing).takeIf { it >= 0 } ?: ring.indices.minBy { max(abs(ring[it].x - closing.x), abs(ring[it].y - closing.y)) }
    }
}

/** Whether the board draws the links between touching dots of one side; for tests, TalkBack does not read it. */
val LinksShown = SemanticsPropertyKey<Boolean>("LinksShown")
private var SemanticsPropertyReceiver.linksShown by LinksShown

/**
 * What a booster adds to the board (A-05). In the booster preview: the [targets] it may be aimed
 * at, the [point] chosen (drag's guides follow it), a [wall] about to go up, the bomb's [plus] and the pieces it would take away ([gone]: the
 * bomb's hits, or the turn Undo takes back), pale in dashed rings. After a Hint: its [hint] point,
 * circled twice in the player's ink.
 */
data class BoardMarks(
    val targets: List<Pos> = emptyList(),
    val point: Pos? = null,
    val wall: Pos? = null,
    val plus: List<Pos> = emptyList(),
    val gone: List<Pos> = emptyList(),
    val hint: Pos? = null,
) {
    companion object {
        val NONE = BoardMarks()
    }
}

/**
 * The tutorial's point to tap: a ring in the player's ink, and a hand, 48 x 56 dp, whose fingertip
 * touches the point from below and to the right, so it hides little of the board around it.
 */
private fun DrawScope.drawPointer(at: Offset, geometry: BoardGeometry, style: BoardStyle) {
    drawRing(at, geometry.spacing * 0.45f, style.color(Player.RED), max(geometry.spacing * 0.05f, 1.5.dp.toPx()))
    val unit = 1.dp.toPx()
    translate(at.x - 6 * unit, at.y + 4 * unit) {
        scale(unit, unit, pivot = Offset.Zero) {
            drawPath(HAND, style.paper)
            drawPath(HAND, style.pencil, style = Stroke(width = 1.6f, join = StrokeJoin.Round))
        }
    }
}

/** A hand pointing up with its first finger, in a 48 x 56 box (the wireframe's tutorial hand). */
private val HAND: Path = Path().apply {
    moveTo(14f, 4f)
    relativeQuadraticTo(4f, -3f, 7f, 0f)
    relativeLineTo(0f, 22f)
    relativeLineTo(2f, -10f)
    relativeQuadraticTo(4f, -3f, 7f, 0f)
    relativeLineTo(0f, 10f)
    relativeLineTo(2f, -6f)
    relativeQuadraticTo(4f, -3f, 7f, 0f)
    relativeLineTo(0f, 8f)
    relativeLineTo(1f, -3f)
    relativeQuadraticTo(4f, -2f, 6f, 1f)
    relativeLineTo(0f, 14f)
    relativeQuadraticTo(0f, 14f, -14f, 14f)
    relativeLineTo(-6f, 0f)
    relativeQuadraticTo(-8f, 0f, -13f, -8f)
    relativeLineTo(-9f, -14f)
    relativeQuadraticTo(-1f, -5f, 4f, -5f)
    relativeLineTo(6f, 6f)
    close()
}

/**
 * Every layer of the position, in the theme's drawing order (theme-notebook.md, "The board"):
 * paper, washes, hatching, grid, links, drag's guides along [guided]'s row and column, the
 * high-contrast bands round the pieces, rings, threat rings, pieces and their cuts, the last
 * move, closing points, the preview. The booster marks lie with the pieces they are about, and
 * the circled points on top.
 */
private fun DrawScope.drawPosition(
    state: GameState,
    areas: List<Area>,
    geometry: BoardGeometry,
    style: BoardStyle,
    lastMove: Pos?,
    preview: Pos?,
    showLinks: Boolean,
    dangers: List<Danger>,
    marked: Pos?,
    marks: BoardMarks,
    guided: Pos?,
    frame: Frame,
    guideReach: Float,
) {
    drawRect(style.paper)
    val strokes = areas.map { ringStroke(it, geometry, style.look) }
    areas.forEachIndexed { i, area ->
        val capturing = frame.capturing[i]
        val lit = frame.glow[i] ?: 0f
        val wash = if (lit > 0f) lerp(style.palette.wash(area.by), style.palette.pale(area.by), lit) else style.palette.wash(area.by)
        if (capturing == null) {
            drawArea(area.by, strokes[i], style, wash)
        } else {
            drawSpreadingArea(area, strokes[i], capturing.capture, frame, geometry, style, wash)
        }
    }
    drawGrid(state, geometry, style)
    drawWalls(state, geometry, style)
    drawTargets(state, marks.targets, geometry, style)
    marks.wall?.let { drawWall(geometry.center(it), geometry, style, preview = true) }
    if (showLinks) drawLinks(state, geometry, style, frame)
    guided?.let { drawGuides(state, it, geometry, style, areas, strokes, guideReach) }
    drawHalos(state, geometry, style, areas, strokes, frame)
    areas.forEachIndexed { i, area ->
        val width = 1f + 0.9f * (frame.glow[i] ?: 0f)
        val capturing = frame.capturing[i]
        val drawn = if (capturing == null) 1f else frame.timing!!.ring(frame.t)
        drawAreaRing(area.by, strokes[i], style, from = capturing?.start ?: 0, drawn = drawn, width = width)
    }
    drawThreatRings(dangers, geometry, style, frame)
    drawRipples(geometry, style, frame)
    drawDots(state, geometry, style, frame)
    drawGone(state, marks.gone, geometry, style)
    drawPlus(state, marks.plus, geometry, style)
    val look = style.look
    val spacing = geometry.spacing
    val moment = frame.moment
    val pinhole = if (moment != null && frame.t < Placing.PINHOLE) moment.pinholeBefore?.takeIf { state.ownerAt(it) != null } else lastMove
    pinhole?.let { drawLastMove(geometry.center(it), geometry, style) }
    drawClosingPoints(state, dangers, geometry, style, frame)
    drawBurst(geometry, style, frame)
    for (circled in listOfNotNull(marked, marks.hint)) {
        val ink = style.color(Player.RED)
        drawRing(geometry.center(circled), geometry.spacing * 0.42f, ink, geometry.spacing * 0.07f)
        drawRing(geometry.center(circled), geometry.spacing * 0.58f, ink, geometry.spacing * 0.05f)
    }
    preview?.let {
        val center = geometry.center(it)
        val piece = style.piece(state.toMove)
        val dot = look.dot.px(spacing, density)
        drawPiece(piece, center, dot, style.palette.pale(state.toMove))
        val outline = look.previewOutline.px(spacing, density)
        if (outline > 0f) drawPiece(piece, center, dot, style.color(state.toMove), outline)
        drawRing(center, look.previewRing.px(spacing, density) / 2, style.color(state.toMove), look.previewLine.px(spacing, density))
    }
    drawEdgeMarks(state, geometry, style, frame)
}

/**
 * A captured area while its capture plays (motion.md, "Capture"): the wash and the hatching spread
 * from the closing dot as a growing circle, kept inside the ring.
 */
private fun DrawScope.drawSpreadingArea(area: Area, stroke: RingStroke, capture: Capture, frame: Frame, geometry: BoardGeometry, style: BoardStyle, wash: Color) {
    val spread = frame.timing!!.wash(frame.t)
    if (spread <= 0f) return
    if (spread >= 1f) return drawArea(area.by, stroke, style, wash)
    val from = geometry.center(capture.closing)
    val reach = area.ring.maxOf { val c = geometry.center(it); hypot(c.x - from.x, c.y - from.y) } + geometry.spacing
    val circle = Path().apply { addOval(Rect(from, reach * spread)) }
    clipPath(circle) { drawArea(area.by, stroke, style, wash) }
}

/**
 * The ripples of an announced dot (motion.md, "The computer's move"): two rings in its side's ink
 * grow from 22 dp to 96 dp across while their line thins from 3 dp to 0.5 dp. They are drawn
 * under the pieces, so they never hide a dot.
 */
private fun DrawScope.drawRipples(geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val moment = frame.moment ?: return
    if (!moment.announced || frame.t >= Announce.LENGTH) return
    val center = geometry.center(moment.dot)
    val ink = style.color(moment.by)
    for (i in Announce.RIPPLES.indices) {
        val p = Announce.ripple(frame.t, i) ?: continue
        val across = Announce.RIPPLE_FROM + (Announce.RIPPLE_TO - Announce.RIPPLE_FROM) * p
        val line = Announce.LINE_FROM + (Announce.LINE_TO - Announce.LINE_FROM) * p
        drawRing(center, across.dp.toPx() / 2, ink, line.dp.toPx())
    }
}

/**
 * The edge marks of an announced dot: a short stroke on the top edge above its column and one on
 * the left edge beside its row, in its side's ink, in the half spacing of paper outside the grid.
 * They grow in from the edges as the dot lands, and shrink back when the player's next dot lands.
 */
private fun DrawScope.drawEdgeMarks(state: GameState, geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val moment = frame.moment
    val t = frame.t
    val shown = frame.edgeMarks
    if (shown != null) {
        val reach = if (moment != null && moment.announced && moment.dot == shown) Announce.marksIn(t) else 1f
        drawEdgeMark(shown, reach, state, geometry, style)
    }
    val gone = moment?.marksGone
    if (gone != null && gone != shown) drawEdgeMark(gone, Announce.marksOut(t), state, geometry, style)
}

private fun DrawScope.drawEdgeMark(dot: Pos, reach: Float, state: GameState, geometry: BoardGeometry, style: BoardStyle) {
    if (reach <= 0f) return
    val owner = state.ownerAt(dot) ?: return
    val ink = style.color(owner)
    val length = Announce.MARK_LENGTH.dp.toPx() * reach
    val width = Announce.MARK_LINE.dp.toPx()
    val center = geometry.center(dot)
    drawLine(ink, Offset(center.x, 0f), Offset(center.x, length), width)
    drawLine(ink, Offset(0f, center.y), Offset(length, center.y), width)
}

/**
 * A big capture's burst (motion.md, "Big capture"): 14 short strokes in the captor's ink around
 * the area move outwards, grow and shrink away.
 */
private fun DrawScope.drawBurst(geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val timing = frame.timing ?: return
    val p = timing.burst(frame.t) ?: return
    for (capture in frame.moment?.captures.orEmpty()) {
        val ring = capture.ring.map(geometry::center)
        val cx = ring.sumOf { it.x.toDouble() }.toFloat() / ring.size
        val cy = ring.sumOf { it.y.toDouble() }.toFloat() / ring.size
        val radius = ring.maxOf { hypot(it.x - cx, it.y - cy) } + geometry.spacing * 0.35f
        val out = geometry.spacing * 0.8f * p
        val length = geometry.spacing * 0.45f * sin(PI * p).toFloat()
        val ink = style.color(capture.by)
        for (k in 0 until Capturing.BURST_STROKES) {
            val angle = 2 * PI * k / Capturing.BURST_STROKES + 0.2
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()
            val from = radius + out
            drawLine(ink, Offset(cx + dx * from, cy + dy * from), Offset(cx + dx * (from + length), cy + dy * (from + length)), 2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

/**
 * A captured area's ring as a pen stroke (theme-notebook.md, "The ring"): its [outline], through the
 * centres of the ring's dots, each side bowed a little, and the [sides] one by one, each with its
 * own width. The bow and the width come from a seed taken from the area, so the same ring looks
 * the same on every frame.
 */
private class RingStroke(val outline: Path, val sides: List<Pair<Path, Float>>)

private fun DrawScope.ringStroke(area: Area, geometry: BoardGeometry, look: BoardLook): RingStroke {
    val first = area.region.first()
    val random = Random(31 * (31 * area.by.ordinal + first.x) + first.y)
    val points = area.ring.map(geometry::center)
    val bow = look.ringBow.dp.toPx()
    val width = look.ring.px(geometry.spacing, density)
    val outline = Path()
    val sides = ArrayList<Pair<Path, Float>>(points.size)
    for (i in points.indices) {
        val a = points[i]
        val b = points[(i + 1) % points.size]
        val length = hypot(b.x - a.x, b.y - a.y)
        // A quadratic curve's middle lies halfway to its control point: the control point sits
        // twice the bow off the straight line.
        val offset = if (length > 0f) 2 * bow * (random.nextFloat() * 2 - 1) / length else 0f
        val cx = (a.x + b.x) / 2 - (b.y - a.y) * offset
        val cy = (a.y + b.y) / 2 + (b.x - a.x) * offset
        val sideWidth = width * (1 + look.ringWobble * (random.nextFloat() * 2 - 1))
        if (i == 0) outline.moveTo(a.x, a.y)
        outline.quadraticTo(cx, cy, b.x, b.y)
        sides += Path().apply {
            moveTo(a.x, a.y)
            quadraticTo(cx, cy, b.x, b.y)
        } to sideWidth
    }
    outline.close()
    return RingStroke(outline, sides)
}

/**
 * Inside a ring: the captor's wash, then its hatching, clipped to the ring (theme-notebook.md,
 * "Captured areas"). Red's lines rise to the right, Blue's fall, one every [BoardLook.hatchGap] dp
 * at every spacing.
 */
private fun DrawScope.drawArea(by: Player, stroke: RingStroke, style: BoardStyle, wash: Color = style.palette.wash(by)) {
    drawPath(stroke.outline, wash)
    val width = style.look.hatch.px(0f, density)
    if (width <= 0f) return
    val ink = style.palette.pale(by)
    // Lines x + y = c rise to the right; lines x - y = c fall. A gap of g between lines is g times
    // the square root of 2 along an axis.
    val step = style.look.hatchGap.dp.toPx() * sqrt(2f)
    val box = stroke.outline.getBounds()
    val rising = by == Player.RED
    val from = if (rising) box.left + box.top else box.left - box.bottom
    val to = if (rising) box.right + box.bottom else box.right - box.top
    clipPath(stroke.outline) {
        var c = floor(from / step) * step
        while (c <= to) {
            if (rising) {
                drawLine(ink, Offset(c - box.top, box.top), Offset(c - box.bottom, box.bottom), width)
            } else {
                drawLine(ink, Offset(c + box.top, box.top), Offset(c + box.bottom, box.bottom), width)
            }
            c += step
        }
    }
}

/**
 * The ring round a captured area, in the captor's ink, each side with round ends, [width] times
 * as thick as drawn. While a capture plays, it is drawn like a pen stroke: from side [from] (the
 * closing dot) round the area, as far as [drawn] of its length.
 */
private fun DrawScope.drawAreaRing(by: Player, stroke: RingStroke, style: BoardStyle, from: Int = 0, drawn: Float = 1f, width: Float = 1f) {
    val ink = style.color(by)
    if (drawn >= 1f) {
        for ((side, w) in stroke.sides) drawPath(side, ink, style = Stroke(w * width, cap = StrokeCap.Round))
        return
    }
    if (drawn <= 0f) return
    val sides = stroke.sides.drop(from) + stroke.sides.take(from)
    val measure = PathMeasure()
    val lengths = sides.map { (side, _) -> measure.setPath(side, false); measure.length }
    var left = lengths.sum() * drawn
    for ((i, pair) in sides.withIndex()) {
        val (side, w) = pair
        val length = lengths[i]
        if (left >= length) {
            drawPath(side, ink, style = Stroke(w * width, cap = StrokeCap.Round))
            left -= length
        } else {
            measure.setPath(side, false)
            val part = Path()
            measure.getSegment(0f, left, part, true)
            drawPath(part, ink, style = Stroke(w * width, cap = StrokeCap.Round))
            break
        }
    }
}

/** Every blocked point, a wall booster's or the level's own, with the wall's mark. */
private fun DrawScope.drawWalls(state: GameState, geometry: BoardGeometry, style: BoardStyle) {
    for (y in 0 until state.height) for (x in 0 until state.width) {
        if (state.isBlocked(x, y) && !state.isHole(x, y)) drawWall(geometry.center(x, y), geometry, style, preview = false)
    }
}

/**
 * The wall's mark (motion.md, Boosters: Wall): a square, 0.7 of the spacing, round the point in one
 * Pencil stroke, filled with five hatch lines in Graphite. In the [preview] its outline is dashed.
 */
private fun DrawScope.drawWall(at: Offset, geometry: BoardGeometry, style: BoardStyle, preview: Boolean) {
    val half = geometry.spacing * 0.35f
    val width = max(geometry.spacing * 0.075f, 1.5.dp.toPx())
    val left = at.x - half
    val top = at.y - half
    clipRect(left, top, at.x + half, at.y + half) {
        val graphite = style.graphite
        for (i in 1..5) {
            // Lines rising to the right, evenly across the square's diagonal.
            val d = 4 * half * i / 6f
            drawLine(graphite, Offset(left, top + d), Offset(left + d, top), width * 0.6f)
        }
    }
    val effect = if (preview) PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())) else null
    drawRect(style.pencil, Offset(left, top), Size(2 * half, 2 * half), style = Stroke(width, pathEffect = effect))
}

/** The points a booster may be aimed at: a small dot in the pale ink of the side to move. */
private fun DrawScope.drawTargets(state: GameState, targets: List<Pos>, geometry: BoardGeometry, style: BoardStyle) {
    val ink = style.palette.pale(state.toMove)
    for (point in targets) drawCircle(ink, geometry.spacing * 0.1f, geometry.center(point))
}

/** The bomb's plus: each point in a dashed ring in the ink of the side to move, on its wash. */
private fun DrawScope.drawPlus(state: GameState, plus: List<Pos>, geometry: BoardGeometry, style: BoardStyle) {
    val ink = style.color(state.toMove)
    val wash = style.palette.wash(state.toMove)
    for (point in plus) {
        val c = geometry.center(point)
        drawCircle(wash, geometry.spacing * 0.46f, c)
        drawDashedRing(c, geometry.spacing * 0.46f, ink, 1.5.dp.toPx())
    }
}

/** Pieces a booster would take away: pale, in a dashed Graphite ring (Undo's preview, motion.md). */
private fun DrawScope.drawGone(state: GameState, gone: List<Pos>, geometry: BoardGeometry, style: BoardStyle) {
    val dot = style.look.dot.px(geometry.spacing, density)
    for (point in gone) {
        val owner = state.ownerAt(point.x, point.y) ?: continue
        val c = geometry.center(point)
        val piece = style.piece(owner)
        drawPiece(piece.copy(across = piece.across * 1.1f), c, dot, style.paper)
        drawPiece(piece, c, dot, style.palette.pale(owner))
        drawDashedRing(c, geometry.spacing * 0.45f, style.graphite, 1.5.dp.toPx())
    }
}

private fun DrawScope.drawDashedRing(center: Offset, radius: Float, color: Color, width: Float) {
    val dash = 3.dp.toPx()
    drawCircle(color, radius, center, style = Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))))
}

/**
 * Drag's guides (theme-notebook.md, "Drag's guides"): four arms from the preview's ring along its
 * row and column to the board's outermost grid lines, in the ink of the side to move, dashed in
 * the theme's dashes, each arm's dashes starting at the ring. A fingertip covers about 60 dp; the
 * arms run past it.
 */
private fun DrawScope.drawGuides(state: GameState, point: Pos, geometry: BoardGeometry, style: BoardStyle, areas: List<Area>, strokes: List<RingStroke>, reach: Float) {
    val look = style.look
    val ink = style.color(state.toMove).copy(alpha = look.guideAlpha)
    val width = look.guide.px(geometry.spacing, density)
    val dashes = if (look.guideDash > 0f) PathEffect.dashPathEffect(floatArrayOf(look.guideDash.dp.toPx(), look.guideGap.dp.toPx())) else null
    val center = geometry.center(point)
    val ring = look.previewRing.px(geometry.spacing, density) / 2
    val ends = listOf(
        geometry.center(0, point.y),
        geometry.center(state.width - 1, point.y),
        geometry.center(point.x, 0),
        geometry.center(point.x, state.height - 1),
    )
    val arms = ends.mapNotNull { end ->
        val length = hypot(end.x - center.x, end.y - center.y)
        if (length <= ring) return@mapNotNull null
        val start = Offset(center.x + (end.x - center.x) * ring / length, center.y + (end.y - center.y) * ring / length)
        // Drawn out from the ring to the edge as the finger goes down (motion.md, "Placing by drag").
        start to if (reach >= 1f) end else Offset(start.x + (end.x - start.x) * reach, start.y + (end.y - start.y) * reach)
    }
    // High contrast: each arm on a strip of the paper or wash under it, hiding the grid and hatching there.
    val casing = look.guideCasing.px(geometry.spacing, density)
    if (casing > 0f) onSurface(areas, strokes, style) { surface -> for ((start, end) in arms) drawLine(surface, start, end, width + 2 * casing) }
    for ((start, end) in arms) drawLine(ink, start, end, width, pathEffect = dashes)
}

/**
 * Draws [layer] in the paper, then again inside each captured area's ring in its wash, so it takes
 * the colour of whatever lies under it (high contrast's bands and strips).
 */
private fun DrawScope.onSurface(areas: List<Area>, strokes: List<RingStroke>, style: BoardStyle, layer: DrawScope.(Color) -> Unit) {
    layer(style.paper)
    for ((area, stroke) in areas.zip(strokes)) clipPath(stroke.outline) { layer(style.palette.wash(area.by)) }
}

/**
 * High contrast (pieces.md, "High contrast"): a band [BoardLook.halo] wide round every piece, in the
 * paper or wash under it, so grid lines, hatching, links and guides stop short of the piece. Rings
 * and threat rings are drawn after it and stay whole.
 */
private fun DrawScope.drawHalos(state: GameState, geometry: BoardGeometry, style: BoardStyle, areas: List<Area>, strokes: List<RingStroke>, frame: Frame) {
    val halo = style.look.halo.px(geometry.spacing, density)
    if (halo <= 0f) return
    val dot = style.look.dot.px(geometry.spacing, density)
    val newDot = frame.newDot
    onSurface(areas, strokes, style) { surface ->
        for (y in 0 until state.height) for (x in 0 until state.width) {
            val owner = state.ownerAt(x, y) ?: continue
            val scale = if (newDot != null && newDot.x == x && newDot.y == y) Placing.dotScale(frame.t) else 1f
            if (scale <= 0f) continue
            drawPiece(style.piece(owner).grown(halo, dot).let { it.copy(across = it.across * scale) }, geometry.center(x, y), dot, surface)
        }
    }
}

/**
 * The last move: a Paper pinhole in the middle of the newest piece (pieces.md), or, in a theme
 * without one, a Grid ring round it.
 */
private fun DrawScope.drawLastMove(at: Offset, geometry: BoardGeometry, style: BoardStyle) {
    val look = style.look
    val spacing = geometry.spacing
    if (look.pinhole > 0f) {
        drawCircle(style.paper, look.dot.px(spacing, density) * look.pinhole / 2, at)
    } else {
        drawRing(at, look.lastMoveRing.px(spacing, density) / 2, style.grid, look.lastMoveLine.px(spacing, density))
    }
}

/** The grid lines, the outermost ones (the board's edge) in the edge's width. */
private fun DrawScope.drawGrid(state: GameState, geometry: BoardGeometry, style: BoardStyle) {
    val line = style.look.grid.px(geometry.spacing, density)
    val edge = style.look.edge.px(geometry.spacing, density)
    for (x in 0 until state.width) {
        val width = if (x == 0 || x == state.width - 1) edge else line
        drawLine(style.grid, geometry.center(x, 0), geometry.center(x, state.height - 1), width)
    }
    for (y in 0 until state.height) {
        val width = if (y == 0 || y == state.height - 1) edge else line
        drawLine(style.grid, geometry.center(0, y), geometry.center(state.width - 1, y), width)
    }
}

/** A line in the side's pale ink between every two live dots of one colour that touch, orthogonally or diagonally. */
private fun DrawScope.drawLinks(state: GameState, geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val width = style.look.link.px(geometry.spacing, density)
    // A new dot's links grow from each neighbour to it (motion.md, "Placing a dot and chain lines").
    val newDot = frame.moment?.dot?.takeIf { frame.t < Placing.LINKS_TO }
    val growth = Placing.links(frame.t)
    for (y in 0 until state.height) for (x in 0 until state.width) {
        val owner = state.ownerAt(x, y) ?: continue
        if (state.isCaptured(x, y)) continue
        for ((dx, dy) in LINK_DIRECTIONS) {
            val nx = x + dx
            val ny = y + dy
            if (!state.inBounds(nx, ny) || state.ownerAt(nx, ny) != owner || state.isCaptured(nx, ny)) continue
            var from = geometry.center(x, y)
            var to = geometry.center(nx, ny)
            if (newDot != null && (newDot == Pos(x, y) || newDot == Pos(nx, ny))) {
                if (growth <= 0f) continue
                if (newDot == Pos(x, y)) from = to.also { to = from }
                to = Offset(from.x + (to.x - from.x) * growth, from.y + (to.y - from.y) * growth)
            }
            drawLine(style.palette.pale(owner), from, to, width)
        }
    }
}

/** Each touching pair once: right, down, and the two diagonals going right. */
private val LINK_DIRECTIONS = listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)

/**
 * How a threat looks in this frame (motion.md, "Threat pulse"): a new one draws in ([drawIn] from 0
 * to 1) and, when one dot away, pulses ([pulse] 0 to 1). Older threats are still; a new one is not
 * shown until 200 ms after the dot that made it.
 */
private class ThreatLook(val drawIn: Float, val pulse: Float)

private val STILL_THREAT = ThreatLook(1f, 0f)

private fun Frame.threat(danger: Danger): ThreatLook? {
    val moment = moment ?: return STILL_THREAT
    if (t >= Threat.LENGTH || danger.group.toSet() !in moment.freshThreats) return STILL_THREAT
    val since = t - Threat.DELAY
    if (since < 0f) return null
    return ThreatLook(Threat.drawIn(since), if (danger.dotsNeeded == 1) Threat.pulse(since) else 0f)
}

/**
 * A Highlighter ring around every dot of a group close to capture: solid when one dot away, faint
 * when two. A new one shrinks from 130% to its size while its colour blends from Paper, then
 * pulses three times: its line from 2 dp to 3.5 dp and the ring to 108%.
 */
private fun DrawScope.drawThreatRings(dangers: List<Danger>, geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val look = style.look
    val spacing = geometry.spacing
    val radius = look.threatRing.px(spacing, density) / 2
    for (danger in dangers) {
        val shown = frame.threat(danger) ?: continue
        val near = danger.dotsNeeded == 1
        val full = if (near) style.palette.highlighter else style.palette.highlighterFaint
        val ring = if (shown.drawIn >= 1f) full else lerp(style.paper, full, shown.drawIn)
        val width = (if (near) look.threatNear else look.threatFar).px(spacing, density) * (1f + 0.75f * shown.pulse)
        val size = (if (shown.drawIn >= 1f) 1f else 1.3f - 0.3f * shown.drawIn) * (1f + 0.08f * shown.pulse)
        for (dot in danger.group) drawRing(geometry.center(dot), radius * size, ring, width)
    }
}

/**
 * For a group one dot from capture, a small circle in the attacker's ink on the point that would
 * close the ring. A new one grows from nothing, then swells to 135% with each pulse.
 */
private fun DrawScope.drawClosingPoints(state: GameState, dangers: List<Danger>, geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val look = style.look
    val spacing = geometry.spacing
    for (danger in dangers) {
        if (danger.dotsNeeded != 1) continue
        val shown = frame.threat(danger) ?: continue
        val size = shown.drawIn * (1f + 0.35f * shown.pulse)
        if (size <= 0f) continue
        val attacker = state.ownerAt(danger.group.first())?.opponent ?: continue
        for (point in danger.closingPoints) {
            drawRing(geometry.center(point), look.closingPoint.px(spacing, density) / 2 * size, style.color(attacker), look.closingLine.px(spacing, density))
        }
    }
}

/**
 * Each side's pieces. Captured ones keep the theme's share of their ink and carry its cut
 * (pieces.md): a band in the captor's wash through the middle of the piece, along the captor's
 * hatching, rising for Red and falling for Blue.
 */
private fun DrawScope.drawDots(state: GameState, geometry: BoardGeometry, style: BoardStyle, frame: Frame) {
    val look = style.look
    val dot = look.dot.px(geometry.spacing, density)
    val newDot = frame.newDot
    for (y in 0 until state.height) for (x in 0 until state.width) {
        val owner = state.ownerAt(x, y) ?: continue
        val color = style.color(owner)
        val center = geometry.center(x, y)
        // A dot just placed grows to 115% and settles (motion.md, "Placing a dot").
        val scale = if (newDot != null && newDot.x == x && newDot.y == y) Placing.dotScale(frame.t) else 1f
        if (scale <= 0f) continue
        val piece = style.piece(owner).let { if (scale == 1f) it else it.copy(across = it.across * scale) }
        // A dot being captured keeps its ink until its cut is drawn across it.
        val cut = if (state.isCaptured(x, y)) frame.cuts[Pos(x, y)] ?: 1f else 0f
        drawPiece(piece, center, dot, if (cut > 0f) color.copy(alpha = look.capturedAlpha) else color)
        if (cut > 0f && look.cut > 0f) {
            val captor = owner.opponent
            // Half the cut's length along each axis, at 45°; it is drawn from its lower-left end.
            val d = dot * look.cutLength / 2 / sqrt(2f)
            val dy = if (captor == Player.RED) -d else d
            val start = Offset(center.x - d, center.y - dy)
            val end = Offset(center.x + d, center.y + dy)
            drawLine(style.palette.wash(captor), start, Offset(start.x + (end.x - start.x) * cut, start.y + (end.y - start.y) * cut), dot * look.cut)
        }
    }
}

private fun DrawScope.drawRing(center: Offset, radius: Float, color: Color, width: Float) {
    drawCircle(color, radius, center, style = Stroke(width))
}
