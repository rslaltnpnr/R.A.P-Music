package com.ozin.music.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.repository.SongRepository
import androidx.appcompat.app.AppCompatDelegate
import com.ozin.music.core.settings.AppSettings
import com.ozin.music.core.settings.LanguageOption
import com.ozin.music.core.settings.RepeatMode
import com.ozin.music.core.settings.SettingsRepository
import com.ozin.music.core.settings.toLocaleListCompat
import com.ozin.music.core.ui.theme.AccentColorOption
import com.ozin.music.core.ui.theme.ThemeMode
import com.ozin.music.core.ui.theme.ThemePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val songRepository: SongRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings()
    )

    fun toggleCrossfade(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCrossfadeEnabled(enabled) }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.setCrossfadeSeconds(seconds) }
    }

    fun toggleNormalization(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNormalizationEnabled(enabled) }
    }

    fun toggleShuffleDefault(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShuffleDefault(enabled) }
    }

    fun setRepeatDefault(mode: RepeatMode) {
        viewModelScope.launch { settingsRepository.setRepeatDefault(mode) }
    }

    fun toggleMiniPlayerCompact(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMiniPlayerCompact(enabled) }
    }

    fun toggleSmartCrossfade(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSmartCrossfadeEnabled(enabled) }
    }

    fun toggleFadeInOut(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setFadeInOutEnabled(enabled) }
    }

    fun addExcludedFolder(path: String) {
        if (path.isBlank()) return
        viewModelScope.launch {
            settingsRepository.addExcludedFolder(path.trim())
            songRepository.rescan()
        }
    }

    fun removeExcludedFolder(path: String) {
        viewModelScope.launch {
            settingsRepository.removeExcludedFolder(path)
            songRepository.rescan()
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch { songRepository.rescan() }
    }

    /** Runs the heuristic mood-tag pass (see [com.ozin.music.core.domain.MoodClassifier])
     * over the whole library as a one-shot plain-coroutine background task. */
    fun computeMoodTags() {
        viewModelScope.launch { songRepository.computeMoodTags() }
    }

    fun setThemePreset(preset: ThemePreset) {
        viewModelScope.launch { settingsRepository.setThemePreset(preset) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setAccentColorOption(option: AccentColorOption) {
        viewModelScope.launch { settingsRepository.setAccentColorOption(option) }
    }

    /** Persists the chosen language and applies it immediately via
     * [AppCompatDelegate], so the change takes effect without needing an app
     * restart (AppCompat recreates activities that need recomposing). */
    fun setLanguageOption(option: LanguageOption) {
        viewModelScope.launch { settingsRepository.setLanguageOption(option) }
        AppCompatDelegate.setApplicationLocales(option.toLocaleListCompat())
    }
}
