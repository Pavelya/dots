package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pavelya.dots.R
import io.github.pavelya.dots.ui.theme.Palette
import kotlin.math.roundToInt

/**
 * The menu screens' paper (theme-notebook.md, Menu screens; the wireframes' Settings and Settings
 * details): squared paper with a line every 24 dp, the notebook margin line, Back and a
 * handwritten [title] at the top, and the [content] in a column right of the margin that scrolls
 * under the header, so Back never needs scrolling.
 */
@Composable
internal fun PaperPage(
    title: String,
    onBack: () -> Unit,
    backLabel: String = stringResource(R.string.back),
    content: @Composable ColumnScope.() -> Unit,
) {
    val style = Palette.current
    Column(Modifier.fillMaxSize().squaredPaper(style)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = backLabel },
            ) {
                Icon(remember(style) { Doodles.icon(Doodles.BACK, style.blue, width = 2f) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(26.dp))
            }
            // A long title shrinks to fit, never below the 24 sp that Caveat needs.
            BasicText(
                title,
                style = PAGE_TITLE.copy(color = style.blue),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 34.sp),
                modifier = Modifier.semantics { heading() },
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 64.dp, end = 16.dp, bottom = 16.dp),
            content = content,
        )
    }
}

/**
 * The menu screens' paper (theme-notebook.md, "Menu screens"): Paper, Grid lines 1 dp wide every
 * 24 dp, and one Margin line at 58 dp from the left, placed as the canvas places them. The margin
 * line never crosses text or buttons: a screen whose content starts further left goes without it
 * ([margin] false).
 */
internal fun Modifier.squaredPaper(style: Palette, margin: Boolean = true): Modifier {
    val grid = style.grid
    val marginInk = style.margin
    return background(style.paper).drawBehind {
        val step = 24.dp.toPx()
        val line = 1.dp.toPx()
        // Each line's 1 dp band ends where its 24 dp square ends (the canvas's -1 dp offset).
        val first = step - line / 2 - line
        var x = first
        while (x < size.width) {
            drawLine(grid, Offset(x, 0f), Offset(x, size.height), line)
            x += step
        }
        var y = first
        while (y < size.height) {
            drawLine(grid, Offset(0f, y), Offset(size.width, y), line)
            y += step
        }
        if (margin) {
            val left = MARGIN_LINE.toPx() + line / 2
            drawLine(marginInk, Offset(left, 0f), Offset(left, size.height), line)
        }
    }
}

/** Where the notebook margin line is drawn on the menu screens (the canvas's map frame). */
internal val MARGIN_LINE = 58.dp

/** A section's name in red handwriting, sitting on the bottom of its 40 dp line. */
@Composable
internal fun SectionHead(text: String) {
    val style = Palette.current
    Box(contentAlignment = Alignment.BottomStart, modifier = Modifier.fillMaxWidth().height(40.dp).semantics { heading() }) {
        Text(text, style = HAND, color = style.red, maxLines = 1, modifier = Modifier.padding(bottom = 4.dp))
    }
}

/**
 * A volume from 0 to [max]: its [label], then a slider. The slider moves freely; the new volume is
 * saved ([onChange]) when the finger lifts.
 */
@Composable
internal fun VolumeRow(label: String, volume: Int, max: Int, onChange: (Int) -> Unit) {
    val style = Palette.current
    var moving by remember(volume) { mutableFloatStateOf(volume.toFloat()) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Text(label, style = ROW_TEXT, color = style.pencil, modifier = Modifier.width(104.dp).clearAndSetSemantics {})
        Slider(
            value = moving,
            onValueChange = { moving = it },
            onValueChangeFinished = { onChange(moving.roundToInt()) },
            valueRange = 0f..max.toFloat(),
            colors = SliderDefaults.colors(thumbColor = style.blue, activeTrackColor = style.blue, inactiveTrackColor = style.blue.copy(alpha = 0.2f)),
            modifier = Modifier.weight(1f).semantics { contentDescription = label },
        )
    }
}

