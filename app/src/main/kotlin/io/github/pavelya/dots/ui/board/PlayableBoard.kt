package io.github.pavelya.dots.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dots.ai.Danger
import dots.engine.GameState
import dots.engine.Pos
import io.github.pavelya.dots.data.PlacingMethod
import kotlin.math.max

/**
 * How the board's touches reach the game: the placing [method] and the three events it sends
 * (GameViewModel's onTap, aim and release). [onTap] is a whole tap under the two-tap rule; [onAim]
 * moves the preview while a finger is down; [onRelease] ends a touch asking for a dot at a point
 * (null: no dot).
 */
class BoardInput(
    val method: PlacingMethod,
    val onTap: (Pos?) -> Unit,
    val onAim: (Pos?) -> Unit,
    val onRelease: (Pos?) -> Unit,
) {
    companion object {
        /** A board that ignores touch, for previews and tests. */
        val NONE = BoardInput(PlacingMethod.TWO_TAPS, {}, {}, {})
    }
}

/**
 * The game's board: [BoardView] plus touch, placing dots by the [input]'s method (A-13 compares
 * them; see [PlacingMethod]):
 *
 * - **Two taps:** a tap shows the preview, a second tap on it places the dot.
 * - **Zoom:** a tap shows the preview and zooms the board in around it, to [ZOOM_SPACING] a point;
 *   the next tap places a dot where it lands and zooms out. A tap on a taken point zooms out.
 * - **Magnifier:** while a finger is down, a magnifier above it shows the point it is on; lifting
 *   the finger places the dot there. Lifting it off the board places nothing.
 * - **Drag:** as the magnifier, but the preview's row and column are drawn across the board instead.
 * - **Long press:** holding a point draws its row and column out to the edges over [HOLD_MILLIS];
 *   when they arrive, the dot is placed. Lifting the finger earlier places nothing.
 *
 * It decides nothing about the game: which points are playable comes back as the [preview].
 */
