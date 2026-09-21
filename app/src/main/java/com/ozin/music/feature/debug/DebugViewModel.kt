package com.ozin.music.feature.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.domain.ArtworkSource
import com.ozin.music.core.domain.AudioOutputDetector
import com.ozin.music.core.domain.AudioOutputKind
import com.ozin.music.core.player.PlaybackUiState
import com.ozin.music.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebugUiState(
    val playback: PlaybackUiState = PlaybackUiState(),
    val audioOutput: AudioOutputKind = AudioOutputKind.THIS_DEVICE,
)

/**
 * Backs the Debug screen (item 9) with only real, live data pulled straight
 * from [PlayerController]'s state (the same state the rest of the app's UI
 * observes) and [AudioOutputDetector] (the same detector item 8's Now
 * Playing indicator uses) — no parallel/fabricated diagnostic path.
 */
@HiltViewModel
class DebugViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val audioOutputDetector: AudioOutputDetector,
) : ViewModel() {

    private val _audioOutput = MutableStateFlow(AudioOutputKind.THIS_DEVICE)

    val state: StateFlow<DebugUiState> = combine(
        playerController.state,
        _audioOutput,
    ) { playback, audioOutput ->
        DebugUiState(playback = playback, audioOutput = audioOutput)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebugUiState())

    init {
        // Audio-routing changes have no dedicated callback wired through this
        // app's existing code, so this screen (debug-only, low refresh
        // sensitivity) polls the same detector item 8 uses.
        viewModelScope.launch {
            while (true) {
                _audioOutput.value = audioOutputDetector.currentOutput()
                delay(2_000)
            }
        }
    }

    fun artworkSourceLabelKey(): ArtworkSource? = state.value.playback.currentArtworkSource
}
