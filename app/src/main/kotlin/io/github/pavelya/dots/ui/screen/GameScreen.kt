package io.github.pavelya.dots.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dots.engine.GameState
import dots.engine.Player
import io.github.pavelya.dots.R
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.ui.board.BoardStyle
import io.github.pavelya.dots.ui.board.BoardView

/**
 * The game board. The pause icon, or Back, opens Pause over the board; when the game ends, Result
 * opens over the final board (docs/design/flows.md, "Around the board").
 */
@Composable
fun GameScreen(viewModel: GameViewModel, onToMap: () -> Unit, onRules: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val state = ui.state
    val names = Names(ui.vsComputer, viewModel.human)
    // Saved, so Pause is still open after Rules, opened from it, closes.
    var paused by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = !state.isOver) { paused = true }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.game_score, names.of(Player.RED), state.score(Player.RED), names.of(Player.BLUE), state.score(Player.BLUE)),
                style = MaterialTheme.typography.titleLarge,
            )
            if (!state.isOver) {
                PauseButton(onClick = { paused = true }, modifier = Modifier.align(Alignment.CenterEnd))
            }
        }
        Text(
            status(ui, names),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        BoardView(
            state = state,
            style = BoardStyle.plain(MaterialTheme.colorScheme, isSystemInDarkTheme()),
            description = stringResource(R.string.board_description, state.width, state.height, status(ui, names)),
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).testTag(BOARD_TAG),
            lastMove = ui.lastMove,
            preview = ui.preview,
            dangers = ui.dangers,
            onTap = viewModel::onTap,
        )
        if (!state.isOver) {
            Text(stringResource(R.string.game_hint), style = MaterialTheme.typography.bodySmall)
            if (ui.vsComputer) {
                OutlinedButton(onClick = viewModel::undo, enabled = ui.canUndo) { Text(stringResource(R.string.game_undo)) }
            }
        }
    }
    when {
        state.isOver -> Result(state, names, onPlayAgain = { paused = false; viewModel.playAgain() }, onMap = onToMap)
        paused -> Pause(
            onResume = { paused = false },
            onRestart = { paused = false; viewModel.playAgain() },
            onToMap = onToMap,
            onRules = onRules,
        )
    }
}

const val BOARD_TAG = "board"

/** How players are called: "You" and "Computer" against the computer, colours between two people. */
private class Names(val vsComputer: Boolean, val human: Player) {
    @Composable
    fun of(player: Player): String = when {
        !vsComputer -> playerName(player)
        player == human -> stringResource(R.string.player_you)
        else -> stringResource(R.string.player_computer)
    }
}

/** Over the board: Resume, Restart after asking, To the map, and How to play. Back resumes. */
@Composable
private fun Pause(onResume: () -> Unit, onRestart: () -> Unit, onToMap: () -> Unit, onRules: () -> Unit) {
    var asking by rememberSaveable { mutableStateOf(false) }
    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            text = { Text(stringResource(R.string.restart_question)) },
            confirmButton = { TextButton(onClick = onRestart) { Text(stringResource(R.string.pause_restart)) } },
            dismissButton = { TextButton(onClick = { asking = false }) { Text(stringResource(R.string.restart_cancel)) } },
        )
        return
    }
    Dialog(onDismissRequest = onResume) {
        Note {
            Text(stringResource(R.string.pause), style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onResume, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(R.string.pause_resume))
            }
            TextButton(onClick = { asking = true }) { Text(stringResource(R.string.pause_restart)) }
            TextButton(onClick = onToMap) { Text(stringResource(R.string.pause_to_map)) }
            TextButton(onClick = onRules) { Text(stringResource(R.string.map_rules)) }
        }
    }
}

/** Over the final board: the outcome, Play again, and a small Map. Back goes to the map. */
@Composable
private fun Result(state: GameState, names: Names, onPlayAgain: () -> Unit, onMap: () -> Unit) {
    Dialog(onDismissRequest = onMap, properties = DialogProperties(dismissOnClickOutside = false)) {
        Note {
            Text(outcome(state, names), style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(R.string.game_play_again))
            }
            TextButton(onClick = onMap) { Text(stringResource(R.string.result_map)) }
        }
    }
}

/** Two bars, drawn here until the hand-drawn icons arrive (V-03). */
@Composable
private fun PauseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.pause)
    IconButton(onClick = onClick, modifier = modifier.semantics { contentDescription = label }) {
        val ink = MaterialTheme.colorScheme.onSurface
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(2) { Box(Modifier.size(width = 5.dp, height = 18.dp).background(ink)) }
        }
    }
}

@Composable
private fun outcome(state: GameState, names: Names): String {
    val winner = state.winner ?: return stringResource(R.string.game_draw, state.score(Player.RED))
    val score = state.score(winner)
    val other = state.score(winner.opponent)
    return when {
        !names.vsComputer -> stringResource(R.string.game_won, playerName(winner), score, other)
        winner == names.human -> stringResource(R.string.game_you_win, score, other)
        else -> stringResource(R.string.game_computer_wins, score, other)
    }
}

@Composable
private fun status(ui: GameUiState, names: Names): String {
    val state = ui.state
    if (state.isOver) return outcome(state, names)
    if (ui.thinking) return stringResource(R.string.game_thinking)
    val left = state.dotsLeft(state.toMove)
    val mover = state.toMove
    return when {
        names.vsComputer && mover == names.human && left != null ->
            pluralStringResource(R.plurals.game_your_turn_dots, left, left)
        names.vsComputer && mover == names.human -> stringResource(R.string.game_your_turn)
        left != null -> pluralStringResource(R.plurals.game_to_move_dots, left, names.of(mover), left)
        else -> stringResource(R.string.game_to_move, names.of(mover))
    }
}

@Composable
fun playerName(player: Player): String =
    stringResource(if (player == Player.RED) R.string.player_red else R.string.player_blue)
