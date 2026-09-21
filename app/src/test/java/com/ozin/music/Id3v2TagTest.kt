package com.ozin.music

import com.ozin.music.core.domain.Id3v2Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Id3v2TagTest {

    @Test
    fun `text frame round-trips through encode and decode`() {
        val frame = Id3v2Tag.textFrame("TIT2", "Bohemian Rhapsody")
        assertEquals("Bohemian Rhapsody", Id3v2Tag.decodeTextFrame(frame.data))
    }

    @Test
    fun `text frame round-trips non-latin text`() {
        val frame = Id3v2Tag.textFrame("TPE1", "Şarkıcı Örnek")
        assertEquals("Şarkıcı Örnek", Id3v2Tag.decodeTextFrame(frame.data))
    }

    @Test
    fun `empty string round-trips to empty string`() {
        val frame = Id3v2Tag.textFrame("TALB", "")
        assertEquals("", Id3v2Tag.decodeTextFrame(frame.data))
    }

    @Test
    fun `full tag encodes and decodes all frames back out`() {
        val frames = listOf(
            Id3v2Tag.textFrame("TIT2", "Title"),
            Id3v2Tag.textFrame("TPE1", "Artist"),
            Id3v2Tag.textFrame("TALB", "Album"),
            Id3v2Tag.textFrame("TYER", "2024"),
            Id3v2Tag.textFrame("TRCK", "3"),
        )
        val tagBytes = Id3v2Tag.encodeTag(frames)

        // Well-formed ID3 header.
        assertEquals('I', tagBytes[0].toInt().toChar())
        assertEquals('D', tagBytes[1].toInt().toChar())
        assertEquals('3', tagBytes[2].toInt().toChar())

        val decoded = Id3v2Tag.decodeTag(tagBytes)
        assertEquals(5, decoded.size)
        val byId = decoded.associateBy { it.id }
        assertEquals("Title", Id3v2Tag.decodeTextFrame(byId.getValue("TIT2").data))
        assertEquals("Artist", Id3v2Tag.decodeTextFrame(byId.getValue("TPE1").data))
        assertEquals("Album", Id3v2Tag.decodeTextFrame(byId.getValue("TALB").data))
        assertEquals("2024", Id3v2Tag.decodeTextFrame(byId.getValue("TYER").data))
        assertEquals("3", Id3v2Tag.decodeTextFrame(byId.getValue("TRCK").data))
    }

    @Test
    fun `comment frame round-trips its text`() {
        val frame = Id3v2Tag.commentFrame("A nice comment")
        assertEquals("A nice comment", Id3v2Tag.decodeCommentText(frame.data))
    }

    @Test
    fun `existingTagLength reports the tag size for a valid tag and zero otherwise`() {
        val tagBytes = Id3v2Tag.encodeTag(listOf(Id3v2Tag.textFrame("TIT2", "X")))
        assertEquals(tagBytes.size, Id3v2Tag.existingTagLength(tagBytes))
        assertEquals(0, Id3v2Tag.existingTagLength(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun `decodeTag on malformed bytes returns empty list rather than throwing`() {
        assertTrue(Id3v2Tag.decodeTag(byteArrayOf(1, 2, 3)).isEmpty())
        assertTrue(Id3v2Tag.decodeTag(ByteArray(0)).isEmpty())
    }

    @Test
    fun `picture frame preserves image bytes after the header fields`() {
        val imageBytes = byteArrayOf(0x11, 0x22, 0x33, 0x44)
        val frame = Id3v2Tag.pictureFrame("image/jpeg", imageBytes)
        assertTrue(frame.data.toList().containsAll(imageBytes.toList()))
    }
}
