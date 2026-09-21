package com.ozin.music.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ozin.music.core.data.model.ProblemFile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProblemFileDao {

    @Query("SELECT * FROM problem_files ORDER BY detectedAt DESC")
    fun observeAll(): Flow<List<ProblemFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(problemFile: ProblemFile)

    @Query("DELETE FROM problem_files WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("SELECT * FROM problem_files WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): ProblemFile?
}
