package com.ozin.music

import com.ozin.music.core.domain.dj.DjSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DjSyncTest {
    @Test
    fun `matching bpm gives speed 1`() {
        val result = DjSync.computeSyncSpeed(128f, 128f)
        assertTrue(result is DjSync.Result.NewSpeed)
        assertEquals(1f, (result as DjSync.Result.NewSpeed).speed, 0.001f)
    }

    @Test
    fun `half speed target doubles otherwise`() {
        val result = DjSync.computeSyncSpeed(120f, 140f)
        assertTrue(result is DjSync.Result.NewSpeed)
        assertEquals(140f / 120f, (result as DjSync.Result.NewSpeed).speed, 0.001f)
    }

    @Test
    fun `unknown bpm is a real no-op`() {
        assertEquals(DjSync.Result.BpmUnknown, DjSync.computeSyncSpeed(null, 128f))
        assertEquals(DjSync.Result.BpmUnknown, DjSync.computeSyncSpeed(128f, null))
        assertEquals(DjSync.Result.BpmUnknown, DjSync.computeSyncSpeed(0f, 128f))
    }

    @Test
    fun `extreme ratio is clamped to safe playback range`() {
        val result = DjSync.computeSyncSpeed(60f, 400f) as DjSync.Result.NewSpeed
        assertTrue(result.speed <= 2.0f)
    }
}
