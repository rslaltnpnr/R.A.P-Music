package com.ozin.music.core.domain

import java.io.ByteArrayOutputStream

/** One decoded ID3v2 frame: a 4-character frame id and its raw payload bytes. */
data class Id3Frame(val id: String, val data: ByteArray)

/**
 * A minimal, dependency-free ID3v2.3 tag reader/writer covering the text
 * information frames this app's metadata editor needs (TIT2/TPE1/TALB/
 * TPE2/TCON/TYER/TRCK/TPOS), plus COMM (comment) and APIC (embedded
 * picture). Deliberately scoped to MP3 only — FLAC/OGG use a different
 * comment format (Vorbis comments) that this class does not write; for
 * those formats the app updates MediaStore's own columns only.
 *
 * This class only ever operates on in-memory byte arrays: building a full
 * tag, or reading one back out of a byte array. File I/O (reading the
 * existing tag off disk, splicing the new one in, writing it back) lives in
 * the Android-specific `Mp3TagWriter` so this class stays plain, pure
 * Kotlin and unit-testable on the JVM.
 */
object Id3v2Tag {

    private const val HEADER_SIZE = 10

    fun textFrame(id: String, value: String): Id3Frame {
        // Encoding byte 0x01 = UTF-16 with BOM, safe for any language/script.
        val bytes = ByteArrayOutputStream()
        bytes.write(0x01)
        val utf16 = value.toByteArray(Charsets.UTF_16LE)
        bytes.write(0xFF); bytes.write(0xFE) // BOM (little-endian)
        bytes.write(utf16)
        return Id3Frame(id, bytes.toByteArray())
    }

    fun commentFrame(text: String, language: String = "eng"): Id3Frame {
        val bytes = ByteArrayOutputStream()
        bytes.write(0x01)
        val lang = language.padEnd(3, 'X').take(3).toByteArray(Charsets.US_ASCII)
        bytes.write(lang)
        bytes.write(0xFF); bytes.write(0xFE) // empty short-description, BOM + null terminator
        bytes.write(0x00); bytes.write(0x00)
        bytes.write(0xFF); bytes.write(0xFE)
        bytes.write(text.toByteArray(Charsets.UTF_16LE))
        return Id3Frame("COMM", bytes.toByteArray())
    }

    fun pictureFrame(mimeType: String, imageData: ByteArray, pictureType: Int = 3): Id3Frame {
        val bytes = ByteArrayOutputStream()
        bytes.write(0x00) // ISO-8859-1 for the ASCII-only header fields
        bytes.write(mimeType.toByteArray(Charsets.US_ASCII))
        bytes.write(0x00)
        bytes.write(pictureType)
        bytes.write(0x00) // empty description, terminated
        bytes.write(imageData)
        return Id3Frame("APIC", bytes.toByteArray())
    }

    /** Decodes the text back out of a text-information or comment frame's payload. */
    fun decodeTextFrame(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val encoding = data[0].toInt() and 0xFF
        val payload = data.copyOfRange(1, data.size)
        return when (encoding) {
            0x00 -> String(payload, Charsets.ISO_8859_1).trimEnd('\u0000')
            0x01 -> decodeUtf16WithBom(payload)
            0x02 -> String(payload, Charsets.UTF_16BE).trimEnd('\u0000')
            0x03 -> String(payload, Charsets.UTF_8).trimEnd('\u0000')
            else -> String(payload, Charsets.ISO_8859_1).trimEnd('\u0000')
        }
    }

    private fun decodeUtf16WithBom(payload: ByteArray): String {
        if (payload.size < 2) return ""
        val hasBom = (payload[0] == 0xFF.toByte() && payload[1] == 0xFE.toByte()) ||
            (payload[0] == 0xFE.toByte() && payload[1] == 0xFF.toByte())
        val body = if (hasBom) payload.copyOfRange(2, payload.size) else payload
        val charset = if (payload.size >= 2 && payload[0] == 0xFE.toByte() && payload[1] == 0xFF.toByte()) {
            Charsets.UTF_16BE
        } else {
            Charsets.UTF_16LE
        }
        return String(body, charset).trimEnd('\u0000')
    }

    fun decodeCommentText(data: ByteArray): String {
        if (data.size < 5) return ""
        val encoding = data[0].toInt() and 0xFF
        // Skip encoding byte(1) + language(3) + short description + its terminator.
        var index = 4
        val descTerminatorLen = if (encoding == 0x01 || encoding == 0x02) 2 else 1
        // Find the description terminator, then skip it.
        index = findTerminator(data, index, descTerminatorLen) + descTerminatorLen
        if (index > data.size) return ""
        val textBytes = data.copyOfRange(index, data.size)
        return when (encoding) {
            0x00 -> String(textBytes, Charsets.ISO_8859_1).trimEnd('\u0000')
            0x01 -> decodeUtf16WithBom(textBytes)
            0x02 -> String(textBytes, Charsets.UTF_16BE).trimEnd('\u0000')
            0x03 -> String(textBytes, Charsets.UTF_8).trimEnd('\u0000')
            else -> String(textBytes, Charsets.ISO_8859_1).trimEnd('\u0000')
        }
    }

