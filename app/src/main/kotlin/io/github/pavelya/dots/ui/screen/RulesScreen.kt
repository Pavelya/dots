package io.github.pavelya.dots.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dots.engine.GameState
import dots.engine.Pos
import io.github.pavelya.dots.R
import io.github.pavelya.dots.ui.board.BoardStyle
import io.github.pavelya.dots.ui.board.BoardView

/** The three rule pictures, as real engine positions, so a picture can never show an illegal game. */
object RulePictures {
    val chain: GameState = GameState.fromAscii(
        """
        .......
        .R.....
        ..R.B..
        ..RR.B.
        .......
        """,
    )

    val capture: GameState = GameState.fromAscii(
        """
        .......
        ...R...
        ..RBR..
        .......
        ...B...
        .......
        """,
    ).play(Pos(3, 3)).state

    val edge: GameState = GameState.fromAscii(
        """
        ......
        RR....
        B.....
        RR....
        ......
        """,
    ).play(Pos(1, 2)).state
}

@Composable
fun RulesScreen(onBack: () -> Unit) {
    val style = BoardStyle.plain(MaterialTheme.colorScheme, isSystemInDarkTheme())
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(R.string.home_rules, onBack)
        Text(stringResource(R.string.rules_goal), style = MaterialTheme.typography.titleMedium)
        Rule(RulePictures.chain, null, R.string.rules_chain, R.string.rules_chain_picture, style)
        Rule(RulePictures.capture, Pos(3, 3), R.string.rules_capture, R.string.rules_capture_picture, style)
        Rule(RulePictures.edge, Pos(1, 2), R.string.rules_edge, R.string.rules_edge_picture, style)
        Text(stringResource(R.string.rules_end), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.game_hint), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Rule(state: GameState, lastMove: Pos?, @StringRes text: Int, @StringRes picture: Int, style: BoardStyle) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        BoardView(state, style, stringResource(picture), Modifier.width(120.dp), lastMove = lastMove, showLinks = true)
        Text(stringResource(text), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}
