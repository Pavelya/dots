package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pavelya.dots.R
import io.github.pavelya.dots.ui.theme.Palette

/**
 * A page of cards to choose one look from, as the Theme, Pieces and Rings pages in Settings are
 * built (flows.md, "Choosing a theme" and "Choosing pieces and rings"): a full screen on Notebook
 * paper titled [title], one card for each of the [choices] the player can choose, each with a
 * small board ([sample]), its [name] and [line] (string resources). The one [inUse] has a tick and "In use"; tapping
 * another puts it in use ([onUse]), and tapping the one in use does nothing. A debug build says so
 * at the top ([debugNote]); [under] is the small text under the cards; [shopLink], when given, ends
 * the page with a link to the Shop ([onShop]). For TalkBack the cards are one group of choices
 * named [title], each read as its name and line, then selected or not.
 *
 * [sample] gets whether the phone is tall: the wireframes use 14 dp between cards on a tall phone
 * and 8 dp on a small one (320 x 568), and the Pieces and Rings pages shrink their boards there.
 */
@Composable
internal fun <T> ChoicePage(
    title: String,
    choices: List<T>,
    inUse: T,
    onUse: (T) -> Unit,
    name: (T) -> Int,
    line: (T) -> Int,
    debugNote: String?,
    under: String,
    shopLink: String?,
    onShop: () -> Unit,
    onBack: () -> Unit,
    sample: @Composable (item: T, tall: Boolean) -> Unit,
) {
    val style = Palette.current
    BoxWithConstraints {
        val tall = maxHeight >= 700.dp
        val gap = if (tall) 14.dp else 8.dp
        PaperPage(title, onBack, stringResource(R.string.back_to_settings)) {
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                if (debugNote != null) DebugNote(debugNote, style)
                Column(
                    verticalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.selectableGroup().semantics { contentDescription = title },
                ) {
                    choices.forEachIndexed { i, item ->
                        ChoiceCard(
                            name = stringResource(name(item)),
                            line = stringResource(line(item)),
                            selected = item == inUse,
                            turn = CARD_TURNS[i % CARD_TURNS.size],
                            style = style,
                            sample = { sample(item, tall) },
                        ) {
                            if (item != inUse) onUse(item)
                        }
                    }
                }
                Text(under, style = CARD_LINE.copy(lineHeight = 18.sp), color = style.graphite)
                if (shopLink != null) {
                    Text(
                        shopLink,
                        style = ROW_TEXT.copy(textDecoration = TextDecoration.Underline),
                        color = style.blue,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable(role = Role.Button, onClick = onShop)
                            .wrapContentHeight(),
                    )
                }
            }
        }
    }
}

/**
 * One card: its small board ([sample]), its [name] and [line], and "In use" with a tick when
 * [selected]. TalkBack reads the name and the line, then selected or not; the board and "In use"
 * are not read.
 */
@Composable
private fun ChoiceCard(
    name: String,
    line: String,
    selected: Boolean,
    turn: Float,
    style: Palette,
    sample: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .rotate(turn)
            .shadow(2.dp, shape, clip = false)
            .background(style.card, shape)
            .border(2.dp, if (selected) style.blue else Color.Transparent, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(8.dp),
    ) {
        Box(Modifier.clearAndSetSemantics {}) { sample() }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = CARD_NAME, color = style.pencil)
            Text(line, style = CARD_LINE, color = style.pencil)
            if (selected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clearAndSetSemantics {},
                ) {
                    Icon(remember(style) { Doodles.icon(Doodles.TICK, style.red, width = 3f) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.theme_in_use), style = CARD_LINE.copy(fontWeight = FontWeight.ExtraBold), color = style.red)
                }
            }
        }
    }
}

/** A note for testers in a dashed frame: what a player's build shows instead. */
@Composable
private fun DebugNote(text: String, style: Palette) {
    Text(
        text,
        style = CARD_LINE.copy(lineHeight = 18.sp),
        color = style.graphite,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val line = 1.5.dp.toPx()
                drawRoundRect(
                    style.graphite,
                    topLeft = Offset(line / 2, line / 2),
                    size = Size(size.width - line, size.height - line),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(line, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                )
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** How far each card is turned, in degrees, in the wireframe's order. */
private val CARD_TURNS = listOf(-0.6f, 0.5f, -0.3f)

/** A card's name: Caveat, 26 sp. */
private val CARD_NAME: TextStyle
    @Composable @ReadOnlyComposable
    get() = NoteText.display.copy(fontSize = 26.sp, lineHeight = 26.sp)

/** A card's line, and the small text under the cards: 13 sp. */
private val CARD_LINE: TextStyle
    @Composable @ReadOnlyComposable
    get() = NoteText.body.copy(fontSize = 13.sp, lineHeight = 17.sp)

/** Whether a page that lists [choices] of [all] should end with a link to the Shop: once the Shop exists ([onShop]) and the player lacks one. */
internal fun <T> lacksOne(choices: List<T>, all: List<T>, onShop: (() -> Unit)?): Boolean = onShop != null && choices.size < all.size
