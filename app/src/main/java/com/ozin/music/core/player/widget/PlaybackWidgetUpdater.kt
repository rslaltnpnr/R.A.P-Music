package com.ozin.music.core.player.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.media3.common.Player
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ozin.music.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time push updater for [PlaybackWidgetProvider]. Registered as a
 * `Player.Listener` on the single app-wide ExoPlayer instance inside
 * [com.ozin.music.core.player.PlaybackService] (same pattern as
 * `ListeningStatsRecorder`/`EffectsChain`/`CrossfadeController` already
 * registered there) - it never opens its own player or MediaController, it
 * just reads the state of the one that's already playing and reflects it
 * into any installed widget instances via [AppWidgetManager.updateAppWidget].
 * A no-op, cheap check (`getAppWidgetIds`) means this costs nothing when no
 * widget is installed.
 */
@Singleton
class PlaybackWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) : Player.Listener {

    override fun onEvents(player: Player, events: Player.Events) {
        if (
            events.containsAny(
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_MEDIA_METADATA_CHANGED,
            )
        ) {
            update(player)
        }
    }

    private fun update(player: Player) {
        val manager = AppWidgetManager.getInstance(context)
        val metadata = player.mediaMetadata
        val hasItem = player.currentMediaItem != null
        val title = if (hasItem) (metadata.title?.toString() ?: context.getString(R.string.widget_no_song))
        else context.getString(R.string.widget_no_song)
        val artist = if (hasItem) metadata.artist?.toString().orEmpty() else ""
        val artworkUri = metadata.artworkUri.takeIf { hasItem }
        val playPauseIcon = if (player.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseDescription =
            context.getString(if (player.isPlaying) R.string.widget_pause_description else R.string.widget_play_description)

        updateMedium(manager, title, artist, artworkUri, playPauseIcon, playPauseDescription)
        updateLarge(manager, title, artist, artworkUri, playPauseIcon, playPauseDescription)
        updateSmall(manager, artworkUri, playPauseIcon, playPauseDescription)
    }

    private fun updateMedium(
        manager: AppWidgetManager,
        title: String,
        artist: String,
        artworkUri: android.net.Uri?,
        playPauseIcon: Int,
        playPauseDescription: String,
    ) {
        val ids = manager.getAppWidgetIds(PlaybackWidgetProvider.widgetComponent(context))
        if (ids.isEmpty()) return
        val views = RemoteViews(context.packageName, R.layout.widget_playback)
        views.setTextViewText(R.id.widget_title, title)
        views.setTextViewText(R.id.widget_artist, artist)
        if (artworkUri != null) views.setImageViewUri(R.id.widget_art, artworkUri)
        else views.setImageViewResource(R.id.widget_art, R.drawable.default_artwork)
        views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)
        views.setContentDescription(R.id.widget_play_pause, playPauseDescription)
        views.setOnClickPendingIntent(
            R.id.widget_play_pause,
            PlaybackWidgetProvider.actionPendingIntent(context, WidgetCommands.ACTION_TOGGLE),
        )
        views.setOnClickPendingIntent(
            R.id.widget_next,
            PlaybackWidgetProvider.actionPendingIntent(context, WidgetCommands.ACTION_NEXT),
        )
        views.setOnClickPendingIntent(
            R.id.widget_prev,
            PlaybackWidgetProvider.actionPendingIntent(context, WidgetCommands.ACTION_PREVIOUS),
        )
        for (id in ids) manager.updateAppWidget(id, views)
    }

    private fun updateLarge(
        manager: AppWidgetManager,
        title: String,
        artist: String,
        artworkUri: android.net.Uri?,
        playPauseIcon: Int,
        playPauseDescription: String,
    ) {
        val ids = manager.getAppWidgetIds(PlaybackWidgetLargeProvider.widgetComponent(context))
        if (ids.isEmpty()) return
        val views = RemoteViews(context.packageName, R.layout.widget_playback_large)
        views.setTextViewText(R.id.widget_title, title)
        views.setTextViewText(R.id.widget_artist, artist)
        if (artworkUri != null) views.setImageViewUri(R.id.widget_art, artworkUri)
        else views.setImageViewResource(R.id.widget_art, R.drawable.default_artwork)
        views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)
        views.setContentDescription(R.id.widget_play_pause, playPauseDescription)
        views.setOnClickPendingIntent(
            R.id.widget_play_pause,
            WidgetCommands.actionPendingIntent(context, PlaybackWidgetLargeProvider::class.java, WidgetCommands.ACTION_TOGGLE),
        )
        views.setOnClickPendingIntent(
            R.id.widget_next,
            WidgetCommands.actionPendingIntent(context, PlaybackWidgetLargeProvider::class.java, WidgetCommands.ACTION_NEXT),
        )
        views.setOnClickPendingIntent(
            R.id.widget_prev,
            WidgetCommands.actionPendingIntent(context, PlaybackWidgetLargeProvider::class.java, WidgetCommands.ACTION_PREVIOUS),
        )
        for (id in ids) manager.updateAppWidget(id, views)
    }

    private fun updateSmall(
        manager: AppWidgetManager,
        artworkUri: android.net.Uri?,
        playPauseIcon: Int,
        playPauseDescription: String,
    ) {
        val ids = manager.getAppWidgetIds(PlaybackWidgetSmallProvider.widgetComponent(context))
        if (ids.isEmpty()) return
        val views = RemoteViews(context.packageName, R.layout.widget_playback_small)
        if (artworkUri != null) views.setImageViewUri(R.id.widget_art, artworkUri)
        else views.setImageViewResource(R.id.widget_art, R.drawable.default_artwork)
        views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)
        views.setContentDescription(R.id.widget_play_pause, playPauseDescription)
        views.setOnClickPendingIntent(
            R.id.widget_play_pause,
            WidgetCommands.actionPendingIntent(context, PlaybackWidgetSmallProvider::class.java, WidgetCommands.ACTION_TOGGLE),
        )
        for (id in ids) manager.updateAppWidget(id, views)
    }
}
