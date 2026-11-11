package io.github.pavelya.dots.ui.board

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import dots.ai.Danger
import dots.engine.GameState
import dots.engine.Pos
import io.github.pavelya.dots.data.PlacingMethod

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
 * The game's board: [BoardView] plus touch, placing dots by the [input]'s method (decision 0024):
 *
 * - **Two taps:** a tap shows the preview, a second tap on it places the dot.
 * - **Drag:** press, drag to the point and lift to place the dot. While the finger is down, the
 *   preview's row and column are drawn across the board, so they show around the finger. Lifting
 *   it off the board places nothing.
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
    var fingerDown by remember { mutableStateOf(false) }
    // The gestures send to the events as they are now, not as they were when the gesture began.
    val events by rememberUpdatedState(input)

    val gestures = Modifier.pointerInput(method, state.width, state.height) {
        fun pointAt(at: Offset): Pos? =
            BoardGeometry(state.width, state.height, size.width.toFloat(), size.height.toFloat()).pointAt(at)
        when (method) {
            PlacingMethod.TWO_TAPS -> detectTapGestures { at -> events.onTap(pointAt(at)) }
            PlacingMethod.DRAG -> awaitEachGesture {
                val down = awaitFirstDown()
                var at = down.position
                fingerDown = true
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
                        events.onAim(pointAt(at))
                    }
                } finally {
                    fingerDown = false
                }
                events.onRelease(if (lifted) pointAt(at) else null)
            }
        }
    }

    BoardView(
        state = state,
        style = style,
        description = description,
        modifier = modifier.then(gestures),
        lastMove = lastMove,
        preview = preview,
        dangers = dangers,
        marked = marked,
        guides = method == PlacingMethod.DRAG && fingerDown,
    )
}
