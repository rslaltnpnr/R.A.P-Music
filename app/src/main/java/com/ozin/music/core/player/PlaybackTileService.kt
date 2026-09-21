package com.ozin.music.core.player

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ozin.music.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings tile: play/pause for whatever is currently loaded in
 * [PlayerController]. Uses the same singleton controller/MediaController the
 * rest of the app uses, so it never starts a second playback path and never
 * builds a new queue - tapping it when nothing is queued is a no-op.
 *
 * Deliberately uses the simple always-available TileService callback
 * pattern (onStartListening/onClick/onStopListening) rather than the
 * "active tile" APIs, since a passive toggle tile does not need them.
 * Single-tap (play/pause) only: `TileService` has no long-press callback
 * on any public API level, so a "long-press to skip" tile gesture is not
 * possible here.
 */
@AndroidEntryPoint
class PlaybackTileService : TileService() {

    @javax.inject.Inject
    lateinit var playerController: PlayerController

    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        playerController.connect()
        val job = Job()
        scope = CoroutineScope(Dispatchers.Main.immediate + job)
        scope?.launch {
            playerController.state.collect { state ->
                updateTile(isPlaying = state.isPlaying, hasQueue = state.currentSong != null)
            }
        }
    }

    override fun onClick() {
        super.onClick()
        playerController.togglePlayPause()
    }

    override fun onStopListening() {
        super.onStopListening()
        scope?.cancel()
        scope = null
    }

    private fun updateTile(isPlaying: Boolean, hasQueue: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (hasQueue) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name)
        tile.icon = Icon.createWithResource(
            this,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
        )
        tile.updateTile()
    }
}
