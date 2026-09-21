package com.ozin.music.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ozin.music.core.data.model.RemoteServer
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteServerDao {

    @Query("SELECT * FROM remote_servers ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RemoteServer>>

    @Query("SELECT * FROM remote_servers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RemoteServer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: RemoteServer): Long

    @Update
    suspend fun update(server: RemoteServer)

    @Query("DELETE FROM remote_servers WHERE id = :id")
    suspend fun deleteById(id: Long)
}
