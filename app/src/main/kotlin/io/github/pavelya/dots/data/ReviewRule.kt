package io.github.pavelya.dots.data

import dots.campaign.Level
import dots.campaign.LevelResult
import dots.campaign.Stars
import dots.campaign.Tutorial

/**
 * When the game asks Google Play for its review prompt: after a campaign level won with all
 * [Stars.MAX] stars ([earns]), at most once every [GAP_DAYS] days ([isDue]). Never after a loss,
 * a practice game or one of the tutorial's levels, where a new player has seen too little of the
 * game to judge it. Google decides whether the prompt really shows, and limits how often, so a
 * time asked counts whether it showed or not.
 */
object ReviewRule {
    /** The shortest time between two asks: about a month. */
    const val GAP_DAYS = 30

    const val GAP_MILLIS: Long = GAP_DAYS * 24L * 60 * 60 * 1000

    /** True when [level], ended with [result], is one the prompt may follow. */
    fun earns(level: Level, result: LevelResult, practice: Boolean): Boolean =
        !practice && result.isPassed && result.stars == Stars.MAX && Tutorial.lesson(level) == null

    /**
     * True when the prompt may be asked for [now], having last been asked at [lastAsked] (null if
     * never). Times are milliseconds since 1970. A clock set back before [lastAsked] waits.
     */
    fun isDue(lastAsked: Long?, now: Long): Boolean = lastAsked == null || now - lastAsked >= GAP_MILLIS
}
