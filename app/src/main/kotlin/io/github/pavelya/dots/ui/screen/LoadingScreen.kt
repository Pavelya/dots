package io.github.pavelya.dots.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pavelya.dots.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The loading screen (the wireframes' Loading frame; docs/design/flows.md, Loading): the app icon
 * on paper, its red ring drawing itself while the game loads, the name, and a one-line [tip]. No
 * buttons. [onDrawn] runs once the ring is closed; the app leaves this screen when that has
 * happened and the game has loaded.
 *
 * Android's own start-up screen shows the same icon (res/drawable/loading_icon.xml) at the same
 * size, in the middle of the window, so this screen takes over without a jump. [outside] is the
 * part of the window around this screen (the system bars), so the icon is put in the middle of the
 * window, not of this screen.
 */
@Composable
fun LoadingScreen(tip: String, onDrawn: () -> Unit, outside: WindowInsets = WindowInsets(0)) {
    val ring = remember { Animatable(0f) }
    val lastDot = remember { Animatable(0f) }
    val words = remember { Animatable(0f) }
    val drawn by rememberUpdatedState(onDrawn)
    LaunchedEffect(Unit) {
        launch { words.animateTo(1f, tween(WORDS_MS, easing = LinearOutSlowInEasing)) }
        val dot = launch {
            snapshotFlow { ring.value >= LAST_DOT_AT }.first { it }
            lastDot.animateTo(1f, tween(DOT_MS, easing = LinearOutSlowInEasing))
        }
        ring.animateTo(1f, tween(RING_MS, easing = FastOutSlowInEasing))
        dot.join()
        drawn()
    }
    LoadingPicture(tip, ring = { ring.value }, lastDot = { lastDot.value }, words = { words.value }, outside = outside)
}

/** How long the ring takes to draw: the pen stroke of a capture (docs/design/motion.md), a little slower. */
private const val RING_MS = 600

/** The fourth dot lands when the ring reaches it, three quarters of the way round, and grows as a placed dot does. */
private const val LAST_DOT_AT = 0.75f
private const val DOT_MS = 200

/** The name and the tip fade in, so nothing appears at once beside the icon Android showed. */
private const val WORDS_MS = 200

/** The tile's side. It fits the circle, 192 dp across, that Android's start-up screen clips its icon to. */
private val TILE = 148.dp

/** The start-up screen's icon square, which res/drawable/loading_icon.xml fills. */
private val ICON_BOX = 288.dp
private val NAME_GAP = 14.dp
private val TIP_SIDE = 32.dp
private val TIP_BOTTOM = 56.dp

/**
 * The loading screen at one moment: [ring] drawn from 0 to 1, from the top dot clockwise, [lastDot]
 * grown from 0 to 1, the name and the tip faded in by [words]. The values are read only when drawn.
 */
@Composable
internal fun LoadingPicture(
    tip: String,
    ring: () -> Float,
    lastDot: () -> Float,
    words: () -> Float,
    outside: WindowInsets = WindowInsets(0),
) {
    val loading = stringResource(R.string.loading)
    val ink = MaterialTheme.colorScheme
    Layout(
        content = {
            Box(Modifier.size(ICON_BOX)) {
                Image(painterResource(R.drawable.loading_icon), contentDescription = null, modifier = Modifier.fillMaxSize())
                Canvas(Modifier.fillMaxSize()) { drawRing(ring(), lastDot()) }
            }
            Text(
                stringResource(R.string.app_name),
                style = HAND.copy(fontSize = 48.sp, lineHeight = 56.sp),
                color = ink.onSurface,
                modifier = Modifier.graphicsLayer { alpha = words() },
            )
            Text(
                tip,
                style = NoteText.body,
                color = ink.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = words() },
            )
        },
        modifier = Modifier.fillMaxSize().background(colorResource(R.color.paper)).semantics { contentDescription = loading },
    ) { (icon, name, hint), constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val iconPlaced = icon.measure(Constraints.fixed(ICON_BOX.roundToPx(), ICON_BOX.roundToPx()))
        val namePlaced = name.measure(Constraints(maxWidth = width))
        val hintPlaced = hint.measure(Constraints(maxWidth = (width - 2 * TIP_SIDE.roundToPx()).coerceAtLeast(0)))
        // The window's middle, in this screen's coordinates.
        val centerX = width / 2 + (outside.getRight(this, layoutDirection) - outside.getLeft(this, layoutDirection)) / 2
        val centerY = height / 2 + (outside.getBottom(this) - outside.getTop(this)) / 2
        layout(width, height) {
            iconPlaced.place(centerX - iconPlaced.width / 2, centerY - iconPlaced.height / 2)
            val nameTop = centerY + TILE.roundToPx() / 2 + NAME_GAP.roundToPx()
            namePlaced.place(centerX - namePlaced.width / 2, nameTop)
            // At the bottom, but never over the name, even with large text.
            val hintTop = maxOf(height - TIP_BOTTOM.roundToPx() - hintPlaced.height, nameTop + namePlaced.height + NAME_GAP.roundToPx())
            hintPlaced.place((width - hintPlaced.width) / 2, hintTop)
        }
    }
}

private val RED = Color(0xFFC0392B)
private val BLUE = Color(0xFF1F4E9C)

/** The icon's ring, in its own 108-unit coordinates (docs/design/brand/pinfold-icon.svg), from the top dot clockwise. */
private val RING = Path().apply {
    moveTo(54f, 41f)
    lineTo(67f, 54f)
    lineTo(54f, 67f)
    lineTo(41f, 54f)
    close()
}

/** The red dots on the icon before the ring is drawn: top, right, bottom. */
private val RED_DOTS = listOf(Offset(54f, 41f), Offset(67f, 54f), Offset(54f, 67f))

/** The fourth red dot, on the left; the ring reaches it last. */
private val LAST_DOT = Offset(41f, 54f)

/**
 * Draws the part of the ring that is [drawn] (0 to 1) over the icon square, then the dots over it as
 * on the icon: the three of res/drawable/loading_icon.xml again, the fourth grown to [lastDot], the
 * blue dot.
 */
private fun DrawScope.drawRing(drawn: Float, lastDot: Float) {
    val unit = TILE.toPx() / 72f
    // The tile shows the middle 72 units of the icon's 108, so icon unit 18 is the tile's edge.
    val origin = (size.width - TILE.toPx()) / 2f - 18f * unit
    translate(origin, origin) {
        scale(unit, pivot = Offset.Zero) {
            val stroke = Stroke(width = 2.75f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when {
                drawn >= 1f -> drawPath(RING, RED, style = stroke)
                drawn > 0f -> {
                    val measure = PathMeasure().apply { setPath(RING, false) }
                    val part = Path()
                    measure.getSegment(0f, measure.length * drawn, part, true)
                    drawPath(part, RED, style = stroke)
                }
            }
            for (dot in RED_DOTS) drawCircle(RED, 4.25f, dot)
            if (lastDot > 0f) drawCircle(RED, 4.25f * lastDot, LAST_DOT)
            drawCircle(BLUE, 3.75f, Offset(54f, 54f))
        }
    }
}
