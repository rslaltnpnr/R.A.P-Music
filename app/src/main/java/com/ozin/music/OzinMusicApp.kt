package com.ozin.music

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.ozin.music.core.data.mediastore.MediaStoreChangeWatcher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OzinMusicAppEntryPoint {
    fun mediaStoreChangeWatcher(): MediaStoreChangeWatcher
}

@HiltAndroidApp
class OzinMusicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createPlaybackNotificationChannel()
        EntryPointAccessors.fromApplication(this, OzinMusicAppEntryPoint::class.java)
            .mediaStoreChangeWatcher()
            .start()
    }

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
