package io.github.pavelya.dots.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import dots.engine.areas
import io.github.pavelya.dots.ui.theme.BoardLook
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Draws a position: grid, captured areas (washed and hatched in the captor's colours, inside a ring
 * drawn like a pen stroke), live and captured dots, the last move, and a placement preview. It
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
) {
    val areas = remember(state) { state.areas() }
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
        drawPosition(state, areas, geometry, style, lastMove, preview, showLinks, dangers, marked, marks, guided)
        pointer?.let { drawPointer(geometry.center(it), geometry, style) }
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
 * paper, washes, hatching, grid, links, drag's guides along [guided]'s row and column, rings,
 * threat rings, pieces, the last move, closing points, the preview. The booster marks lie with
 * the pieces they are about, and the circled points on top.
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
) {
    drawRect(style.paper)
    val strokes = areas.map { ringStroke(it, geometry, style.look) }
    for ((area, stroke) in areas.zip(strokes)) drawArea(area.by, stroke, style)
    drawGrid(state, geometry, style)
    drawWalls(state, geometry, style)
    drawTargets(state, marks.targets, geometry, style)
    marks.wall?.let { drawWall(geometry.center(it), geometry, style, preview = true) }
    if (showLinks) drawLinks(state, geometry, style)
    guided?.let { drawGuides(state, it, geometry, style) }
    for ((area, stroke) in areas.zip(strokes)) drawAreaRing(area.by, stroke, style)
    drawThreatRings(dangers, geometry, style)
    drawDots(state, geometry, style)
    drawGone(state, marks.gone, geometry, style)
    drawPlus(state, marks.plus, geometry, style)
    val look = style.look
    val spacing = geometry.spacing
    lastMove?.let {
        drawRing(geometry.center(it), look.lastMoveRing.px(spacing, density) / 2, style.grid, look.lastMoveLine.px(spacing, density))
    }
    drawClosingPoints(state, dangers, geometry, style)
    for (circled in listOfNotNull(marked, marks.hint)) {
        val ink = style.color(Player.RED)
        drawRing(geometry.center(circled), geometry.spacing * 0.42f, ink, geometry.spacing * 0.07f)
        drawRing(geometry.center(circled), geometry.spacing * 0.58f, ink, geometry.spacing * 0.05f)
    }
    preview?.let {
        val center = geometry.center(it)
        drawPiece(style.piece(state.toMove), center, look.dot.px(spacing, density), style.palette.pale(state.toMove))
        drawRing(center, look.previewRing.px(spacing, density) / 2, style.color(state.toMove), look.previewLine.px(spacing, density))
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
private fun DrawScope.drawArea(by: Player, stroke: RingStroke, style: BoardStyle) {
    drawPath(stroke.outline, style.palette.wash(by))
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

/** The ring round a captured area, in the captor's ink, each side with round ends. */
private fun DrawScope.drawAreaRing(by: Player, stroke: RingStroke, style: BoardStyle) {
    val ink = style.color(by)
    for ((side, width) in stroke.sides) drawPath(side, ink, style = Stroke(width, cap = StrokeCap.Round))
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
private fun DrawScope.drawGuides(state: GameState, point: Pos, geometry: BoardGeometry, style: BoardStyle) {
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
    for (end in ends) {
        val length = hypot(end.x - center.x, end.y - center.y)
        if (length <= ring) continue
        val start = Offset(center.x + (end.x - center.x) * ring / length, center.y + (end.y - center.y) * ring / length)
        drawLine(ink, start, end, width, pathEffect = dashes)
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
private fun DrawScope.drawLinks(state: GameState, geometry: BoardGeometry, style: BoardStyle) {
    val width = style.look.link.px(geometry.spacing, density)
    for (y in 0 until state.height) for (x in 0 until state.width) {
        val owner = state.ownerAt(x, y) ?: continue
        if (state.isCaptured(x, y)) continue
        for ((dx, dy) in LINK_DIRECTIONS) {
            val nx = x + dx
            val ny = y + dy
            if (!state.inBounds(nx, ny) || state.ownerAt(nx, ny) != owner || state.isCaptured(nx, ny)) continue
            drawLine(style.palette.pale(owner), geometry.center(x, y), geometry.center(nx, ny), width)
        }
    }
}

/** Each touching pair once: right, down, and the two diagonals going right. */
private val LINK_DIRECTIONS = listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)

/** A Highlighter ring around every dot of a group close to capture: solid when one dot away, faint when two. */
private fun DrawScope.drawThreatRings(dangers: List<Danger>, geometry: BoardGeometry, style: BoardStyle) {
    val look = style.look
    val spacing = geometry.spacing
    val radius = look.threatRing.px(spacing, density) / 2
    for (danger in dangers) {
        val near = danger.dotsNeeded == 1
        val ring = if (near) style.palette.highlighter else style.palette.highlighterFaint
        val width = (if (near) look.threatNear else look.threatFar).px(spacing, density)
        for (dot in danger.group) drawRing(geometry.center(dot), radius, ring, width)
    }
}

/** For a group one dot from capture, a small circle in the attacker's ink on the point that would close the ring. */
private fun DrawScope.drawClosingPoints(state: GameState, dangers: List<Danger>, geometry: BoardGeometry, style: BoardStyle) {
    val look = style.look
    val spacing = geometry.spacing
    for (danger in dangers) {
        if (danger.dotsNeeded != 1) continue
        val attacker = state.ownerAt(danger.group.first())?.opponent ?: continue
        for (point in danger.closingPoints) {
            drawRing(geometry.center(point), look.closingPoint.px(spacing, density) / 2, style.color(attacker), look.closingLine.px(spacing, density))
        }
    }
}

/** Each side's pieces; captured ones keep the theme's share of their ink. */
private fun DrawScope.drawDots(state: GameState, geometry: BoardGeometry, style: BoardStyle) {
    val dot = style.look.dot.px(geometry.spacing, density)
    for (y in 0 until state.height) for (x in 0 until state.width) {
        val owner = state.ownerAt(x, y) ?: continue
        val color = style.color(owner)
        val ink = if (state.isCaptured(x, y)) color.copy(alpha = style.look.capturedAlpha) else color
        drawPiece(style.piece(owner), geometry.center(x, y), dot, ink)
    }
}

private fun DrawScope.drawRing(center: Offset, radius: Float, color: Color, width: Float) {
    drawCircle(color, radius, center, style = Stroke(width))
}
