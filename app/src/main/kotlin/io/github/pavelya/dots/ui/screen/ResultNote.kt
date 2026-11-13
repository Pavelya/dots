package io.github.pavelya.dots.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dots.campaign.Level
import dots.campaign.Mission
import dots.campaign.Stars
import dots.engine.Booster
import dots.engine.Player
import io.github.pavelya.dots.R
import io.github.pavelya.dots.data.LevelEnd
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.ui.theme.Palette

/**
 * Result: one note for every mode, taped over the final board (canvas frame `result-win`; the
 * wireframes' Win and Loss; docs/design/flows.md, "One board, one result"). The outcome, the score,
 * and in the campaign the stars and anything newly opened. One big button and one small one:
 *
 * - Campaign win: Next ([onNext]), with a small Retry.
 * - Campaign failure or draw: Retry, with a small Map. After the third failure in a row, Retry
 *   with a free Hint ([onAgainWithHint]) as well.
 * - Free play and two players: Play again, with a small Map.
 *
 * Retry and Play again call [onAgain]. Back goes to the map ([onMap]). The daily puzzle's Result
 * (Share) comes with A-07.
 */
@Composable
internal fun ResultNote(
    ui: GameUiState,
    names: Names,
    onNext: () -> Unit,
    onAgain: () -> Unit,
    onAgainWithHint: () -> Unit,
    onMap: () -> Unit,
) {
    val style = Palette.current
    BackHandler(onBack = onMap)
    val level = ui.level
    val end = ui.levelEnd
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } }, // the final board takes no taps
    ) {
        TapedNote(style, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 22.dp), angle = -1.5f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            ) {
                if (level != null && end != null) {
                    LevelResult(level, end, style, onNext, onAgain, onAgainWithHint, onMap)
                } else {
                    FreeResult(ui, names, style, onAgain, onMap)
                }
            }
        }
    }
}

@Composable
private fun LevelResult(
    level: Level,
    end: LevelEnd,
    style: Palette,
    onNext: () -> Unit,
    onAgain: () -> Unit,
    onAgainWithHint: () -> Unit,
    onMap: () -> Unit,
) {
    val result = end.result
    val status = result.status
    if (result.isPassed) {
        Headline(stringResource(R.string.result_won), style.red)
        StarRow(result.stars, style)
        val captured = pluralStringResource(R.plurals.result_captured, status.captured, status.captured)
        val score = result.starScore
        Text(
            if (score == null) captured else stringResource(R.string.result_line, captured, starScoreText(level, score)),
            style = NoteText.body,
            color = style.pencil,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        val targets = level.stars
        if (targets != null && result.stars < Stars.MAX) {
            val (need, target) = if (result.stars < 2) R.string.result_two_stars_need to targets.two else R.string.result_three_stars_need to targets.three
            Text(stringResource(need, starScoreText(level, target)), style = NoteText.small, color = style.graphite, textAlign = TextAlign.Center)
        }
        Opened(end, style)
        Buttons {
            SquareButton(Doodles.RETRY, stringResource(R.string.result_retry), style, onAgain)
            BigButton(stringResource(R.string.result_next), style, onNext, Modifier.weight(1f), after = Doodles.NEXT)
        }
    } else {
        val missed = missedBy(level, status)
        Headline(stringResource(if (missed == 1) R.string.result_so_close else R.string.result_not_this_time), style.blue)
        Text(failureText(level, status), style = NoteText.title, color = style.pencil, textAlign = TextAlign.Center)
        val capture = level.mission as? Mission.Capture
        if (capture != null) CaptureBar(status.captured, capture.dots, style)
        Buttons {
            SquareButton(Doodles.MAP, stringResource(R.string.result_map), style, onMap)
            BigButton(stringResource(R.string.result_retry), style, onAgain, Modifier.weight(1f), before = Doodles.RETRY)
        }
        if (end.failed >= LevelEnd.HELP_AFTER_FAILURES) HelpOffer(style, onAgainWithHint)
    }
}

@Composable
private fun FreeResult(ui: GameUiState, names: Names, style: Palette, onAgain: () -> Unit, onMap: () -> Unit) {
    val state = ui.state
    val winner = state.winner
    val (headline, ink) = when {
        winner == null -> stringResource(R.string.result_draw) to style.graphite
        !names.vsComputer -> stringResource(R.string.result_side_wins, playerName(winner)) to style.ink(winner)
        winner == names.human -> stringResource(R.string.result_won) to style.red
        else -> stringResource(R.string.result_computer_won) to style.blue
    }
    Headline(headline, ink)
    val description = outcome(state, names)
    val first = if (names.vsComputer) names.human else Player.RED
    Text(
        stringResource(R.string.result_score, state.score(first), state.score(first.opponent)),
        style = NoteText.title,
        color = style.pencil,
        modifier = Modifier.semantics { contentDescription = description },
    )
    Buttons {
        SquareButton(Doodles.MAP, stringResource(R.string.result_map), style, onMap)
        BigButton(stringResource(R.string.game_play_again), style, onAgain, Modifier.weight(1f))
    }
}

/** The outcome in handwriting, as the note's heading. */
@Composable
private fun Headline(text: String, ink: Color) {
    Text(
        text,
        style = NoteText.display,
        color = ink,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().semantics { heading() },
    )
}

/** Three stars, the middle one bigger and higher (draw.py, the `win` frame); [earned] of them filled. */
@Composable
private fun StarRow(earned: Int, style: Palette) {
    val description = stringResource(R.string.result_stars, earned)
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        for (i in 0 until Stars.MAX) {
            val filled = i < earned
            val middle = i == 1
            ResultStar(filled, if (middle) 56.dp else 48.dp, style, Modifier.offset(y = if (middle) (-4).dp else 4.dp))
        }
    }
}

@Composable
private fun ResultStar(filled: Boolean, size: Dp, style: Palette, modifier: Modifier) {
    Icon(
        remember(filled, style) { Doodles.star(if (filled) style.red else style.graphite, filled) },
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = modifier.size(size),
    )
}

/** A world or level the pass has just opened, if any. */
@Composable
private fun Opened(end: LevelEnd, style: Palette) {
    val world = end.opened.worlds.firstOrNull()
    val level = end.opened.levels.firstOrNull()
    val text = when {
        world != null -> stringResource(R.string.result_world_open, world.number, world.name)
        level != null -> stringResource(R.string.result_level_open, level.code)
        else -> return
    }
    Text(text, style = NoteText.label, color = style.blue, textAlign = TextAlign.Center)
}

/** How far a capture mission got: a bar and "4 / 5" (the wireframes' Loss). */
@Composable
private fun CaptureBar(captured: Int, needed: Int, style: Palette) {
    val shown = captured.coerceIn(0, needed)
    val description = stringResource(R.string.result_progress, shown, needed)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
    ) {
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(style.bluePale)) {
            if (shown > 0) Box(Modifier.fillMaxWidth(shown.toFloat() / needed).height(8.dp).background(style.red))
        }
        Text("$shown / $needed", style = NoteText.label, color = style.pencil)
    }
}

/** The note's buttons: the small one, then the big one. */
@Composable
private fun Buttons(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        content = content,
    )
}

/** After the third failure in a row: play again with a free Hint (the wireframes' Loss). */
@Composable
private fun HelpOffer(style: Palette, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .toolOutline(style.red, dashed = true, shape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        BoosterIcon(Booster.HINT, style.red, Modifier.size(20.dp))
        Text(stringResource(R.string.result_free_hint), style = NoteText.label, color = style.red)
    }
}
