package com.ozin.music.core.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.ozin.music.R
import com.ozin.music.core.player.PlaybackService

/**
 * Home-screen widget for play/pause/next/previous. Deliberately does not
 * start a second playback path: every button sends an explicit broadcast
 * back into this same provider, which then opens a real Media3
 * [MediaController] against the already-running [PlaybackService] session
 * (the exact same session/queue the in-app UI and the Quick Settings tile
 * use) and issues the matching player command.
 *
 * Real-time content updates (art/title/artist/play state) are NOT done by
 * polling here - [PlaybackWidgetUpdater] is attached as a `Player.Listener`
 * directly inside [PlaybackService] (which already listens to the player for
 * effects/stats/crossfade) and pushes `AppWidgetManager.updateAppWidget`
 * synchronously whenever playback state changes, for as long as the service
 * process is alive. `updatePeriodMillis="0"` in the widget's info XML
 * disables Android's periodic (min 30 minute) polling entirely, since it
 * would be both wasteful and far too slow for a "reacts live" widget. The
 * only case this doesn't cover is the service process having been killed
 * with no widget-visible song still queued; the widget then simply shows its
 * last known state until playback resumes, which is standard behavior for
 * this style of widget (same tradeoff every media widget on Android makes).
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
            ACTION_TOGGLE, ACTION_NEXT, ACTION_PREVIOUS -> sendCommand(context, intent.action!!)
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
        views.setOnClickPendingIntent(R.id.widget_play_pause, actionPendingIntent(context, ACTION_TOGGLE))
        views.setOnClickPendingIntent(R.id.widget_next, actionPendingIntent(context, ACTION_NEXT))
        views.setOnClickPendingIntent(R.id.widget_prev, actionPendingIntent(context, ACTION_PREVIOUS))
    }

    /** Builds a real Media3 controller against the running session and
     * issues one command, then releases it. Uses `goAsync()` since a
     * BroadcastReceiver's `onReceive` must return quickly but connecting a
     * MediaController is asynchronous. */
    private fun sendCommand(context: Context, action: String) {
        val pending = goAsync()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            try {
                val controller = future.get()
                when (action) {
                    ACTION_TOGGLE -> if (controller.isPlaying) controller.pause() else controller.play()
                    ACTION_NEXT -> controller.seekToNextMediaItem()
                    ACTION_PREVIOUS -> controller.seekToPreviousMediaItem()
                }
                controller.release()
            } catch (_: Exception) {
                // Session not reachable (e.g. app fully killed and not yet
                // restarted) - a no-op is the correct, safe behavior here.
            } finally {
                pending.finish()
            }
        }, MoreExecutors.directExecutor())
    }

    companion object {
        const val ACTION_TOGGLE = "com.ozin.music.widget.ACTION_TOGGLE"
        const val ACTION_NEXT = "com.ozin.music.widget.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.ozin.music.widget.ACTION_PREVIOUS"

        fun actionPendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, PlaybackWidgetProvider::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        fun widgetComponent(context: Context) = ComponentName(context, PlaybackWidgetProvider::class.java)
    }
}
