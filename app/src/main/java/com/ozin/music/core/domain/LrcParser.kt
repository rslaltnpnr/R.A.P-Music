package com.ozin.music.core.domain

import java.io.File

data class LyricLine(val timeMs: Long, val text: String)

/** Parses standard `[mm:ss.xx]` LRC timestamp lines. Never throws. */
object LrcParser {
    private val lineRegex = Regex("""\[(\d{2}):(\d{2})(?:[.:](\d{1,3}))?]\s*(.*)""")

    fun parse(content: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        content.lineSequence().forEach { raw ->
            val match = lineRegex.find(raw) ?: return@forEach
            try {
                val (minutes, seconds, millisRaw, text) = match.destructured
                val millis = when {
                    millisRaw.isEmpty() -> 0L
                    millisRaw.length == 1 -> millisRaw.toLong() * 100
                    millisRaw.length == 2 -> millisRaw.toLong() * 10
                    else -> millisRaw.toLong()
                }
                val timeMs = minutes.toLong() * 60_000 + seconds.toLong() * 1000 + millis
                lines += LyricLine(timeMs, text.trim())
            } catch (_: NumberFormatException) {
                // Malformed timestamp on this line — skip it, keep the rest.
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    /** Looks for a same-name `.lrc` file next to [audioPath]; returns null if absent/unreadable. */
    fun findAndParseFor(audioPath: String): List<LyricLine>? {
        return try {
            val audioFile = File(audioPath)
            val lrcFile = File(audioFile.parentFile, audioFile.nameWithoutExtension + ".lrc")
            if (!lrcFile.exists() || !lrcFile.canRead()) return null
            parse(lrcFile.readText())
        } catch (_: Exception) {
            null
        }
    }

    fun currentLine(lines: List<LyricLine>, positionMs: Long): LyricLine? =
        lines.lastOrNull { it.timeMs <= positionMs }
}
