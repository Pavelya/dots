package io.github.pavelya.dots.ui.screen

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pavelya.dots.R
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.BoardSize
import io.github.pavelya.dots.data.GameLength
import io.github.pavelya.dots.data.GameMode

/** The hand-cut shape of the big cards (the wireframe's border-radius). */
private val CARD_SHAPE = RoundedCornerShape(topStart = 16.dp, topEnd = 20.dp, bottomEnd = 14.dp, bottomStart = 22.dp)

/** A choice on the setup: a little rounder than a tool. */
private val CHOICE_SHAPE = RoundedCornerShape(14.dp)

/**
 * Play a game, over the map, as the wireframes' frame "Play a game" draws it: a ruled note taped at
 * the bottom of the screen with two big cards, Against the computer (free play) and Two players.
 * A card resumes its mode's saved game, or starts a new one at once with the board size and game
 * length in [settings]. The small link under the cards opens the setup on the same note, where
 * [onSettings] applies a change of board size or game length to the saved settings for the next games (decision 0025: Short,
 * Long or Open-ended). With the setup open, a card starts a new game; if that mode has a saved
 * game, the note asks first.
 *
 * [onPlay] gets the mode and whether to start a new game even though one is [saved]. Close, Back
 * or a tap on the map around the note closes it ([onClose]); while the note asks, Back returns to
 * the cards.
 */
@Composable
fun PlayAGameNote(
    saved: Set<GameMode>,
    settings: AppSettings,
    onSettings: (change: (AppSettings) -> AppSettings) -> Unit,
    onPlay: (mode: GameMode, replace: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val style = rememberGameStyle()
    var setup by rememberSaveable { mutableStateOf(false) }
    var askingKey by rememberSaveable { mutableStateOf<String?>(null) }
    val asking = GameMode.of(askingKey)
    BackHandler(enabled = asking != null) { askingKey = null }
    val title = stringResource(R.string.map_play)
    fun choose(mode: GameMode) {
        when {
            !setup -> onPlay(mode, false)
            mode in saved -> askingKey = mode.key
            else -> onPlay(mode, true)
        }
    }
    BoxWithConstraints(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier.fillMaxSize().semantics { paneTitle = title },
    ) {
        val cardHeight = if (maxHeight >= 700.dp) 88.dp else 72.dp
        val tallest = maxHeight - 40.dp
        // A tap on the map around the note closes it; the note takes its own taps.
        Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onClose() } })
        Box(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 14.dp, top = 24.dp).rotate(0.6f)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = tallest)
                    .shadow(6.dp, RectangleShape, clip = false)
                    .background(style.card)
                    .ruled(style.blue.copy(alpha = 0.12f))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = NoteText.display.copy(fontSize = 38.sp, lineHeight = 40.sp),
                        color = style.blue,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    CloseIcon(style, onClose)
                }
                if (asking != null) {
                    ReplaceQuestion(style, onReplace = { onPlay(asking, true) }, onKeep = { askingKey = null })
                } else {
                    if (setup) Setup(settings, style, onSettings)
                    PlayCard(
                        title = R.string.play_card_computer,
                        detail = when {
                            setup -> R.string.play_card_new
                            GameMode.FREE_PLAY in saved -> R.string.play_card_resume
                            else -> R.string.play_card_computer_detail
                        },
                        icon = remember(style) { Doodles.smile(style.paper) },
                        background = style.blue,
                        style = style,
                        height = cardHeight,
                        onClick = { choose(GameMode.FREE_PLAY) },
                    )
                    PlayCard(
                        title = R.string.play_two,
                        detail = when {
                            setup -> R.string.play_card_new
                            GameMode.TWO_PLAYERS in saved -> R.string.play_card_resume
                            else -> R.string.play_card_two_detail
                        },
                        icon = remember(style) { Doodles.people(style.paper, style.paper) },
                        background = style.red,
                        style = style,
                        height = cardHeight,
                        onClick = { choose(GameMode.TWO_PLAYERS) },
                    )
                    if (!setup) SetupLink(settings, style) { setup = true }
                }
            }
            Tape(style, Modifier.offset(x = 30.dp, y = (-8).dp).rotate(-4f).size(width = 56.dp, height = 16.dp))
        }
    }
}

