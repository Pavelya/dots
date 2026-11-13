package io.github.pavelya.dots.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pavelya.dots.R

/** The canvas's hand-cut shape of a big button. */
private val BIG_BUTTON_SHAPE = RoundedCornerShape(topStart = 16.dp, topEnd = 20.dp, bottomEnd = 14.dp, bottomStart = 22.dp)

/**
 * Pause, as the wireframes draw it: a taped note over the dimmed board with Resume as the one big
 * button, Restart and To the map under it, and small icons for sound on or off and How to play.
 * Restart asks first, on the same note. Back resumes the game (handled by the screen), or, while
 * Restart asks, returns to Pause.
 */
@Composable
internal fun PauseNote(
    sound: Boolean,
    onSound: (Boolean) -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onToMap: () -> Unit,
    onRules: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val style = remember(colors) { GameStyle.plain(colors, colors.background.luminance() < 0.5f) }
    var asking by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = asking) { asking = false }
    val title = stringResource(R.string.paused)
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .background(style.scrim)
            .pointerInput(Unit) { detectTapGestures { } } // the board under it takes no taps
            .semantics { paneTitle = title },
    ) {
        TapedNote(style, Modifier.padding(start = 30.dp, end = 30.dp, bottom = 40.dp)) {
            if (asking) {
                Text(
                    stringResource(R.string.restart_question),
                    style = MaterialTheme.typography.titleMedium,
                    color = style.pencil,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                BigButton(stringResource(R.string.pause_restart), style, onRestart, Modifier.fillMaxWidth())
                SmallButton(stringResource(R.string.restart_cancel), style, { asking = false }, Modifier.fillMaxWidth())
            } else {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    color = style.blue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                )
                BigButton(stringResource(R.string.pause_resume), style, onResume, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallButton(stringResource(R.string.pause_restart), style, { asking = true }, Modifier.weight(1f))
                    SmallButton(stringResource(R.string.pause_to_map), style, onToMap, Modifier.weight(1f))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SmallIcon(
                        if (sound) Doodles.SOUND_ON else Doodles.SOUND_OFF,
                        stringResource(if (sound) R.string.pause_sound_on else R.string.pause_sound_off),
                        style,
                    ) { onSound(!sound) }
                    SmallIcon(Doodles.HELP, stringResource(R.string.map_rules), style, onRules)
                }
            }
        }
    }
}

/** A note taped over the board: a card turned a little ([angle] degrees), with a strip of tape at the top. */
@Composable
internal fun TapedNote(style: GameStyle, modifier: Modifier, angle: Float = -1f, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.rotate(angle), contentAlignment = Alignment.TopCenter) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RectangleShape, clip = false)
                .background(style.card)
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp),
            content = content,
        )
        Tape(style, Modifier.offset(y = (-9).dp).rotate(3f).size(width = 64.dp, height = 18.dp))
    }
}

/**
 * The one big button of a note: 56 dp, in blue ink, with the Title style (canvas README). An icon
 * may come [before] or [after] the text, as path data from [Doodles].
 */
@Composable
internal fun BigButton(
    text: String,
    style: GameStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    before: String? = null,
    after: String? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = BIG_BUTTON_SHAPE,
        colors = ButtonDefaults.buttonColors(
            containerColor = style.blue,
            contentColor = style.paper,
            disabledContainerColor = style.bluePale,
            disabledContentColor = style.blue,
        ),
        modifier = modifier.height(56.dp),
    ) {
        before?.let { ButtonIcon(it, style.paper) }
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        after?.let { ButtonIcon(it, style.paper) }
    }
}

@Composable
private fun ButtonIcon(path: String, ink: Color) {
    Icon(
        remember(path, ink) { Doodles.icon(path, ink, width = 2.2f) },
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier.padding(horizontal = 8.dp).size(22.dp),
    )
}

/**
 * A small square button beside the big one, 56 dp, with only an icon ([path]) and its [label]
 * for TalkBack: Retry or Map on Result.
 */
@Composable
internal fun SquareButton(path: String, label: String, style: GameStyle, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clip(BIG_BUTTON_SHAPE)
            .background(style.card)
            .toolOutline(style.graphite, dashed = false, BIG_BUTTON_SHAPE)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Icon(remember(path, style) { Doodles.icon(path, style.pencil) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(26.dp))
    }
}

/** A button beside or under the big one: blue ink outlined on the note's card. */
@Composable
internal fun SmallButton(text: String, style: GameStyle, onClick: () -> Unit, modifier: Modifier, height: Dp = 48.dp) {
    OutlinedButton(
        onClick = onClick,
        shape = TOOL_SHAPE,
        border = BorderStroke(2.dp, style.blue),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = style.card, contentColor = style.blue),
        modifier = modifier.height(height),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallIcon(path: String, label: String, style: GameStyle, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Icon(remember(path, style) { Doodles.icon(path, style.graphite) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
    }
}
