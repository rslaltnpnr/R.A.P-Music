package com.ozin.music.core.remote

import android.net.Uri
import android.util.Base64
import com.ozin.music.core.data.repository.RemoteServerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maintains an in-memory, host -> "Basic <base64>" Authorization header map
 * derived from configured WebDAV servers, kept in sync with Room + the
 * secure credential store. Consulted synchronously by
 * [RemoteAuthDataSourceFactory] on the playback thread, so no Room/Keystore
 * access happens there directly.
 */
@Singleton
class RemoteCredentialIndex @Inject constructor(
    private val remoteServerRepository: RemoteServerRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var hostToAuthHeader: Map<String, String> = emptyMap()

    init {
        scope.launch {
            remoteServerRepository.servers.collect { servers ->
                hostToAuthHeader = servers.mapNotNull { server ->
                    val host = runCatching { Uri.parse(server.address).host }.getOrNull()
                        ?: return@mapNotNull null
                    val password = remoteServerRepository.getPassword(server.id) ?: return@mapNotNull null
                    val raw = "${server.username}:$password"
                    val encoded = Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    host to "Basic $encoded"
                }.toMap()
            }
        }
    }

    fun authHeaderFor(uri: Uri): String? = uri.host?.let { hostToAuthHeader[it] }
}
