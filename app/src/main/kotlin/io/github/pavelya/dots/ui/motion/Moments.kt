package io.github.pavelya.dots.ui.motion

import androidx.compose.runtime.Composable
import io.github.pavelya.dots.game.BoardMoment
import io.github.pavelya.dots.game.BoosterMoment
import io.github.pavelya.dots.game.StopMoment
import kotlin.math.max

/** How a capture of this moment plays, or null when the dot captured nothing. */
val BoardMoment.capturing: Capturing? get() = if (captures.isEmpty()) null else Capturing.of(captured)

/**
 * How long the dot's own moment plays: placing, the computer's ripples, any capture, and after a
 * booster the sparks of a double dot or a Hint's circles shrinking away. The computer answers no
 * sooner, and the end of a game starts after it (motion.md, rule 2 and "End of game").
 */
val BoardMoment.playOut: Float
    get() = maxOf(
        max(Placing.LENGTH, if (announced) Announce.LENGTH else 0f),
        capturing?.length ?: 0f,
        if (doubleDots != null) DoubleDotEffect.SPARK else 0f,
        if (hintBefore != null) HintEffect.SHRINK else 0f,
    )

/** The whole moment, with the pulses of any new threat. */
val BoardMoment.length: Float get() = max(playOut, if (freshThreats.isEmpty()) 0f else Threat.LENGTH)

/** How long the booster's effect plays: a bomb that broke a capture plays on while it is taken back. */
val BoosterMoment.length: Float get() = if (brokenFrom != null) BombEffect.BROKE_LENGTH else BoosterTimes.length(booster)

/** The clock of the board's moment: it plays each new dot's moment once. */
@Composable
fun rememberMomentTimeline(moment: BoardMoment?): Timeline = rememberTimeline(moment?.id, moment?.length ?: 0f)

/** The clock of a booster's effect: it plays each booster used once. */
@Composable
fun rememberBoosterTimeline(booster: BoosterMoment?): Timeline = rememberTimeline(booster?.id, booster?.length ?: 0f)

/** The clock of a stop (motion.md, "Stop and Done"): it plays each stop once. */
@Composable
fun rememberStopTimeline(stop: StopMoment?): Timeline = rememberTimeline(stop?.id, StopTimes.LENGTH)