/** A setting that is on or off: its [label], and a hand-drawn switch on the right. The whole row toggles. */
@Composable
internal fun SwitchRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val style = Palette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).toggleable(value = on, role = Role.Switch, onValueChange = onChange),
    ) {
        Text(label, style = ROW_TEXT, color = style.pencil, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .size(width = 48.dp, height = 28.dp)
                .clip(SWITCH_SHAPE)
                .background(if (on) style.blue.copy(alpha = 0.18f) else style.card)
                .border(2.dp, style.blue, SWITCH_SHAPE),
        ) {
            Box(
                Modifier
                    .offset(x = if (on) 25.dp else 5.dp, y = 5.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (on) style.blue else style.card)
                    .border(2.dp, style.blue, CircleShape),
            )
        }
    }
}

/** One of several choices: a hand-drawn radio circle, its [label] and a [detail] line under it. */
@Composable
internal fun RadioRow(label: String, detail: String?, selected: Boolean, onClick: () -> Unit) {
    val style = Palette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(22.dp).border(2.dp, style.blue, CircleShape)) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(style.blue))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = ROW_TEXT, color = style.pencil)
            detail?.let { Text(it, style = NoteText.small, color = style.graphite) }
        }
    }
}

/** A row that opens another page or does something: its [label], a [detail] in grey, and an arrow. */
@Composable
internal fun LinkRow(label: String, detail: String? = null, onClick: () -> Unit) {
    val style = Palette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .dashedUnderline(style)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(label, style = ROW_TEXT, color = style.pencil, modifier = Modifier.weight(1f))
        detail?.let { Text(it, style = NoteText.small, color = style.graphite) }
        Icon(remember(style) { Doodles.icon(Doodles.CHEVRON, style.blue, width = 2f) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(18.dp))
    }
}

/** A line of information that does nothing when tapped: its [label] and a [detail] in grey, beside it or, for a long one, [detailBelow]. */
@Composable
internal fun InfoRow(label: String, detail: String, detailBelow: Boolean = false) {
    val style = Palette.current
    val row = Modifier.fillMaxWidth().heightIn(min = 52.dp).dashedUnderline(style).semantics(mergeDescendants = true) {}
    if (detailBelow) {
        Column(row.padding(vertical = 6.dp), verticalArrangement = Arrangement.Center) {
            Text(label, style = ROW_TEXT.copy(fontSize = 16.sp), color = style.pencil)
            Text(detail, style = NoteText.small, color = style.graphite)
        }
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = row,
    ) {
        Text(label, style = ROW_TEXT.copy(fontSize = 16.sp), color = style.pencil, modifier = Modifier.weight(1f).padding(vertical = 4.dp))
        Text(detail, style = NoteText.small, color = style.graphite)
    }
}

/** A short text on a white card laid on the page: what a Settings details page is about. */
@Composable
internal fun IntroCard(text: String) {
    val style = Palette.current
    Text(
        text,
        style = NoteText.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
        color = style.pencil,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
            .shadow(2.dp, RectangleShape, clip = false)
            .background(style.card)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

private fun Modifier.dashedUnderline(style: Palette): Modifier = drawBehind {
    val y = size.height - 0.5.dp.toPx()
    drawLine(
        style.blue.copy(alpha = 0.25f),
        Offset(0f, y),
        Offset(size.width, y),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
    )
}

/** The wireframes' Caveat page title, 34 sp. */
private val PAGE_TITLE: TextStyle
    @Composable @ReadOnlyComposable
    get() = NoteText.display.copy(fontSize = 34.sp, lineHeight = 40.sp)

/** A row's text on the menu screens. */
internal val ROW_TEXT: TextStyle
    @Composable @ReadOnlyComposable
    get() = NoteText.label.copy(fontSize = 15.sp)

/** The switch's hand-cut track. */
private val SWITCH_SHAPE = RoundedCornerShape(topStart = 14.dp, topEnd = 16.dp, bottomEnd = 13.dp, bottomStart = 15.dp)
