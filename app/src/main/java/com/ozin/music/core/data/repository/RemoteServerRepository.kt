package com.ozin.music.core.data.repository

import com.ozin.music.core.data.local.RemoteServerDao
import com.ozin.music.core.data.model.RemoteServer
import com.ozin.music.core.remote.RemoteServerConfig
import com.ozin.music.core.security.RemoteCredentialStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CRUD access to configured remote (WebDAV) servers. Server metadata (name,
 * address, username) lives in Room so it can be observed as a [Flow]; the
 * password itself never touches Room and instead goes through
 * [RemoteCredentialStore], keyed by the Room row id.
 */
@Singleton
class RemoteServerRepository @Inject constructor(
    private val dao: RemoteServerDao,
    private val credentialStore: RemoteCredentialStore,
) {
    val servers: Flow<List<RemoteServer>> = dao.observeAll()

    suspend fun getById(id: Long): RemoteServer? = dao.getById(id)

    fun getPassword(id: Long): String? = credentialStore.getPassword(id)

    suspend fun addServer(name: String, address: String, username: String, password: String): Long {
        val id = dao.insert(
            RemoteServer(name = name, address = address, username = username, createdAt = System.currentTimeMillis())
        )
        credentialStore.savePassword(id, password)
        return id
    }

    /** [password] null keeps the existing stored password unchanged. */
    suspend fun updateServer(id: Long, name: String, address: String, username: String, password: String?) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(name = name, address = address, username = username))
        if (password != null) {
            credentialStore.savePassword(id, password)
        }
    }

    suspend fun deleteServer(id: Long) {
        dao.deleteById(id)
        credentialStore.deletePassword(id)
    }

    /** Combines the Room row with its stored password into a ready-to-use
     * [RemoteServerConfig], or null if either is missing. */
    suspend fun getConfig(id: Long): RemoteServerConfig? {
        val server = dao.getById(id) ?: return null
        val password = credentialStore.getPassword(id) ?: return null
        return RemoteServerConfig(
            id = server.id,
            name = server.name,
            address = server.address,
            username = server.username,
            password = password,
        )
    }
}
