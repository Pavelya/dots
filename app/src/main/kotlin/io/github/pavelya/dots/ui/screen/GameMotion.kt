package io.github.pavelya.dots.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dots.engine.Player
import io.github.pavelya.dots.R
import io.github.pavelya.dots.game.BoardMoment
import io.github.pavelya.dots.ui.board.BoardGeometry
import io.github.pavelya.dots.ui.motion.Capturing
import io.github.pavelya.dots.ui.motion.LocalReduceMotion
import io.github.pavelya.dots.ui.motion.RunningOut
import io.github.pavelya.dots.ui.motion.Timeline
import io.github.pavelya.dots.ui.motion.capturing
import io.github.pavelya.dots.ui.motion.rememberChangeTimeline
import io.github.pavelya.dots.ui.motion.writeIn
import io.github.pavelya.dots.ui.theme.Palette

/**
 * Where the parts a capture's "+N" flies between sit on the screen: the game screen, the board and
 * each side's tally. Kept up to date by layout; read while drawing.
 */
internal class Spots {
    var screen: LayoutCoordinates? = null
    var board: LayoutCoordinates? = null
    val tally = HashMap<Player, LayoutCoordinates>()
}

/**
 * Over the whole game screen, a capture's "+N" (motion.md, "Capture" and "Big capture"): it grows
 * in over the area in the captor's ink, then flies along a curve to the captor's tally, shrinking
 * to 55%. A big capture's +N is bigger and below the area, with the dots counted in words under it
 * ("Six at once!"), which stay a little longer and shrink away.
 */
@Composable
internal fun CaptureOverlay(moment: BoardMoment?, clock: Timeline, columns: Int, rows: Int, spots: Spots, style: Palette) {
    val timing = moment?.capturing ?: return
    val first = moment.captures.first()
    val ink = style.ink(first.by)
    val measurer = rememberTextMeasurer()
    val plusStyle = if (timing.big) NoteText.score.copy(fontSize = 40.sp, color = ink) else NoteText.score.copy(color = ink)
    val wordsStyle = NoteText.display.copy(fontSize = NoteText.headline.fontSize, lineHeight = NoteText.headline.lineHeight, color = ink)
    val words = if (timing.big) bigCaptureWords(moment.captured) else null
    val plus = "+${moment.points}"
    Canvas(Modifier.fillMaxSize()) {
        val t = clock.now
        if (t < timing.plusIn || t >= timing.length) return@Canvas
        val screen = spots.screen ?: return@Canvas
        val board = spots.board ?: return@Canvas
        if (!screen.isAttached || !board.isAttached) return@Canvas
        val geometry = BoardGeometry(columns, rows, board.size.width.toFloat(), board.size.height.toFloat())
        val ring = first.ring.map(geometry::center)
        val cx = ring.sumOf { it.x.toDouble() }.toFloat() / ring.size
        val cy = ring.sumOf { it.y.toDouble() }.toFloat() / ring.size
        val bottom = ring.maxOf { it.y }
        // A big capture's +N sits below the area, to the lower right, clear of the words under it.
        val right = ring.maxOf { it.x }
        val anchor = screen.localPositionOf(board, if (timing.big) Offset((cx + right) / 2 + geometry.spacing, bottom + geometry.spacing * 0.6f) else Offset(cx, cy))
        val tally = spots.tally[first.by]?.takeIf { it.isAttached }
        val target = tally?.let { screen.localPositionOf(it, Offset(it.size.width / 2f, it.size.height / 2f)) } ?: anchor
        if (t < timing.plusLands) {
            val fly = timing.plusFlight(t)
            val control = Offset((anchor.x + target.x) / 2 + 40.dp.toPx(), minOf(anchor.y, target.y) - 24.dp.toPx())
            val at = quadratic(anchor, control, target, fly)
            drawCentred(measurer, plus, plusStyle, at, timing.plusGrow(t) * (1f - 0.45f * fly))
        }
        if (words != null) {
            val size = timing.words(t)
            if (size > 0f) {
                val under = Offset(anchor.x, anchor.y + 34.dp.toPx())
                rotate(-6f, under) { drawCentred(measurer, words, wordsStyle, under, size) }
            }
        }
    }
}

private fun quadratic(a: Offset, c: Offset, b: Offset, p: Float): Offset {
    val q = 1 - p
    return Offset(q * q * a.x + 2 * q * p * c.x + p * p * b.x, q * q * a.y + 2 * q * p * c.y + p * p * b.y)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCentred(measurer: TextMeasurer, text: String, style: TextStyle, at: Offset, size: Float) {
    if (size <= 0f) return
    val layout = measurer.measure(text, style)
    scale(size, size, pivot = at) {
        drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
    }
}

/** A big capture's count in words: "Six at once!". Handwriting is never used for figures. */
@Composable
private fun bigCaptureWords(dots: Int): String {
    val words = stringArrayResource(R.array.big_capture_words)
    return words.getOrNull(dots - Capturing.BIG) ?: stringResource(R.string.big_capture_many)
}

/**
 * The dots the player below the board has left: a big number and a label. From 5 left (motion.md,
 * "Dots running out") the number turns Highlighter, and a row of small pieces in the side's shape
 * and ink shows the last dots: it writes in when the count reaches 5, and with each dot after that
 * the number swells and the last small piece shrinks away. At 1 left, "Last dot!" writes in in
 * place of the number. TalkBack reads the count.
 */
@Composable
internal fun DotsLeft(left: Int, player: Player, style: Palette) {
    val description = pluralStringResource(R.plurals.game_dots_left, left, left)
    val low = left in 1..RunningOut.LOW
    val clock = rememberChangeTimeline(left, RunningOut.LENGTH) { old, new -> new < old && new in 1..RunningOut.LOW }
    val reduce = LocalReduceMotion.current
    val shrinking by remember(clock) { derivedStateOf { clock.now < RunningOut.PIECE } }
    val ink by animateColorAsState(if (low) style.highlighter else style.pencil, if (reduce) snap() else tween(RunningOut.COLOUR.toInt()), label = "dots left")
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        if (low) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                // The row writes in when the count reaches 5; after that the piece just used shrinks away.
                val shown = if (left < RunningOut.LOW && shrinking) left + 1 else left
                for (i in 0 until shown) {
                    Piece(
                        player,
                        style,
                        Modifier.size(8.dp).graphicsLayer {
                            val size = when {
                                i == left -> RunningOut.pieceOut(clock.now)
                                left == RunningOut.LOW -> RunningOut.pieceIn(clock.now, i)
                                else -> 1f
                            }
                            scaleX = size
                            scaleY = size
                        },
                    )
                }
            }
        }
        if (left == 1) {
            Text(
                stringResource(R.string.game_last_dot),
                style = HAND,
                color = style.highlighter,
                modifier = Modifier.writeIn { RunningOut.lastDot(clock.now) },
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    left.toString(),
                    style = NoteText.score,
                    color = ink,
                    modifier = Modifier.alignByBaseline().graphicsLayer {
                        val size = if (left < RunningOut.LOW) RunningOut.numberScale(clock.now) else 1f
                        scaleX = size
                        scaleY = size
                    },
                )
                Text(
                    pluralStringResource(R.plurals.game_dots_left_label, left),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = style.graphite,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}
