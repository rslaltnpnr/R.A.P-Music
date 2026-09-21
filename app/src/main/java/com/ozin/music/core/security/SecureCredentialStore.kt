package com.ozin.music.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Storage-mechanism-agnostic interface so callers (and tests) never depend
 * on EncryptedSharedPreferences/Keystore directly. */
interface RemoteCredentialStore {
    fun savePassword(serverId: Long, password: String)
    fun getPassword(serverId: Long): String?
    fun deletePassword(serverId: Long)
}

/**
 * Real [RemoteCredentialStore] backed by [EncryptedSharedPreferences] (AES256
 * via the Android Keystore). Never stores credentials in plaintext DataStore.
 * Not unit-testable outside a real Android environment (the Keystore is not
 * available on the JVM), so it is kept deliberately thin — all business logic
 * lives in [com.ozin.music.core.data.repository.RemoteServerRepository].
 */
@Singleton
class SecureCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : RemoteCredentialStore {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "ozin_remote_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun savePassword(serverId: Long, password: String) {
        prefs.edit().putString(key(serverId), password).apply()
    }

    override fun getPassword(serverId: Long): String? = prefs.getString(key(serverId), null)

    override fun deletePassword(serverId: Long) {
        prefs.edit().remove(key(serverId)).apply()
    }

    private fun key(serverId: Long) = "password_$serverId"
}
