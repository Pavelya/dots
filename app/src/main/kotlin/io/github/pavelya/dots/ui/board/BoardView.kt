package io.github.pavelya.dots.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dots.ai.Danger
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import kotlin.math.max

/**
 * Draws a position: grid, enclosed areas shaded in the colour of the player who closed them, live
 * and captured dots, the last move, and a placement preview. It decides nothing about the game;
 * [PlayableBoard] adds touch. [showLinks] draws the links between neighbouring dots of one colour,
 * which makes chains and rings visible (used to teach the rules). [dangers] marks groups close to
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
    Canvas(
        modifier = modifier
            .aspectRatio(state.width.toFloat() / state.height)
            .semantics { contentDescription = description },
    ) {
        val geometry = BoardGeometry(state.width, state.height, size.width, size.height)
        drawPosition(state, geometry, style, lastMove, preview, showLinks, dangers, marked, marks)
        val guided = preview ?: marks.point
        if (guided != null && guides) drawGuides(state, guided, geometry, style)
        pointer?.let { drawPointer(geometry.center(it), geometry, style) }
    }
}

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

/** Every layer of the position, in the theme's drawing order. */
private fun DrawScope.drawPosition(
    state: GameState,
    geometry: BoardGeometry,
    style: BoardStyle,
    lastMove: Pos?,
    preview: Pos?,
    showLinks: Boolean,
    dangers: List<Danger>,
    marked: Pos?,
    marks: BoardMarks,
) {
    drawRect(style.paper)
    drawAreas(state, geometry, style)
    drawGrid(state, geometry, style)
    drawWalls(state, geometry, style)
    drawTargets(state, marks.targets, geometry, style)
    marks.wall?.let { drawWall(geometry.center(it), geometry, style, preview = true) }
    if (showLinks) drawLinks(state, geometry, style)
    drawDots(state, geometry, style)
    drawGone(state, marks.gone, geometry, style)
    drawPlus(state, marks.plus, geometry, style)
    drawDangers(state, dangers, geometry, style)
    for (circled in listOfNotNull(marked, marks.hint)) {
        val ink = style.color(Player.RED)
        drawRing(geometry.center(circled), geometry.spacing * 0.42f, ink, geometry.spacing * 0.07f)
        drawRing(geometry.center(circled), geometry.spacing * 0.58f, ink, geometry.spacing * 0.05f)
    }
    val look = style.look
    val spacing = geometry.spacing
    lastMove?.let {
        drawRing(geometry.center(it), look.lastMoveRing.px(spacing, density) / 2, style.grid, look.lastMoveLine.px(spacing, density))
    }
    preview?.let {
        val center = geometry.center(it)
        drawPiece(style.piece(state.toMove), center, look.dot.px(spacing, density), style.palette.pale(state.toMove))
        drawRing(center, look.previewRing.px(spacing, density) / 2, style.color(state.toMove), look.previewLine.px(spacing, density))
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
 * The preview's row and column across the board, in the ink of the side to move. A fingertip
 * covers about 60 dp; the lines run past it.
 */
private fun DrawScope.drawGuides(state: GameState, preview: Pos, geometry: BoardGeometry, style: BoardStyle) {
    val ink = style.color(state.toMove).copy(alpha = style.look.guideAlpha)
    val width = style.look.guide.px(geometry.spacing, density)
    drawLine(ink, geometry.center(0, preview.y), geometry.center(state.width - 1, preview.y), width)
    drawLine(ink, geometry.center(preview.x, 0), geometry.center(preview.x, state.height - 1), width)
}

private fun DrawScope.drawAreas(state: GameState, geometry: BoardGeometry, style: BoardStyle) {
    val half = geometry.spacing / 2
    for (y in 0 until state.height) for (x in 0 until state.width) {
        val by = state.enclosedBy(x, y) ?: continue
        val c = geometry.center(x, y)
        drawRect(style.palette.wash(by), Offset(c.x - half, c.y - half), Size(geometry.spacing, geometry.spacing))
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

/**
 * A Highlighter ring around every dot of a group close to capture: solid when one dot away, faint
 * when two. When one dot away, the point that would close the ring gets a small circle in the
 * attacker's ink.
 */
private fun DrawScope.drawDangers(state: GameState, dangers: List<Danger>, geometry: BoardGeometry, style: BoardStyle) {
    val look = style.look
    val spacing = geometry.spacing
    val radius = look.threatRing.px(spacing, density) / 2
    for (danger in dangers) {
        val near = danger.dotsNeeded == 1
        val ring = if (near) style.palette.highlighter else style.palette.highlighterFaint
        val width = (if (near) look.threatNear else look.threatFar).px(spacing, density)
        for (dot in danger.group) drawRing(geometry.center(dot), radius, ring, width)
        if (near) {
            val attacker = state.ownerAt(danger.group.first())?.opponent ?: continue
            for (point in danger.closingPoints) {
                drawRing(geometry.center(point), look.closingPoint.px(spacing, density) / 2, style.color(attacker), look.closingLine.px(spacing, density))
            }
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
