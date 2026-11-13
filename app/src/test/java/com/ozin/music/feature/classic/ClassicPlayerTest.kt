package com.ozin.music.feature.classic

import com.ozin.music.core.domain.EqPresetId
import com.ozin.music.core.domain.EqPresets
import com.ozin.music.core.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassicPlayerTest {
    @Test fun timerFormatsLongTracksAndClampsNegativePositions() {
        assertEquals("0:00", classicTime(-1))
        assertEquals("4:03", classicTime(243_999))
        assertEquals("120:00", classicTime(7_200_000))
    }

    @Test fun customCurvePreservesAllTenValues() {
        val levels = List(10) { it * 100 - 500 }
        assertEquals(levels, classicEqCurve(AppSettings(eqPreset = EqPresetId.CUSTOM, eqCustomBands = levels)))
    }

    @Test fun everyClassicBandContributesOnFiveBandHardware() {
        for (index in 0 until 10) {
            val source = IntArray(10) { if (it == index) 1000 else 0 }
            val result = EqPresets.compressCustomCurve(source, 5, -1200..1200)
            assertEquals(500, result[index / 2])
            assertEquals(500, result.sum())
        }
    }

    @Test fun legacyDeviceCurveExpandsWithoutChangingItsEndpoints() {
        val bands = classicEqCurve(AppSettings(eqPreset = EqPresetId.CUSTOM, eqCustomBands = listOf(-300, 0, 600)))
        assertEquals(10, bands.size)
        assertEquals(-300, bands.first())
        assertEquals(600, bands.last())
    }
}
