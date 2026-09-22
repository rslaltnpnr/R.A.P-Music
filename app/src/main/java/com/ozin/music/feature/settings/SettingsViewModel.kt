package com.ozin.music.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.backup.BackupManager
import com.ozin.music.core.data.repository.SongRepository
import androidx.appcompat.app.AppCompatDelegate
import com.ozin.music.core.settings.AppSettings
import com.ozin.music.core.settings.ArtworkQuality
import com.ozin.music.core.settings.LanguageOption
import com.ozin.music.core.settings.LockScreenPrivacy
import com.ozin.music.core.settings.RepeatMode
import com.ozin.music.core.settings.SettingsRepository
import com.ozin.music.core.settings.toLocaleListCompat
import com.ozin.music.core.ui.theme.AccentColorOption
import com.ozin.music.core.ui.theme.ThemeMode
import com.ozin.music.core.ui.theme.ThemePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Result of the last backup export/import attempt, surfaced to the UI as a
 * one-shot message (a Snackbar). Never silent: both success and failure are
 * represented, failure carries a real (if brief) reason. */
sealed interface BackupResult {
    data object ExportSuccess : BackupResult
    data class ExportFailure(val reason: String) : BackupResult
    data object ImportSuccess : BackupResult
    data class ImportFailure(val reason: String) : BackupResult
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val songRepository: SongRepository,
    private val backupManager: BackupManager,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings()
    )

    private val _backupResult = MutableStateFlow<BackupResult?>(null)
    val backupResult: StateFlow<BackupResult?> = _backupResult.asStateFlow()

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.export(uri)
            _backupResult.value = result.fold(
                onSuccess = { BackupResult.ExportSuccess },
                onFailure = { BackupResult.ExportFailure(it.message ?: it.javaClass.simpleName) },
            )
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.import(uri)
            _backupResult.value = result.fold(
                onSuccess = { BackupResult.ImportSuccess },
                onFailure = { BackupResult.ImportFailure(it.message ?: it.javaClass.simpleName) },
            )
        }
    }

    fun consumeBackupResult() {
        _backupResult.value = null
    }

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

    fun toggleEqSuggestion(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEqSuggestionEnabled(enabled) }
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

    fun toggleLockScreenShowArtwork(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLockScreenShowArtwork(enabled) }
    }

    fun toggleLockScreenShowMediaInfo(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLockScreenShowMediaInfo(enabled) }
    }

    fun setArtworkQuality(quality: ArtworkQuality) {
        viewModelScope.launch { settingsRepository.setArtworkQuality(quality) }
    }

    fun setLockScreenPrivacy(privacy: LockScreenPrivacy) {
        viewModelScope.launch { settingsRepository.setLockScreenPrivacy(privacy) }
    }

    fun toggleNowPlayingGestures(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNowPlayingGesturesEnabled(enabled) }
    }

    fun toggleShakeToPause(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShakeToPauseEnabled(enabled) }
    }

    private val _loudnessAnalysisProgress = MutableStateFlow<LoudnessAnalysisProgress?>(null)
    val loudnessAnalysisProgress: StateFlow<LoudnessAnalysisProgress?> = _loudnessAnalysisProgress.asStateFlow()

    private var loudnessAnalysisJob: Job? = null

    /** Runs the real per-track loudness analyzer over every song that has
     * never been analyzed, a few at a time. Real, working cancellation: the
     * loop lives inside [loudnessAnalysisJob], which [stopLoudnessAnalysis]
     * cancels outright (called from the screen's onDispose), following the
     * same cancellable-background-Job pattern as this app's other
     * long-running one-shot passes. */
    fun startLoudnessAnalysis() {
        if (loudnessAnalysisJob?.isActive == true) return
        _loudnessAnalysisProgress.value = LoudnessAnalysisProgress(0, 0, running = true)
        loudnessAnalysisJob = viewModelScope.launch {
            songRepository.analyzeLibraryLoudness { processed, total ->
                _loudnessAnalysisProgress.value = LoudnessAnalysisProgress(processed, total, running = true)
            }
            _loudnessAnalysisProgress.value = _loudnessAnalysisProgress.value?.copy(running = false)
        }
    }

    fun stopLoudnessAnalysis() {
        loudnessAnalysisJob?.cancel()
        loudnessAnalysisJob = null
        _loudnessAnalysisProgress.value = _loudnessAnalysisProgress.value?.copy(running = false)
    }

}

/** [processed]/[total] songs analyzed so far in the current manual "analyze
 * library loudness" run; [running] is false once it finishes or is
 * cancelled. */
data class LoudnessAnalysisProgress(val processed: Int, val total: Int, val running: Boolean)
