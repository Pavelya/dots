package io.github.pavelya.dots.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.pavelya.dots.R

/**
 * Play a game, over the map: two big cards, Free play and Two players. A card starts a game at
 * once, or resumes the saved one of that kind. A-08 adds the link that changes board size and
 * game length.
 *
 * @param saved whether the saved game is against the computer (true), between two people (false),
 *   or there is none (null)
 */
@Composable
fun PlayAGameNote(saved: Boolean?, onFreePlay: () -> Unit, onTwoPlayers: () -> Unit, onClose: () -> Unit) {
    Note {
        Text(stringResource(R.string.map_play), style = MaterialTheme.typography.headlineSmall)
        PlayCard(R.string.play_free, R.string.play_free_detail, resume = saved == true, onClick = onFreePlay)
        PlayCard(R.string.play_two, R.string.play_two_detail, resume = saved == false, onClick = onTwoPlayers)
        TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
    }
}

@Composable
private fun PlayCard(@StringRes title: Int, @StringRes detail: Int, resume: Boolean, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(if (resume) R.string.play_resume else detail), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
