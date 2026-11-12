package io.github.pavelya.dots.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dots.campaign.Progress
import io.github.pavelya.dots.R
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.data.SampleProgress

/** The number of taps on the version line that opens the developer menu, as Android's "Build number" does. */
const val DEVELOPER_TAPS = 7

/**
 * Settings for players: the threat highlight, how a dot is placed, and Replay the tutorial
 * ([onReplayTutorial]) under Help; A-09 adds sound, vibration and the rest. Tapping the version
 * line [DEVELOPER_TAPS] times opens the developer menu.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    version: String,
    onDetails: () -> Unit,
    onDeveloperMenu: () -> Unit,
    onReplayTutorial: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScreenHeader(R.string.home_settings, onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(
                    value = settings.threatHighlight,
                    role = Role.Switch,
                    onValueChange = { onChange(settings.copy(threatHighlight = it)) },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_threats), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.settings_threats_detail), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = settings.threatHighlight, onCheckedChange = null)
        }

        Section(R.string.settings_placing)
        Choices(
            options = PlacingMethod.entries,
            selected = settings.placing,
            label = {
                stringResource(
                    when (it) {
                        PlacingMethod.TWO_TAPS -> R.string.placing_two_taps
                        PlacingMethod.DRAG -> R.string.placing_drag
                    },
                )
            },
            onSelect = { onChange(settings.copy(placing = it)) },
        )

        Section(R.string.settings_help)
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onReplayTutorial),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.settings_replay_tutorial), style = MaterialTheme.typography.titleMedium)
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onDetails),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.settings_credits), style = MaterialTheme.typography.titleMedium)
        }

        var taps by rememberSaveable { mutableIntStateOf(0) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(interactionSource = null, indication = null) {
                    taps += 1
                    if (taps >= DEVELOPER_TAPS) {
                        taps = 0
                        onDeveloperMenu()
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.settings_version, version), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * The developer menu, hidden from players: sample campaign progress, and the game log to share.
 * Opens from the version line in Settings.
 */
@Composable
fun DeveloperScreen(
    onProgress: (Progress) -> Unit,
    loggedGames: Int,
    onShareLog: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScreenHeader(R.string.developer_title, onBack)
        Text(stringResource(R.string.developer_note), style = MaterialTheme.typography.bodyMedium)

        Section(R.string.developer_progress)
        Text(stringResource(R.string.developer_progress_note), style = MaterialTheme.typography.bodyMedium)
        for ((label, progress) in listOf(
            R.string.developer_progress_none to { Progress.NONE },
            R.string.developer_progress_world_needs_stars to { SampleProgress.worldNeedsStars },
            R.string.developer_progress_mid_world to { SampleProgress.midWorld },
            R.string.developer_progress_everything to { SampleProgress.everything },
        )) {
            OutlinedButton(onClick = { onProgress(progress()) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(label))
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Section(R.string.log_title)
        Text(
            if (loggedGames == 0) stringResource(R.string.log_empty) else pluralStringResource(R.plurals.log_detail, loggedGames, loggedGames),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = onShareLog, enabled = loggedGames > 0, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.log_share))
        }
    }
}

@Composable
private fun Section(@StringRes title: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun <T> Choices(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    Column(Modifier.selectableGroup()) {
        for (option in options) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(selected = option == selected, role = Role.RadioButton, onClick = { onSelect(option) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option == selected, onClick = null)
                Text(label(option), modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}
