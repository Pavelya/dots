package io.github.pavelya.dots.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dots.campaign.Campaign
import dots.campaign.CampaignMap
import dots.campaign.Level
import dots.campaign.LevelResult
import dots.campaign.LevelState
import dots.campaign.Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * The player's campaign progress, kept on the phone: the best stars of each passed level, and how
 * many times in a row each unpassed level was failed. [scope] keeps [map] up to date while the app
 * runs.
 */
class ProgressRepository(private val store: DataStore<Preferences>, scope: CoroutineScope) : CampaignJournal {

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

    /**
     * Saves a level's end in one write: a pass keeps its stars ([record]) and clears the level's
     * failures; a failure adds one. Then works out what the pass opened and which level Next opens.
     */
    override suspend fun finished(level: Level, result: LevelResult): LevelEnd {
        var before = Progress.NONE
        var after = Progress.NONE
        var failed = 0
        store.edit { prefs ->
            before = Progress.parse(prefs[STARS].orEmpty())
            val failures = Failures.parse(prefs[FAILURES].orEmpty())
            if (result.isPassed) {
                after = before.record(level, result.stars)
                prefs[STARS] = after.text
                prefs[FAILURES] = Failures.text(failures - level.code)
            } else {
                after = before
                failed = (failures[level.code] ?: 0) + 1
                prefs[FAILURES] = Failures.text(failures + (level.code to failed))
            }
        }
        return withContext(Dispatchers.Default) {
            val old = Campaign.map(before)
            val new = Campaign.map(after)
            val following = Campaign.levels.getOrNull(Campaign.levels.indexOf(level) + 1)
            LevelEnd(result, new.openedSince(old), failed, following?.takeIf { new.stop(it).state != LevelState.LOCKED })
        }
    }

    /** Replaces all progress; for the developer menu. */
    suspend fun replace(progress: Progress) {
        store.edit {
            it[STARS] = progress.text
            it.remove(FAILURES)
        }
    }

    private companion object {
        val STARS = stringPreferencesKey("stars")

        /** Failures in a row, by level code: `1-7:2 2-3:1`. */
        val FAILURES = stringPreferencesKey("failures")
    }
}

/** Failures in a row by level code, stored as one line; an entry that cannot be read is left out. */
private object Failures {
    fun parse(text: String): Map<String, Int> = text.split(' ').mapNotNull { entry ->
        val code = entry.substringBefore(':', "")
        val count = entry.substringAfter(':', "").toIntOrNull()
        if (code.isNotEmpty() && count != null && count > 0) code to count else null
    }.toMap()

    fun text(failures: Map<String, Int>): String = failures.entries.joinToString(" ") { "${it.key}:${it.value}" }
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
