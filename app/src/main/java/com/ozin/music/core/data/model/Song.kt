package com.ozin.music.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Source-of-truth row for a single audio track, populated from MediaStore and
 * enriched with app-local state (favorite, play count, last played).
 */
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val path: String,
    val sizeBytes: Long,
    val year: Int,
    val trackNumber: Int,
    val dateAdded: Long,
    val genre: String = "Unknown",
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    /** 0 = unrated, 1-5 stars otherwise. */
    val rating: Int = 0,
)
