package com.ozin.music.core.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ozin.music.core.data.model.BluetoothDeviceProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface BluetoothDeviceProfileDao {

    @Query("SELECT * FROM bluetooth_device_profiles ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<BluetoothDeviceProfile>>

    @Query("SELECT * FROM bluetooth_device_profiles WHERE address = :address LIMIT 1")
    suspend fun getByAddress(address: String): BluetoothDeviceProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: BluetoothDeviceProfile)

    @Delete
    suspend fun delete(profile: BluetoothDeviceProfile)

    @Query("DELETE FROM bluetooth_device_profiles WHERE address = :address")
    suspend fun deleteByAddress(address: String)
}
