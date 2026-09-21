package com.ozin.music.feature.problems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.model.ProblemFile
import com.ozin.music.core.data.repository.ProblemFileRepository
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.files.MetadataEditor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProblemFilesViewModel @Inject constructor(
    private val problemFileRepository: ProblemFileRepository,
    private val songRepository: SongRepository,
    private val metadataEditor: MetadataEditor,
) : ViewModel() {

    val problemFiles: StateFlow<List<ProblemFile>> = problemFileRepository.problemFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun ignore(problemFile: ProblemFile) {
        viewModelScope.launch { problemFileRepository.ignore(problemFile.path) }
    }

    fun remove(problemFile: ProblemFile, alsoDeleteFile: Boolean) {
        viewModelScope.launch {
            if (alsoDeleteFile && problemFile.songId != null) {
                metadataEditor.deleteSongs(listOf(problemFile.songId))
            }
            problemFileRepository.ignore(problemFile.path)
        }
    }

    fun rescan(problemFile: ProblemFile) {
        viewModelScope.launch {
            songRepository.rescan()
            // If the path is now readable, rescan() will have removed the
            // stale problem entry the next time this path errors — but if it
            // no longer errors at all, nothing re-reports it, so clear it
            // optimistically and let a future failure re-add it.
            val stillPresent = java.io.File(problemFile.path).let { it.exists() && it.canRead() }
            if (stillPresent) problemFileRepository.clear(problemFile.path)
        }
    }
}
