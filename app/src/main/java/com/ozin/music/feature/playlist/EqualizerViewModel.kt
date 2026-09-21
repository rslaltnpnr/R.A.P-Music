package com.ozin.music.feature.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.player.EffectsChain
import com.ozin.music.core.player.EqPreset
import com.ozin.music.core.player.SleepTimer
import com.ozin.music.core.player.SleepTimerMode
import com.ozin.music.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class EqualizerUiState(
    val enabled: Boolean = false,
    val preset: EqPreset = EqPreset.FLAT,
    val sleepRemainingMs: Long = 0L,
)

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val effectsChain: EffectsChain,
    private val sleepTimer: SleepTimer,
    private val playerController: PlayerController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EqualizerUiState())
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    val sleepRemainingMs: StateFlow<Long> = sleepTimer.remainingMs

    fun setEnabled(enabled: Boolean) {
        effectsChain.enabled = enabled
        _uiState.value = _uiState.value.copy(enabled = enabled)
    }

    fun applyPreset(preset: EqPreset) {
        effectsChain.applyPreset(preset)
        _uiState.value = _uiState.value.copy(preset = preset)
    }

    fun startSleepTimer(minutes: Int) {
        val player = playerControllerPlayerOrNull() ?: return
        sleepTimer.start(SleepTimerMode.Fixed(minutes), player)
    }

    fun startSleepTimerEndOfTrack() {
        val player = playerControllerPlayerOrNull() ?: return
        sleepTimer.start(SleepTimerMode.EndOfTrack, player)
    }

    fun cancelSleepTimer() = sleepTimer.cancel()

    // PlayerController hides the raw ExoPlayer behind a controller StateFlow;
    // the sleep timer needs a real androidx.media3.common.Player to pause/fade,
    // which the MediaController itself satisfies since it implements Player.
    private fun playerControllerPlayerOrNull(): androidx.media3.common.Player? = playerController.rawPlayer()
}
