package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pavelya.dots.R
import io.github.pavelya.dots.ui.motion.NoteMotion
import io.github.pavelya.dots.ui.motion.rememberNoteMotion
import io.github.pavelya.dots.ui.theme.Palette

/*
 * Stop and Done in an open-ended game (decision 0025; flows.md, "Free play and two players"; the
 * canvas frames `game-free-open`, `game-free-stop`, `game-computer-stops` and `game-two-done`).
 */

/**
 * Stop's note (canvas frame `game-free-stop`): taped over the player's line and the tool bar, as a
 * booster preview is, so the board stays where it is. "Stop? The computer takes what is left." (or
 * [other] takes it, between two people), how many of the player's dots can still be captured
 * ([open], circled on the board), then Cancel and Stop here. The screen slides it up from below
 * and down as it leaves ([motion]); while it leaves, the tool bar under it takes taps again.
 */
@Composable
internal fun StopNote(
    open: Int,
    other: String?,
    style: Palette,
    onCancel: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    motion: NoteMotion = rememberNoteMotion(),
) {
    val question = stringResource(R.string.stop_question)
    val takes = if (other == null) stringResource(R.string.stop_computer_takes) else stringResource(R.string.stop_other_takes, other)
    val risk = if (open == 0) stringResource(R.string.stop_all_safe) else pluralStringResource(R.plurals.stop_open_dots, open, open)
    Box(
        modifier
            .rotate(-0.6f)
            .then(if (motion.open) Modifier.pointerInput(Unit) { detectTapGestures { } } else Modifier) // the tool bar under it takes no taps
            .semantics { paneTitle = question },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RectangleShape, clip = false)
                .background(style.card)
                .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 6.dp), // as tight as the canvas: the board's last row shows
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(remember(style) { Doodles.icon(Doodles.FLAG, style.blue, width = 2f) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
                BasicText(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = style.blue, fontWeight = FontWeight.Bold)) { append(question) }
                        append(' ')
                        append(takes)
                    },
                    style = NoteText.body.copy(color = style.pencil, fontSize = 14.sp, lineHeight = 22.sp),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 14.sp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 2.dp),
            ) {
                OpenRing(style, Modifier.size(16.dp))
                Text(risk, style = NoteText.small.copy(fontWeight = FontWeight.Bold), color = style.graphite)
            }
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallButton(
                    stringResource(R.string.stop_cancel),
                    style,
                    onCancel,
                    Modifier.weight(1f),
                    height = 56.dp,
                    padding = PaddingValues(horizontal = 4.dp), // "Cancel" fits a third of a 320 dp phone
                )
                BigButton(stringResource(R.string.stop_here), style, onStop, Modifier.weight(2f))
            }
        }
        Tape(style, Modifier.offset(x = 20.dp, y = (-8).dp).rotate(-3f).size(width = 44.dp, height = 16.dp))
    }
}

/** The dashed Highlighter ring of a dot that can still be captured, as a glyph in a line of text (draw.py, `open_ring`). */
@Composable
private fun OpenRing(style: Palette, modifier: Modifier) {
    val ink = style.highlighter
    Canvas(modifier) {
        val width = 1.5.dp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5.dp.toPx()))
        drawCircle(ink, size.minDimension / 2 - width, style = Stroke(width, pathEffect = dash))
    }
}

/**
 * Beside the name of the side that stopped, until the game ends: a flag and "stopped" in Graphite
 * (draw.py, `stopped_chip`). TalkBack reads "[name] stopped".
 */
@Composable
internal fun StoppedChip(name: String, style: Palette, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.game_stopped_description, name)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .height(26.dp)
            .border(1.5.dp, style.graphite, RoundedCornerShape(13.dp))
            .padding(start = 6.dp, end = 9.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Icon(remember(style) { Doodles.icon(Doodles.FLAG, style.graphite, width = 2f) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
        Text(stringResource(R.string.game_stopped), style = NoteText.small.copy(fontWeight = FontWeight.Bold), color = style.graphite, maxLines = 1)
    }
}

/**
 * A tool of free play and two players (decision 0023; draw.py, `free_tool`): its icon ([path]) and
 * its [name], in the campaign's tool shape, with no count. When it cannot be used, its outline is
 * dashed and its ink grey, as an empty booster on the canvas.
 */
@Composable
internal fun FreeTool(name: String, path: String, enabled: Boolean, style: Palette, onClick: () -> Unit, modifier: Modifier) {
    val ink = if (enabled) style.blue else style.graphite
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(56.dp)
            .clip(TOOL_SHAPE)
            .background(style.card)
            .toolOutline(if (enabled) style.bluePale else style.graphite, dashed = !enabled, TOOL_SHAPE)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Icon(remember(path, ink) { Doodles.icon(path, ink) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
        Text(name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ink, maxLines = 1)
    }
}

/**
 * For the side that collects after the other stopped, in place of the tool bar (draw.py,
 * `collect_bar`): Undo in free play ([onUndo]; none between two people), what to do ([line]), and
 * Done, the one big button.
 */
@Composable
internal fun CollectRow(line: String, onUndo: (() -> Unit)?, undoEnabled: Boolean, style: Palette, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        onUndo?.let { undo ->
            val ink = if (undoEnabled) style.blue else style.graphite
            val label = stringResource(R.string.game_undo)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(TOOL_SHAPE)
                    .background(style.card)
                    .toolOutline(if (undoEnabled) style.bluePale else style.graphite, dashed = !undoEnabled, TOOL_SHAPE)
                    .clickable(enabled = undoEnabled, role = Role.Button, onClick = undo)
                    .semantics { contentDescription = label },
            ) {
                Icon(remember(ink) { Doodles.icon(Doodles.UNDO, ink) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
            }
        }
        Text(line, style = NoteText.label, color = style.pencil, modifier = Modifier.weight(1f))
        BigButton(
            stringResource(R.string.game_done),
            style,
            onDone,
            Modifier.width(DONE_WIDTH),
            after = Doodles.TICK,
            padding = PaddingValues(horizontal = 8.dp),
        )
    }
}

/** Done's width, as the canvas draws it. */
private val DONE_WIDTH = 118.dp
