package com.ozin.music.core.remote

import android.net.Uri

/**
 * Opaque per-instance connection info for a remote music source. Deliberately
 * generic (no WebDAV-specific fields) so a future SMB implementation can
 * reuse the same shape, or a future variant can add fields without breaking
 * [RemoteMusicSource] callers.
 */
data class RemoteServerConfig(
    val id: Long,
    val name: String,
    val address: String,
    val username: String,
    val password: String,
)

/** A single entry inside a remote directory listing. */
sealed interface RemoteEntry {
    val name: String
    /** Absolute path on the remote server, e.g. "/music/Rock". */
    val path: String
}

data class RemoteFolder(
    override val name: String,
    override val path: String,
) : RemoteEntry

data class RemoteAudioFile(
    override val name: String,
    override val path: String,
    val sizeBytes: Long?,
) : RemoteEntry

sealed interface RemoteError {
    data object AuthFailed : RemoteError
    data class ServerError(val code: Int) : RemoteError
    data class ConnectionFailed(val message: String) : RemoteError
    data object MalformedResponse : RemoteError
}

sealed interface RemoteBrowseResult {
    data class Success(val entries: List<RemoteEntry>) : RemoteBrowseResult
    data class Error(val reason: RemoteError) : RemoteBrowseResult
}

sealed interface RemoteConnectionResult {
    data object Success : RemoteConnectionResult
    data class Failure(val reason: RemoteError) : RemoteConnectionResult
}

/**
 * A protocol-agnostic remote music source. Implemented by WebDAV today; a
 * future SMB implementation can implement this same interface without
 * leaking WebDAV-specific types (XML, PROPFIND, etc.) into callers.
 */
interface RemoteMusicSource {
    /** Real round-trip connectivity/auth check against the server root. */
    suspend fun testConnection(config: RemoteServerConfig): RemoteConnectionResult

    /** Lists the folders/audio files directly under [path] ("/" for root). */
    suspend fun listEntries(config: RemoteServerConfig, path: String): RemoteBrowseResult

    /** Builds the streamable [Uri] for [file]. Does not itself carry auth;
     * playback auth is applied per-request by the player's data source. */
    fun streamUri(config: RemoteServerConfig, file: RemoteAudioFile): Uri
}
