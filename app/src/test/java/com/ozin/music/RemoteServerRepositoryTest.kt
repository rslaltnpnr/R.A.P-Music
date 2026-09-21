package com.ozin.music

import com.ozin.music.core.data.repository.RemoteServerRepository
import com.ozin.music.fakes.FakeRemoteCredentialStore
import com.ozin.music.fakes.FakeRemoteServerDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("UNCHECKED_CAST")
private fun <T> Flow<T>.snapshot(): T = (this as StateFlow<T>).value

class RemoteServerRepositoryTest {

    private lateinit var dao: FakeRemoteServerDao
    private lateinit var credentialStore: FakeRemoteCredentialStore
    private lateinit var repository: RemoteServerRepository

    @Before
    fun setUp() {
        dao = FakeRemoteServerDao()
        credentialStore = FakeRemoteCredentialStore()
        repository = RemoteServerRepository(dao, credentialStore)
    }

    @Test
    fun `addServer stores metadata in dao and password in credential store`() = runTest {
        val id = repository.addServer("Home NAS", "https://nas.local:443/dav", "user", "secret")

        val stored = repository.getById(id)
        assertEquals("Home NAS", stored?.name)
        assertEquals("https://nas.local:443/dav", stored?.address)
        assertEquals("user", stored?.username)
        assertEquals("secret", credentialStore.getPassword(id))
        assertEquals(1, repository.servers.snapshot().size)
    }

    @Test
    fun `getConfig combines room metadata with the stored password`() = runTest {
        val id = repository.addServer("Office", "https://office.example.com", "admin", "hunter2")

        val config = repository.getConfig(id)
        assertEquals("Office", config?.name)
        assertEquals("admin", config?.username)
        assertEquals("hunter2", config?.password)
    }

    @Test
    fun `getConfig is null when the password is missing`() = runTest {
        val id = repository.addServer("Temp", "https://temp.example.com", "u", "p")
        credentialStore.deletePassword(id)

        assertNull(repository.getConfig(id))
    }

    @Test
    fun `updateServer with null password keeps the existing password`() = runTest {
        val id = repository.addServer("NAS", "https://a.example.com", "u", "old-pass")

        repository.updateServer(id, "NAS 2", "https://b.example.com", "u2", password = null)

        val stored = repository.getById(id)
        assertEquals("NAS 2", stored?.name)
        assertEquals("https://b.example.com", stored?.address)
        assertEquals("old-pass", credentialStore.getPassword(id))
    }

    @Test
    fun `updateServer with a new password replaces it`() = runTest {
        val id = repository.addServer("NAS", "https://a.example.com", "u", "old-pass")

        repository.updateServer(id, "NAS", "https://a.example.com", "u", password = "new-pass")

        assertEquals("new-pass", credentialStore.getPassword(id))
    }

    @Test
    fun `deleteServer removes both the room row and the password`() = runTest {
        val id = repository.addServer("Gone", "https://gone.example.com", "u", "p")

        repository.deleteServer(id)

        assertNull(repository.getById(id))
        assertNull(credentialStore.getPassword(id))
        assertTrue(repository.servers.snapshot().isEmpty())
    }
}
