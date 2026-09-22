package com.ozin.music.core.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ozin.music.R

/**
 * Home-screen widget for play/pause/next/previous (medium, ~4x1 horizontal
 * bar). Deliberately does not start a second playback path: every button
 * sends an explicit broadcast back into this same provider, which then opens
 * a real Media3 `MediaController` against the already-running
 * [com.ozin.music.core.player.PlaybackService] session (the exact same
 * session/queue the in-app UI and the Quick Settings tile use) and issues
 * the matching player command via [WidgetCommands].
 *
 * Real-time content updates (art/title/artist/play state) are NOT done by
 * polling here - [PlaybackWidgetUpdater] is attached as a `Player.Listener`
 * directly inside [com.ozin.music.core.player.PlaybackService] (which
 * already listens to the player for effects/stats/crossfade) and pushes
 * `AppWidgetManager.updateAppWidget` synchronously whenever playback state
 * changes, for as long as the service process is alive, across every widget
 * size/provider that is installed. `updatePeriodMillis="0"` in the widget's
 * info XML disables Android's periodic (min 30 minute) polling entirely,
 * since it would be both wasteful and far too slow for a "reacts live"
 * widget. The only case this doesn't cover is the service process having
 * been killed with no widget-visible song still queued; the widget then
 * simply shows its last known state until playback resumes, which is
 * standard behavior for this style of widget (same tradeoff every media
 * widget on Android makes).
 *
 * This is one of three sibling widget providers offering distinct sizes -
 * see also [PlaybackWidgetSmallProvider] (icon-only, ~2x1) and
 * [PlaybackWidgetLargeProvider] (~4x2, bigger art). Android's widget picker
 * shows each `AppWidgetProvider` as its own separately pickable widget.
 */
class PlaybackWidgetProvider : AppWidgetProvider() {

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
        val views = RemoteViews(context.packageName, R.layout.widget_playback)
        views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_no_song))
        views.setTextViewText(R.id.widget_artist, "")
        views.setImageViewResource(R.id.widget_art, R.drawable.default_artwork)
        views.setImageViewResource(R.id.widget_play_pause, android.R.drawable.ic_media_play)
        wireButtons(context, views)
        return views
    }

    private fun wireButtons(context: Context, views: RemoteViews) {
        views.setOnClickPendingIntent(R.id.widget_play_pause, actionPendingIntent(context, WidgetCommands.ACTION_TOGGLE))
        views.setOnClickPendingIntent(R.id.widget_next, actionPendingIntent(context, WidgetCommands.ACTION_NEXT))
        views.setOnClickPendingIntent(R.id.widget_prev, actionPendingIntent(context, WidgetCommands.ACTION_PREVIOUS))
    }

    companion object {
        fun actionPendingIntent(context: Context, action: String): PendingIntent =
            WidgetCommands.actionPendingIntent(context, PlaybackWidgetProvider::class.java, action)

        fun widgetComponent(context: Context) = ComponentName(context, PlaybackWidgetProvider::class.java)
    }
}
