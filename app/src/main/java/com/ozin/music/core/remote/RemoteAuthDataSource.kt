package com.ozin.music.core.remote

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener

/**
 * [DataSource.Factory] that produces HTTP data sources for remote (WebDAV)
 * playback, injecting the right Basic-auth Authorization header per request
 * based on the target host, via [RemoteCredentialIndex]. Passed as the base
 * factory to [androidx.media3.datasource.DefaultDataSource.Factory], which
 * routes local file/content URIs elsewhere and only reaches this factory for
 * http(s) URIs — so local playback is unaffected.
 */
@OptIn(UnstableApi::class)
class RemoteAuthDataSourceFactory(
    private val credentialIndex: RemoteCredentialIndex,
) : DataSource.Factory {
    override fun createDataSource(): DataSource =
        RemoteAuthDataSource(DefaultHttpDataSource.Factory().createDataSource(), credentialIndex)
}

@OptIn(UnstableApi::class)
private class RemoteAuthDataSource(
    private val delegate: DefaultHttpDataSource,
    private val credentialIndex: RemoteCredentialIndex,
) : DataSource {

    override fun addTransferListener(transferListener: TransferListener) {
        delegate.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val header = credentialIndex.authHeaderFor(dataSpec.uri)
        if (header != null) {
            delegate.setRequestProperty("Authorization", header)
        } else {
            delegate.clearRequestProperty("Authorization")
        }
        return delegate.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = delegate.read(buffer, offset, length)

    override fun getUri(): Uri? = delegate.uri

    override fun getResponseHeaders(): Map<String, List<String>> = delegate.responseHeaders

    override fun close() {
        delegate.close()
    }
}
