package com.ozin.music.feature.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.mediastore.MediaStoreScanner
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.domain.FolderExclusionSuggestions
import com.ozin.music.core.domain.LibraryGrouping
import com.ozin.music.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FolderRow(val path: String, val songCount: Int, val excluded: Boolean)

data class FoldersUiState(
    val folders: List<FolderRow> = emptyList(),
    val suggestions: List<String> = emptyList(),
)

/**
 * Folder list is derived from an *unfiltered* scan (bypassing the current
 * exclusion set) so an already-excluded folder still shows up with its
 * checkbox unchecked, instead of disappearing once excluded.
 */
@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository,
    private val scanner: MediaStoreScanner,
) : ViewModel() {

    private val _state = MutableStateFlow(FoldersUiState())
    val state: StateFlow<FoldersUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val allSongs = scanner.scan(excludedFolders = emptySet())
            val settings = settingsRepository.settings.first()
            val groups = LibraryGrouping.byFolder(allSongs)
            val folders = groups.map { FolderRow(it.subtitle, it.songs.size, it.subtitle in settings.excludedFolders) }
            val suggestions = FolderExclusionSuggestions.suggestedFolders(
                folders.map { it.path },
                settings.excludedFolders,
            )
            _state.value = FoldersUiState(folders = folders, suggestions = suggestions)
        }
    }

    fun toggleExcluded(path: String, excluded: Boolean) {
        viewModelScope.launch {
            if (excluded) settingsRepository.addExcludedFolder(path) else settingsRepository.removeExcludedFolder(path)
            songRepository.rescan()
            refresh()
        }
    }

    fun dismissSuggestion(path: String) {
        // Suggestions are advisory-only and re-derive from current settings;
        // excluding the folder is the only persistent action available.
        toggleExcluded(path, excluded = true)
    }
}
