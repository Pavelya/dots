package io.github.pavelya.dots

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.pavelya.dots.data.CountingJournal
import io.github.pavelya.dots.data.DailyRepository
import io.github.pavelya.dots.data.GameJournal
import io.github.pavelya.dots.data.GameStore
import io.github.pavelya.dots.data.ProgressRepository
import io.github.pavelya.dots.data.PuzzleClock
import io.github.pavelya.dots.data.SettingsRepository
import io.github.pavelya.dots.data.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.util.concurrent.Executors

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Holds the app-wide objects; screens reach them through the application context. */
class DotsApplication : Application() {
    val settings: SettingsRepository by lazy { SettingsRepository(settingsStore) }

    private val progressScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    /** The progress store, one for the campaign, the daily puzzle and the stats (a file has one DataStore). Backed up by Android. */
    private val progressStore by lazy { ProgressRepository.store(progressScope) { preferencesDataStoreFile(ProgressRepository.NAME) } }

    /** Campaign progress: stars, the player's own boosters, chests and cosmetics. Backed up by Android. */
    val progress: ProgressRepository by lazy { ProgressRepository(progressStore, progressScope) }

    /** Google Play's review prompt, asked for after a level won with three stars; tests replace it. */
    var reviews: ReviewPrompt = PlayReviewPrompt()

    /** The daily puzzle's clock; tests replace it with one they set. */
    var clock: PuzzleClock = PuzzleClock()

    /** The daily puzzles solved and the recent attempts, for the streak and Result. Backed up with the progress. */
    val daily: DailyRepository by lazy { DailyRepository(progressStore, progressScope) { clock } }

    /** The saved game and the game log; files are written on one background thread, in order. */
    val games: GameStore by lazy {
        val disk = Executors.newSingleThreadExecutor { task -> Thread(task, "game-store").apply { isDaemon = true } }
        GameStore({ File(filesDir, "games") }, BuildConfig.VERSION_NAME, disk)
    }

    /** The player's totals on Stats: games played and won, captures. Backed up with the progress. */
    val stats: StatsRepository by lazy { StatsRepository(progressStore, progressScope) }

    /** Where a game reports itself: the saved games and the log ([games]), and the totals on Stats once it ends. */
    val journal: GameJournal by lazy { CountingJournal(games, stats) }

    override fun onCreate() {
        super.onCreate()
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            // Debug builds report slow work on the main thread and leaks in the log (logcat "StrictMode").
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
        }
    }
}
