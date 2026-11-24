package io.github.pavelya.dots

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
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
import io.github.pavelya.dots.data.ShopMarks
import io.github.pavelya.dots.data.StoredPurchases
import io.github.pavelya.dots.haptics.AndroidHaptics
import io.github.pavelya.dots.haptics.Haptics
import io.github.pavelya.dots.shop.NoStore
import io.github.pavelya.dots.shop.Owned
import io.github.pavelya.dots.shop.PlayBilling
import io.github.pavelya.dots.shop.PlayStore
import io.github.pavelya.dots.shop.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.util.concurrent.Executors

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** The purchases Google Play last confirmed; not in the progress store, and not backed up. */
private val Context.purchasesStore by preferencesDataStore(name = StoredPurchases.NAME)

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

    private val shop = MutableStateFlow<Store?>(null)

    /**
     * Where the Shop sells (docs/design/money.md): Google Play, made the first time it is asked
     * for, or [NoStore] on a phone without the Play Store app, such as the tests' phone. Tests may
     * replace it, before the app's screen opens.
     */
    var store: Store
        @Synchronized get() = shop.value ?: (if (hasPlayStore()) PlayStore(PlayBilling(this), StoredPurchases(purchasesStore), progressScope) else NoStore).also { shop.value = it }

        @Synchronized set(value) {
            shop.value = value
        }

    /** The cosmetics the player owns: earned by playing (the progress) or bought (Google Play). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val owned: StateFlow<Owned> by lazy {
        store // made now, if not yet
        combine(progress.saved.filterNotNull(), shop.filterNotNull().flatMapLatest { it.state }) { saved, play -> Owned.of(saved, play) }
            .stateIn(progressScope, SharingStarted.Eagerly, Owned.NONE)
    }

    /** What the Shop has shown of the items earned: New marks and the map's red dot. Backed up with the progress. */
    val marks: ShopMarks by lazy { ShopMarks(progressStore) }

    /**
     * The theme, piece style and ring effect in use, and those the player can choose: those owned,
     * or every one on a debug build (decision 0029). Backed up with the progress.
     */
    val themes: ThemeRepository by lazy { ThemeRepository(progressStore, owned) { CosmeticRule.owned(debugBuild, owned.value) } }

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

    /** True when the phone has Google Play's store app, which sells the Shop's items. */
    private fun hasPlayStore(): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(PLAY_STORE, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(PLAY_STORE, 0)
        }
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    override fun onCreate() {
        super.onCreate()
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            // Debug builds report slow work on the main thread and leaks in the log (logcat "StrictMode").
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
        }
    }

    private companion object {
        /** Google Play's store app (AndroidManifest.xml asks to see it). */
        const val PLAY_STORE = "com.android.vending"
    }
}
