package com.ozin.music.core.player.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ozin.music.R

/**
 * Large home-screen widget (~4x2): bigger album art, title/artist and the
 * full prev/play-pause/next row. Same command-routing pattern as
 * [PlaybackWidgetProvider] - see its doc comment for the full explanation of
 * the live-update mechanism, shared here via [PlaybackWidgetUpdater] and
 * [WidgetCommands].
 */
class PlaybackWidgetLargeProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildInitialViews(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetCommands.ACTION_TOGGLE, WidgetCommands.ACTION_NEXT, WidgetCommands.ACTION_PREVIOUS -> {
                val pending = goAsync()
                WidgetCommands.sendCommand(context, intent.action!!) { pending.finish() }
            }
        }
    }

    private fun buildInitialViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_playback_large)
        views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_no_song))
        views.setTextViewText(R.id.widget_artist, "")
        views.setImageViewResource(R.id.widget_art, R.drawable.default_artwork)
        views.setImageViewResource(R.id.widget_play_pause, android.R.drawable.ic_media_play)
        wireButtons(context, views)
        return views
    }

    private fun wireButtons(context: Context, views: RemoteViews) {
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
    }

    companion object {
        fun widgetComponent(context: Context) = ComponentName(context, PlaybackWidgetLargeProvider::class.java)
    }
}
