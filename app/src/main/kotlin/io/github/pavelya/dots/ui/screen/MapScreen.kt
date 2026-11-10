package io.github.pavelya.dots.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.pavelya.dots.R

/** Where the map's entries lead (docs/design/flows.md, "Navigation in short"). */
class MapActions(
    val onLevel: (Int) -> Unit,
    val onDaily: () -> Unit,
    val onPlay: () -> Unit,
    val onRules: () -> Unit,
    val onStats: () -> Unit,
    val onShop: () -> Unit,
    val onSettings: () -> Unit,
)

/**
 * Home: the world map. A stand-in until A-02 draws the map: Level 1 as the one level, and the
 * labelled entries to everything else. As on the real map, there are no Play buttons; a level
 * opens its Level card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapScreen(actions: MapActions) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        ) {
            Entry(R.string.map_daily, actions.onDaily)
            Entry(R.string.map_play, actions.onPlay)
            Entry(R.string.map_rules, actions.onRules)
            Entry(R.string.map_stats, actions.onStats)
            Entry(R.string.map_shop, actions.onShop)
            Entry(R.string.home_settings, actions.onSettings)
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.map_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.stand_in, "A-02"), style = MaterialTheme.typography.bodyMedium)
            }
        }
        // The next level sits low on the screen, near the thumb.
        OutlinedButton(onClick = { actions.onLevel(1) }, shape = CircleShape, modifier = Modifier.size(96.dp)) {
            Text(stringResource(R.string.map_level, 1))
        }
    }
}

@Composable
private fun Entry(@StringRes label: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(stringResource(label)) }
}
