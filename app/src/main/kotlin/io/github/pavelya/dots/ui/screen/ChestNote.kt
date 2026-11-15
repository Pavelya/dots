package io.github.pavelya.dots.ui.screen

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dots.campaign.Chest
import dots.campaign.Reward
import io.github.pavelya.dots.R
import io.github.pavelya.dots.ui.theme.Palette

/**
 * The Chest (the wireframes' Chest), over the map after every 10th level: the chest, "Ten levels
 * done!", and its two rewards side by side. Tapping one keeps it ([onPick]). Back, or a tap on the
 * map around the note, closes it unopened ([onClose]); the chest then waits on the map.
 */
@Composable
fun ChestNote(chest: Chest, onPick: (Reward) -> Unit, onClose: () -> Unit) {
    val style = Palette.current
    val title = stringResource(R.string.screen_chest)
    BoxWithConstraints(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .background(style.scrim)
            .semantics { paneTitle = title },
    ) {
        val tall = maxHeight >= 700.dp
        val tallest = maxHeight - 48.dp
        Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onClose() } })
        Box(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 24.dp, top = 24.dp).rotate(1f)) {
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
                Icon(
                    remember(style) { Doodles.bigChest(style.highlighter, style.paper, style.red) },
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(if (tall) 96.dp else 64.dp),
                )
                Text(
                    stringResource(R.string.chest_title),
                    style = NoteText.display.copy(fontSize = 38.sp, lineHeight = 38.sp),
                    color = style.highlighter,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(R.string.chest_pick), style = NoteText.body, color = style.pencil, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    for (reward in chest.choices) {
                        Choice(reward, style, Modifier.weight(1f).height(if (tall) 150.dp else 128.dp)) { onPick(reward) }
                    }
                }
            }
            Tape(style, Modifier.align(Alignment.TopCenter).offset(y = (-8).dp).rotate(-3f).size(width = 60.dp, height = 16.dp))
        }
    }
}

/** One reward: its booster's picture, "Bomb ×2" and "a booster", in a hand-cut frame. */
@Composable
private fun Choice(reward: Reward, style: Palette, modifier: Modifier, onClick: () -> Unit) {
    val name = boosterName(reward.booster)
    val description = pluralStringResource(R.plurals.chest_choice, reward.count, name, reward.count)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        modifier = modifier
            .clip(CHOICE_SHAPE)
            .background(style.card)
            .toolOutline(style.blue, dashed = false, CHOICE_SHAPE)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick { onClick(); true }
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
