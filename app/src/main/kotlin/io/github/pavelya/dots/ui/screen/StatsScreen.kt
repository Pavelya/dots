package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dots.campaign.CampaignMap
import dots.log.PlayerStats
import io.github.pavelya.dots.R
import io.github.pavelya.dots.ui.theme.Palette

/**
 * Stats, as the wireframes' frame "Stats" draws it (flows.md, Stats): on the notebook's paper,
 * the player's title and the stars to the next one ([TitleCard]); six totals, two a row: games
 * played and won, dots captured, the best capture, the longest daily streak ([longestStreak]) and
 * the stars; then the stars of each world of the [map]. Back returns to the map.
 *
 * Leaderboards and Achievements sit at the bottom only when [onLeaderboards] and [onAchievements]
 * are given. The app gives them once O-02 opens Google Play Games from them; until then Stats has
 * no links (decision 0037).
 */
@Composable
fun StatsScreen(
    map: CampaignMap,
    stats: PlayerStats,
    longestStreak: Int,
    onBack: () -> Unit,
    onLeaderboards: (() -> Unit)? = null,
    onAchievements: (() -> Unit)? = null,
) {
    val style = Palette.current
    PaperPage(
        stringResource(R.string.screen_stats),
        onBack,
        backLabel = stringResource(R.string.stats_back),
        footer = links(onLeaderboards, onAchievements, style),
    ) {
        Spacer(Modifier.height(4.dp))
        TitleCard(map.totalStars)
        Spacer(Modifier.height(14.dp))
        val tiles = listOf(
            stats.played to R.string.stats_played,
            stats.won to R.string.stats_won,
            stats.captured to R.string.stats_captured,
            stats.bestCapture to R.string.stats_best_capture,
            longestStreak to R.string.stats_longest_streak,
            map.totalStars to R.string.stats_stars,
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in tiles.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for ((value, label) in row) StatTile(value, stringResource(label), style, Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.stats_stars_per_world),
            style = HAND24,
            color = style.pencil,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (world in map.worlds) WorldStars(world.world.number, world.stars, world.world.maxStars, style)
        }
    }
}

/** The Leaderboards and Achievements buttons, or no footer at all while either is missing. */
private fun links(onLeaderboards: (() -> Unit)?, onAchievements: (() -> Unit)?, style: Palette): (@Composable () -> Unit)? {
    if (onLeaderboards == null || onAchievements == null) return null
    return {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            val padding = PaddingValues(horizontal = 8.dp)
            SmallButton(stringResource(R.string.stats_leaderboards), style, onLeaderboards, Modifier.weight(1f), height = 56.dp, padding = padding)
            SmallButton(stringResource(R.string.stats_achievements), style, onAchievements, Modifier.weight(1f), height = 56.dp, padding = padding)
        }
    }
}

/** One total: the number in handwriting, what it counts under it, in a hand-drawn blue box. */
@Composable
private fun StatTile(value: Int, label: String, style: Palette, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(TOOL_SHAPE)
            .background(style.card)
            .border(2.dp, style.blue, TOOL_SHAPE)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clearAndSetSemantics { contentDescription = "$label: $value" },
    ) {
        Text("$value", style = NoteText.display.copy(fontSize = 30.sp, lineHeight = 30.sp), color = style.pencil, maxLines = 1)
        Text(label, style = NoteText.small.copy(fontSize = 11.sp, lineHeight = 14.sp), color = style.graphite, maxLines = 1)
    }
}

/** A world's stars: its name, a bar of the stars earned, and the count out of the most it has. */
@Composable
private fun WorldStars(number: Int, stars: Int, max: Int, style: Palette) {
    val name = stringResource(R.string.map_world_number, number)
    val description = pluralStringResource(R.plurals.map_world_stars, max, stars, max)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "$name: $description" },
    ) {
        Text(name, style = NoteText.small.copy(fontSize = 13.sp), color = style.pencil, maxLines = 1, modifier = Modifier.width(64.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(BAR_SHAPE).background(style.bluePale)) {
            if (max > 0 && stars > 0) {
                Box(Modifier.fillMaxWidth((stars.toFloat() / max).coerceIn(0f, 1f)).height(8.dp).background(style.blue))
            }
        }
        Text(
            stringResource(R.string.stats_world_stars, stars, max),
            style = NoteText.small.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            color = style.pencil,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 44.dp),
        )
    }
}

private val BAR_SHAPE = RoundedCornerShape(4.dp)
