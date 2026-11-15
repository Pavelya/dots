package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dots.daily.Daily
import dots.daily.DailyClock
import dots.daily.DailyPuzzle
import dots.daily.Day
import dots.daily.Streak
import io.github.pavelya.dots.R
import io.github.pavelya.dots.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** What the big button of Today's puzzle does. */
enum class DailyAction {
    /** Starts an attempt. */
    PLAY,

    /** Brings back the unfinished attempt. */
    RESUME,

    /** The puzzle is solved: shows its Result over the final board. */
    VIEW_RESULT,
}

/**
 * Today's puzzle, a note over the map (the wireframes' Today's puzzle; canvas frame `map`, whose
 * taped note opens it): "Today's puzzle", the puzzle's number and date, a small picture of the
 * board, the mission, the [streak] and whether this week's freeze is left, and the time to the next
 * puzzle ([toNextPuzzle], kept up to date). One big button: Play, Resume or View result
 * ([action]). Close, Back or a tap on the map around it closes it.
 *
 * Before the first puzzle, or after the last one the app has, [puzzle] is null: the note says
 * when the puzzles start, or that new ones come with an update, and has no button.
 */
@Composable
fun DailyPuzzleNote(
    puzzle: DailyPuzzle?,
    today: Day,
    action: DailyAction,
    streak: Streak,
    toNextPuzzle: () -> Long,
    onAction: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = Palette.current
    val title = stringResource(R.string.daily_title)
    BoxWithConstraints(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier.fillMaxSize().semantics { paneTitle = title },
    ) {
        val picture = if (maxHeight < 700.dp) 88.dp else 112.dp
        // Read here: inside the Box below, the Box's own scope hides maxHeight.
        val noteHeight = maxHeight - 40.dp
        // A tap on the map around the note closes it; the note above takes its own taps.
        Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onClose() } })
        Box(
            Modifier
                .padding(start = 12.dp, end = 12.dp, bottom = 14.dp, top = 24.dp)
                .rotate(-1f),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = noteHeight)
                    .shadow(6.dp, RectangleShape, clip = false)
                    .background(style.card)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
                        Text(title, style = NoteText.display, color = style.red)
                        val date = dayText(puzzle?.day ?: today)
                        Text(
                            if (puzzle != null) stringResource(R.string.daily_number_and_date, puzzle.number, date) else date,
                            style = NoteText.small,
                            fontWeight = FontWeight.Bold,
                            color = style.graphite,
                        )
                    }
                    CloseButton(style, onClose)
                }
                if (puzzle == null) {
                    val first = Daily.FIRST_DAY
                    Text(
                        if (today < first) stringResource(R.string.daily_none_yet, dayText(first, year = true)) else stringResource(R.string.daily_none_left),
                        style = NoteText.body,
                        color = style.pencil,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        BoardPicture(puzzle.level, Modifier.width(picture))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(missionText(puzzle.mission), style = NoteText.title, color = style.pencil)
                            Text(stringResource(R.string.daily_same_for_everyone), style = NoteText.small, color = style.graphite)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        StreakFigure(streak, style, Modifier.weight(1f))
                        NextPuzzleFigure(toNextPuzzle, style, Modifier.weight(1f))
                    }
                    val label = when (action) {
                        DailyAction.PLAY -> R.string.daily_play
                        DailyAction.RESUME -> R.string.level_resume
                        DailyAction.VIEW_RESULT -> R.string.daily_view_result
                    }
                    BigButton(stringResource(label), style, onAction, Modifier.fillMaxWidth())
                }
            }
            Tape(style, Modifier.align(Alignment.TopEnd).offset(x = (-40).dp, y = (-8).dp).rotate(4f).size(width = 56.dp, height = 16.dp))
        }
    }
}

/** The streak in handwriting, with the flame, and whether this week's freeze is left. */
@Composable
private fun StreakFigure(streak: Streak, style: Palette, modifier: Modifier) {
    val days = pluralStringResource(R.plurals.daily_streak_days, streak.days, streak.days)
    val detail = stringResource(if (streak.freezeLeft) R.string.daily_freeze_left else R.string.daily_freeze_used)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.clearAndSetSemantics { contentDescription = "$days $detail" },
    ) {
        Icon(remember(style) { Doodles.icon(Doodles.FLAME, style.red) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
        Column {
            Text(days, style = HAND24, color = style.pencil)
            Text(detail, style = NoteText.small, color = style.graphite)
        }
    }
}

/** The time to the next puzzle in handwriting, kept up to date. */
@Composable
private fun NextPuzzleFigure(toNextPuzzle: () -> Long, style: Palette, modifier: Modifier) {
    val tick by rememberTicks()
    val left = remember(tick) { countdown(toNextPuzzle()) }
    val detail = stringResource(R.string.daily_to_next)
    Column(modifier.clearAndSetSemantics { contentDescription = "$left $detail" }) {
        Text(left, style = HAND24, color = style.pencil)
        Text(detail, style = NoteText.small, color = style.graphite)
    }
}

/** The canvas's 24 sp handwriting (draw.py, `HAND24`). */
internal val HAND24: TextStyle
    @Composable @ReadOnlyComposable
    get() = NoteText.headline.copy(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)

/**
 * Hours and minutes, as a clock shows them: `14:22`, `0:05`. Rounded up, so it reads `0:01` until
 * the last second.
 */
fun countdown(millis: Long): String {
    val minutes = (millis.coerceAtLeast(0) + 59_999) / 60_000
    return "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"
}

/**
 * A count that goes up every [periodMillis], for what changes with the time (the countdown to the
 * next puzzle). It waits on a background thread, so it never holds up a test's clock.
 */
@Composable
internal fun rememberTicks(periodMillis: Long = 1_000): State<Long> =
    remember(periodMillis) {
        flow {
            var n = 0L
            while (true) {
                delay(periodMillis)
                emit(++n)
            }
        }.flowOn(Dispatchers.Default)
    }.collectAsState(0L)

/** [day] as the player's language writes a date: "Sunday, November 8", with the year if [year]. */
@Composable
private fun dayText(day: Day, year: Boolean = false): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return remember(day, year, locale) {
        val skeleton = if (year) "EEEEdMMMMyyyy" else "EEEEdMMMM"
        val format = SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton), locale)
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.format(Date(day.epochDay * DailyClock.DAY_MILLIS))
    }
}
