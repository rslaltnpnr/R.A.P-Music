package com.ozin.music

import com.ozin.music.core.remote.AudioFileTypes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFileTypesTest {

    @Test
    fun `recognizes every supported extension case-insensitively`() {
        listOf("mp3", "flac", "wav", "aac", "m4a", "ogg", "opus").forEach { ext ->
            assertTrue("$ext should be recognized", AudioFileTypes.isAudioFile("track.$ext"))
            assertTrue("upper-case .$ext should be recognized", AudioFileTypes.isAudioFile("track.${ext.uppercase()}"))
        }
    }

    @Test
    fun `rejects non-audio extensions`() {
        listOf("readme.txt", "cover.jpg", "playlist.m3u", "archive.zip").forEach { name ->
            assertFalse("$name should not be recognized as audio", AudioFileTypes.isAudioFile(name))
        }
    }

    @Test
    fun `rejects names with no extension`() {
        assertFalse(AudioFileTypes.isAudioFile("Rock"))
        assertFalse(AudioFileTypes.isAudioFile(""))
    }
}
