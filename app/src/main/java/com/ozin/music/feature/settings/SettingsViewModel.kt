package com.ozin.music.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.settings.AppSettings
import com.ozin.music.core.settings.RepeatMode
import com.ozin.music.core.settings.SettingsRepository
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
}
