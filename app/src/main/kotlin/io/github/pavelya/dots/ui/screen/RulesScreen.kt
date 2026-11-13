package io.github.pavelya.dots.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dots.engine.GameState
import dots.engine.Pos
import io.github.pavelya.dots.R
import io.github.pavelya.dots.ui.board.BoardStyle
import io.github.pavelya.dots.ui.board.BoardView
import io.github.pavelya.dots.ui.theme.Palette

/** The rules' pictures, as real engine positions, so a picture can never show an illegal game. */
object RulePictures {
    /** Placing: three red dots linked into a chain, one link diagonal, and a blue dot. */
    val placing: GameState = GameState.fromAscii(
        """
        .....
        ..RR.
        .R...
        ..B..
        .....
        """,
    )

    /** Capturing: the fourth red dot closes the ring around the blue one. */
    val capture: GameState = GameState.fromAscii(
        """
        .....
        ..R..
        .RBR.
        .....
        .....
        """,
    ).play(Pos(2, 3)).state

    /** Empty rings: a red ring around no blue dot captures nothing. */
    val emptyRing: GameState = GameState.fromAscii(
        """
        .....
        ..R..
        .R.R.
        ..R..
        .....
        """,
    )

    /** The border: a blue dot on the edge, with red dots all round it inside the board, is safe. */
    val border: GameState = GameState.fromAscii(
        """
        .....
        RR...
        B....
        RR...
        .....
        """,
    ).play(Pos(1, 2)).state

    /** How a game ends: two dots each, the same number for both sides. */
    val end: GameState = GameState.fromAscii(
        """
        .....
        .R.B.
        .....
        .B.R.
        .....
        """,
    )
}

/**
 * How to play (the wireframes' "Rules"): placing, capturing, empty rings, the border and how a game
 * ends, each with a small board picture, and Play the tutorial ([onPlayTutorial]) as the one big
 * button, always in reach below the list.
 */
@Composable
fun RulesScreen(onBack: () -> Unit, onPlayTutorial: () -> Unit) {
    val style = Palette.current
    val boardStyle = BoardStyle.current
    Column(Modifier.fillMaxSize().background(style.paper).padding(bottom = 16.dp)) {
        Column(Modifier.padding(horizontal = 16.dp)) { ScreenHeader(R.string.rules_title, onBack) }
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        ) {
            Rule(RulePictures.placing, null, R.string.rules_placing, R.string.rules_placing_text, R.string.rules_placing_picture, style, boardStyle)
            Rule(RulePictures.capture, Pos(2, 3), R.string.rules_capturing, R.string.rules_capturing_text, R.string.rules_capturing_picture, style, boardStyle)
            Rule(RulePictures.emptyRing, null, R.string.rules_empty, R.string.rules_empty_text, R.string.rules_empty_picture, style, boardStyle)
            Rule(RulePictures.border, Pos(1, 2), R.string.rules_border, R.string.rules_border_text, R.string.rules_border_picture, style, boardStyle)
            Rule(RulePictures.end, null, R.string.rules_end, R.string.rules_end_text, R.string.rules_end_picture, style, boardStyle)
        }
        BigButton(stringResource(R.string.play_tutorial), style, onPlayTutorial, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
    }
}

@Composable
private fun Rule(
    state: GameState,
    lastMove: Pos?,
    @StringRes title: Int,
    @StringRes text: Int,
    @StringRes picture: Int,
    style: Palette,
    boardStyle: BoardStyle,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        BoardView(
            state,
            boardStyle,
            stringResource(picture),
            Modifier.size(72.dp).shadow(2.dp, RectangleShape, clip = false),
            lastMove = lastMove,
            showLinks = true,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), style = HAND, color = style.pencil)
            Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, color = style.pencil)
        }
    }
}
