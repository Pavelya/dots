package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dots.campaign.Chest
import dots.campaign.Reward
import io.github.pavelya.dots.R
import io.github.pavelya.dots.ui.motion.NoteMotion
import io.github.pavelya.dots.ui.motion.dim
import io.github.pavelya.dots.ui.motion.rememberNoteMotion
import io.github.pavelya.dots.ui.motion.slideUp
import io.github.pavelya.dots.ui.theme.GameTheme
import io.github.pavelya.dots.ui.theme.Palette

/**
 * The Chest (the wireframes' Chest), over the map after every 10th level: the chest, "Ten levels
 * done!", and its two rewards side by side. Tapping one keeps it ([onPick]). A chest of one reward
 * (decision 0038) shows it in the middle with one big button, Keep it, which keeps it the same way.
 * Back, or a tap on the map around the note, closes it unopened ([onClose]); the chest then waits
 * on the map. It slides up as the map behind it dims, and down as it leaves ([motion]).
 */
@Composable
fun ChestNote(chest: Chest, onPick: (Reward) -> Unit, onClose: () -> Unit, motion: NoteMotion = rememberNoteMotion()) {
    val style = Palette.current
    val title = stringResource(R.string.screen_chest)
    BoxWithConstraints(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .dim(style.scrim) { motion.up }
            .semantics { paneTitle = title },
    ) {
        val tall = maxHeight >= 700.dp
        val tallest = maxHeight - 48.dp
        Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onClose() } })
        Box(Modifier.slideUp { motion.up }.padding(start = 14.dp, end = 14.dp, bottom = 24.dp, top = 24.dp).rotate(1f)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = tallest)
                    .shadow(6.dp, RectangleShape, clip = false)
                    .background(style.card)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 18.dp),
            ) {
                OpenChest(if (tall) 96.dp else 64.dp)
                Text(
                    stringResource(R.string.chest_title),
                    style = NoteText.display.copy(fontSize = 38.sp, lineHeight = 38.sp),
                    color = style.highlighter,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                val choiceHeight = if (tall) 150.dp else 128.dp
                if (chest.isSingle) {
                    val reward = chest.choices.single()
                    Text(stringResource(R.string.chest_single), style = NoteText.body, color = style.pencil, textAlign = TextAlign.Center)
                    // The width of one of two choices side by side.
                    Choice(reward, style, Modifier.fillMaxWidth(0.5f).height(choiceHeight), onClick = null)
                    BigButton(stringResource(R.string.chest_keep), style, onClick = { onPick(reward) }, Modifier.fillMaxWidth())
                } else {
                    Text(stringResource(R.string.chest_pick), style = NoteText.body, color = style.pencil, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        for (reward in chest.choices) {
                            Choice(reward, style, Modifier.weight(1f).height(choiceHeight)) { onPick(reward) }
                        }
                    }
                }
            }
            Tape(style, Modifier.align(Alignment.TopCenter).offset(y = (-8).dp).rotate(-3f).size(width = 60.dp, height = 16.dp))
        }
    }
}

/**
 * The open chest, [height] tall, from the same sheet as the chest on the map's path
 * ([MapPages.CHEST_OPEN]), so the chest the player tapped is the one that opens.
 */
@Composable
private fun OpenChest(height: Dp) {
    val image = ImageBitmap.imageResource(MapPages.CHEST_OPEN.of(GameTheme.isDark))
    Image(
        image,
        contentDescription = null,
        filterQuality = FilterQuality.High,
        modifier = Modifier.size(width = height * image.width / image.height, height = height),
    )
}

/**
 * One reward: its booster's picture, "Bomb ×2" and "a booster", in a hand-cut frame. Tapping it
 * keeps it ([onClick]); a chest's only reward has no [onClick], as Keep it below it keeps it.
 */
@Composable
private fun Choice(reward: Reward, style: Palette, modifier: Modifier, onClick: (() -> Unit)?) {
    val name = boosterName(reward.booster)
    val description = if (onClick == null) {
        pluralStringResource(R.plurals.chest_single_reward, reward.count, name, reward.count)
    } else {
        pluralStringResource(R.plurals.chest_choice, reward.count, name, reward.count)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        modifier = modifier
            .clip(CHOICE_SHAPE)
            .background(style.card)
            .toolOutline(style.blue, dashed = false, CHOICE_SHAPE)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .clearAndSetSemantics {
                contentDescription = description
                if (onClick != null) {
                    role = Role.Button
                    onClick { onClick(); true }
                }
            }
            .padding(8.dp),
    ) {
        BoosterIcon(reward.booster, style.blue, Modifier.size(44.dp))
        Text(stringResource(R.string.chest_reward, name, reward.count), style = HAND.copy(fontSize = 26.sp), color = style.blue)
        Text(stringResource(R.string.chest_a_booster), style = NoteText.small, color = style.graphite)
    }
}

/** The canvas's hand-cut button shape, as on Play a game. */
private val CHOICE_SHAPE = RoundedCornerShape(topStart = 16.dp, topEnd = 20.dp, bottomEnd = 14.dp, bottomStart = 22.dp)
