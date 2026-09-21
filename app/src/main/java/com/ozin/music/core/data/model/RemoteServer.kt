package com.ozin.music.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room-persisted metadata for a configured WebDAV server. The password is
 * deliberately NOT stored here — it lives in
 * [com.ozin.music.core.security.SecureCredentialStore], keyed by [id].
 */
@Entity(tableName = "remote_servers")
data class RemoteServer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val address: String,
    val username: String,
    val createdAt: Long,
)
