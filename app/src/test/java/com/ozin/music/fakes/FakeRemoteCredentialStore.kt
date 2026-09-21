package com.ozin.music.fakes

import com.ozin.music.core.security.RemoteCredentialStore

/** In-memory fake standing in for EncryptedSharedPreferences in JVM unit
 * tests (the real Android Keystore cannot run outside a device/emulator). */
class FakeRemoteCredentialStore : RemoteCredentialStore {
    private val passwords = mutableMapOf<Long, String>()

    override fun savePassword(serverId: Long, password: String) {
        passwords[serverId] = password
    }

    override fun getPassword(serverId: Long): String? = passwords[serverId]

    override fun deletePassword(serverId: Long) {
        passwords.remove(serverId)
    }
}
