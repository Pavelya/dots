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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dots.campaign.Campaign
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
 * A reward the very next level does not allow says from which level it can be used.
 * Back, or a tap on the map around the note, closes it unopened ([onClose]); the chest then waits
 * on the map. It slides up as the map behind it dims, and down as it leaves ([motion]).
 *
 * From World 2, a chest of two boosters offers a rewarded ad to keep both ([adOffer], [onWatchAd];
 * docs/design/money.md). Once the ad is watched ([kept]), both choices are marked Kept, "Both are
 * yours!" heads the note, and the next level's card opens as after a pick.
 */
@Composable
fun ChestNote(
    chest: Chest,
    onPick: (Reward) -> Unit,
    onClose: () -> Unit,
    motion: NoteMotion = rememberNoteMotion(),
    adOffer: Boolean = false,
    onWatchAd: () -> Unit = {},
    kept: Boolean = false,
) {
    val style = Palette.current
    val title = stringResource(R.string.screen_chest)
    BoxWithConstraints(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .dim(style.scrim) { motion.up }
            .semantics { paneTitle = title },
    ) {
        // Each reward's first level, when it is not the very next one.
        val from = remember(chest) {
            val next = Campaign.after(chest.after)?.code
            chest.choices.associate { reward -> reward.booster to Campaign.firstUse(chest, reward.booster)?.code?.takeIf { it != next } }
        }
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
                    stringResource(if (kept) R.string.chest_both else R.string.chest_title),
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
                    Choice(reward, from[reward.booster], style, Modifier.fillMaxWidth(0.5f).height(choiceHeight), onClick = null)
                    BigButton(stringResource(R.string.chest_keep), style, onClick = { onPick(reward) }, Modifier.fillMaxWidth())
                } else {
                    Text(
                        stringResource(if (kept) R.string.chest_opening_next else R.string.chest_pick),
                        style = NoteText.body,
                        color = style.pencil,
                        textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        for (reward in chest.choices) {
                            val pick = if (kept) null else ({ onPick(reward) })
                            Choice(reward, from[reward.booster], style, Modifier.weight(1f).height(choiceHeight), onClick = pick, kept = kept)
                        }
                    }
                    if (adOffer && !kept) AdOfferButton(stringResource(R.string.chest_ad_offer), style, onWatchAd)
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
 * One reward: its booster's picture, "Bomb ×2" and "a booster", in a hand-cut frame. A reward
 * first usable on a later level says "from level 3-1" ([from]) in pencil instead of "a booster".
 * Tapping it keeps it ([onClick]); a chest's only reward has no [onClick], as Keep it below it
 * keeps it. After an ad keeps both, each is marked Kept ([kept]).
 */
@Composable
private fun Choice(reward: Reward, from: String?, style: Palette, modifier: Modifier, onClick: (() -> Unit)?, kept: Boolean = false) {
    val name = boosterName(reward.booster)
    // A chosen reward is described as the only reward is: no "Keep it." once it is kept.
    val single = onClick == null
    val choice = when {
        single && from != null -> pluralStringResource(R.plurals.chest_single_reward_later, reward.count, name, reward.count, from)
        single -> pluralStringResource(R.plurals.chest_single_reward, reward.count, name, reward.count)
        from != null -> pluralStringResource(R.plurals.chest_choice_later, reward.count, name, reward.count, from)
        else -> pluralStringResource(R.plurals.chest_choice, reward.count, name, reward.count)
    }
    val keptWord = stringResource(R.string.chest_kept)
    val description = if (kept) "$choice $keptWord." else choice
    Box(modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxSize()
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
            if (from != null) {
                Text(stringResource(R.string.chest_from, from), style = NoteText.small.copy(fontWeight = FontWeight.ExtraBold), color = style.pencil)
            } else {
                Text(stringResource(R.string.chest_a_booster), style = NoteText.small, color = style.graphite)
            }
        }
        if (kept) KeptChip(keptWord, style, Modifier.align(Alignment.TopEnd).padding(6.dp))
    }
}

/** "✓ Kept" on a choice the ad kept: a small blue chip with a tick (the wireframes' Chest, after the ad). */
@Composable
private fun KeptChip(word: String, style: Palette, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(style.blue)
            .padding(horizontal = 8.dp)
            .clearAndSetSemantics {},
    ) {
        Icon(remember(style) { Doodles.icon(Doodles.TICK, style.card, width = 3f) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(11.dp))
        Text(word, style = NoteText.small.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold), color = style.card)
    }
}

/** The canvas's hand-cut button shape, as on Play a game. */
private val CHOICE_SHAPE = RoundedCornerShape(topStart = 16.dp, topEnd = 20.dp, bottomEnd = 14.dp, bottomStart = 22.dp)
