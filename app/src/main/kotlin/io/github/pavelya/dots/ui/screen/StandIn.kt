package io.github.pavelya.dots.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.pavelya.dots.R

/**
 * A full screen whose real content a later task builds ([task], for example "A-11"). It shows the
 * screen's name and only the links the flow gives it (docs/design/flows.md), so navigation can be
 * built and tested before the screen itself.
 */
@Composable
fun StandInScreen(@StringRes title: Int, task: String, onBack: () -> Unit, links: @Composable ColumnScope.() -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(title, onBack)
        Text(stringResource(R.string.stand_in, task), style = MaterialTheme.typography.bodyLarge)
        links()
    }
}

/** A note laid over the map whose real content a later task builds; see [StandInScreen]. */
@Composable
fun StandInNote(title: String, task: String, onClose: () -> Unit, links: @Composable ColumnScope.() -> Unit = {}) {
    Note {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.stand_in, task), style = MaterialTheme.typography.bodyLarge)
        links()
        TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
    }
}

/** A card laid over the map or the board: Level card, Pause, Result and the other notes. */
@Composable
fun Note(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}
