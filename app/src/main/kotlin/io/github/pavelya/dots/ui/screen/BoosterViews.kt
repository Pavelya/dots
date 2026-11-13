package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dots.engine.Booster
import dots.engine.Player
import io.github.pavelya.dots.R
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.game.BoosterAim
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.ui.theme.Palette

/**
 * One booster in the campaign's tool bar: how many the player has [left], whether it can be
 * [usable] now, and, for Undo before the player's first move, [waiting] for a turn to take back.
 */
internal data class BoosterTool(val booster: Booster, val left: Int, val usable: Boolean, val waiting: Boolean = false)

/** The five boosters of [ui]'s level for the [human] player, in the engine's order: Undo last, nearest the thumb. */
internal fun boosterTools(ui: GameUiState, human: Player): List<BoosterTool> = Booster.entries.map { booster ->
    val left = ui.state.boostersLeft(human, booster)
    val usable = booster in ui.usable && !ui.thinking && ui.aim == null && !ui.isOver
    BoosterTool(booster, left, usable, waiting = booster == Booster.UNDO && left > 0 && !usable && ui.state.dotsPlaced(human) == 0)
}

/**
 * The campaign's tool bar (canvas frame `game-start`; draw.py, `toolbar`): the five boosters as
 * icons with a count on each. A booster with none left is dashed and grey, with 0. One that cannot
 * be used now (the computer's turn, a booster already used this turn, Undo before there is a turn
 * to take back) is grey on paper, with its count still red. A tap opens its preview ([onBooster]).
 */
@Composable
internal fun RowScope.BoosterBar(tools: List<BoosterTool>, style: Palette, onBooster: (Booster) -> Unit) {
    for (tool in tools) BoosterButton(tool, style, Modifier.weight(1f)) { onBooster(tool.booster) }
}

@Composable
private fun BoosterButton(tool: BoosterTool, style: Palette, modifier: Modifier, onClick: () -> Unit) {
    val any = tool.left > 0
    val name = boosterName(tool.booster)
    val label = when {
        !any -> stringResource(R.string.booster_tool_none, name)
        tool.waiting -> pluralStringResource(R.plurals.booster_tool_waiting, tool.left, name, tool.left)
        else -> pluralStringResource(R.plurals.booster_tool, tool.left, name, tool.left)
    }
    Box(modifier.height(56.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(TOOL_SHAPE)
                .background(if (any && !tool.usable) style.paper else style.card)
                .toolOutline(if (any) style.bluePale else style.graphite, dashed = !any, TOOL_SHAPE)
                .clickable(enabled = tool.usable, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        ) {
            BoosterPicture(tool.booster, if (tool.usable) style.blue else style.graphite, style, active = tool.usable)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-7).dp)
                .widthIn(min = 22.dp)
                .height(22.dp)
                .background(if (any) style.red else style.graphite, CircleShape)
                .padding(horizontal = 5.dp)
                .clearAndSetSemantics {},
        ) {
            Text(tool.left.toString(), fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold, color = style.paper)
        }
    }
}

/**
 * A booster's icon at tool bar size: 28 dp, the double dot 30 x 16 dp in the player's red when it
 * can be used (draw.py, `booster_pic`).
 */
@Composable
private fun BoosterPicture(booster: Booster, ink: Color, style: Palette, active: Boolean) {
    if (booster == Booster.DOUBLE_DOT) {
        BoosterIcon(booster, if (active) style.red else ink, Modifier.size(width = 30.dp, height = 16.dp))
    } else {
        BoosterIcon(booster, ink, Modifier.size(28.dp))
    }
}

/**
 * The booster preview (docs/design/flows.md, "Booster preview"; canvas frame `game-undo`): a note
 * taped over the player's line and the tool bar, so the board does not move. One line on what the
 * booster does, then Cancel and the main button, which applies it. Wall and Bomb first need a point,
 * chosen on the board the way the player places a dot ([method]): until then the main button says
 * how, and cannot be pressed.
 */
