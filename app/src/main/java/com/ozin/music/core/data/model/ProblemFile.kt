package com.ozin.music.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A song row (or a path MediaStore never even resolved to a row) that failed
 * to play or to be read, tracked so the user can review/ignore/remove/rescan
 * it instead of it silently vanishing from the library.
 */
@Entity(tableName = "problem_files")
data class ProblemFile(
    @PrimaryKey val path: String,
    val songId: Long?,
    val title: String,
    val reason: String,
    val detectedAt: Long,
)
