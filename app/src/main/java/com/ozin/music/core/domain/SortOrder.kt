package com.ozin.music.core.domain

import com.ozin.music.core.data.model.Song

enum class SortOrder {
    TITLE_ASC,
    TITLE_DESC,
    DATE_ADDED,
    MOST_PLAYED,
    DURATION,
    ARTIST,
    ALBUM,
}

/** Pure, unit-testable comparator/sort utility used by the library screen. */
object SongSorter {
    fun sort(songs: List<Song>, order: SortOrder): List<Song> = when (order) {
        SortOrder.TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
        SortOrder.TITLE_DESC -> songs.sortedByDescending { it.title.lowercase() }
        SortOrder.DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
        SortOrder.MOST_PLAYED -> songs.sortedByDescending { it.playCount }
        SortOrder.DURATION -> songs.sortedByDescending { it.durationMs }
        SortOrder.ARTIST -> songs.sortedBy { it.artist.lowercase() }
        SortOrder.ALBUM -> songs.sortedBy { it.album.lowercase() }
    }
}
