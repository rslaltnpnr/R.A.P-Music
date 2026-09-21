package com.ozin.music

import com.ozin.music.core.domain.AbRepeat
import com.ozin.music.core.domain.AbRepeatState
import com.ozin.music.core.domain.PlaybackSpeed
import com.ozin.music.core.domain.SmartCrossfade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackControlsTest {

    @Test
    fun `speed clamps to supported range`() {
        assertEquals(0.5f, PlaybackSpeed.clamp(0.1f))
        assertEquals(2f, PlaybackSpeed.clamp(5f))
        assertEquals(1.25f, PlaybackSpeed.clamp(1.25f))
    }

    @Test
    fun `ab repeat does not loop when disabled`() {
        val state = AbRepeatState(pointAMs = 1000, pointBMs = 5000, enabled = false)
        assertFalse(AbRepeat.shouldLoop(state, positionMs = 5000))
    }

    @Test
    fun `ab repeat does not loop with invalid points`() {
        val state = AbRepeatState(pointAMs = 5000, pointBMs = 1000, enabled = true)
        assertFalse(AbRepeat.shouldLoop(state, positionMs = 6000))
    }

    @Test
    fun `ab repeat loops once position reaches point b`() {
        val state = AbRepeatState(pointAMs = 1000, pointBMs = 5000, enabled = true)
        assertFalse(AbRepeat.shouldLoop(state, positionMs = 4999))
        assertTrue(AbRepeat.shouldLoop(state, positionMs = 5000))
        assertTrue(AbRepeat.shouldLoop(state, positionMs = 5001))
    }

    @Test
    fun `smart crossfade skips same album`() {
        assertFalse(
            SmartCrossfade.shouldCrossfade(
                crossfadeEnabled = true,
                smartCrossfadeEnabled = true,
                currentAlbum = "Album A",
                nextAlbum = "Album A",
            )
        )
    }

    @Test
    fun `smart crossfade allows different albums`() {
        assertTrue(
            SmartCrossfade.shouldCrossfade(
                crossfadeEnabled = true,
                smartCrossfadeEnabled = true,
                currentAlbum = "Album A",
                nextAlbum = "Album B",
            )
        )
    }

    @Test
    fun `smart crossfade off behaves like plain crossfade`() {
        assertTrue(
            SmartCrossfade.shouldCrossfade(
                crossfadeEnabled = true,
                smartCrossfadeEnabled = false,
                currentAlbum = "Album A",
                nextAlbum = "Album A",
            )
        )
    }

    @Test
    fun `crossfade disabled never crossfades`() {
        assertFalse(
            SmartCrossfade.shouldCrossfade(
                crossfadeEnabled = false,
                smartCrossfadeEnabled = true,
                currentAlbum = "Album A",
                nextAlbum = "Album B",
            )
        )
    }
}
