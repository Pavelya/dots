package io.github.pavelya.dots.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dots.campaign.Progress
import io.github.pavelya.dots.R
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.data.SampleProgress

/** The number of taps on the version line that opens the developer menu, as Android's "Build number" does. */
const val DEVELOPER_TAPS = 7

/** The pages Settings opens (the wireframes' Settings details, and the stand-ins of later tasks), by [key] in the route. */
enum class SettingsPage(val key: String) {
    /** A-12 makes Send feedback open an email. */
    FEEDBACK("feedback"),
    PRIVACY("privacy"),

    /** O-02 builds the Play Games backup. */
    BACKUP("backup"),
    CREDITS("credits"),

    /** The open-source libraries and the full text of their licences, from Credits. */
    LICENCES("licences"),
    ;

    companion object {
        fun of(key: String?): SettingsPage? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Settings, as the wireframes' frame "Settings" draws it: one scrolling page on the notebook's
 * paper. Sound and feel (music and sound volumes, vibration), the board (high-contrast pieces,
 * threat highlights, and placing a dot by two taps or drag, decision 0024), and help (Replay the
 * tutorial, Send feedback, Privacy, Play Games backup, Credits and licences). Every change goes
 * to [onChange] at once. Language joins Help once a second language ships (A-10). Tapping the
 * version line [DEVELOPER_TAPS] times opens the developer menu.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: (change: (AppSettings) -> AppSettings) -> Unit,
    version: String,
    onPage: (SettingsPage) -> Unit,
    onDeveloperMenu: () -> Unit,
    onReplayTutorial: () -> Unit,
    onBack: () -> Unit,
) {
    val style = rememberGameStyle()
    PaperPage(stringResource(R.string.home_settings), onBack) {
        SectionHead(stringResource(R.string.settings_sound_and_feel))
        VolumeRow(stringResource(R.string.settings_music), settings.musicVolume, AppSettings.MAX_VOLUME) { volume ->
            onChange { it.copy(musicVolume = volume) }
        }
        VolumeRow(stringResource(R.string.settings_effects), settings.effectsVolume, AppSettings.MAX_VOLUME) { volume ->
            onChange { it.copy(effectsVolume = volume) }
        }
        SwitchRow(stringResource(R.string.settings_vibration), settings.vibration) { on -> onChange { it.copy(vibration = on) } }

        SectionHead(stringResource(R.string.settings_the_board))
        SwitchRow(stringResource(R.string.settings_high_contrast), settings.highContrast) { on -> onChange { it.copy(highContrast = on) } }
        SwitchRow(stringResource(R.string.settings_threats), settings.threatHighlight) { on -> onChange { it.copy(threatHighlight = on) } }
        Text(
            stringResource(R.string.settings_placing),
            style = ROW_TEXT,
            color = style.graphite,
            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
        )
        Column(Modifier.selectableGroup()) {
            for (method in PlacingMethod.entries) {
                RadioRow(
                    label = stringResource(if (method == PlacingMethod.TWO_TAPS) R.string.placing_two_taps else R.string.placing_drag),
                    detail = stringResource(if (method == PlacingMethod.TWO_TAPS) R.string.placing_two_taps_detail else R.string.placing_drag_detail),
                    selected = settings.placing == method,
                ) { onChange { it.copy(placing = method) } }
            }
        }

        SectionHead(stringResource(R.string.settings_help))
        LinkRow(stringResource(R.string.settings_replay_tutorial), onClick = onReplayTutorial)
        LinkRow(stringResource(R.string.settings_feedback)) { onPage(SettingsPage.FEEDBACK) }
        LinkRow(stringResource(R.string.settings_privacy)) { onPage(SettingsPage.PRIVACY) }
        LinkRow(stringResource(R.string.settings_backup)) { onPage(SettingsPage.BACKUP) }
        LinkRow(stringResource(R.string.settings_credits)) { onPage(SettingsPage.CREDITS) }

        var taps by rememberSaveable { mutableIntStateOf(0) }
        Text(
            stringResource(R.string.settings_version, version),
            style = NoteText.small,
            color = style.graphite,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(interactionSource = null, indication = null) {
                    taps += 1
                    if (taps >= DEVELOPER_TAPS) {
                        taps = 0
                        onDeveloperMenu()
                    }
                }
                .wrapContentHeight(),
        )
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
