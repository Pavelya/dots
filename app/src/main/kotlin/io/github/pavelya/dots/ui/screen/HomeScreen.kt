package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.pavelya.dots.R
import io.github.pavelya.dots.ui.theme.DotsTheme

@Composable
fun HomeScreen(
    onPlayComputer: () -> Unit,
    onTwoPlayers: () -> Unit,
    onRules: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayMedium)
        Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        val wide = Modifier.fillMaxWidth().widthIn(max = 360.dp)
        Button(onClick = onPlayComputer, modifier = wide) { Text(stringResource(R.string.home_play_computer)) }
        Button(onClick = onTwoPlayers, modifier = wide) { Text(stringResource(R.string.home_two_players)) }
        OutlinedButton(onClick = onRules, modifier = wide) { Text(stringResource(R.string.home_rules)) }
        OutlinedButton(onClick = onSettings, modifier = wide) { Text(stringResource(R.string.home_settings)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    DotsTheme { HomeScreen({}, {}, {}, {}) }
}
