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
    /** Comma-separated [com.ozin.music.core.domain.Mood] names computed by
     * [com.ozin.music.core.domain.MoodClassifier] (encoded via
     * [com.ozin.music.core.domain.MoodTagCodec]). Empty until the user runs
     * "Compute mood tags" in Settings, or after a rescan if this field is
     * populated by that pass; simple heuristic labels, not machine learning. */
    val moodTags: String = "",
    /** Real per-track loudness estimate in dB (relative, not absolute LUFS
     * despite the field name kept for API continuity), computed by
     * [com.ozin.music.core.domain.LoudnessAnalyzer]. Null until analyzed -
     * see that analyzer's kdoc for exactly what algorithm this value comes
     * from (a simplified RMS-based estimate, not full EBU R128). */
    val loudnessLufs: Float? = null,
)
