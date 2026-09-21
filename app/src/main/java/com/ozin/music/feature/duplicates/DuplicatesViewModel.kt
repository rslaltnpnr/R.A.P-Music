package com.ozin.music.feature.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.domain.DuplicateDetector
import com.ozin.music.core.domain.DuplicateGroup
import com.ozin.music.core.files.MetadataEditor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DuplicatesUiState(
    val isScanning: Boolean = false,
    val groups: List<DuplicateGroup> = emptyList(),
    val hasScanned: Boolean = false,
)

@HiltViewModel
class DuplicatesViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val metadataEditor: MetadataEditor,
) : ViewModel() {

    private val _state = MutableStateFlow(DuplicatesUiState())
    val state: StateFlow<DuplicatesUiState> = _state.asStateFlow()

    /** On-demand background scan (a plain coroutine here rather than
     * WorkManager, since it only needs to run while this screen is open and
     * completes in well under a second even for large libraries). */
    fun scan() {
        _state.value = _state.value.copy(isScanning = true)
        viewModelScope.launch {
            val songs = songRepository.songs.first()
            val groups = DuplicateDetector.findDuplicates(songs)
            _state.value = DuplicatesUiState(isScanning = false, groups = groups, hasScanned = true)
        }
    }

    /** Deletes every song in the group except the first, after explicit user confirmation. */
    fun keepFirstDeleteRest(group: DuplicateGroup) {
        viewModelScope.launch {
            val toDelete = group.songs.drop(1).map { it.id }
            metadataEditor.deleteSongs(toDelete)
            songRepository.rescan()
            scan()
        }
    }

    fun deleteOne(songId: Long) {
        viewModelScope.launch {
            metadataEditor.deleteSongs(listOf(songId))
            songRepository.rescan()
            scan()
        }
    }
}
