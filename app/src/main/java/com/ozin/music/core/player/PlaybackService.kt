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
import com.ozin.music.core.player.widget.PlaybackWidgetUpdater
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
    @Inject lateinit var widgetUpdater: PlaybackWidgetUpdater

    private var mediaSession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate)
    private var settingsJob: Job? = null

    private var shakeDetector: ShakeDetector? = null
    private var shakeToPauseEnabled = false
    private var shakeDetectorActive = false

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
        exoPlayer.addListener(widgetUpdater)
        crossfadeController.attach(exoPlayer)
        playbackFader.attach(exoPlayer)

        shakeDetector = ShakeDetector(applicationContext) {
            if (exoPlayer.isPlaying) exoPlayer.pause()
        }
        exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = updateShakeListenerState()
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                AudioSessionIdBridge.update(audioSessionId)
            }
        })

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
                shakeToPauseEnabled = settings.shakeToPauseEnabled
                updateShakeListenerState()
            }
        }
    }

    /** Registers/unregisters the accelerometer listener so it only runs
     * while it can actually do something useful: the setting is on AND a
     * song is currently playing. Anything else (setting off, or nothing
     * playing) unregisters it, avoiding a continuous high-frequency sensor
     * listener draining battery for no reason. */
    private fun updateShakeListenerState() {
        val shouldBeActive = shakeToPauseEnabled && exoPlayer.isPlaying
        if (shouldBeActive == shakeDetectorActive) return
        shakeDetectorActive = shouldBeActive
        if (shouldBeActive) shakeDetector?.start() else shakeDetector?.stop()
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
        shakeDetector?.stop()
        effectsChain.release()
        crossfadeController.release()
        playbackFader.release()
        mediaSession?.run {
            player.removeListener(listeningStatsRecorder)
            player.removeListener(effectsChain)
            player.removeListener(crossfadeController)
            player.removeListener(playbackFader)
            player.removeListener(widgetUpdater)
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
