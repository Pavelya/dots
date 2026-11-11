package io.github.pavelya.dots.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dots.ai.Danger
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import kotlin.math.max
import kotlin.math.min

/**
 * What a finger on the board shows while a dot is being placed (A-13, [PlayableBoard]):
 *
 * - [zoom] above 1 enlarges the board around [zoomAround] (the zoom method).
 * - [finger], where a finger is down, gets a magnifier above it (the magnifier method).
 * - [guides] draws the preview's row and column, from 0 (none) to 1 (across the board): they show
 *   around a finger that hides the point itself (the drag and long-press methods).
 */
data class BoardTouch(
    val zoom: Float = 1f,
    val zoomAround: Pos? = null,
    val finger: Offset? = null,
    val guides: Float = 0f,
)

/**
 * Draws a position: grid, enclosed areas shaded in the colour of the player who closed them, live
 * and captured dots, the last move, and a placement preview. It decides nothing about the game;
 * [PlayableBoard] adds touch. [showLinks] draws the links between neighbouring dots of one colour,
 * which makes chains and rings visible (used to teach the rules). [dangers] marks groups close to
 * capture (the threat highlight). [marked] is the dot a level's mission is about (rescue or
 * target), circled twice in the player's ink. [touch] is what a finger placing a dot shows.
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
    touch: BoardTouch = BoardTouch(),
) {
    Canvas(
        modifier = modifier
            .aspectRatio(state.width.toFloat() / state.height)
            .semantics { contentDescription = description },
    ) {
        val board = BoardGeometry(state.width, state.height, size.width, size.height)
        val zoomAround = touch.zoomAround
        val drawBoard = { geometry: BoardGeometry ->
            drawPosition(state, geometry, style, lastMove, preview, showLinks, dangers, marked)
            if (preview != null && touch.guides > 0f) drawGuides(state, preview, touch.guides, geometry, style)
        }
        if (touch.zoom > 1f && zoomAround != null) {
            clipRect { drawBoard(board.zoomed(touch.zoom, zoomAround)) }
        } else {
            drawBoard(board)
        }
        touch.finger?.let { drawMagnifier(it, board, style, drawBoard) }
    }
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
) {
    drawRect(style.background)
    drawAreas(state, geometry, style)
    drawGrid(state, geometry, style)
    if (showLinks) drawLinks(state, geometry, style)
    drawDots(state, geometry, style)
    drawDangers(state, dangers, geometry, style)
    marked?.let {
        val ink = style.color(Player.RED)
        drawRing(geometry.center(it), geometry.spacing * 0.42f, ink, geometry.spacing * 0.07f)
        drawRing(geometry.center(it), geometry.spacing * 0.58f, ink, geometry.spacing * 0.05f)
    }
    lastMove?.let { drawRing(geometry.center(it), geometry.spacing * 0.42f, style.grid, geometry.spacing * 0.06f) }
    preview?.let {
        val color = style.color(state.toMove)
        drawCircle(color.copy(alpha = 0.45f), geometry.spacing * style.dotRadius, geometry.center(it))
        drawRing(geometry.center(it), geometry.spacing * 0.45f, color, geometry.spacing * 0.08f)
    }
}

/**
 * The preview's row and column, in the ink of the side to move, growing from the point to the
 * edges as [progress] goes from 0 to 1. A finger covers about 60 dp; the lines run past it.
 */
private fun DrawScope.drawGuides(state: GameState, preview: Pos, progress: Float, geometry: BoardGeometry, style: BoardStyle) {
    val ink = style.color(state.toMove).copy(alpha = 0.6f)
    val width = max(geometry.spacing * 0.08f, 2.dp.toPx())
    val at = geometry.center(preview)
    val first = geometry.center(0, 0)
    val last = geometry.center(state.width - 1, state.height - 1)
    fun toward(from: Float, to: Float) = from + (to - from) * progress
    drawLine(ink, at, Offset(toward(at.x, first.x), at.y), width)
    drawLine(ink, at, Offset(toward(at.x, last.x), at.y), width)
    drawLine(ink, at, Offset(at.x, toward(at.y, first.y)), width)
    drawLine(ink, at, Offset(at.x, toward(at.y, last.y)), width)
}

/** The magnifier's size, how far above the finger it sits, and how much it enlarges. */
internal val MAGNIFIER_RADIUS = 52.dp
internal val MAGNIFIER_LIFT = 88.dp
internal const val MAGNIFIER_ZOOM = 2f

/**
 * A round magnifier showing the board around [finger], [MAGNIFIER_ZOOM] times larger, with the
 * preview in the middle. It sits [MAGNIFIER_LIFT] above the finger, clear of a fingertip; near the
 * top of the board, where that would leave the screen, it sits beside the finger, toward the middle.
 * It is drawn by the board but may reach past its top edge, over the opponent's line.
 */
