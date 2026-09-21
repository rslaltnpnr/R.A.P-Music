package com.ozin.music.core.domain

import com.ozin.music.core.data.model.Song
import java.io.File

/** One grouped bucket in a grouped library view (Albums/Artists/Folders/Genres). */
data class SongGroup(
    val key: String,
    val title: String,
    val subtitle: String,
    val albumId: Long,
    val songs: List<Song>,
)

/**
 * Pure, unit-testable grouping logic for the Library screen's Albums,
 * Artists, Folders and Genres tabs. Each function preserves a stable,
 * case-insensitive alphabetical order over the group title so the UI is
 * deterministic regardless of scan order.
 */
object LibraryGrouping {

    fun byAlbum(songs: List<Song>): List<SongGroup> =
        songs.groupBy { it.albumId to it.album }
            .map { (key, grouped) ->
                SongGroup(
                    key = "album:${key.first}",
                    title = key.second.ifBlank { "Unknown Album" },
                    subtitle = "${grouped.size} song${if (grouped.size == 1) "" else "s"}",
                    albumId = key.first,
                    songs = grouped.sortedBy { it.trackNumber },
                )
            }
            .sortedBy { it.title.lowercase() }

    fun byArtist(songs: List<Song>): List<SongGroup> =
        songs.groupBy { it.artist.ifBlank { "Unknown Artist" } }
            .map { (artist, grouped) ->
                SongGroup(
                    key = "artist:$artist",
                    title = artist,
                    subtitle = "${grouped.size} song${if (grouped.size == 1) "" else "s"}",
                    albumId = grouped.first().albumId,
                    songs = grouped.sortedBy { it.title.lowercase() },
                )
            }
            .sortedBy { it.title.lowercase() }

    fun byFolder(songs: List<Song>): List<SongGroup> =
        songs.groupBy { File(it.path).parent ?: "/" }
            .map { (folder, grouped) ->
                SongGroup(
                    key = "folder:$folder",
                    title = File(folder).name.ifBlank { folder },
                    subtitle = folder,
                    albumId = grouped.first().albumId,
                    songs = grouped.sortedBy { it.title.lowercase() },
                )
            }
            .sortedBy { it.title.lowercase() }

    fun byGenre(songs: List<Song>): List<SongGroup> =
        songs.groupBy { it.genre.ifBlank { "Unknown" } }
            .map { (genre, grouped) ->
                SongGroup(
                    key = "genre:$genre",
                    title = genre,
                    subtitle = "${grouped.size} song${if (grouped.size == 1) "" else "s"}",
                    albumId = grouped.first().albumId,
                    songs = grouped.sortedBy { it.title.lowercase() },
                )
            }
            .sortedBy { it.title.lowercase() }
}
