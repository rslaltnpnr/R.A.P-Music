package com.ozin.music.feature.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.domain.LrcParser
import com.ozin.music.core.domain.LyricLine
import com.ozin.music.core.player.PlayerController
import com.ozin.music.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LyricsUiState(
    val songTitle: String = "",
    val songPath: String? = null,
    val lines: List<LyricLine> = emptyList(),
    val rawText: String = "",
    val hasSyncedLyrics: Boolean = false,
    val hasAnyLyrics: Boolean = false,
    val currentLine: LyricLine? = null,
    val offsetMs: Int = 0,
    val isEditing: Boolean = false,
    val editText: String = "",
    val saveError: String? = null,
    val saveSuccess: Boolean = false,
)

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val rawLyricsText = MutableStateFlow<String?>(null)
    private val isEditing = MutableStateFlow(false)
    private val editText = MutableStateFlow("")
    private val saveError = MutableStateFlow<String?>(null)
    private val saveSuccess = MutableStateFlow(false)

    val uiState: StateFlow<LyricsUiState> = combine(
        playerController.state,
        settingsRepository.settings,
        rawLyricsText,
        isEditing,
    ) { playback, settings, raw, editing ->
        val song = playback.currentSong
        val parsedLines = song?.path?.let { path ->
            if (raw != null) LrcParser.parse(raw) else LrcParser.findAndParseFor(path)
        } ?: emptyList()
        val adjustedPosition = playback.positionMs + settings.lyricsOffsetMs
        LyricsUiState(
            songTitle = song?.title.orEmpty(),
            songPath = song?.path,
            lines = parsedLines,
            rawText = raw ?: song?.path?.let { readLrcTextOrEmpty(it) }.orEmpty(),
            hasSyncedLyrics = parsedLines.isNotEmpty(),
            hasAnyLyrics = parsedLines.isNotEmpty() || (raw?.isNotBlank() == true),
            currentLine = LrcParser.currentLine(parsedLines, adjustedPosition),
            offsetMs = settings.lyricsOffsetMs,
            isEditing = editing,
            editText = editText.value,
            saveError = saveError.value,
            saveSuccess = saveSuccess.value,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LyricsUiState())

    init {
        // Keep the current-line highlight ticking in step with playback,
        // reusing the same 500ms cadence PlayerViewModel already ticks at.
        viewModelScope.launch {
            while (true) {
                delay(300)
                playerController.tickPosition()
            }
        }
    }

    private fun readLrcTextOrEmpty(audioPath: String): String = try {
        val audioFile = File(audioPath)
        val lrcFile = File(audioFile.parentFile, audioFile.nameWithoutExtension + ".lrc")
        if (lrcFile.exists() && lrcFile.canRead()) lrcFile.readText() else ""
    } catch (_: Exception) {
        ""
    }

    fun adjustOffset(deltaMs: Int) {
        viewModelScope.launch { settingsRepository.adjustLyricsOffset(deltaMs) }
    }

    fun startEditing() {
        editText.value = uiState.value.rawText
        saveError.value = null
        saveSuccess.value = false
        isEditing.value = true
    }

    fun cancelEditing() {
        isEditing.value = false
    }

    fun onEditTextChanged(text: String) {
        editText.value = text
    }

    fun saveLyrics() {
        val path = uiState.value.songPath ?: return
        val content = editText.value
        viewModelScope.launch {
            val result = runCatching {
                val audioFile = File(path)
                val lrcFile = File(audioFile.parentFile, audioFile.nameWithoutExtension + ".lrc")
                lrcFile.writeText(content)
            }
            if (result.isSuccess) {
                rawLyricsText.value = content
                saveError.value = null
                saveSuccess.value = true
                isEditing.value = false
            } else {
                saveError.value = result.exceptionOrNull()?.message
                    ?: "Could not write .lrc file (storage may be read-only for this location)"
            }
        }
    }

    fun dismissSaveSuccess() {
        saveSuccess.value = false
    }
}