private fun DrawScope.drawMagnifier(finger: Offset, board: BoardGeometry, style: BoardStyle, drawBoard: (BoardGeometry) -> Unit) {
    val radius = MAGNIFIER_RADIUS.toPx()
    val lift = MAGNIFIER_LIFT.toPx()
    val center = if (finger.y - lift - radius >= -TOP_ROOM.toPx()) {
        Offset(finger.x, finger.y - lift)
    } else {
        val side = if (finger.x < size.width / 2) 1 else -1
        Offset(finger.x + side * (radius + SIDE_GAP.toPx()), max(finger.y - SIDE_GAP.toPx(), radius - TOP_ROOM.toPx()))
    }
    val circle = Path().apply { addOval(Rect(center, radius)) }
    clipPath(circle) {
        drawRect(style.background, topLeft = Offset(center.x - radius, center.y - radius), size = Size(2 * radius, 2 * radius))
        val enlarged = BoardGeometry(board.columns, board.rows, board.width, board.height, MAGNIFIER_ZOOM, finger)
        translate(center.x - finger.x, center.y - finger.y) { drawBoard(enlarged) }
    }
    drawCircle(style.grid, radius, center, style = Stroke(min(2.dp.toPx(), radius / 10)))
}

/** The height above the board the magnifier may use: at least the top row is there. */
private val TOP_ROOM = 56.dp

/** The gap between a fingertip's middle and a magnifier beside it. */
private val SIDE_GAP = 44.dp

private fun DrawScope.drawAreas(state: GameState, geometry: BoardGeometry, style: BoardStyle) {
    val half = geometry.spacing / 2
    for (y in 0 until state.height) for (x in 0 until state.width) {
        val by = state.enclosedBy(x, y) ?: continue
        val c = geometry.center(x, y)
        drawRect(style.color(by).copy(alpha = style.areaAlpha), Offset(c.x - half, c.y - half), Size(geometry.spacing, geometry.spacing))
    }
}

private fun DrawScope.drawGrid(state: GameState, geometry: BoardGeometry, style: BoardStyle) {
    val stroke = geometry.spacing * 0.03f
    for (x in 0 until state.width) {
        drawLine(style.grid, geometry.center(x, 0), geometry.center(x, state.height - 1), stroke)
    }
    for (y in 0 until state.height) {
        drawLine(style.grid, geometry.center(0, y), geometry.center(state.width - 1, y), stroke)
    }
}

/** A line between every two live dots of one colour that touch, orthogonally or diagonally. */
private fun DrawScope.drawLinks(state: GameState, geometry: BoardGeometry, style: BoardStyle) {
    val width = geometry.spacing * 0.08f
    for (y in 0 until state.height) for (x in 0 until state.width) {
        val owner = state.ownerAt(x, y) ?: continue
        if (state.isCaptured(x, y)) continue
        for ((dx, dy) in LINK_DIRECTIONS) {
            val nx = x + dx
            val ny = y + dy
            if (!state.inBounds(nx, ny) || state.ownerAt(nx, ny) != owner || state.isCaptured(nx, ny)) continue
            drawLine(style.color(owner).copy(alpha = 0.6f), geometry.center(x, y), geometry.center(nx, ny), width)
        }
    }
}

/** Each touching pair once: right, down, and the two diagonals going right. */
private val LINK_DIRECTIONS = listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)

/**
 * An amber ring around every dot of a group close to capture: solid when one dot away, faint
 * when two. When one dot away, the point that would close the ring gets a small marker.
 */
private fun DrawScope.drawDangers(state: GameState, dangers: List<Danger>, geometry: BoardGeometry, style: BoardStyle) {
    for (danger in dangers) {
        val near = danger.dotsNeeded == 1
        val ring = style.danger.copy(alpha = if (near) 1f else 0.45f)
        for (dot in danger.group) {
            drawRing(geometry.center(dot), geometry.spacing * 0.42f, ring, geometry.spacing * (if (near) 0.09f else 0.06f))
        }
        if (near) {
            val attacker = state.ownerAt(danger.group.first())?.opponent ?: continue
            for (point in danger.closingPoints) {
                drawRing(geometry.center(point), geometry.spacing * 0.16f, style.color(attacker), geometry.spacing * 0.06f)
            }
        }
    }
}

private fun DrawScope.drawDots(state: GameState, geometry: BoardGeometry, style: BoardStyle) {
    val radius = geometry.spacing * style.dotRadius
    for (y in 0 until state.height) for (x in 0 until state.width) {
        val owner = state.ownerAt(x, y) ?: continue
        val color = style.color(owner)
        drawCircle(if (state.isCaptured(x, y)) color.copy(alpha = style.capturedAlpha) else color, radius, geometry.center(x, y))
    }
}

private fun DrawScope.drawRing(center: Offset, radius: Float, color: androidx.compose.ui.graphics.Color, width: Float) {
    drawCircle(color, radius, center, style = Stroke(width))
}
