package com.ozin.music.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ozin.music.core.data.model.ListeningEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningEventDao {

    @Query("SELECT * FROM listening_events ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<ListeningEvent>>

    /** Real DB-level date-range filter for the listening recap screen,
     * instead of pulling the whole event log into memory and filtering
     * there - matters once a long-lived library has thousands of events. */
    @Query("SELECT * FROM listening_events WHERE timestampMs BETWEEN :startMs AND :endMs ORDER BY timestampMs DESC")
    fun observeInRange(startMs: Long, endMs: Long): Flow<List<ListeningEvent>>

    @Insert
    suspend fun insert(event: ListeningEvent)
}
