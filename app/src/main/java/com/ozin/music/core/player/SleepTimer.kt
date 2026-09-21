package com.ozin.music.core.player

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SleepTimerMode {
    data class Fixed(val minutes: Int) : SleepTimerMode
    data object EndOfTrack : SleepTimerMode
}

/**
 * Coroutine countdown that fades volume down in the final seconds, then
 * pauses playback and restores volume to full for the next session.
 */
@Singleton
class SleepTimer @Inject constructor() {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var job: Job? = null

    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    val isActive: Boolean get() = job?.isActive == true

    fun start(mode: SleepTimerMode, player: Player) {
        cancel()
        when (mode) {
            is SleepTimerMode.Fixed -> startFixed(mode.minutes * 60_000L, player)
            SleepTimerMode.EndOfTrack -> startEndOfTrack(player)
        }
    }

    private fun startFixed(totalMs: Long, player: Player) {
        job = scope.launch {
            var remaining = totalMs
            _remainingMs.value = remaining
            val fadeWindowMs = 10_000L
            while (isActive && remaining > 0) {
                delay(TICK_MS)
                remaining -= TICK_MS
                _remainingMs.value = remaining.coerceAtLeast(0)
                if (remaining in 0..fadeWindowMs) {
                    val fraction = (remaining.toFloat() / fadeWindowMs).coerceIn(0f, 1f)
                    runCatching { player.volume = fraction }
                }
            }
            runCatching {
                player.pause()
                player.volume = 1f
            }
        }
    }

    private fun startEndOfTrack(player: Player) {
        job = scope.launch {
            val remainingForTrack = (player.duration - player.currentPosition).coerceAtLeast(0)
            _remainingMs.value = remainingForTrack
            while (isActive) {
                delay(TICK_MS)
                val remaining = (player.duration - player.currentPosition).coerceAtLeast(0)
                _remainingMs.value = remaining
                if (remaining <= 0) break
            }
            runCatching { player.pause() }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remainingMs.value = 0L
    }

    companion object {
        private const val TICK_MS = 500L
    }
}
