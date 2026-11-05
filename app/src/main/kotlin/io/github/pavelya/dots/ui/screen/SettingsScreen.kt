package io.github.pavelya.dots.ui.screen

import androidx.annotation.StringRes
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.pavelya.dots.R
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.BoardSize
import io.github.pavelya.dots.data.Difficulty
import io.github.pavelya.dots.data.GameLength

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    loggedGames: Int,
    onShareLog: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScreenHeader(R.string.home_settings, onBack)

        Section(R.string.settings_board)
        Choices(
            options = BoardSize.entries,
            selected = settings.boardSize,
            label = { stringResource(R.string.settings_board_size, it.points, it.points) },
            onSelect = { onChange(settings.copy(boardSize = it)) },
        )

        Section(R.string.settings_length)
        Choices(
            options = GameLength.entries,
            selected = settings.length,
            label = {
                val dots = AppSettings.dotsFor(settings.boardSize, it)
                pluralStringResource(if (it == GameLength.SHORT) R.plurals.settings_length_short else R.plurals.settings_length_long, dots, dots)
            },
            onSelect = { onChange(settings.copy(length = it)) },
        )

        Section(R.string.settings_computer)
        Choices(
            options = Difficulty.entries,
            selected = settings.difficulty,
            label = {
                stringResource(
                    when (it) {
                        Difficulty.EASY -> R.string.difficulty_easy
                        Difficulty.NORMAL -> R.string.difficulty_normal
                        Difficulty.HARD -> R.string.difficulty_hard
                    },
                )
            },
            onSelect = { onChange(settings.copy(difficulty = it)) },
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
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
