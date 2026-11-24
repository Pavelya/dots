package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dots.engine.Booster
import io.github.pavelya.dots.R
import io.github.pavelya.dots.audio.Effect
import io.github.pavelya.dots.audio.LocalAudio
import io.github.pavelya.dots.ui.theme.Palette

/**
 * A rewarded ad's offer (the wireframes' Result: loss and Chest, D-28; docs/design/money.md): a
 * small button under the main one, with the ad's mark, the [text] that names the reward, and on
 * Result the [booster]'s picture. A tap shows the ad ([onClick]).
 */
@Composable
internal fun AdOfferButton(text: String, style: Palette, onClick: () -> Unit, booster: Booster? = null) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, if (booster == null) Alignment.CenterHorizontally else Alignment.Start),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(style.card)
            .thinOutline(style.graphite, dashed = false, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(remember(style) { Doodles.ad(style.graphite) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(width = 26.dp, height = 22.dp))
        Text(
            text,
            style = AD_TEXT,
            color = style.pencil,
            textAlign = if (booster == null) TextAlign.Center else TextAlign.Start,
            modifier = if (booster == null) Modifier else Modifier.weight(1f),
        )
        booster?.let { BoosterIcon(it, style.blue, Modifier.size(24.dp)) }
    }
}

/**
 * After the ad on Result: "One Wall ready for your retry", in a dashed frame in the player's blue,
 * with the booster's picture (the wireframes' Result: loss, after the ad). The reward sound plays
 * as it first appears, not again when the phone turns.
 */
@Composable
internal fun AdReadyLine(booster: Booster, round: Int, style: Palette) {
    val audio = LocalAudio.current
    var heard by rememberSaveable(round) { mutableStateOf(false) }
    LaunchedEffect(round) {
        if (!heard) {
            heard = true
            audio.play(Effect.REWARD)
        }
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .thinOutline(style.blue, dashed = true, shape)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        BoosterIcon(booster, style.blue, Modifier.size(24.dp))
        Text(stringResource(R.string.result_ad_ready, boosterName(booster)), style = AD_TEXT, color = style.blue, textAlign = TextAlign.Center)
    }
}

/** The offers' text: 14 sp, extra bold, as the wireframes draw it. */
private val AD_TEXT: androidx.compose.ui.text.TextStyle
    @Composable get() = NoteText.label.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold)

/** A 1.5 dp outline in [ink], inside the edge, dashed or not: the wireframes' ad offers. */
private fun Modifier.thinOutline(ink: Color, dashed: Boolean, shape: Shape): Modifier = drawBehind {
    val width = 1.5.dp.toPx()
    val effect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null
    inset(width / 2) {
        drawOutline(shape.createOutline(size, layoutDirection, this), ink, style = Stroke(width = width, pathEffect = effect))
    }
}
