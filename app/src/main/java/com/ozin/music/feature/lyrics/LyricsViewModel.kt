package com.ozin.music.feature.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.network.LrcLibClient
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
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val lrcLibClient: LrcLibClient,
) : ViewModel() {

    private val rawLyricsText = MutableStateFlow<String?>(null)
    private val isEditing = MutableStateFlow(false)
    private val editText = MutableStateFlow("")
    private val saveError = MutableStateFlow<String?>(null)
    private val saveSuccess = MutableStateFlow(false)

    /** Song paths already tried (success or failure) via the LRCLIB
     * auto-download this ViewModel's lifetime, so revisiting the same song
     * in one session never re-hits the network once we already know the
     * answer (a success is also cached to disk, see [maybeAutoDownloadLyrics];
     * a failure is only remembered in-memory, and is retried next app run). */
    private val autoDownloadAttempted = mutableSetOf<String>()

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

        // Auto-download fallback: whenever the current song changes (or the
        // setting is turned on) and there is no local .lrc file for it, try
        // LRCLIB once. This only reacts to the song identity, not every
        // playback tick, so it never re-fires 3x/second.
        viewModelScope.launch {
            combine(
                playerController.state,
                settingsRepository.settings,
            ) { playback, settings -> Triple(playback.currentSong?.path, playback.currentSong, settings.autoDownloadLyricsEnabled) }
                .distinctUntilChanged()
                .collect { (path, song, enabled) ->
                    if (path == null || song == null || !enabled) return@collect
                    maybeAutoDownloadLyrics(path, song.title, song.artist, song.album, song.durationMs)
                }
        }
    }

    /** If [audioPath] has no local .lrc file already, looks it up on LRCLIB
     * off the main thread and, on a match, writes it to a real local .lrc
     * file next to the song (same base-filename convention the local lookup
     * above already uses) so it is picked up on the next state recompute
     * (driven by the 300ms position tick above) and never re-downloaded.
     * Fails completely silently - no error is surfaced to the UI - since
     * this is a best-effort background fallback, not a user-initiated action. */
    private fun maybeAutoDownloadLyrics(
        audioPath: String,
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
    ) {
        if (audioPath in autoDownloadAttempted) return
        if (LrcParser.findAndParseFor(audioPath) != null) return
        val audioFile = File(audioPath)
        val lrcFile = File(audioFile.parentFile, audioFile.nameWithoutExtension + ".lrc")
        if (lrcFile.exists()) return // has an unsynced/local file already - don't overwrite it
        autoDownloadAttempted += audioPath
        viewModelScope.launch {
            val lyrics = runCatching {
                lrcLibClient.fetchSyncedLyrics(
                    trackName = title,
                    artistName = artist,
                    albumName = album,
                    durationMs = durationMs,
                )
            }.getOrNull() ?: return@launch
            // Written straight to disk (not through `rawLyricsText`, which
            // is scoped to the manual editor and not per-song): the next
            // state recompute - driven by the 300ms position tick already
            // running above - re-reads the file via LrcParser.findAndParseFor
            // and picks it up automatically.
            runCatching { lrcFile.writeText(lyrics) }
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
