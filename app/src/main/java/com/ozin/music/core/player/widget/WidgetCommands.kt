package com.ozin.music.core.player.widget

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.ozin.music.OzinMusicAppEntryPoint
import com.ozin.music.core.domain.AutoMediaTree
import com.ozin.music.core.player.PlaybackService
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /** Large-widget-only (item 3): jump to a recently-played track without
     * a full collection-widget rewrite. Each tap advances to the next of
     * the last [RECENT_CYCLE_COUNT] played songs and plays it, cycling back
     * to the first after the last. */
    const val ACTION_CYCLE_RECENT = "com.ozin.music.widget.ACTION_CYCLE_RECENT"

    private const val RECENT_CYCLE_COUNT = 3
    private const val PREFS_NAME = "ozin_widget_recent_cycle"
    private const val KEY_CYCLE_INDEX = "cycle_index"

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

    /**
     * Item 3: real, working "quick access to recently played" for the large
     * widget only. Reads the actual last [RECENT_CYCLE_COUNT] played songs
     * from [com.ozin.music.core.data.repository.SongRepository] (via the
     * Hilt [OzinMusicAppEntryPoint], since this is a plain object with no
     * injection of its own - same escape hatch [com.ozin.music.OzinMusicApp]
     * already uses), advances a small cycle index kept in plain
     * [android.content.SharedPreferences] (no DataStore/coroutine dependency
     * needed for a single int), and plays the resulting song by handing the
     * running session a mediaId-only [MediaItem] - resolved into a real
     * playable item by [com.ozin.music.core.player.OzinLibrarySessionCallback.onAddMediaItems],
     * exactly the same mechanism Android Auto's browse tree already relies
     * on, rather than a second, parallel way of starting playback.
     *
     * Deliberately NOT a `RemoteViews`/`RemoteViewsService` collection
     * widget (which would need the whole provider rewritten from
     * `AppWidgetProvider` to a list-backed widget) - this is the smaller,
     * safer "cycle through recents on repeated taps" option called out as
     * acceptable in place of that rewrite.
     */
    fun cycleRecentAndPlay(context: Context, onDone: () -> Unit) {
        val appContext = context.applicationContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope.launch {
            try {
                val songRepository = EntryPointAccessors
                    .fromApplication(appContext, OzinMusicAppEntryPoint::class.java)
                    .songRepository()
                val recent = withContext(Dispatchers.IO) {
                    songRepository.recentlyPlayed(limit = RECENT_CYCLE_COUNT).first()
                }
                if (recent.isEmpty()) {
                    onDone()
                    return@launch
                }

                val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val nextIndex = (prefs.getInt(KEY_CYCLE_INDEX, -1) + 1) % recent.size
                prefs.edit().putInt(KEY_CYCLE_INDEX, nextIndex).apply()
                val song = recent[nextIndex]

                val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
                val future = MediaController.Builder(appContext, token).buildAsync()
                future.addListener({
                    try {
                        val controller = future.get()
                        val mediaItem = MediaItem.Builder()
                            .setMediaId(AutoMediaTree.songMediaId(song.id))
                            .build()
                        controller.setMediaItem(mediaItem)
                        controller.prepare()
                        controller.play()
                        controller.release()
                    } catch (_: Exception) {
                        // Session not reachable - same safe no-op as sendCommand above.
                    } finally {
                        onDone()
                    }
                }, MoreExecutors.directExecutor())
            } catch (_: Exception) {
                onDone()
            }
        }
    }
}
