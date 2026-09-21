package com.ozin.music.core.remote.webdav

import android.net.Uri
import com.ozin.music.core.remote.RemoteAudioFile
import com.ozin.music.core.remote.RemoteBrowseResult
import com.ozin.music.core.remote.RemoteConnectionResult
import com.ozin.music.core.remote.RemoteError
import com.ozin.music.core.remote.RemoteMusicSource
import com.ozin.music.core.remote.RemoteServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val PROPFIND_BODY = """<?xml version="1.0" encoding="utf-8" ?>
<D:propfind xmlns:D="DAV:">
  <D:prop>
    <D:displayname/>
    <D:resourcetype/>
    <D:getcontentlength/>
  </D:prop>
</D:propfind>"""

/**
 * Real WebDAV implementation of [RemoteMusicSource]: PROPFIND for directory
 * listings (Depth: 1) with Basic auth, and plain HTTPS/HTTP GET URLs for
 * streaming (auth for the GET itself is applied by the player's own data
 * source, see [com.ozin.music.core.remote.RemoteAuthDataSourceFactory]).
 * Never throws: connection/auth/parsing failures are surfaced as a real
 * [RemoteError] result.
 */
@Singleton
class WebDavMusicSource @Inject constructor(
    private val client: OkHttpClient,
) : RemoteMusicSource {

    override suspend fun testConnection(config: RemoteServerConfig): RemoteConnectionResult =
        when (val result = propfind(config, "/")) {
            is RemoteBrowseResult.Success -> RemoteConnectionResult.Success
            is RemoteBrowseResult.Error -> RemoteConnectionResult.Failure(result.reason)
        }

    override suspend fun listEntries(config: RemoteServerConfig, path: String): RemoteBrowseResult =
        propfind(config, path)

    override fun streamUri(config: RemoteServerConfig, file: RemoteAudioFile): Uri {
        val base = config.address.trimEnd('/')
        val path = if (file.path.startsWith("/")) file.path else "/${file.path}"
        return Uri.parse(base + path)
    }

    private suspend fun propfind(config: RemoteServerConfig, path: String): RemoteBrowseResult =
        withContext(Dispatchers.IO) {
            try {
                val base = config.address.trimEnd('/')
                val normalizedPath = if (path.startsWith("/")) path else "/$path"
                val url = base + normalizedPath
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", Credentials.basic(config.username, config.password))
                    .header("Depth", "1")
                    .method("PROPFIND", PROPFIND_BODY.toRequestBody("application/xml; charset=utf-8".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext RemoteBrowseResult.Error(
                            when (response.code) {
                                401, 403 -> RemoteError.AuthFailed
                                else -> RemoteError.ServerError(response.code)
                            }
                        )
                    }
                    val xml = response.body?.string()
                        ?: return@withContext RemoteBrowseResult.Error(RemoteError.MalformedResponse)
                    val entries = runCatching { WebDavXmlParser.parseMultistatus(xml, normalizedPath) }
                        .getOrElse { return@withContext RemoteBrowseResult.Error(RemoteError.MalformedResponse) }
                    RemoteBrowseResult.Success(entries)
                }
            } catch (e: IOException) {
                RemoteBrowseResult.Error(RemoteError.ConnectionFailed(e.message ?: "Connection failed"))
            } catch (e: Exception) {
                RemoteBrowseResult.Error(RemoteError.MalformedResponse)
            }
        }
}
