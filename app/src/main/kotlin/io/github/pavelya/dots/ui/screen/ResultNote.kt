package io.github.pavelya.dots.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.sp
import dots.campaign.Cosmetic
import io.github.pavelya.dots.data.LookPart
import io.github.pavelya.dots.data.PieceStyleId
import io.github.pavelya.dots.data.RingEffectId
import io.github.pavelya.dots.data.ThemeId
import io.github.pavelya.dots.ui.theme.GameTheme
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dots.campaign.Level
import dots.campaign.Mission
import dots.campaign.MissionStatus
import dots.campaign.Stars
import dots.daily.DailyPuzzle
import dots.daily.DailyShare
import dots.engine.Booster
import dots.engine.Player
import io.github.pavelya.dots.R
import io.github.pavelya.dots.data.DailyEnd
import io.github.pavelya.dots.data.LevelEnd
import io.github.pavelya.dots.game.GameUiState
import io.github.pavelya.dots.ui.motion.Beats
import io.github.pavelya.dots.ui.motion.Cues
import io.github.pavelya.dots.ui.motion.LocalReduceMotion
import io.github.pavelya.dots.ui.motion.PlayCues
import io.github.pavelya.dots.ui.motion.ResultReveal
import io.github.pavelya.dots.ui.motion.Timeline
import io.github.pavelya.dots.ui.motion.rememberTimeline
import io.github.pavelya.dots.ui.motion.writeIn
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
 * - Daily puzzle solved (the wireframes' Daily result): the moves against the fewest possible, the
 *   streak, what the share text shows, and the time to the next puzzle ([toNextPuzzle]); Share
 *   ([onShare], with the share text), with a small Map.
 * - Daily puzzle failed: Retry, with a small Map, and the time to the next puzzle.
 *
 * Retry and Play again call [onAgain]. Back goes to the map ([onMap]).
 *
 * With [sounds] (the game ended on this screen), the outcome's sound plays as the headline writes
 * in, and a stamp as each earned star lands; a solved daily puzzle's streak goes up with a sparkle
 * (docs/design/sound.md).
 *
 * Motion (docs/design/motion.md, "End of game" and "Star reveal"): the note slides up from below,
 * tips and settles at its slant; then the headline writes in and each star is stamped, or drawn in
 * outline when not earned. The buttons work at once; a tap elsewhere on the note finishes the reveal.
 */
@Composable
internal fun ResultNote(
    ui: GameUiState,
    names: Names,
    onNext: () -> Unit,
    onAgain: () -> Unit,
    onAgainWithHint: () -> Unit,
    onMap: () -> Unit,
    onShare: (String) -> Unit = {},
    toNextPuzzle: () -> Long = { 0L },
    sounds: Boolean = false,
) {
    val style = Palette.current
    BackHandler(onBack = onMap)
    val level = ui.level
    val end = ui.levelEnd
    val daily = ui.daily
    val dailyEnd = ui.dailyEnd
    val reveal = rememberTimeline(ui.round, ResultReveal.LENGTH)
    val still = LocalReduceMotion.current
    val stars = end?.result?.takeIf { it.isPassed }?.stars ?: 0
    PlayCues(ui.round.takeIf { sounds }, reveal, beats = { Beats.result(stars, still) }) {
        Cues.result(Cues.headline(ui, names.human), stars, streak = dailyEnd?.passed == true, still = still)
    }
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(reveal) { detectTapGestures { reveal.finish() } }, // the final board takes no taps
    ) {
        val coming = Modifier.graphicsLayer {
            translationY = (1f - ResultReveal.slide(reveal.now)) * (size.height + 22.dp.toPx())
            rotationZ = ResultReveal.tip(reveal.now)
        }
        TapedNote(style, coming.padding(start = 20.dp, end = 20.dp, bottom = 22.dp), angle = -1.5f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            ) {
                if (level != null && end != null) {
                    LevelResult(level, end, style, onNext, onAgain, onAgainWithHint, onMap, reveal)
                } else if (daily != null && dailyEnd != null) {
                    DailyResult(daily, dailyEnd, ui.mission, style, toNextPuzzle, onShare, onAgain, onMap, reveal)
                } else {
                    FreeResult(ui, names, style, onAgain, onMap, reveal)
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
    reveal: Timeline,
) {
    val result = end.result
    val status = result.status
    if (result.isPassed) {
        Headline(stringResource(R.string.result_won), style.red, reveal)
        StarRow(result.stars, style, reveal)
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
        for (cosmetic in end.earned) EarnedLine(cosmetic, style)
        Buttons {
            SquareButton(Doodles.RETRY, stringResource(R.string.result_retry), style, onAgain)
            BigButton(stringResource(R.string.result_next), style, onNext, Modifier.weight(1f), after = Doodles.NEXT)
        }
    } else {
        val missed = missedBy(level, status)
        Headline(stringResource(if (missed == 1) R.string.result_so_close else R.string.result_not_this_time), style.blue, reveal)
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
private fun DailyResult(
    puzzle: DailyPuzzle,
    end: DailyEnd,
    status: MissionStatus?,
    style: Palette,
    toNextPuzzle: () -> Long,
    onShare: (String) -> Unit,
    onAgain: () -> Unit,
    onMap: () -> Unit,
    reveal: Timeline,
) {
    val share = end.share
    if (end.passed && share != null) {
        Headline(stringResource(R.string.daily_solved), style.red, reveal)
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            DailyFigure(end.moves.toString(), stringResource(R.string.daily_your_moves), style.pencil, Modifier.weight(1f))
            DailyFigure(end.best.toString(), stringResource(R.string.daily_fewest), style.pencil, Modifier.weight(1f))
            DailyFigure(end.streak.days.toString(), stringResource(R.string.daily_day_streak), style.red, Modifier.weight(1f))
        }
        ShareBox(end, style)
        NextPuzzleLine(toNextPuzzle, style)
        Buttons {
            SquareButton(Doodles.MAP, stringResource(R.string.result_map), style, onMap)
            BigButton(stringResource(R.string.daily_share), style, { onShare(share) }, Modifier.weight(1f))
        }
    } else {
        val level = puzzle.level
        val missed = status?.let { missedBy(level, it) }
        Headline(stringResource(if (missed == 1) R.string.result_so_close else R.string.result_not_this_time), style.blue, reveal)
        if (status != null) {
            Text(failureText(level, status), style = NoteText.title, color = style.pencil, textAlign = TextAlign.Center)
            val capture = level.mission as? Mission.Capture
            if (capture != null) CaptureBar(status.captured, capture.dots, style)
        }
        NextPuzzleLine(toNextPuzzle, style)
        Buttons {
            SquareButton(Doodles.MAP, stringResource(R.string.result_map), style, onMap)
            BigButton(stringResource(R.string.result_retry), style, onAgain, Modifier.weight(1f), before = Doodles.RETRY)
        }
    }
}

/** A number in handwriting over its label: the moves, the fewest possible, the streak. */
@Composable
private fun DailyFigure(value: String, label: String, ink: Color, modifier: Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clearAndSetSemantics { contentDescription = "$value $label" },
    ) {
        Text(value, style = NoteText.headline, color = ink)
        Text(label, style = NoteText.small, color = Palette.current.graphite, textAlign = TextAlign.Center)
    }
}

/**
 * What the share text shows, in a dashed box: its first line (the puzzle's number, the moves and
 * the best) and a square for each move of the attempt (red, the player captured; blue, the
 * computer did; empty, neither). Never the board or the moves.
 */
@Composable
private fun ShareBox(end: DailyEnd, style: Palette) {
    val head = stringResource(R.string.daily_what_you_share)
    val headline = end.headline.orEmpty()
    val shape = RoundedCornerShape(8.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .toolOutline(style.bluePale, dashed = true, shape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clearAndSetSemantics { contentDescription = "$head: $headline" },
    ) {
        Text(head.uppercase(), style = NoteText.small, fontWeight = FontWeight.ExtraBold, color = style.graphite)
        Text(headline, style = NoteText.label, color = style.pencil)
        ShareMarks(end.marks, style)
    }
}

@Composable
private fun ShareMarks(marks: List<DailyShare.Mark>, style: Palette) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 2.dp)) {
        for (mark in marks) {
            Canvas(Modifier.size(12.dp)) {
                val corner = CornerRadius(2.dp.toPx())
                when (mark) {
                    DailyShare.Mark.CAPTURED -> drawRoundRect(style.red, cornerRadius = corner)
                    DailyShare.Mark.LOST -> drawRoundRect(style.blue, cornerRadius = corner)
                    DailyShare.Mark.QUIET -> {
                        val inset = 0.5.dp.toPx()
                        drawRoundRect(
                            style.graphite,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - 2 * inset, size.height - 2 * inset),
                            cornerRadius = corner,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                }
            }
        }
    }
}

/** "Next puzzle in 14:22", kept up to date. */
@Composable
private fun NextPuzzleLine(toNextPuzzle: () -> Long, style: Palette) {
    val tick by rememberTicks()
    val left = remember(tick) { toNextPuzzle() }
    Text(
        stringResource(R.string.daily_next_in, countdown(left)),
        style = NoteText.small,
        color = style.graphite,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FreeResult(ui: GameUiState, names: Names, style: Palette, onAgain: () -> Unit, onMap: () -> Unit, reveal: Timeline) {
    val state = ui.state
    val winner = state.winner
    val (headline, ink) = when {
        winner == null -> stringResource(R.string.result_draw) to style.graphite
        !names.vsComputer -> stringResource(R.string.result_side_wins, playerName(winner)) to style.ink(winner)
        winner == names.human -> stringResource(R.string.result_won) to style.red
        else -> stringResource(R.string.result_computer_won) to style.blue
    }
    Headline(headline, ink, reveal)
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

/** The outcome in handwriting, as the note's heading. It writes in, left to right, once the note has settled. */
@Composable
private fun Headline(text: String, ink: Color, reveal: Timeline) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = NoteText.display,
            color = ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .semantics { heading() }
                .writeIn { ResultReveal.headline(reveal.now) },
        )
    }
}

/**
 * Three stars, the middle one bigger and higher (draw.py, the `win` frame); [earned] of them
 * filled. One after another, each earned star is stamped: it shrinks from 180% and turns from −15°
 * to straight. A star not earned is drawn in outline.
 */
@Composable
private fun StarRow(earned: Int, style: Palette, reveal: Timeline) {
    val description = stringResource(R.string.result_stars, earned)
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        for (i in 0 until Stars.MAX) {
            val filled = i < earned
            val middle = i == 1
            val stamp = Modifier
                .graphicsLayer {
                    val p = ResultReveal.star(reveal.now, i, filled)
                    if (filled) {
                        val size = if (p >= 1f) 1f else 1.8f - 0.8f * p
                        scaleX = size
                        scaleY = size
                        rotationZ = -15f * (1f - p)
                        alpha = if (p > 0f) 1f else 0f
                    }
                }
                .drawWithContent {
                    val p = ResultReveal.star(reveal.now, i, filled)
                    if (filled) drawContent() else clipRect(right = size.width * p) { this@drawWithContent.drawContent() }
                }
            ResultStar(filled, if (middle) 56.dp else 48.dp, style, Modifier.offset(y = if (middle) (-4).dp else 4.dp).then(stamp))
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

/**
 * A cosmetic the pass earned (the wireframes' Result: win, a theme earned; flows.md, Campaign): its
 * picture on the Shop's small board, "New theme: Star chart" and where it waits, in a dashed frame
 * in the earning ink. No button: it waits in the Shop, marked New.
 */
@Composable
private fun EarnedLine(cosmetic: Cosmetic, style: Palette) {
    val part: LookPart = ThemeId.of(cosmetic.id) ?: PieceStyleId.of(cosmetic.id) ?: RingEffectId.of(cosmetic.id) ?: return
    val title = when (part) {
        is ThemeId -> R.string.result_new_theme
        is PieceStyleId -> R.string.result_new_pieces
        is RingEffectId -> R.string.result_new_rings
    }
    val inUse = ThemeId.entries.firstOrNull { GameTheme.of(it) == GameTheme.current } ?: ThemeId.DEFAULT
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .drawBehind {
                val line = 1.5.dp.toPx()
                drawRoundRect(
                    style.highlighter,
                    topLeft = Offset(line / 2, line / 2),
                    size = Size(size.width - line, size.height - line),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(line, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                )
            }
            .clip(shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        val picture = Modifier.size(40.dp).clearAndSetSemantics {}
        when (part) {
            is ThemeId -> LookBoard(LOOK_SAMPLE, LOOK_SAMPLE_LAST, part, picture)
            is PieceStyleId -> LookBoard(LOOK_SAMPLE, LOOK_SAMPLE_LAST, inUse, picture, pieces = part)
            is RingEffectId -> LookBoard(LOOK_SAMPLE, LOOK_SAMPLE_LAST, inUse, picture, rings = part)
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(title, stringResource(lookName(part))),
                style = NoteText.label.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold),
                color = style.highlighter,
            )
            Text(
                pluralStringResource(R.plurals.result_earned_with, cosmetic.stars, cosmetic.stars),
                style = NoteText.small.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = style.graphite,
            )
        }
    }
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
