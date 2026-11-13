package com.ozin.music.feature.classic

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.data.repository.PlaylistRepository
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.domain.EqPresetId
import com.ozin.music.core.domain.EqPresets
import com.ozin.music.core.player.ClassicAudioProcessor
import com.ozin.music.core.player.PlayerController
import com.ozin.music.core.settings.AppSettings
import com.ozin.music.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class ClassicPlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val songs: SongRepository,
    private val playlists: PlaylistRepository,
    private val settingsRepository: SettingsRepository,
    private val processor: ClassicAudioProcessor,
    val player: PlayerController,
) : ViewModel() {
    val playback = player.state
    val audioFrame = processor.frame
    val library = songs.songs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings = settingsRepository.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings(),
    )
    private val audio = context.getSystemService(AudioManager::class.java)
    private val _volume = MutableStateFlow(readVolume())
    val volume = _volume.asStateFlow()
    private val _scanning = MutableStateFlow(false)
    val scanning = _scanning.asStateFlow()

    init {
        rescan()
        viewModelScope.launch {
            while (true) {
                player.tickPosition()
                _volume.value = readVolume()
                delay(300)
            }
        }
    }

    fun rescan() {
        if (_scanning.value) return
        _scanning.value = true
        viewModelScope.launch {
            try {
                songs.rescan()
            } finally {
                _scanning.value = false
            }
        }
    }

    fun play() {
        if (player.hasQueue()) player.play()
        else if (library.value.isNotEmpty()) player.playSongs(library.value, 0)
    }

    fun add(song: Song) = player.addToQueue(song)

    fun saveQueue(name: String) {
        val snapshot = playback.value.queue.filter { it.id >= 0 }
        if (name.isBlank() || snapshot.isEmpty()) return
        viewModelScope.launch {
            val id = playlists.create(name.trim())
            snapshot.forEach { playlists.addSong(id, it.id) }
        }
    }

    fun setCapture(enabled: Boolean) { processor.capture = enabled }

    private fun readVolume(): Float {
        val stream = AudioManager.STREAM_MUSIC
        return runCatching { audio.getStreamVolume(stream).toFloat() / audio.getStreamMaxVolume(stream).coerceAtLeast(1) }
            .getOrDefault(0f)
    }

    fun setVolume(value: Float) {
        val stream = AudioManager.STREAM_MUSIC
        runCatching {
            val max = audio.getStreamMaxVolume(stream)
            val min = if (android.os.Build.VERSION.SDK_INT >= 28) audio.getStreamMinVolume(stream) else 0
            audio.setStreamVolume(stream, (value * max).roundToInt().coerceIn(min, max), 0)
        }
        _volume.value = readVolume()
    }

    fun setBalance(value: Float) {
        viewModelScope.launch { settingsRepository.setStereoBalance((value * 100).roundToInt()) }
    }

    fun setEqEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEqEnabled(enabled) }
    }

    fun setEqAuto(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEqSuggestionEnabled(enabled) }
    }

    fun setPreamp(level: Int) {
        viewModelScope.launch { settingsRepository.setEqPreampMb(level.coerceIn(-1200, 1200)) }
    }

    fun setBand(index: Int, level: Int) {
        viewModelScope.launch {
            val curve = classicEqCurve(settingsRepository.settings.first()).toMutableList()
            curve[index] = level.coerceIn(-1200, 1200)
            settingsRepository.setCustomEqCurve(curve)
        }
    }

    fun applyPreset(id: EqPresetId) {
        viewModelScope.launch { settingsRepository.setEqPreset(id) }
    }
}

internal val classicEqLabels = listOf("60", "170", "310", "600", "1K", "3K", "6K", "12K", "14K", "16K")

internal fun classicEqCurve(settings: AppSettings): List<Int> {
    val reference = if (settings.eqPreset == EqPresetId.CUSTOM && settings.eqCustomBands.isNotEmpty()) {
        settings.eqCustomBands.toIntArray()
    } else {
        EqPresets.referenceTables.getValue(settings.eqPreset)
    }
    return EqPresets.mapToDeviceBands(reference, classicEqLabels.size, -1200..1200).toList()
}

internal fun classicTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
