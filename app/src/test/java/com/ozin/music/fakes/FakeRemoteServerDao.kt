package com.ozin.music.fakes

import com.ozin.music.core.data.local.RemoteServerDao
import com.ozin.music.core.data.model.RemoteServer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory fake standing in for Room in JVM unit tests. */
class FakeRemoteServerDao : RemoteServerDao {

    private val servers = mutableMapOf<Long, RemoteServer>()
    private var nextId = 1L
    private val _all = MutableStateFlow<List<RemoteServer>>(emptyList())

    private fun emit() {
        _all.value = servers.values.sortedByDescending { it.createdAt }
    }

    override fun observeAll(): Flow<List<RemoteServer>> = _all.asStateFlow()

    override suspend fun getById(id: Long): RemoteServer? = servers[id]

    override suspend fun insert(server: RemoteServer): Long {
        val id = if (server.id != 0L) server.id else nextId++
        servers[id] = server.copy(id = id)
        emit()
        return id
    }

    override suspend fun update(server: RemoteServer) {
        servers[server.id] = server
        emit()
    }

    override suspend fun deleteById(id: Long) {
        servers.remove(id)
        emit()
    }
}
