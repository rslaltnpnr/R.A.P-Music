package com.ozin.music.core.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.ozin.music.core.remote.RemoteAuthDataSourceFactory
import com.ozin.music.core.remote.RemoteCredentialIndex
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    /**
     * Base HTTP [DataSource.Factory] used for remote (WebDAV) streaming; it
     * injects a per-request Basic-auth header resolved by host via
     * [RemoteCredentialIndex]. Handed to [DefaultDataSource.Factory] below,
     * which routes local file/content URIs through its own built-in
     * FileDataSource/ContentDataSource and only reaches this factory for
     * http(s) URIs, so local playback is unaffected.
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideHttpDataSourceFactory(credentialIndex: RemoteCredentialIndex): DataSource.Factory =
        RemoteAuthDataSourceFactory(credentialIndex)

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        httpDataSourceFactory: DataSource.Factory,
    ): ExoPlayer {
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }
}
