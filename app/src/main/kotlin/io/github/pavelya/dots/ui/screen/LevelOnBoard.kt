package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import dots.campaign.Level
import dots.campaign.Mission
import dots.campaign.MissionStatus
import dots.campaign.Outcome
import io.github.pavelya.dots.R
import io.github.pavelya.dots.ui.theme.Palette

/** The most boxes the mission note ticks; a bigger capture mission counts in words. */
private const val MAX_BOXES = 8

/**
 * What the corner note says during a level (canvas frames `game-threat` and `result-win`):
 * "Mission", or "Mission done" once passed, the mission in one line, and how it stands. A capture
 * mission has a box per dot, ticked as they fall, and the moves left; a survive mission the moves
 * still to go. TalkBack reads it as one sentence.
 */
@Composable
fun MissionNoteContent(level: Level, status: MissionStatus) {
    val style = Palette.current
    val mission = level.mission
    val head = stringResource(if (status.outcome == Outcome.PASSED) R.string.mission_done else R.string.mission_head)
    val text = missionText(mission)
    val capture = mission as? Mission.Capture
    val movesLeft = status.movesLeft?.takeIf { status.outcome == Outcome.PLAYING }
    val progress = capture?.let { stringResource(R.string.result_progress, status.captured.coerceAtMost(it.dots), it.dots) }
    val left = movesLeft?.let { pluralStringResource(R.plurals.mission_moves_left, it, it) }
    val description = listOfNotNull(head, text, progress, left).joinToString(". ")
    Column(Modifier.clearAndSetSemantics { contentDescription = description; liveRegion = LiveRegionMode.Polite }) {
        Text(head, style = NoteText.small, color = style.graphite)
        Text(text, style = NoteText.label, color = style.pencil)
        if (capture != null && capture.dots <= MAX_BOXES) {
            Boxes(capture.dots, status.captured.coerceAtMost(capture.dots), style, Modifier.padding(top = 3.dp))
        } else if (progress != null) {
            Text(progress, style = NoteText.small, color = style.red)
        }
        left?.let { Text(it, style = NoteText.small, color = style.graphite) }
    }
}

/** One box per dot to capture, the first [ticked] filled red with a tick (draw.py, `mission_note`). */
@Composable
private fun Boxes(count: Int, ticked: Int, style: Palette, modifier: Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = 3.dp
        val side = min(12.dp, (maxWidth - gap * (count - 1)) / count)
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            repeat(count) { i -> MissionBox(i < ticked, style, Modifier.size(side)) }
        }
    }
}

@Composable
private fun MissionBox(ticked: Boolean, style: Palette, modifier: Modifier) {
    Canvas(modifier) {
        val u = size.width / 12f
        val inset = 0.75f * u
        val boxSize = Size(size.width - 2 * inset, size.height - 2 * inset)
        val corner = CornerRadius(2.5f * u)
        if (ticked) {
            drawRoundRect(style.red, Offset(inset, inset), boxSize, corner)
            val tick = Path().apply {
                moveTo(2.5f * u, 6.5f * u)
                lineTo(5f * u, 9f * u)
                lineTo(10f * u, 3f * u)
            }
            drawPath(tick, style.paper, style = Stroke(width = 1.8f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            drawRoundRect(style.graphite, Offset(inset, inset), boxSize, corner, style = Stroke(width = 1.5f * u))
        }
    }
}

/**
 * The level opening (canvas frame `game-start`): a taped note over the board with the level's
 * name, its mission and the computer's head start, and a boss's rule. It shows for a moment and
 * takes no taps, so the board under it does; then the mission sits in the corner note. TalkBack
 * reads it as it appears.
 */
@Composable
fun LevelOpening(level: Level, modifier: Modifier = Modifier) {
    val style = Palette.current
    TapedNote(style, modifier.semantics { liveRegion = LiveRegionMode.Polite }, angle = -1.5f) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.level_title, level.world, level.number), style = NoteText.label, color = style.graphite)
            Text(level.name, style = NoteText.display, color = if (level.rule != null) style.red else style.blue, textAlign = TextAlign.Center)
            MissionLine(level, style, Modifier.padding(top = 4.dp))
            Text(
                headStartSentence(level),
                style = NoteText.body,
                color = style.graphite,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            level.rule?.let {
                Text(ruleText(it), style = NoteText.body, color = style.red, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** The mission in one line after the red target icon, as the Level card and the opening show it. */
@Composable
internal fun MissionLine(level: Level, style: Palette, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        Icon(
            remember(style) { Doodles.icon(Doodles.TARGET, style.red) },
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp),
        )
        Text(missionText(level.mission), style = NoteText.title, color = style.pencil)
    }
}
