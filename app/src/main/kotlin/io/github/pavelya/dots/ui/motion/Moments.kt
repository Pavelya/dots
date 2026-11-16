package io.github.pavelya.dots.ui.motion

import androidx.compose.runtime.Composable
import io.github.pavelya.dots.game.BoardMoment
import kotlin.math.max

/** How a capture of this moment plays, or null when the dot captured nothing. */
val BoardMoment.capturing: Capturing? get() = if (captures.isEmpty()) null else Capturing.of(captured)

/**
 * How long the dot's own moment plays: placing, the computer's ripples and any capture. The
 * computer answers no sooner, and the end of a game starts after it (motion.md, rule 2 and "End
 * of game").
 */
val BoardMoment.playOut: Float
    get() = max(max(Placing.LENGTH, if (announced) Announce.LENGTH else 0f), capturing?.length ?: 0f)

/** The whole moment, with the pulses of any new threat. */
val BoardMoment.length: Float get() = max(playOut, if (freshThreats.isEmpty()) 0f else Threat.LENGTH)

/** The clock of the board's moment: it plays each new dot's moment once. */
@Composable
fun rememberMomentTimeline(moment: BoardMoment?): Timeline = rememberTimeline(moment?.id, moment?.length ?: 0f)
