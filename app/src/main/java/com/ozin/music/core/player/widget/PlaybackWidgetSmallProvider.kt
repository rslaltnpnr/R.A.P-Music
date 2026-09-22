package com.ozin.music.core.player.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ozin.music.R

/**
 * Icon-only home-screen widget (~1x1/2x1): just the current album art as the
 * background with a single play/pause tap target overlaid on it. Same
 * command-routing pattern as [PlaybackWidgetProvider] - see its doc comment
 * for the full explanation of the live-update mechanism, shared here via
 * [PlaybackWidgetUpdater] and [WidgetCommands].
 */
class PlaybackWidgetSmallProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildInitialViews(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetCommands.ACTION_TOGGLE) {
            val pending = goAsync()
            WidgetCommands.sendCommand(context, intent.action!!) { pending.finish() }
        }
    }

    private fun buildInitialViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_playback_small)
        views.setImageViewResource(R.id.widget_art, R.drawable.default_artwork)
        views.setImageViewResource(R.id.widget_play_pause, android.R.drawable.ic_media_play)
        views.setOnClickPendingIntent(
            R.id.widget_play_pause,
            WidgetCommands.actionPendingIntent(context, PlaybackWidgetSmallProvider::class.java, WidgetCommands.ACTION_TOGGLE),
        )
        return views
    }

    companion object {
        fun widgetComponent(context: Context) = ComponentName(context, PlaybackWidgetSmallProvider::class.java)
    }
}
