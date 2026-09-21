package com.ozin.music.core.domain

import com.ozin.music.core.data.model.Song
import java.io.File

/** One group of songs judged likely to be duplicates of each other. */
data class DuplicateGroup(val songs: List<Song>)

/**
 * Pure, unit-testable similarity heuristic used by the duplicate-scan
 * feature. Two songs are considered likely duplicates when any of the
 * following hold:
 *  - same normalized filename (case-insensitive, extension stripped,
 *    common "(1)"/" - copy" suffixes stripped), OR
 *  - same duration (within [durationToleranceMs]) AND same file size, OR
 *  - same normalized title+artist AND duration within [durationToleranceMs].
 *
 * Never mutates or deletes anything — it only groups. The caller decides
 * what, if anything, to remove.
 */
object DuplicateDetector {

    private val copySuffixRegex = Regex("""(?i)(\s*\(\d+\)|\s*-\s*copy\d*|\s*copy\d*)$""")

    fun normalizeFileName(path: String): String {
        val base = File(path).nameWithoutExtension.lowercase().trim()
        return copySuffixRegex.replace(base, "").trim()
    }

    private fun normalizeText(value: String): String = value.lowercase().trim()

    fun findDuplicates(
        songs: List<Song>,
        durationToleranceMs: Long = 1500L,
    ): List<DuplicateGroup> {
        if (songs.size < 2) return emptyList()

        val used = BooleanArray(songs.size)
        val groups = mutableListOf<DuplicateGroup>()

        for (i in songs.indices) {
            if (used[i]) continue
            val a = songs[i]
            val bucket = mutableListOf(a)
            for (j in i + 1 until songs.size) {
                if (used[j]) continue
                val b = songs[j]
                if (areLikelyDuplicates(a, b, durationToleranceMs)) {
                    bucket += b
                    used[j] = true
                }
            }
            if (bucket.size > 1) {
                used[i] = true
                groups += DuplicateGroup(bucket)
            }
        }
        return groups
    }

    private fun areLikelyDuplicates(a: Song, b: Song, durationToleranceMs: Long): Boolean {
        if (a.id == b.id) return false

        val sameFileName = normalizeFileName(a.path) == normalizeFileName(b.path)
        if (sameFileName) return true

        val durationClose = kotlin.math.abs(a.durationMs - b.durationMs) <= durationToleranceMs
        val sameSize = a.sizeBytes > 0 && a.sizeBytes == b.sizeBytes
        if (durationClose && sameSize) return true

        val sameTitleArtist = normalizeText(a.title) == normalizeText(b.title) &&
            normalizeText(a.artist) == normalizeText(b.artist)
        if (sameTitleArtist && durationClose) return true

        return false
    }
}
