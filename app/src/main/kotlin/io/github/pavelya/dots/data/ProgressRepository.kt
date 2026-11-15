package io.github.pavelya.dots.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dots.campaign.Campaign
import dots.campaign.CampaignMap
import dots.campaign.Chest
import dots.campaign.Level
import dots.campaign.LevelResult
import dots.campaign.LevelState
import dots.campaign.Progress
import dots.campaign.Reward
import dots.engine.Booster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The player's campaign progress, kept on the phone and in Android's backup: the stars, the
 * player's own boosters, the chests opened and the cosmetics owned ([Progress]), and how many times
 * in a row each unpassed level was failed. [scope] keeps [saved] and [map] up to date while the app
 * runs, and saves the boosters a game spends.
 *
 * Each part is one line under its own key. A key the store does not have yet (progress saved by an
 * older version) reads as empty, and an entry that cannot be read is left out ([Progress.parse]).
 */
class ProgressRepository(private val store: DataStore<Preferences>, scope: CoroutineScope) : CampaignJournal {

    val progress: Flow<Progress> = store.data.map(::read)

    /** The progress as last read; null until it is first read. */
    val saved: StateFlow<Progress?> = progress.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * What the world map shows; null until the progress is first read. The level files are read off
     * the main thread. Kept for the whole app, so the map is drawn at once when the player returns
     * to it, at the place they scrolled to.
     */
    val map: StateFlow<CampaignMap?> = saved.filterNotNull().map(Campaign::map).flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** Spent boosters, saved one after the other in the order they were reported ([spent]). */
    private val spending = Channel<Pair<Long, Map<Booster, Int>>>(Channel.UNLIMITED)

    init {
        scope.launch {
            for ((game, used) in spending) {
                store.edit { prefs ->
                    val before = Spent.parse(prefs[SPENT].orEmpty())
                    val already = if (before?.first == game) before.second else emptyMap()
                    val change = (used.keys + already.keys).associateWith { (used[it] ?: 0) - (already[it] ?: 0) }
                    write(prefs, read(prefs).spend(change))
                    prefs[SPENT] = Spent.text(game, used)
                }
            }
        }
    }

    /** The player's own boosters a new game of [level] takes, as last read. */
    fun owned(level: Level): Map<Booster, Int> = saved.value?.owned(level).orEmpty()

    /** Keeps [stars] for [level] if they beat the stars it has. */
    suspend fun record(level: Level, stars: Int) {
        store.edit { write(it, read(it).record(level, stars)) }
    }

    /**
     * Saves a level's end in one write: a pass keeps its stars ([record]) and clears the level's
     * failures; a failure adds one. Then works out what the pass opened, the chest it made ready,
     * and which level Next opens: the next level, or the first level of a world the pass opened.
     */
    override suspend fun finished(level: Level, result: LevelResult): LevelEnd {
        var before = Progress.NONE
        var after = Progress.NONE
        var failed = 0
        store.edit { prefs ->
            before = read(prefs)
            val failures = Failures.parse(prefs[FAILURES].orEmpty())
            if (result.isPassed) {
                after = before.record(level, result.stars)
                write(prefs, after)
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
            val opened = new.openedSince(old)
            val following = opened.worlds.firstOrNull()?.levels?.first()
                ?: Campaign.levels.getOrNull(Campaign.levels.indexOf(level) + 1)
            val chest = Campaign.chestAfter(level)?.takeIf { after.isReady(it) }
            LevelEnd(result, opened, failed, following?.takeIf { new.stop(it).state != LevelState.LOCKED }, chest)
        }
    }

    /** Opens [chest] and keeps [reward]; a chest already opened stays as it was. */
    suspend fun open(chest: Chest, reward: Reward) {
        store.edit { write(it, read(it).open(chest, reward)) }
    }

    override fun spent(game: Long, used: Map<Booster, Int>) {
        spending.trySend(game to used)
    }

    /** Replaces all progress; for the developer menu. */
    suspend fun replace(progress: Progress) {
        store.edit {
            write(it, progress)
            it.remove(FAILURES)
            it.remove(SPENT)
        }
    }

    private fun read(prefs: Preferences): Progress =
        Progress.parse(prefs[STARS].orEmpty(), prefs[BOOSTERS].orEmpty(), prefs[CHESTS].orEmpty(), prefs[COSMETICS].orEmpty())

    private fun write(prefs: MutablePreferences, progress: Progress) {
        prefs[STARS] = progress.text
        prefs[BOOSTERS] = progress.boostersText
        prefs[CHESTS] = progress.chestsText
        prefs[COSMETICS] = progress.cosmeticsText
    }

    companion object {
        /**
         * The store's name: its file is `files/datastore/progress.preferences_pb`, which Android's
         * backup takes (res/xml/data_extraction_rules.xml).
         */
        const val NAME = "progress"

        /**
         * The progress store in [file]. A file that cannot be read at all (damaged on the disk) is
         * replaced by an empty one, so the app still opens; Android's backup can bring progress back.
         */
        fun store(scope: CoroutineScope, file: () -> File): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = scope,
            produceFile = file,
        )

        private val STARS = stringPreferencesKey("stars")

        /** The player's own boosters: `wall:2 bomb:1`. */
        private val BOOSTERS = stringPreferencesKey("boosters")

        /** The chests opened, by the level before each: `1-10 2-5`. */
        private val CHESTS = stringPreferencesKey("chests")

        /** The cosmetics owned, by name. */
        private val COSMETICS = stringPreferencesKey("cosmetics")

        /** Failures in a row, by level code: `1-7:2 2-3:1`. */
        private val FAILURES = stringPreferencesKey("failures")

        /**
         * The level game that last spent the player's own boosters, and how many it has used in
         * all: `1731600000000 wall:1`. A later report of the same game changes the boosters by the
         * difference only.
         */
        private val SPENT = stringPreferencesKey("spent")
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

/** A game's start time and the owned boosters it has used, as one line; null if it cannot be read. */
private object Spent {
    fun parse(text: String): Pair<Long, Map<Booster, Int>>? {
        val game = text.substringBefore(' ').toLongOrNull() ?: return null
        return game to Progress.parseBoosters(text.substringAfter(' ', ""))
    }

    fun text(game: Long, used: Map<Booster, Int>): String = "$game ${Progress.boosterText(used)}".trim()
}

/** Progress to try the map with, from the developer menu and the screenshot tests. */
object SampleProgress {
    /** Stars for levels in a varied pattern, so the map shows ones, twos and threes. */
    private fun passed(levels: List<Level>, stars: (Int) -> Int = { listOf(3, 2, 3, 1, 3)[it % 5] }): Progress =
        Progress(levels.withIndex().associate { (i, level) -> level.code to stars(i) })

    /** World 1 passed and World 2 open, its next level 2-10; the chest after 1-10 is opened, after 2-5 ready. */
    val midWorld: Progress get() {
        val first = Campaign.chests.first()
        return passed(Campaign.levels.takeWhile { it.code != "2-10" }).open(first, first.choices.first())
    }

    /** World 1 passed with enough stars, so World 2 has just opened; its chest is still closed. */
    val worldComplete: Progress get() = passed(Campaign.worlds.first().levels)

    /** World 1 passed with one star on each level: World 2 still needs stars. */
    val worldNeedsStars: Progress get() = passed(Campaign.worlds.first().levels) { 1 }

    /** Every level passed with three stars, every chest opened. */
    val everything: Progress get() = Campaign.chests.fold(passed(Campaign.levels) { 3 }) { progress, chest -> progress.open(chest, chest.choices.first()) }
}
