package com.ozin.music.core.remote

import com.ozin.music.core.remote.webdav.WebDavMusicSource
import com.ozin.music.core.security.RemoteCredentialStore
import com.ozin.music.core.security.SecureCredentialStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteModule {

    @Binds
    abstract fun bindRemoteMusicSource(impl: WebDavMusicSource): RemoteMusicSource

    @Binds
    abstract fun bindRemoteCredentialStore(impl: SecureCredentialStore): RemoteCredentialStore

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()
    }
}
