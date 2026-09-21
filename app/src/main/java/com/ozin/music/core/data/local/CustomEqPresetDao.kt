package com.ozin.music.core.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ozin.music.core.data.model.CustomEqPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomEqPresetDao {

    @Query("SELECT * FROM custom_eq_presets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CustomEqPreset>>

    @Query("SELECT * FROM custom_eq_presets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CustomEqPreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: CustomEqPreset): Long

    @Update
    suspend fun update(preset: CustomEqPreset)

    @Delete
    suspend fun delete(preset: CustomEqPreset)

    @Query("DELETE FROM custom_eq_presets WHERE id = :id")
    suspend fun deleteById(id: Long)
}