@Composable
internal fun BoosterNote(
    aim: BoosterAim,
    method: PlacingMethod,
    style: Palette,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = boosterName(aim.booster)
    val rest = stringResource(
        when (aim.booster) {
            Booster.WALL -> R.string.booster_wall_line
            Booster.HINT -> R.string.booster_hint_line
            Booster.BOMB -> R.string.booster_bomb_line
            Booster.DOUBLE_DOT -> R.string.booster_double_dot_line
            Booster.UNDO -> R.string.booster_undo_line
        },
    )
    val go = when {
        !aim.ready && method == PlacingMethod.DRAG -> R.string.booster_drag_point
        !aim.ready -> R.string.booster_tap_point
        else -> when (aim.booster) {
            Booster.WALL -> R.string.booster_wall_go
            Booster.HINT -> R.string.booster_hint_go
            Booster.BOMB -> R.string.booster_bomb_go
            Booster.DOUBLE_DOT -> R.string.booster_double_dot_go
            Booster.UNDO -> R.string.booster_undo_go
        }
    }
    Box(
        modifier
            .rotate(-0.6f)
            .pointerInput(Unit) { detectTapGestures { } } // the tool bar under it takes no taps
            .semantics { paneTitle = name },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RectangleShape, clip = false)
                .background(style.card)
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BoosterPicture(aim.booster, style.blue, style, active = true)
                BasicText(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = style.blue, fontWeight = FontWeight.Bold)) { append(name) }
                        append(' ')
                        append(rest)
                    },
                    style = NoteText.body.copy(color = style.pencil),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 16.sp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallButton(
                    stringResource(R.string.booster_cancel),
                    style,
                    onCancel,
                    Modifier.weight(1f),
                    height = 56.dp,
                    padding = PaddingValues(horizontal = 4.dp), // "Cancel" fits a third of a 320 dp phone
                )
                BigButton(stringResource(go), style, onConfirm, Modifier.weight(2f), enabled = aim.ready)
            }
        }
        Tape(style, Modifier.offset(x = 20.dp, y = (-8).dp).rotate(-3f).size(width = 44.dp, height = 16.dp))
    }
}

/**
 * The Boosters screen (canvas frame `boosters`), from a booster on the Level card: a note over the
 * map with each of the five boosters, what it does and how many the player has in this level
 * ([left]), and how more are earned. Close, Back or a tap around the note returns to the card.
 */
@Composable
fun BoostersNote(left: Map<Booster, Int>, onClose: () -> Unit) {
    val style = Palette.current
    val title = stringResource(R.string.screen_boosters)
    BoxWithConstraints(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .background(style.scrim)
            .semantics { paneTitle = title },
    ) {
        val tallest = maxHeight - 40.dp
        Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onClose() } })
        Box(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp, top = 24.dp).rotate(0.8f)) {
            Column(
                modifier = Modifier
                    .heightIn(max = tallest)
                    .shadow(6.dp, RectangleShape, clip = false)
                    .background(style.card)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = NoteText.display,
                        color = style.blue,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    CloseButton(style, onClose)
                }
                for (booster in Booster.entries) BoosterRow(booster, left[booster] ?: 0, style)
                Text(
                    stringResource(R.string.boosters_footer),
                    style = NoteText.small,
                    color = style.graphite,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Tape(style, Modifier.align(Alignment.TopCenter).offset(y = (-9).dp).rotate(-3f).size(width = 56.dp, height = 16.dp))
        }
    }
}

@Composable
private fun BoosterRow(booster: Booster, left: Int, style: Palette) {
    val any = left > 0
    val name = boosterName(booster)
    val what = stringResource(
        when (booster) {
            Booster.WALL -> R.string.booster_wall_what
            Booster.HINT -> R.string.booster_hint_what
            Booster.BOMB -> R.string.booster_bomb_what
            Booster.DOUBLE_DOT -> R.string.booster_double_dot_what
            Booster.UNDO -> R.string.booster_undo_what
        },
    )
    val description = pluralStringResource(R.plurals.boosters_row, left, name, left, what)
    val grid = style.bluePale
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // A dashed line under each row, in the grid's blue.
                val dash = 3.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(grid, Offset(x, size.height), Offset(minOf(x + dash, size.width), size.height), 1.dp.toPx())
                    x += 2 * dash
                }
            }
            .padding(vertical = 6.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .toolOutline(if (any) style.blue else style.graphite, dashed = !any, TOOL_SHAPE),
        ) {
            BoosterPicture(booster, if (any) style.blue else style.graphite, style, active = any)
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = HAND, color = style.pencil)
            Text(what, style = NoteText.label.copy(fontWeight = FontWeight.Medium), color = style.graphite)
        }
        Text(
            stringResource(R.string.boosters_count, left),
            style = HAND.copy(fontSize = 28.sp, fontStyle = FontStyle.Normal, fontWeight = FontWeight.ExtraBold),
            color = if (any) style.red else style.graphite,
        )
    }
}
