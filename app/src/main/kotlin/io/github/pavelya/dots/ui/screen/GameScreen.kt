package io.github.pavelya.dots.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dots.engine.GameState
import dots.engine.Player
import io.github.pavelya.dots.R
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.game.GameViewModel

/**
 * What only a campaign level adds to the board: the level's [title] in the top row, the
 * [mission] note's content (A-03) and the [boosters] in the tool bar (A-05, with Undo among them,
 * decision 0023). Free play and two players leave it out.
 */
class CampaignParts(
    val title: String,
    val mission: @Composable () -> Unit,
    val boosters: @Composable RowScope.() -> Unit,
)

/**
 * The game board, laid out as the canvas game frames (docs/design/canvas): see [GameBoard]. The
 * pause icon, or Back, opens Pause over the board; when the game ends, Result opens over the final
 * board (docs/design/flows.md, "Around the board"). [sound] is the setting the sound icon in Pause
 * turns on or off through [onSound].
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onToMap: () -> Unit,
    onRules: () -> Unit,
    sound: Boolean = true,
    onSound: (Boolean) -> Unit = {},
    campaign: CampaignParts? = null,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val state = ui.state
    val names = Names(ui.vsComputer, viewModel.human)
    // Saved, so Pause is still open after Rules, opened from it, closes.
    var paused by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = !state.isOver) { paused = !paused }
    Box {
        GameBoard(
            ui = ui,
            human = viewModel.human,
            campaign = campaign,
            onTap = viewModel::onTap,
            onUndo = viewModel::undo,
            onPause = { paused = true },
            // Behind Pause, the board is out of reach, also for TalkBack.
            modifier = if (paused && !state.isOver) Modifier.clearAndSetSemantics {} else Modifier,
        )
        if (paused && !state.isOver) {
            PauseNote(
                sound = sound,
                onSound = onSound,
                onResume = { paused = false },
                onRestart = { paused = false; viewModel.playAgain() },
                onToMap = onToMap,
                onRules = onRules,
            )
        }
    }
    if (state.isOver) Result(state, names, onPlayAgain = { paused = false; viewModel.playAgain() }, onMap = onToMap)
}

const val BOARD_TAG = "board"

/** How players are called: "You" and "Computer" against the computer, colours between two people. */
internal class Names(val vsComputer: Boolean, val human: Player) {
    @Composable
    fun of(player: Player): String = when {
        !vsComputer -> playerName(player)
        player == human -> stringResource(R.string.player_you)
        else -> stringResource(R.string.player_computer)
    }
}

/** Over the final board: the outcome, Play again, and a small Map. Back goes to the map. A-03, A-07 and A-08 build the designed Result. */
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

/** The state of the game in one sentence, for TalkBack: the board's description. */
@Composable
internal fun status(ui: GameUiState, names: Names): String {
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
