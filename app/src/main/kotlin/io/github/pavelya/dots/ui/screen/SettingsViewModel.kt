package io.github.pavelya.dots.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    /** Null until the stored settings have been read. */
    val settings: StateFlow<AppSettings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Saves a change; it applies to the stored settings, so two quick changes never undo each other. */
    fun change(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repository.update(change) }
    }
}