    private fun findTerminator(data: ByteArray, from: Int, width: Int): Int {
        var i = from
        while (i + width <= data.size) {
            if (width == 1 && data[i] == 0.toByte()) return i
            if (width == 2 && data[i] == 0.toByte() && data[i + 1] == 0.toByte()) return i
            i += 1
        }
        return data.size
    }

    private fun synchsafe(size: Int): ByteArray = byteArrayOf(
        ((size shr 21) and 0x7F).toByte(),
        ((size shr 14) and 0x7F).toByte(),
        ((size shr 7) and 0x7F).toByte(),
        (size and 0x7F).toByte(),
    )

    private fun unsynchsafe(bytes: ByteArray): Int =
        ((bytes[0].toInt() and 0x7F) shl 21) or
            ((bytes[1].toInt() and 0x7F) shl 14) or
            ((bytes[2].toInt() and 0x7F) shl 7) or
            (bytes[3].toInt() and 0x7F)

    private fun beInt(value: Int): ByteArray = byteArrayOf(
        ((value shr 24) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        (value and 0xFF).toByte(),
    )

    /** Builds a full, standalone ID3v2.3 tag (header + frames) as bytes. */
    fun encodeTag(frames: List<Id3Frame>): ByteArray {
        val frameBytes = ByteArrayOutputStream()
        for (frame in frames) {
            val idBytes = frame.id.padEnd(4).take(4).toByteArray(Charsets.US_ASCII)
            frameBytes.write(idBytes)
            frameBytes.write(beInt(frame.data.size))
            frameBytes.write(byteArrayOf(0, 0)) // flags
            frameBytes.write(frame.data)
        }
        val body = frameBytes.toByteArray()

        val out = ByteArrayOutputStream()
        out.write("ID3".toByteArray(Charsets.US_ASCII))
        out.write(byteArrayOf(3, 0)) // version 2.3.0
        out.write(0) // flags
        out.write(synchsafe(body.size))
        out.write(body)
        return out.toByteArray()
    }

    /**
     * Parses a full ID3v2.3/2.4 tag (as produced by [encodeTag], or read off
     * an existing file) back into its frames. Returns an empty list for
     * anything malformed rather than throwing.
     */
    fun decodeTag(bytes: ByteArray): List<Id3Frame> {
        if (bytes.size < HEADER_SIZE) return emptyList()
        if (bytes[0] != 'I'.code.toByte() || bytes[1] != 'D'.code.toByte() || bytes[2] != '3'.code.toByte()) {
            return emptyList()
        }
        val tagSize = unsynchsafe(bytes.copyOfRange(6, 10))
        val end = (HEADER_SIZE + tagSize).coerceAtMost(bytes.size)

        val frames = mutableListOf<Id3Frame>()
        var pos = HEADER_SIZE
        while (pos + 10 <= end) {
            val idBytes = bytes.copyOfRange(pos, pos + 4)
            if (idBytes.all { it == 0.toByte() }) break
            val id = String(idBytes, Charsets.US_ASCII)
            val sizeBytes = bytes.copyOfRange(pos + 4, pos + 8)
            val size = beInt4(sizeBytes)
            val dataStart = pos + 10
            val dataEnd = (dataStart + size).coerceIn(dataStart, end)
            if (size < 0 || dataStart > end) break
            frames += Id3Frame(id, bytes.copyOfRange(dataStart, dataEnd))
            pos = dataEnd
        }
        return frames
    }

    private fun beInt4(b: ByteArray): Int =
        ((b[0].toInt() and 0xFF) shl 24) or
            ((b[1].toInt() and 0xFF) shl 16) or
            ((b[2].toInt() and 0xFF) shl 8) or
            (b[3].toInt() and 0xFF)

    /** Finds the byte length of an existing ID3v2 tag at the start of [bytes], or 0 if none. */
    fun existingTagLength(bytes: ByteArray): Int {
        if (bytes.size < HEADER_SIZE) return 0
        if (bytes[0] != 'I'.code.toByte() || bytes[1] != 'D'.code.toByte() || bytes[2] != '3'.code.toByte()) return 0
        val tagSize = unsynchsafe(bytes.copyOfRange(6, 10))
        return HEADER_SIZE + tagSize
    }
}
