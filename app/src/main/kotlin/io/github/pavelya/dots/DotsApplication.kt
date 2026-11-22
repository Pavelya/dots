package io.github.pavelya.dots

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.pavelya.dots.analytics.Analytics
import io.github.pavelya.dots.analytics.ReportingJournal
import io.github.pavelya.dots.analytics.buildAnalytics
import io.github.pavelya.dots.audio.AndroidAudio
import io.github.pavelya.dots.audio.Audio
import io.github.pavelya.dots.data.CountingJournal
import io.github.pavelya.dots.data.DailyRepository
import io.github.pavelya.dots.data.GameJournal
import io.github.pavelya.dots.data.GameStore
import io.github.pavelya.dots.data.ProgressRepository
import io.github.pavelya.dots.data.PuzzleClock
import io.github.pavelya.dots.data.SettingsRepository
import io.github.pavelya.dots.data.StatsRepository
import io.github.pavelya.dots.data.ThemeRepository
import io.github.pavelya.dots.data.CosmeticRule
import io.github.pavelya.dots.haptics.AndroidHaptics
import io.github.pavelya.dots.haptics.Haptics
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

    private var sound: Audio? = null

    /**
     * The game's sound effects and music (docs/design/sound.md), made the first time the app's
     * screen asks for them. Tests may replace it.
     */
    var audio: Audio
        get() = sound ?: AndroidAudio(this).also { sound = it }
        set(value) {
            sound = value
        }

    private var vibrations: Haptics? = null

    /** The game's vibrations (docs/design/motion.md, rule 6), made the first time the app's screen asks for them. Tests may replace it. */
    var haptics: Haptics
        get() = vibrations ?: AndroidHaptics(this).also { vibrations = it }
        set(value) {
            vibrations = value
        }

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

    /**
     * Whether this is a debug build, which offers every theme, piece style and ring effect
     * (decision 0029). Tests set it to false to see what a release build shows.
     */
    var debugBuild: Boolean = BuildConfig.DEBUG

    /** The theme, piece style and ring effect in use, and those the player can choose. Backed up with the progress. */
    val themes: ThemeRepository by lazy { ThemeRepository(progressStore) { CosmeticRule.beforeShop(debugBuild) } }

    /** The player's totals on Stats: games played and won, captures. Backed up with the progress. */
    val stats: StatsRepository by lazy { StatsRepository(progressStore, progressScope) }

    private var events: Analytics? = null

    /**
     * Where the game reports level starts and ends, boosters and daily puzzles (decision 0033):
     * Google Analytics in store builds, nothing in debug builds. Tests may replace it.
     */
    var analytics: Analytics
        get() = events ?: buildAnalytics(this).also { events = it }
        set(value) {
            events = value
        }

    /**
     * Where a game reports itself: the saved games and the log ([games]), the totals on Stats once
     * it ends, and each new level game to [analytics]. Every new game starts here.
     */
    val journal: GameJournal by lazy { ReportingJournal(CountingJournal(games, stats)) { analytics } }

    override fun onCreate() {
        super.onCreate()
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            // Debug builds report slow work on the main thread and leaks in the log (logcat "StrictMode").
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
        }
    }
}
