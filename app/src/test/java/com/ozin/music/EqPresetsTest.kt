package com.ozin.music

import com.ozin.music.core.domain.EqPresetId
import com.ozin.music.core.domain.EqPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqPresetsTest {

    @Test
    fun `normal preset is flat`() {
        val table = EqPresets.referenceTables.getValue(EqPresetId.NORMAL)
        assertTrue(table.all { it == 0 })
    }

    @Test
    fun `mapping to fewer bands stays within device range`() {
        val reference = EqPresets.referenceTables.getValue(EqPresetId.DEEP_BASS)
        val mapped = EqPresets.mapToDeviceBands(reference, deviceBandCount = 5, levelRange = -1500..1500)
        assertEquals(5, mapped.size)
        mapped.forEach { assertTrue(it in -1500..1500) }
    }

    @Test
    fun `mapping to same band count preserves endpoints`() {
        val reference = EqPresets.referenceTables.getValue(EqPresetId.ROCK)
        val mapped = EqPresets.mapToDeviceBands(reference, deviceBandCount = 10, levelRange = -1500..1500)
        assertEquals(reference[0], mapped[0])
        assertEquals(reference[9], mapped[9])
    }

    @Test
    fun `mapping to a single band averages the reference`() {
        val reference = intArrayOf(100, 100, 100, 100, 100, 100, 100, 100, 100, 100)
        val mapped = EqPresets.mapToDeviceBands(reference, deviceBandCount = 1, levelRange = -1500..1500)
        assertEquals(1, mapped.size)
        assertEquals(100, mapped[0])
    }

    @Test
    fun `preamp shifts and clamps every band`() {
        val bands = intArrayOf(1400, 0, -1400)
        val result = EqPresets.applyPreamp(bands, preampMb = 300, levelRange = -1500..1500)
        assertEquals(1500, result[0]) // clamped
        assertEquals(300, result[1])
        assertEquals(-1100, result[2])
    }
}
