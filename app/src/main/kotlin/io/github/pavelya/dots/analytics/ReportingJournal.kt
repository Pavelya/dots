package io.github.pavelya.dots.analytics

import dots.campaign.LevelRecord
import dots.log.GameLogEntry
import io.github.pavelya.dots.data.GameJournal

/**
 * [journal], which also reports each new game of a campaign level to [analytics]. Every new game
 * goes through [start] (from the map and from the game screen), while a game resumed after the app
 * was closed does not, so a level start is reported once.
 */
class ReportingJournal(private val journal: GameJournal, private val analytics: () -> Analytics) : GameJournal by journal {
    override fun start(entry: GameLogEntry) {
        journal.start(entry)
        val level = (entry.record as? LevelRecord)?.level ?: return
        if (!entry.practice) analytics().log(Events.levelStart(level))
    }
}