/** The note's ruled lines, 24 dp apart, as the notebook's page. */
private fun Modifier.ruled(ink: Color): Modifier = drawBehind {
    val step = 24.dp.toPx()
    var y = step
    while (y < size.height) {
        drawLine(ink, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        y += step
    }
}

@Composable
private fun CloseIcon(style: GameStyle, onClose: () -> Unit) {
    val label = stringResource(R.string.close)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(x = 10.dp)
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClose)
            .semantics { contentDescription = label },
    ) {
        Icon(remember(style) { Doodles.icon(Doodles.CLOSE, style.pencil, width = 2f) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
    }
}

/** A big card: the button of one mode, with its icon, its name in handwriting, one line under it and a play triangle. */
@Composable
private fun PlayCard(
    @StringRes title: Int,
    @StringRes detail: Int,
    icon: ImageVector,
    background: Color,
    style: GameStyle,
    height: Dp,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(CARD_SHAPE)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(52.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = NoteText.display.copy(fontSize = 28.sp, lineHeight = 32.sp), color = style.paper, maxLines = 1)
            Text(stringResource(detail), style = NoteText.small.copy(fontSize = 13.sp), color = style.paper.copy(alpha = 0.9f))
        }
        Icon(remember(style) { Doodles.play(style.paper) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
    }
}

/** The small link under the cards: the board size and game length the next game gets, and "change". */
@Composable
private fun SetupLink(settings: AppSettings, style: GameStyle, onClick: () -> Unit) {
    val size = stringResource(R.string.settings_board_size, settings.boardSize.points, settings.boardSize.points)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.play_setup_link, size, stringResource(lengthName(settings.length))),
            style = NoteText.small.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
            color = style.graphite,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 14.dp),
        )
    }
}

/** Board size and game length for the next games, each a row of choices. */
@Composable
private fun Setup(settings: AppSettings, style: GameStyle, onSettings: ((AppSettings) -> AppSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.settings_board), style = NoteText.label, color = style.graphite, modifier = Modifier.semantics { heading() })
        ChoiceRow {
            for (size in BoardSize.entries) {
                Choice(
                    label = stringResource(R.string.settings_board_size, size.points, size.points),
                    detail = null,
                    selected = settings.boardSize == size,
                    style = style,
                ) { onSettings { it.copy(boardSize = size) } }
            }
        }
        Text(
            stringResource(R.string.settings_length),
            style = NoteText.label,
            color = style.graphite,
            modifier = Modifier.padding(top = 4.dp).semantics { heading() },
        )
        ChoiceRow {
            for (length in GameLength.entries) {
                val dots = AppSettings.dotsFor(settings.boardSize, length)
                Choice(
                    label = stringResource(lengthName(length)),
                    detail = if (dots == null) stringResource(R.string.length_open_detail) else pluralStringResource(R.plurals.length_dots, dots, dots),
                    selected = settings.length == length,
                    style = style,
                ) { onSettings { it.copy(length = length) } }
            }
        }
    }
}

@Composable
private fun ChoiceRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().selectableGroup(), content = content)
}

/** One choice: filled blue when chosen, outlined otherwise. */
@Composable
private fun RowScope.Choice(label: String, detail: String?, selected: Boolean, style: GameStyle, onClick: () -> Unit) {
    val ink = if (selected) style.paper else style.blue
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .clip(CHOICE_SHAPE)
            .background(if (selected) style.blue else style.card)
            .toolOutline(style.blue, dashed = false, CHOICE_SHAPE)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Text(label, style = CHOICE_TEXT, color = ink, maxLines = 1)
        detail?.let { Text(it, style = NoteText.small, color = ink, maxLines = 1) }
    }
}

private val CHOICE_TEXT = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)

/** Before a new game takes the place of a saved one: New game, or Keep it. */
@Composable
private fun ReplaceQuestion(style: GameStyle, onReplace: () -> Unit, onKeep: () -> Unit) {
    Text(
        stringResource(R.string.play_replace_question),
        style = NoteText.title,
        color = style.pencil,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    Text(
        stringResource(R.string.play_replace_detail),
        style = NoteText.body,
        color = style.graphite,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    BigButton(stringResource(R.string.play_replace), style, onReplace, Modifier.fillMaxWidth())
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.play_keep),
            style = NoteText.label.copy(textDecoration = TextDecoration.Underline),
            color = style.blue,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onKeep)
                .padding(horizontal = 12.dp, vertical = 14.dp),
        )
    }
}

@StringRes
private fun lengthName(length: GameLength): Int = when (length) {
    GameLength.SHORT -> R.string.length_short
    GameLength.LONG -> R.string.length_long
    GameLength.OPEN_ENDED -> R.string.length_open
}
