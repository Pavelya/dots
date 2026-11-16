package io.github.pavelya.dots.data

import dots.campaign.CampaignMap
import dots.campaign.Chest
import dots.campaign.Level
import dots.campaign.LevelResult
import dots.engine.Booster

/**
 * How a campaign level's game ended, once it is saved: the [result] (stars included), the worlds
 * and levels the pass [opened], how many times in a row the level has now been [failed] (0 after a
 * pass), the level [next] opens over the map, if it is open, and the [chest] the pass made ready,
 * which Next opens first. [starsBefore] is the level's stars before this game: the map's unlock
 * plays the new ones (motion.md, "Level unlock").
 */
data class LevelEnd(
    val result: LevelResult,
    val opened: CampaignMap.Opened,
    val failed: Int,
    val next: Level?,
    val chest: Chest? = null,
    val starsBefore: Int = 0,
) {
    companion object {
        /** After this many failures in a row, Result offers help (docs/design/flows.md, Campaign). */
        const val HELP_AFTER_FAILURES = 3
    }
}

/** Where the end of a campaign level is saved: stars, unlocks and failures ([ProgressRepository]). */
fun interface CampaignJournal {
    /** Saves how [level] ended and returns what Result shows. Result opens only after this returns. */
    suspend fun finished(level: Level, result: LevelResult): LevelEnd

    /**
     * The level game that started at [game] (its start time) has now [used] this many of the
     * player's own boosters, by kind, in all: they leave the player's boosters, and come back if an
     * undo lowers the count. Returns at once; the change is saved in order.
     */
    fun spent(game: Long, used: Map<Booster, Int>) {}
}
