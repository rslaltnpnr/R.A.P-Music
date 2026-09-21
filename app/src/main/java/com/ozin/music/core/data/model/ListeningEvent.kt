package com.ozin.music.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One real listening session: a song actually playing for [durationMs]
 * starting at [timestampMs]. This is the source of truth the statistics
 * dashboard derives all of its time-range aggregates and the hourly
 * histogram from.
 */
@Entity(tableName = "listening_events")
data class ListeningEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val timestampMs: Long,
    val durationMs: Long,
)
