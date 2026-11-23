package io.github.pavelya.dots.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import dots.ai.Danger
import dots.engine.GameState
import dots.engine.Pos
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.game.BoardMoment
import io.github.pavelya.dots.game.BoosterMoment
import io.github.pavelya.dots.ui.motion.Ease
import io.github.pavelya.dots.ui.motion.Guides
import io.github.pavelya.dots.ui.motion.LocalReduceMotion
import kotlinx.coroutines.launch

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
 * It decides nothing about the game: which points are playable comes back as the [preview]. The
 * tutorial's [pointer], the boosters' [marks] and the links ([showLinks]) are drawn as [BoardView] draws them. In a booster
 * preview the same touches choose the booster's point (the game decides). Under drag the guides
 * show there only while the finger is over a target, which then holds the booster's mark; over any
 * other point the mark stays where it was chosen and the guides go (motion.md, "Aiming Wall or
 * Bomb by drag").
 *
 * Motion (docs/design/motion.md): the [moment], [edgeMarks], [glow] and [booster] play as [BoardView] plays
 * them. When the finger goes down, drag's guides are drawn out from the ring in 100 ms, then jump
 * with the finger. Every touch calls [onTouch] first, so a moment still playing can jump to its
 * end; the touch counts as usual.
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
    pointer: Pos? = null,
    marks: BoardMarks = BoardMarks.NONE,
    showLinks: Boolean = false,
    moment: BoardMoment? = null,
    time: () -> Float = { Float.MAX_VALUE },
    edgeMarks: Pos? = null,
    glow: AreaGlow? = null,
    booster: BoosterMoment? = null,
    boosterTime: () -> Float = { Float.MAX_VALUE },
    onTouch: () -> Unit = {},
) {
    val method = input.method
    var fingerDown by remember { mutableStateOf(false) }
    // The point under the finger while it is down, placing by drag; null off the board.
    var fingerAt by remember { mutableStateOf<Pos?>(null) }
    // The gestures send to the events as they are now, not as they were when the gesture began.
    val events by rememberUpdatedState(input)
    val touched by rememberUpdatedState(onTouch)
    val reduce = LocalReduceMotion.current
    val guideReach = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val drawOut = {
        if (!reduce) {
            scope.launch {
                guideReach.snapTo(0f)
                guideReach.animateTo(1f, tween(Guides.DRAWN.toInt(), easing = Ease.OUT))
            }
        }
    }

    // Seen before the placing gestures, without taking the touch from them.
    val touches = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            touched()
        }
    }

    val gestures = Modifier.pointerInput(method, state.width, state.height) {
        fun pointAt(at: Offset): Pos? =
            BoardGeometry(state.width, state.height, size.width.toFloat(), size.height.toFloat()).pointAt(at)
        when (method) {
            PlacingMethod.TWO_TAPS -> detectTapGestures { at -> events.onTap(pointAt(at)) }
            PlacingMethod.DRAG -> awaitEachGesture {
                val down = awaitFirstDown()
                var at = down.position
                drawOut()
                fingerDown = true
                fingerAt = pointAt(at)
                events.onAim(fingerAt)
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
                        val point = pointAt(at)
                        if (point != fingerAt) fingerAt = point
                        events.onAim(point)
                    }
                } finally {
                    fingerDown = false
                    fingerAt = null
                }
                events.onRelease(if (lifted) pointAt(at) else null)
            }
        }
    }

    BoardView(
        state = state,
        style = style,
        description = description,
        modifier = modifier.then(touches).then(gestures),
        lastMove = lastMove,
        preview = preview,
        dangers = dangers,
        marked = marked,
        // In a booster preview, only while the finger holds the booster's mark: it is over a target.
        guides = method == PlacingMethod.DRAG && fingerDown && (marks.targets.isEmpty() || (fingerAt != null && fingerAt == marks.point)),
        pointer = pointer,
        marks = marks,
        showLinks = showLinks,
        moment = moment,
        time = time,
        edgeMarks = edgeMarks,
        guideReach = { guideReach.value },
        glow = glow,
        booster = booster,
        boosterTime = boosterTime,
    )
}
