package io.github.pavelya.dots.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dots.campaign.Tutorial
import dots.campaign.Tutorial.Lesson
import dots.campaign.Tutorial.Prompt
import io.github.pavelya.dots.R
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.ui.theme.Palette

/**
 * The tutorial's step note (the wireframes' "Tutorial level"): the step and its lesson in small
 * capitals, one sentence in handwriting, and Skip. In level 1, when dots are placed by two taps,
 * a line says that drag can be chosen in Settings (decision 0024). The sentence follows the
 * placing [method], so the tutorial works with either. TalkBack reads each new sentence.
 */
@Composable
internal fun TutorialNote(step: Tutorial.Step, method: PlacingMethod, style: Palette, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.tutorial_step, step.number, Tutorial.LESSONS, stringResource(lessonName(step.lesson)))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .rotate(-1f)
            .shadow(4.dp, RectangleShape, clip = false)
            .background(style.card)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(label.uppercase(), style = STEP_LABEL, color = style.graphite)
            Text(stringResource(sentence(step, method)), style = SENTENCE, color = style.pencil)
            if (step.lesson == Lesson.PLACING && method == PlacingMethod.TWO_TAPS) {
                Text(stringResource(R.string.tutorial_drag_in_settings), style = NoteText.small, color = style.graphite)
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onSkip)
                .padding(horizontal = 10.dp),
        ) {
            Text(stringResource(R.string.tutorial_skip), style = NoteText.label, color = style.graphite, textDecoration = TextDecoration.Underline)
        }
    }
}

/** The wireframe's step label: 11 sp, extra bold, spaced out. */
private val STEP_LABEL: TextStyle
    @Composable @ReadOnlyComposable
    get() = NoteText.small.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.06.em)

/** The step's sentence in handwriting (Caveat 24 on the wireframe, V-03). */
private val SENTENCE = HAND.copy(fontSize = 22.sp, lineHeight = 26.sp)

@StringRes
private fun lessonName(lesson: Lesson): Int = when (lesson) {
    Lesson.PLACING -> R.string.tutorial_lesson_placing
    Lesson.LINKING -> R.string.tutorial_lesson_linking
    Lesson.CAPTURING -> R.string.tutorial_lesson_capturing
}

/** The sentence for [step] when dots are placed by [method]. */
@StringRes
internal fun sentence(step: Tutorial.Step, method: PlacingMethod): Int {
    val drag = method == PlacingMethod.DRAG
    return when (step.prompt) {
        Prompt.PLACE -> if (drag) R.string.tutorial_place_drag else R.string.tutorial_place
        Prompt.CONFIRM -> if (drag) R.string.tutorial_confirm_drag else R.string.tutorial_confirm
        Prompt.JOIN -> if (drag) R.string.tutorial_join_drag else R.string.tutorial_join
        Prompt.CLOSE -> if (drag) R.string.tutorial_close_drag else R.string.tutorial_close
        Prompt.BLOCK -> R.string.tutorial_block
        Prompt.SURROUND -> when {
            step.point == null -> R.string.tutorial_surround_anywhere
            drag -> R.string.tutorial_surround_drag
            else -> R.string.tutorial_surround
        }
    }
}
