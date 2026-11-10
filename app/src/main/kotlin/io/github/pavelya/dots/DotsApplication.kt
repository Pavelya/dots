package io.github.pavelya.dots

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import androidx.datastore.preferences.preferencesDataStore
import io.github.pavelya.dots.data.GameStore
import io.github.pavelya.dots.data.ProgressRepository
import io.github.pavelya.dots.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.util.concurrent.Executors

private val Context.settingsStore by preferencesDataStore(name = "settings")
private val Context.progressStore by preferencesDataStore(name = "progress")

/** Holds the app-wide objects; screens reach them through the application context. */
class DotsApplication : Application() {
    val settings: SettingsRepository by lazy { SettingsRepository(settingsStore) }

    /** Campaign progress: the stars of each passed level. */
    val progress: ProgressRepository by lazy { ProgressRepository(progressStore, CoroutineScope(SupervisorJob() + Dispatchers.Default)) }

    /** The saved game and the game log; files are written on one background thread, in order. */
    val games: GameStore by lazy {
        val disk = Executors.newSingleThreadExecutor { task -> Thread(task, "game-store").apply { isDaemon = true } }
        GameStore({ File(filesDir, "games") }, BuildConfig.VERSION_NAME, disk)
    }

    override fun onCreate() {
        super.onCreate()
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            // Debug builds report slow work on the main thread and leaks in the log (logcat "StrictMode").
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
        }
    }
}