@Composable
fun PlayableBoard(
    state: GameState,
    style: BoardStyle,
    description: String,
    input: BoardInput,
    preview: Pos?,
    modifier: Modifier = Modifier,
    lastMove: Pos? = null,
    dangers: List<Danger> = emptyList(),
    marked: Pos? = null,
) {
    val method = input.method
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val zoomSpacing = with(LocalDensity.current) { ZOOM_SPACING.toPx() }

    // Zoom: in while a preview shows, around it; out again around the same point.
    var lastPreview by remember { mutableStateOf<Pos?>(null) }
    SideEffect { if (preview != null) lastPreview = preview }
    val zoomAround = preview ?: lastPreview
    val zoomTarget = if (method == PlacingMethod.ZOOM && preview != null && boardSize != IntSize.Zero) {
        zoomFactor(BoardGeometry(state.width, state.height, boardSize.width.toFloat(), boardSize.height.toFloat()).spacing, zoomSpacing)
    } else {
        1f
    }
    val zoom by animateFloatAsState(zoomTarget, tween(ZOOM_MILLIS), label = "zoom")

    // A finger that is down, for the magnifier and the drag guides.
    var finger by remember { mutableStateOf<Offset?>(null) }

    // Long press: each new hold restarts the guides from the point.
    var holdCount by remember { mutableIntStateOf(0) }
    var holding by remember { mutableStateOf(false) }
    val hold = remember { Animatable(0f) }
    LaunchedEffect(holdCount) {
        hold.snapTo(0f)
        if (holding) hold.animateTo(1f, tween(HOLD_MILLIS.toInt(), easing = LinearEasing))
    }

    // The gestures read these as they are now, not as they were when the gesture began.
    val zoomNow by rememberUpdatedState(zoomTarget)
    val zoomAroundNow by rememberUpdatedState(zoomAround)
    val currentPreview by rememberUpdatedState(preview)
    val events by rememberUpdatedState(input)

    val gestures = Modifier.pointerInput(method, state.width, state.height) {
        // Touches land on the board as it will be once a zoom has finished.
        fun pointAt(at: Offset): Pos? {
            val plain = BoardGeometry(state.width, state.height, size.width.toFloat(), size.height.toFloat())
            val around = zoomAroundNow
            return (if (zoomNow > 1f && around != null) plain.zoomed(zoomNow, around) else plain).pointAt(at)
        }
        when (method) {
            PlacingMethod.TWO_TAPS -> detectTapGestures { at -> events.onTap(pointAt(at)) }
            PlacingMethod.ZOOM -> detectTapGestures { at ->
                val point = pointAt(at)
                if (currentPreview == null) events.onTap(point) else events.onRelease(point)
            }
            PlacingMethod.MAGNIFIER, PlacingMethod.DRAG -> awaitEachGesture {
                val down = awaitFirstDown()
                var at = down.position
                finger = at
                events.onAim(pointAt(at))
                var lifted = false
                try {
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        at = change.position
                        change.consume()
                        if (!change.pressed) {
                            lifted = true
                            break
                        }
                        finger = at
                        events.onAim(pointAt(at))
                    }
                } finally {
                    finger = null
                }
                events.onRelease(if (lifted) pointAt(at) else null)
            }
            PlacingMethod.LONG_PRESS -> awaitEachGesture {
                val down = awaitFirstDown()
                var point = pointAt(down.position)
                events.onAim(point)
                holding = true
                holdCount++
                try {
                    while (true) {
                        when (val next = nextHoldEvent(down.id, point) { pointAt(it) }) {
                            HoldEvent.Held -> if (point != null && point == currentPreview) {
                                events.onRelease(point)
                                break
                            }
                            is HoldEvent.Moved -> {
                                point = next.point
                                events.onAim(point)
                                holdCount++
                            }
                            HoldEvent.Lifted -> {
                                events.onRelease(null)
                                return@awaitEachGesture
                            }
                        }
                    }
                } finally {
                    holding = false
                    holdCount++
                }
                // Placed: the rest of this touch does nothing.
                do {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
            }
        }
    }

    val touch = when (method) {
        PlacingMethod.TWO_TAPS -> BoardTouch()
        PlacingMethod.ZOOM -> BoardTouch(zoom = zoom, zoomAround = zoomAround)
        PlacingMethod.MAGNIFIER -> BoardTouch(finger = finger)
        PlacingMethod.DRAG -> BoardTouch(guides = if (finger != null) 1f else 0f)
        PlacingMethod.LONG_PRESS -> BoardTouch(guides = if (holding) hold.value else 0f)
    }
    BoardView(
        state = state,
        style = style,
        description = description,
        modifier = modifier.onSizeChanged { boardSize = it }.then(gestures),
        lastMove = lastMove,
        preview = preview,
        dangers = dangers,
        marked = marked,
        touch = touch,
    )
}

/** What happens next while a finger holds a point. */
private sealed interface HoldEvent {
    /** The finger stayed on the point for [HOLD_MILLIS]. */
    data object Held : HoldEvent

    /** The finger moved onto another point, or off the board (null). */
    data class Moved(val point: Pos?) : HoldEvent

    /** The finger was lifted. */
    data object Lifted : HoldEvent
}

/** Waits up to [HOLD_MILLIS] for pointer [id], holding [point], to move to another point or lift. */
private suspend fun AwaitPointerEventScope.nextHoldEvent(id: PointerId, point: Pos?, pointAt: (Offset) -> Pos?): HoldEvent =
    withTimeoutOrNull(HOLD_MILLIS) {
        var next: HoldEvent? = null
        while (next == null) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == id }
            next = when {
                change == null -> HoldEvent.Lifted
                !change.pressed -> HoldEvent.Lifted.also { change.consume() }
                else -> {
                    change.consume()
                    val now = pointAt(change.position)
                    if (now != point) HoldEvent.Moved(now) else null
                }
            }
        }
        requireNotNull(next)
    } ?: HoldEvent.Held

/** The spacing the zoom method enlarges the board to: Android's 48 dp touch target. */
internal val ZOOM_SPACING = 48.dp

/** The zoom method enlarges the board at least this much, also where points are already 48 dp apart. */
private const val MIN_ZOOM = 1.5f

/** How long the board takes to zoom in or out. */
private const val ZOOM_MILLIS = 200

/** How long a finger holds a point to place a dot by long press: Android's long-press time. */
internal const val HOLD_MILLIS = 400L

/** How much the zoom method enlarges a board whose points are [spacing] apart, to reach [target]. */
internal fun zoomFactor(spacing: Float, target: Float): Float = max(target / spacing, MIN_ZOOM)
