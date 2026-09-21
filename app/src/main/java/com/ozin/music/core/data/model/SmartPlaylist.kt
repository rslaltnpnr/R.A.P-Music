package com.ozin.music.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A named, rule-based playlist. [rulesEncoded] is a
 * [com.ozin.music.core.domain.SmartPlaylistRuleCodec]-encoded rule list; the
 * song list is never stored, it is re-evaluated live against the library.
 */
@Entity(tableName = "smart_playlists")
data class SmartPlaylist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val rulesEncoded: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isBuiltIn: Boolean = false,
)
