package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dots.campaign.Title
import io.github.pavelya.dots.R
import io.github.pavelya.dots.ui.theme.Palette

/**
 * The player's title on Stats (the wireframes' Stats, its first card): "Your title", the title in
 * red, and a bar of the [stars] earned towards the next title ([StatsScreen]).
 */
@Composable
fun TitleCard(stars: Int, modifier: Modifier = Modifier) {
    val style = Palette.current
    val title = Title.of(stars)
    val next = title.next
    val name = titleName(title)
    val nextName = next?.let { titleName(it) }
    val share = if (next == null) 1f else (stars - title.stars).toFloat() / (next.stars - title.stars)
    val description = if (next == null || nextName == null) {
        stringResource(R.string.stats_title_description_top, name)
    } else {
        stringResource(R.string.stats_title_description, name, stars, next.stars, nextName)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .rotate(-1f)
            .fillMaxWidth()
            .shadow(4.dp, RectangleShape, clip = false)
            .background(style.card)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Text(
            stringResource(R.string.stats_your_title),
            style = NoteText.small.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp),
            color = style.graphite,
        )
        Text(name, style = NoteText.display.copy(fontSize = 34.sp, lineHeight = 34.sp), color = style.red)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(style.bluePale)) {
                if (share > 0f) Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).height(8.dp).background(style.red))
            }
            Text(
                if (nextName == null) stringResource(R.string.stats_title_top) else stringResource(R.string.stats_title_next, nextName),
                style = NoteText.label,
                color = style.pencil,
            )
        }
    }
}
