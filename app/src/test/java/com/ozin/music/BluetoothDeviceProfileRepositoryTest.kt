package com.ozin.music

import com.ozin.music.core.data.repository.BluetoothDeviceProfileRepository
import com.ozin.music.core.domain.EqPresetId
import com.ozin.music.fakes.FakeBluetoothDeviceProfileDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("UNCHECKED_CAST")
private fun <T> Flow<T>.snapshot(): T = (this as StateFlow<T>).value

class BluetoothDeviceProfileRepositoryTest {

    private lateinit var dao: FakeBluetoothDeviceProfileDao
    private lateinit var repository: BluetoothDeviceProfileRepository

    @Before
    fun setUp() {
        dao = FakeBluetoothDeviceProfileDao()
        repository = BluetoothDeviceProfileRepository(dao)
    }

    @Test
    fun `save creates a retrievable profile`() = runTest {
        repository.save("AA:BB:CC:DD:EE:FF", "Car Stereo", EqPresetId.CAR, 5, true)

        val stored = repository.getByAddress("AA:BB:CC:DD:EE:FF")
        assertEquals("Car Stereo", stored?.name)
        assertEquals(EqPresetId.CAR.name, stored?.eqPresetId)
        assertEquals(5, stored?.crossfadeSeconds)
        assertTrue(stored?.autoplayEnabled == true)
        assertEquals(1, repository.profiles.snapshot().size)
    }

    @Test
    fun `save overwrites an existing profile for the same address`() = runTest {
        repository.save("11:22:33:44:55:66", "Headphones", EqPresetId.HEADPHONES, 2, false)
        repository.save("11:22:33:44:55:66", "Headphones", EqPresetId.BASS, 8, true)

        val stored = repository.getByAddress("11:22:33:44:55:66")
        assertEquals(EqPresetId.BASS.name, stored?.eqPresetId)
        assertEquals(8, stored?.crossfadeSeconds)
        assertTrue(stored?.autoplayEnabled == true)
        assertEquals(1, repository.profiles.snapshot().size)
    }

    @Test
    fun `delete removes the profile`() = runTest {
        repository.save("00:00:00:00:00:01", "Speaker", EqPresetId.NORMAL, 0, false)
        repository.delete("00:00:00:00:00:01")

        assertNull(repository.getByAddress("00:00:00:00:00:01"))
        assertTrue(repository.profiles.snapshot().isEmpty())
    }

    @Test
    fun `getByAddress returns null for an unknown device`() = runTest {
        assertNull(repository.getByAddress("unknown"))
    }
}
