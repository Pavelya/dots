package io.github.pavelya.dots.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dots.engine.GameState
import dots.engine.Pos

/**
 * Draws a position: grid, enclosed areas shaded in the colour of the player who closed them, live
 * and captured dots, the last move, and a placement preview. It decides nothing about the game;
 * a tap is reported as the board point it hit (or null when off the board).
 */
@Composable
fun BoardView(
    state: GameState,
    style: BoardStyle,
    description: String,
    modifier: Modifier = Modifier,
    lastMove: Pos? = null,
    preview: Pos? = null,
    onTap: ((Pos?) -> Unit)? = null,
) {
    val tapModifier = if (onTap == null) Modifier else Modifier.pointerInput(state.width, state.height) {
        detectTapGestures { at ->
            onTap(BoardGeometry(state.width, state.height, size.width.toFloat(), size.height.toFloat()).pointAt(at))
        }
    }
    Canvas(
        modifier = modifier
            .aspectRatio(state.width.toFloat() / state.height)
            .semantics { contentDescription = description }
            .then(tapModifier),
    ) {
        val geometry = BoardGeometry(state.width, state.height, size.width, size.height)
        drawRect(style.background)
        drawAreas(state, geometry, style)
        drawGrid(state, geometry, style)
        drawDots(state, geometry, style)
        lastMove?.let { drawRing(geometry.center(it), geometry.spacing * 0.42f, style.grid, geometry.spacing * 0.06f) }
        preview?.let {
            val color = style.color(state.toMove)
            drawCircle(color.copy(alpha = 0.45f), geometry.spacing * style.dotRadius, geometry.center(it))
            drawRing(geometry.center(it), geometry.spacing * 0.45f, color, geometry.spacing * 0.08f)
        }
    }
}

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
