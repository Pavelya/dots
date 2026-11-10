package io.github.pavelya.dots.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dots.campaign.Campaign
import dots.campaign.CampaignMap
import dots.campaign.Level
import dots.campaign.Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The player's campaign progress, kept on the phone: the best stars of each passed level. [scope]
 * keeps [map] up to date while the app runs.
 */
class ProgressRepository(private val store: DataStore<Preferences>, scope: CoroutineScope) {

    val progress: Flow<Progress> = store.data.map { Progress.parse(it[STARS].orEmpty()) }

    /**
     * What the world map shows; null until the progress is first read. The level files are read off
     * the main thread. Kept for the whole app, so the map is drawn at once when the player returns
     * to it, at the place they scrolled to.
     */
    val map: StateFlow<CampaignMap?> = progress.map(Campaign::map).flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** Keeps [stars] for [level] if they beat the stars it has. */
    suspend fun record(level: Level, stars: Int) {
        store.edit { it[STARS] = Progress.parse(it[STARS].orEmpty()).record(level, stars).text }
    }

    /** Replaces all progress; for the developer menu. */
    suspend fun replace(progress: Progress) {
        store.edit { it[STARS] = progress.text }
    }

    private companion object {
        val STARS = stringPreferencesKey("stars")
    }
}

/** Progress to try the map with, from the developer menu and the screenshot tests. */
object SampleProgress {
    /** Stars for levels in a varied pattern, so the map shows ones, twos and threes. */
    private fun passed(levels: List<Level>, stars: (Int) -> Int = { listOf(3, 2, 3, 1, 3)[it % 5] }): Progress =
        Progress(levels.withIndex().associate { (i, level) -> level.code to stars(i) })

    /** World 1 passed and World 2 open, its next level 2-10. */
    val midWorld: Progress get() = passed(Campaign.levels.takeWhile { it.code != "2-10" })

    /** World 1 passed with enough stars, so World 2 has just opened. */
    val worldComplete: Progress get() = passed(Campaign.worlds.first().levels)

    /** World 1 passed with one star on each level: World 2 still needs stars. */
    val worldNeedsStars: Progress get() = passed(Campaign.worlds.first().levels) { 1 }

    /** Every level passed with three stars. */
    val everything: Progress get() = passed(Campaign.levels) { 3 }
}
