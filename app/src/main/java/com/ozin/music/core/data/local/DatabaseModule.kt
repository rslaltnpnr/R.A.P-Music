package com.ozin.music.core.data.local

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OzinDatabase =
        Room.databaseBuilder(context, OzinDatabase::class.java, "ozin_music.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSongDao(database: OzinDatabase): SongDao = database.songDao()

    @Provides
    fun providePlaylistDao(database: OzinDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideProblemFileDao(database: OzinDatabase): ProblemFileDao = database.problemFileDao()

    @Provides
    fun provideListeningEventDao(database: OzinDatabase): ListeningEventDao = database.listeningEventDao()

    @Provides
    fun provideSmartPlaylistDao(database: OzinDatabase): SmartPlaylistDao = database.smartPlaylistDao()

    @Provides
    fun provideBluetoothDeviceProfileDao(database: OzinDatabase): BluetoothDeviceProfileDao =
        database.bluetoothDeviceProfileDao()

    @Provides
    fun provideRemoteServerDao(database: OzinDatabase): RemoteServerDao = database.remoteServerDao()
}
