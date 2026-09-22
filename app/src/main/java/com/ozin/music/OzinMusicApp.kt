package com.ozin.music

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.ozin.music.core.data.mediastore.MediaStoreChangeWatcher
import com.ozin.music.core.data.repository.SongRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OzinMusicAppEntryPoint {
    fun mediaStoreChangeWatcher(): MediaStoreChangeWatcher

    /** Used by [com.ozin.music.core.player.widget.WidgetCommands] (a plain
     * object, not itself Hilt-injected) to read the real recently-played
     * list for the large widget's "cycle recent" button (item 3). */
    fun songRepository(): SongRepository
}

@HiltAndroidApp
class OzinMusicApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        createPlaybackNotificationChannel()
        EntryPointAccessors.fromApplication(this, OzinMusicAppEntryPoint::class.java)
            .mediaStoreChangeWatcher()
            .start()
    }

    // Album-art thumbnails are small but numerous (every song/album/playlist
    // row shows one); a bounded memory + disk cache keeps scrolling smooth
    // without letting Coil's default unbounded-ish caches grow unchecked.
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.20)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02)
                .build()
        }
        .build()

    private fun createPlaybackNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            PLAYBACK_CHANNEL_ID,
            getString(R.string.playback_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val PLAYBACK_CHANNEL_ID = "ozin_playback_channel"
    }
}
