package com.ozin.music

import com.ozin.music.core.data.model.decodedBands
import com.ozin.music.core.data.repository.CustomEqPresetRepository
import com.ozin.music.fakes.FakeCustomEqPresetDao
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

class CustomEqPresetRepositoryTest {

    private lateinit var dao: FakeCustomEqPresetDao
    private lateinit var repository: CustomEqPresetRepository

    @Before
    fun setUp() {
        dao = FakeCustomEqPresetDao()
        repository = CustomEqPresetRepository(dao)
    }

    @Test
    fun `save stores the preset with its encoded bands`() = runTest {
        val id = repository.save(
            name = "My preset",
            bands = listOf(100, 200, -100, 0, 50, 50, 0, -50, -100, 300),
            preampMb = 200,
            bassBoostStrength = 300,
            virtualizerStrength = 400,
            loudnessGainMb = 500,
        )

        val stored = repository.getById(id)
        assertEquals("My preset", stored?.name)
        assertEquals(listOf(100, 200, -100, 0, 50, 50, 0, -50, -100, 300), stored?.decodedBands())
        assertEquals(200, stored?.preampMb)
        assertEquals(300, stored?.bassBoostStrength)
        assertEquals(400, stored?.virtualizerStrength)
        assertEquals(500, stored?.loudnessGainMb)
        assertEquals(1, repository.presets.snapshot().size)
    }

    @Test
    fun `rename updates the stored name`() = runTest {
        val id = repository.save("Original", emptyList(), 0, 0, 0, 0)
        val stored = repository.getById(id)!!

        repository.rename(stored, "Renamed")

        assertEquals("Renamed", repository.getById(id)?.name)
    }

    @Test
    fun `rename with a blank name is a no-op`() = runTest {
        val id = repository.save("Original", emptyList(), 0, 0, 0, 0)
        val stored = repository.getById(id)!!

        repository.rename(stored, "   ")

        assertEquals("Original", repository.getById(id)?.name)
    }

    @Test
    fun `delete removes the preset`() = runTest {
        val id = repository.save("Gone", emptyList(), 0, 0, 0, 0)
        val stored = repository.getById(id)!!

        repository.delete(stored)

        assertNull(repository.getById(id))
        assertTrue(repository.presets.snapshot().isEmpty())
    }
}
