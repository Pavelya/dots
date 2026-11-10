package io.github.pavelya.dots.data

import dots.campaign.CampaignMap
import dots.campaign.Level
import dots.campaign.LevelResult

/**
 * How a campaign level's game ended, once it is saved: the [result] (stars included), the worlds
 * and levels the pass [opened], how many times in a row the level has now been [failed] (0 after a
 * pass), and the level [next] opens over the map, if it is open.
 */
data class LevelEnd(
    val result: LevelResult,
    val opened: CampaignMap.Opened,
    val failed: Int,
    val next: Level?,
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
}
