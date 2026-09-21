package com.ozin.music.core.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ozin.music.core.data.model.SmartPlaylist
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartPlaylistDao {

    @Query("SELECT * FROM smart_playlists ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<SmartPlaylist>>

    @Query("SELECT COUNT(*) FROM smart_playlists")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: SmartPlaylist): Long

    @Update
    suspend fun update(playlist: SmartPlaylist)

    @Delete
    suspend fun delete(playlist: SmartPlaylist)
}
