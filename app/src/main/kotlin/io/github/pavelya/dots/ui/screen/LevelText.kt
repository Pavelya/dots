package io.github.pavelya.dots.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dots.campaign.Level
import dots.campaign.Mission
import dots.campaign.MissionStatus
import dots.campaign.Reason
import dots.campaign.SpecialRule
import dots.campaign.StarMeasure
import dots.campaign.Strength
import dots.engine.Booster
import io.github.pavelya.dots.R

/*
 * The words the Level card, the level opening, the mission note and Result use for a level: its
 * mission, star targets, boss rule and how it ended. The engine gives the numbers (dots.campaign);
 * these turn them into sentences.
 */

@Composable
private fun dots(n: Int) = pluralStringResource(R.plurals.count_dots, n, n)

@Composable
private fun moves(n: Int) = pluralStringResource(R.plurals.count_moves, n, n)

/** The mission in one line: "Capture 2 dots in 5 moves". */
@Composable
fun missionText(mission: Mission): String = when (mission) {
    Mission.Win -> stringResource(R.string.mission_win)
    is Mission.WinBy -> stringResource(R.string.mission_win_by, dots(mission.margin))
    is Mission.WinWithFewerDots -> stringResource(R.string.mission_win_fewer, dots(mission.dots))
    is Mission.Capture -> stringResource(R.string.mission_capture, dots(mission.dots), moves(mission.moves))
    is Mission.Survive -> stringResource(R.string.mission_survive, moves(mission.moves))
    is Mission.Rescue -> stringResource(R.string.mission_rescue)
    is Mission.Target -> stringResource(R.string.mission_target)
}

/** True for the missions won on the score: win, win by N, win with fewer dots. */
private val Mission.isWin: Boolean get() = this == Mission.Win || this is Mission.WinBy || this is Mission.WinWithFewerDots

/** How the computer plays, after its name: "plays sharp". */
@Composable
fun strengthText(strength: Strength): String = stringResource(
    when (strength) {
        Strength.BEGINNER -> R.string.strength_beginner
        Strength.EASY -> R.string.strength_easy
        Strength.STEADY -> R.string.strength_steady
        Strength.NORMAL -> R.string.strength_normal
        Strength.HARD -> R.string.strength_hard
        Strength.EXPERT -> R.string.strength_expert
    },
)

/** The computer's head start, after its name: "starts 6 dots ahead". */
@Composable
fun headStartText(level: Level): String {
    val n = level.headStart
    return when {
        n > 0 -> pluralStringResource(R.plurals.level_ahead, n, n)
        n < 0 -> pluralStringResource(R.plurals.level_behind, -n, -n)
        else -> stringResource(R.string.level_even)
    }
}

/** The head start as a sentence, for the level opening: "The computer starts 6 dots ahead." */
@Composable
fun headStartSentence(level: Level): String {
    val n = level.headStart
    return when {
        n > 0 -> pluralStringResource(R.plurals.opening_ahead, n, n)
        n < 0 -> pluralStringResource(R.plurals.opening_behind, -n, -n)
        else -> stringResource(R.string.opening_even)
    }
}

/** A boss's rule as a sentence. */
@Composable
fun ruleText(rule: SpecialRule): String = when (rule) {
    is SpecialRule.DoubleMove -> pluralStringResource(R.plurals.rule_double_move, rule.every, rule.every)
    is SpecialRule.Fortress -> pluralStringResource(R.plurals.rule_fortress, rule.dots, rule.dots)
    SpecialRule.TwoGroups -> stringResource(R.string.rule_two_groups)
}

@Composable
fun boosterName(booster: Booster): String = stringResource(
    when (booster) {
        Booster.WALL -> R.string.booster_wall
        Booster.HINT -> R.string.booster_hint
        Booster.BOMB -> R.string.booster_bomb
        Booster.DOUBLE_DOT -> R.string.booster_double_dot
    },
)

/** The count a star measure reaches, as a phrase: "12 dots to spare", "a lead of 5 dots". */
@Composable
private fun measured(level: Level, measure: StarMeasure, score: Int): String = when (measure) {
    StarMeasure.MARGIN -> dots(score.coerceAtLeast(0))
    StarMeasure.MOVES -> if (level.mission is Mission.Capture) moves(score) else dots(score)
    StarMeasure.SAVED -> dots(score)
    StarMeasure.BOOSTERS -> pluralStringResource(R.plurals.count_boosters, score, score)
}

/**
 * What earns [stars] stars on the Level card. One star is the mission itself; two and three are
 * the level's targets: "Win by 4 dots", "With 3 moves to spare".
 */
@Composable
fun starTargetText(level: Level, stars: Int): String {
    val targets = level.stars
    if (stars <= 1 || targets == null) return missionText(level.mission)
    val score = if (stars == 2) targets.two else targets.three
    val amount = measured(level, targets.measure, score)
    return when (targets.measure) {
        StarMeasure.MARGIN -> if (level.mission.isWin) stringResource(R.string.mission_win_by, amount) else stringResource(R.string.target_lead, amount)
        StarMeasure.MOVES -> stringResource(R.string.target_spare, amount)
        StarMeasure.SAVED -> stringResource(R.string.target_saved, amount)
        StarMeasure.BOOSTERS -> stringResource(R.string.target_boosters, amount)
    }
}

/** A star score as Result says it: "12 dots to spare", "a lead of 5 dots". */
@Composable
fun starScoreText(level: Level, score: Int): String {
    val measure = level.stars?.measure ?: return ""
    val amount = measured(level, measure, score)
    return when (measure) {
        StarMeasure.MARGIN -> stringResource(R.string.score_lead, amount)
        StarMeasure.MOVES -> stringResource(R.string.score_spare, amount)
        StarMeasure.SAVED -> stringResource(R.string.score_saved, amount)
        StarMeasure.BOOSTERS -> stringResource(R.string.score_boosters, amount)
    }
}

/**
 * How many captures a failed level missed by, for the missions that count captures (capture and
 * the win missions); null for the others.
 */
fun missedBy(level: Level, status: MissionStatus): Int? = when (val mission = level.mission) {
    is Mission.Capture -> (mission.dots - status.captured).coerceAtLeast(1)
    Mission.Win, is Mission.WinWithFewerDots -> (1 - (status.captured - status.lost)).coerceAtLeast(1)
    is Mission.WinBy -> (mission.margin - (status.captured - status.lost)).coerceAtLeast(1)
    else -> null
}

/** Why a level failed, in one line: "Missed by 1 capture". */
@Composable
fun failureText(level: Level, status: MissionStatus): String {
    missedBy(level, status)?.let { return pluralStringResource(R.plurals.result_missed_by, it, it) }
    return stringResource(
        when (status.reason) {
            Reason.LOST_A_DOT -> R.string.result_lost_a_dot
            Reason.DOT_CAPTURED -> R.string.result_dot_captured
            Reason.TARGET_SAFE -> R.string.result_target_safe
            else -> R.string.result_target_missed
        },
    )
}
