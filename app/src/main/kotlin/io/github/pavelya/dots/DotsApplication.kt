package io.github.pavelya.dots

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import io.github.pavelya.dots.data.SettingsRepository

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Holds the app-wide objects; screens reach them through the application context. */
class DotsApplication : Application() {
    val settings: SettingsRepository by lazy { SettingsRepository(settingsStore) }
}
