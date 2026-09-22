package com.ozin.music.core.player.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.ozin.music.core.player.PlaybackService

/**
 * Shared broadcast-action names and command-sending logic used by every
 * playback widget provider ([PlaybackWidgetProvider], [PlaybackWidgetSmallProvider],
 * [PlaybackWidgetLargeProvider]). Each provider routes its buttons' click
 * intents through its own class (so PendingIntents target the right
 * receiver), but the actual "connect a MediaController to the running
 * [PlaybackService] session and issue one command" logic lives here once
 * instead of being duplicated per provider.
 */
object WidgetCommands {
    const val ACTION_TOGGLE = "com.ozin.music.widget.ACTION_TOGGLE"
    const val ACTION_NEXT = "com.ozin.music.widget.ACTION_NEXT"
    const val ACTION_PREVIOUS = "com.ozin.music.widget.ACTION_PREVIOUS"

    fun actionPendingIntent(context: Context, receiver: Class<*>, action: String): PendingIntent {
        val intent = Intent(context, receiver).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            (receiver.name + action).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Builds a real Media3 controller against the running session and
     * issues one command, then releases it. Callers must invoke this from
     * within a `goAsync()` pending result and call `pending.finish()` once
     * the supplied [onDone] runs. */
    fun sendCommand(context: Context, action: String, onDone: () -> Unit) {
        val token = SessionToken(context, android.content.ComponentName(context, PlaybackService::class.java))
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
                onDone()
            }
        }, MoreExecutors.directExecutor())
    }
}
