package com.ozin.music.feature.files

import android.content.IntentSender
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.files.FileOperationResult
import com.ozin.music.core.files.MetadataEditor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PendingFileOp { NONE, RENAME, DELETE }

data class FileManagementUiState(
    val song: Song? = null,
    val details: MetadataEditor.FileDetails? = null,
    val newDisplayName: String = "",
    val isBusy: Boolean = false,
    val error: String? = null,
    val deleted: Boolean = false,
    val pendingConsent: IntentSender? = null,
    val pendingOp: PendingFileOp = PendingFileOp.NONE,
)

@HiltViewModel
class FileManagementViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val songRepository: SongRepository,
    private val metadataEditor: MetadataEditor,
) : ViewModel() {

    private val songId: Long = checkNotNull(savedStateHandle.get<Long>("songId"))
    private val _state = MutableStateFlow(FileManagementUiState())
    val state: StateFlow<FileManagementUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val song = songRepository.getById(songId) ?: return@launch
            val details = metadataEditor.readFileDetails(song.path, song.sizeBytes)
            _state.value = _state.value.copy(
                song = song,
                details = details,
                newDisplayName = java.io.File(song.path).name,
            )
        }
    }

    fun onNewNameChanged(name: String) {
        _state.value = _state.value.copy(newDisplayName = name)
    }

    fun rename() {
        val song = _state.value.song ?: return
        val name = _state.value.newDisplayName.trim()
        if (name.isBlank()) return
        _state.value = _state.value.copy(isBusy = true, error = null, pendingOp = PendingFileOp.RENAME)
        viewModelScope.launch {
            when (val result = metadataEditor.renameFile(song.id, name)) {
                FileOperationResult.Success -> {
                    songRepository.applyMetadataEdit(song.copy(path = song.path.substringBeforeLast('/') + "/" + name))
                    _state.value = _state.value.copy(isBusy = false, pendingOp = PendingFileOp.NONE)
                }
                is FileOperationResult.NeedsUserConsent ->
                    _state.value = _state.value.copy(isBusy = false, pendingConsent = result.intentSender)
                is FileOperationResult.Failed ->
                    _state.value = _state.value.copy(isBusy = false, error = result.message, pendingOp = PendingFileOp.NONE)
            }
        }
    }

    fun delete() {
        val song = _state.value.song ?: return
        _state.value = _state.value.copy(isBusy = true, error = null, pendingOp = PendingFileOp.DELETE)
        viewModelScope.launch {
            when (val result = metadataEditor.deleteSongs(listOf(song.id))) {
                FileOperationResult.Success -> {
                    songRepository.rescan()
                    _state.value = _state.value.copy(isBusy = false, deleted = true, pendingOp = PendingFileOp.NONE)
                }
                is FileOperationResult.NeedsUserConsent ->
                    _state.value = _state.value.copy(isBusy = false, pendingConsent = result.intentSender)
                is FileOperationResult.Failed ->
                    _state.value = _state.value.copy(isBusy = false, error = result.message, pendingOp = PendingFileOp.NONE)
            }
        }
    }

    fun onConsentGranted() {
        val op = _state.value.pendingOp
        _state.value = _state.value.copy(pendingConsent = null)
        when (op) {
            PendingFileOp.RENAME -> rename()
            PendingFileOp.DELETE -> {
                // On API 30+, createDeleteRequest performs the delete itself once
                // the user approves — there is nothing further to retry.
                viewModelScope.launch {
                    songRepository.rescan()
                    _state.value = _state.value.copy(deleted = true, pendingOp = PendingFileOp.NONE)
                }
            }
            PendingFileOp.NONE -> Unit
        }
    }

    fun onConsentDenied() {
        _state.value = _state.value.copy(pendingConsent = null, pendingOp = PendingFileOp.NONE, error = "Permission denied.")
    }
}
