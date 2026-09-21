package com.ozin.music.core.player

import android.content.pm.PackageManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.ozin.music.core.data.repository.PlaylistRepository
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.domain.ArtworkResolver
import com.ozin.music.core.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground [MediaLibraryService] hosting the single app-wide ExoPlayer
 * instance. Media3 wires audio-focus handling, becoming-noisy handling,
 * Bluetooth/headset button support and the lockscreen/notification MediaStyle
 * UI automatically once the session + player are attached. Extends
 * `MediaLibraryService` (rather than plain `MediaSessionService`) so the same
 * session/player also exposes a browsable tree to Android Auto via
 * [OzinLibrarySessionCallback] — chosen over a second, separate service
 * because Media3 supports only one active session per player, so a second
 * service would need its own session on the same player, which is unsafe to
 * do blind; a single session covering both phone and Auto avoids that risk
 * entirely and is Media3's own documented pattern (see the UAMP sample app).
 */
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject lateinit var exoPlayer: ExoPlayer
    @Inject lateinit var effectsChain: EffectsChain
    @Inject lateinit var listeningStatsRecorder: ListeningStatsRecorder
    @Inject lateinit var crossfadeController: CrossfadeController
    @Inject lateinit var playbackFader: PlaybackFader
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var songRepository: SongRepository
    @Inject lateinit var playlistRepository: PlaylistRepository
    @Inject lateinit var artworkResolver: ArtworkResolver

    private var mediaSession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate)
    private var settingsJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        exoPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true,
        )
        exoPlayer.setHandleAudioBecomingNoisy(true)
        exoPlayer.addListener(listeningStatsRecorder)
        exoPlayer.addListener(effectsChain)
        exoPlayer.addListener(crossfadeController)
        exoPlayer.addListener(playbackFader)
        crossfadeController.attach(exoPlayer)
        playbackFader.attach(exoPlayer)

        mediaSession = MediaLibrarySession.Builder(
            this,
            exoPlayer,
            OzinLibrarySessionCallback(applicationContext, songRepository, playlistRepository, artworkResolver, settingsRepository),
        ).build()

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT).not()) {
            // Devices with no audio output still shouldn't crash the service.
        }

        settingsJob = serviceScope.launch {
            settingsRepository.settings.collect { settings ->
                effectsChain.enabled = settings.eqEnabled
                effectsChain.setBassBoostStrength(settings.bassBoostStrength)
                effectsChain.setVirtualizerStrength(settings.virtualizerStrength)
                effectsChain.setLoudnessGainMb(
                    if (settings.normalizationEnabled) maxOf(settings.loudnessGainMb, 500) else settings.loudnessGainMb
                )
                if (settings.eqPreset == com.ozin.music.core.domain.EqPresetId.CUSTOM && settings.eqCustomBands.isNotEmpty()) {
                    effectsChain.applyCustomBands(settings.eqCustomBands.toIntArray())
                } else {
                    effectsChain.applyPreset(settings.eqPreset)
                }
                effectsChain.setPreampMb(settings.eqPreampMb)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        settingsJob?.cancel()
        effectsChain.release()
        crossfadeController.release()
        playbackFader.release()
        mediaSession?.run {
            player.removeListener(listeningStatsRecorder)
            player.removeListener(effectsChain)
            player.removeListener(crossfadeController)
            player.removeListener(playbackFader)
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
