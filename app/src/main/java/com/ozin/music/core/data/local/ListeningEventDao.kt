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

    @Insert
    suspend fun insert(event: ListeningEvent)
}
