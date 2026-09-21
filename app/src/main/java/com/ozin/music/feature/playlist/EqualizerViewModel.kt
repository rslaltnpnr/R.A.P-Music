package com.ozin.music.feature.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.domain.EqPresetId
import com.ozin.music.core.player.EffectsChain
import com.ozin.music.core.player.PlayerController
import com.ozin.music.core.player.SleepTimer
import com.ozin.music.core.player.SleepTimerMode
import com.ozin.music.core.settings.AppSettings
import com.ozin.music.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val effectsChain: EffectsChain,
    private val sleepTimer: SleepTimer,
    private val playerController: PlayerController,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings()
    )

    val sleepRemainingMs: StateFlow<Long> = sleepTimer.remainingMs

    fun bandCount(): Int = effectsChain.bandCount()
    fun bandLevelRange(): ShortArray = effectsChain.bandLevelRange()
    fun currentBandLevel(band: Int): Short = effectsChain.currentBandLevel(band)
    fun centerFrequencyHz(band: Int): Int = effectsChain.centerFrequencyHz(band)

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEqEnabled(enabled) }
    }

    fun applyPreset(preset: EqPresetId) {
        viewModelScope.launch { settingsRepository.setEqPreset(preset) }
    }

    fun setBandLevel(band: Int, level: Short) {
        effectsChain.setBandLevel(band, level)
        val bands = IntArray(effectsChain.bandCount()) { effectsChain.currentBandLevel(it).toInt() }
        viewModelScope.launch {
            settingsRepository.setEqPreset(EqPresetId.CUSTOM)
            settingsRepository.setEqCustomBands(bands.toList())
        }
    }

    fun setPreamp(mb: Int) {
        viewModelScope.launch { settingsRepository.setEqPreampMb(mb) }
    }

    fun setBassBoost(strength: Int) {
        viewModelScope.launch { settingsRepository.setBassBoostStrength(strength) }
    }

    fun setVirtualizer(strength: Int) {
        viewModelScope.launch { settingsRepository.setVirtualizerStrength(strength) }
    }

    fun setLoudnessGain(mb: Int) {
        viewModelScope.launch { settingsRepository.setLoudnessGainMb(mb) }
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
