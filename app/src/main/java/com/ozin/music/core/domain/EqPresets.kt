package com.ozin.music.core.domain

/**
 * Pure EQ preset logic: a reference 10-band gain table (in millibel, roughly
 * matching typical 10-band consumer EQ center frequencies from 31Hz to
 * 16kHz) per preset, plus a mapper that scales the 10-band reference down (or
 * up) to however many bands the device's real [android.media.audiofx.Equalizer]
 * actually exposes. Kept free of any Android/audiofx dependency so it is
 * directly unit-testable.
 */
enum class EqPresetId {
    NORMAL, BASS, DEEP_BASS, ROCK, POP, RAP, HIP_HOP, ELECTRONIC, VOCAL, CLASSICAL, CAR, HEADPHONES, CUSTOM
}

object EqPresets {

    /** Millibel gain per band, index 0 = lowest frequency (31Hz) .. 9 = highest (16kHz). */
    val referenceTables: Map<EqPresetId, IntArray> = mapOf(
        EqPresetId.NORMAL to intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        EqPresetId.BASS to intArrayOf(600, 500, 400, 200, 0, 0, 0, 0, 0, 0),
        EqPresetId.DEEP_BASS to intArrayOf(900, 800, 600, 300, 100, 0, 0, 0, 0, 0),
        EqPresetId.ROCK to intArrayOf(400, 300, 100, -100, -200, -100, 100, 200, 300, 400),
        EqPresetId.POP to intArrayOf(-100, 0, 200, 300, 300, 100, 0, -100, -100, -200),
        EqPresetId.RAP to intArrayOf(500, 400, 200, 100, -100, -100, 0, 100, 200, 200),
        EqPresetId.HIP_HOP to intArrayOf(550, 450, 250, 50, -50, -50, 50, 150, 250, 300),
        EqPresetId.ELECTRONIC to intArrayOf(400, 350, 100, 0, -150, -150, 0, 100, 300, 400),
        EqPresetId.VOCAL to intArrayOf(-200, -150, -50, 200, 400, 400, 300, 100, 0, -100),
        EqPresetId.CLASSICAL to intArrayOf(300, 250, 150, 100, -50, -50, 0, 150, 250, 300),
        EqPresetId.CAR to intArrayOf(400, 350, 100, 0, 100, 200, 300, 400, 400, 300),
        EqPresetId.HEADPHONES to intArrayOf(300, 200, 50, 0, 0, 50, 100, 200, 300, 350),
        EqPresetId.CUSTOM to intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    )

    /**
     * Maps the 10-band [reference] table onto [deviceBandCount] bands by
     * proportionally sampling the reference index for each device band, then
     * clamps every resulting gain into [levelRange] (min, max) as the real
     * hardware EQ reports it.
     */
    fun mapToDeviceBands(reference: IntArray, deviceBandCount: Int, levelRange: IntRange): IntArray {
        if (deviceBandCount <= 0) return IntArray(0)
        if (deviceBandCount == 1) {
            return intArrayOf(reference.average().toInt().coerceIn(levelRange.first, levelRange.last))
        }
        return IntArray(deviceBandCount) { i ->
            val refIndex = ((i.toFloat() / (deviceBandCount - 1)) * (reference.size - 1)).let { Math.round(it) }
                .coerceIn(0, reference.size - 1)
            reference[refIndex].coerceIn(levelRange.first, levelRange.last)
        }
    }

    /** Applies a preamp offset (millibel) to every band, clamped to range. */
    fun applyPreamp(bands: IntArray, preampMb: Int, levelRange: IntRange): IntArray =
        IntArray(bands.size) { (bands[it] + preampMb).coerceIn(levelRange.first, levelRange.last) }
}
