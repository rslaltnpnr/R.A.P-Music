package com.ozin.music.feature.metadata

import android.content.IntentSender
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.files.FileOperationResult
import com.ozin.music.core.files.MetadataEditor
import com.ozin.music.core.files.SongMetadataEdit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MetadataEditUiState(
    val song: Song? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val genre: String = "",
    val year: String = "",
    val trackNumber: String = "",
    val discNumber: String = "",
    val comment: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val pendingConsent: IntentSender? = null,
)

@HiltViewModel
class MetadataEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val songRepository: SongRepository,
    private val metadataEditor: MetadataEditor,
) : ViewModel() {

    private val songId: Long = checkNotNull(savedStateHandle.get<Long>("songId"))
    private val _state = MutableStateFlow(MetadataEditUiState())
    val state: StateFlow<MetadataEditUiState> = _state.asStateFlow()

    private var pendingAlbumArt: ByteArray? = null

    init {
        viewModelScope.launch {
            val song = songRepository.getById(songId)
            if (song != null) {
                _state.value = MetadataEditUiState(
                    song = song,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    albumArtist = song.artist,
                    genre = song.genre,
                    year = if (song.year > 0) song.year.toString() else "",
                    trackNumber = if (song.trackNumber > 0) song.trackNumber.toString() else "",
                    discNumber = "1",
                    comment = "",
                )
            }
        }
    }

    fun onFieldChange(field: MetadataField, value: String) {
        _state.value = when (field) {
            MetadataField.TITLE -> _state.value.copy(title = value)
            MetadataField.ARTIST -> _state.value.copy(artist = value)
            MetadataField.ALBUM -> _state.value.copy(album = value)
            MetadataField.ALBUM_ARTIST -> _state.value.copy(albumArtist = value)
            MetadataField.GENRE -> _state.value.copy(genre = value)
            MetadataField.YEAR -> _state.value.copy(year = value.filter { it.isDigit() })
            MetadataField.TRACK -> _state.value.copy(trackNumber = value.filter { it.isDigit() })
            MetadataField.DISC -> _state.value.copy(discNumber = value.filter { it.isDigit() })
            MetadataField.COMMENT -> _state.value.copy(comment = value)
        }
    }

    fun onAlbumArtPicked(bytes: ByteArray) {
        pendingAlbumArt = bytes
    }

    fun save() {
        val song = _state.value.song ?: return
        _state.value = _state.value.copy(isSaving = true, error = null)
        viewModelScope.launch { performSave(song) }
    }

    private suspend fun performSave(song: Song) {
        val s = _state.value
        val edit = SongMetadataEdit(
            title = s.title.ifBlank { song.title },
            artist = s.artist.ifBlank { song.artist },
            album = s.album.ifBlank { song.album },
            albumArtist = s.albumArtist.ifBlank { s.artist },
            genre = s.genre.ifBlank { "Unknown" },
            year = s.year.toIntOrNull() ?: song.year,
            trackNumber = s.trackNumber.toIntOrNull() ?: song.trackNumber,
            discNumber = s.discNumber.toIntOrNull() ?: 1,
            comment = s.comment,
        )
        when (val result = metadataEditor.updateMetadata(song.id, song.path, edit, pendingAlbumArt)) {
            is FileOperationResult.Success -> {
                songRepository.getById(song.id) // no-op read; row is refreshed by next rescan/observer
                applyLocalUpdate(song, edit)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            }
            is FileOperationResult.NeedsUserConsent -> {
                _state.value = _state.value.copy(isSaving = false, pendingConsent = result.intentSender)
            }
            is FileOperationResult.Failed -> {
                _state.value = _state.value.copy(isSaving = false, error = result.message)
            }
        }
    }

    /** Called by the screen after the system consent dialog returns OK. */
    fun onConsentGranted() {
        val song = _state.value.song ?: return
        _state.value = _state.value.copy(pendingConsent = null, isSaving = true)
        viewModelScope.launch { performSave(song) }
    }

    fun onConsentDenied() {
        _state.value = _state.value.copy(pendingConsent = null, error = "Permission to modify this file was denied.")
    }

    private suspend fun applyLocalUpdate(song: Song, edit: SongMetadataEdit) {
        // Keep Room in sync immediately so the UI reflects the edit without
        // waiting for a full rescan.
        val updated = song.copy(
            title = edit.title,
            artist = edit.artist,
            album = edit.album,
            genre = edit.genre,
            year = edit.year,
            trackNumber = edit.trackNumber,
        )
        songRepository.applyMetadataEdit(updated)
    }
}

enum class MetadataField { TITLE, ARTIST, ALBUM, ALBUM_ARTIST, GENRE, YEAR, TRACK, DISC, COMMENT }
