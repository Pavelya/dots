package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dots.engine.GameState
import dots.engine.Player
import io.github.pavelya.dots.R
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.ui.board.BoardStyle
import io.github.pavelya.dots.ui.board.BoardView

@Composable
fun GameScreen(viewModel: GameViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val state = ui.state
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.game_score, state.score(Player.RED), state.score(Player.BLUE)),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(status(state), style = MaterialTheme.typography.bodyLarge)
        BoardView(
            state = state,
            style = BoardStyle.plain(MaterialTheme.colorScheme, isSystemInDarkTheme()),
            description = stringResource(R.string.board_description, state.width, state.height, status(state)),
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            lastMove = ui.lastMove,
            preview = ui.preview,
            onTap = viewModel::onTap,
        )
        Text(stringResource(R.string.game_hint), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}

@Composable
private fun status(state: GameState): String {
    if (state.isOver) {
        val winner = state.winner ?: return stringResource(R.string.game_draw, state.score(Player.RED))
        return stringResource(R.string.game_won, playerName(winner), state.score(winner), state.score(winner.opponent))
    }
    val left = state.dotsLeft(state.toMove)
    return if (left == null) {
        stringResource(R.string.game_to_move, playerName(state.toMove))
    } else {
        pluralStringResource(R.plurals.game_to_move_dots, left, playerName(state.toMove), left)
    }
}

@Composable
fun playerName(player: Player): String =
    stringResource(if (player == Player.RED) R.string.player_red else R.string.player_blue)
