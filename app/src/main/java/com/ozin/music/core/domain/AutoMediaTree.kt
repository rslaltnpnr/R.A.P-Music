package com.ozin.music.core.domain

import com.ozin.music.core.data.model.Playlist
import com.ozin.music.core.data.model.Song

/** A single node of the Android Auto browse tree, free of any Media3/Android
 * dependency so the mapping logic is directly unit-testable. [service] maps
 * it 1:1 onto a real `MediaItem` (browsable or playable, with real metadata). */
data class AutoNode(
    val mediaId: String,
    val title: String,
    val subtitle: String? = null,
    val browsable: Boolean,
    val playable: Boolean,
)

/**
 * Pure logic for building Android Auto's browsable media tree: the root's
 * fixed categories, and mapping [Song]/[Playlist] rows onto browse nodes.
 * Deliberately limited to Songs/Favorites/Recently Played/Playlists (no
 * Albums/Artists sub-trees) to keep the browse tree small and its
 * media-id scheme unambiguous; see media id prefixes below.
 */
object AutoMediaTree {
    const val ROOT_ID = "root"
    const val CATEGORY_SONGS = "cat_songs"
    const val CATEGORY_FAVORITES = "cat_favorites"
    const val CATEGORY_RECENT = "cat_recent"
    const val CATEGORY_PLAYLISTS = "cat_playlists"

    private const val SONG_PREFIX = "song_"
    private const val PLAYLIST_PREFIX = "playlist_"

    /** Cap so a huge library does not build an unbounded Auto browse page. */
    const val MAX_ITEMS_PER_CATEGORY = 200

    fun songMediaId(songId: Long): String = "$SONG_PREFIX$songId"

    fun playlistMediaId(playlistId: Long): String = "$PLAYLIST_PREFIX$playlistId"

    /** Extracts the numeric song id from a media id built by [songMediaId], or
     * null if [mediaId] does not refer to a song. */
    fun songIdOrNull(mediaId: String): Long? =
        mediaId.takeIf { it.startsWith(SONG_PREFIX) }?.removePrefix(SONG_PREFIX)?.toLongOrNull()

    /** Extracts the numeric playlist id from a media id built by
     * [playlistMediaId], or null if [mediaId] does not refer to a playlist. */
    fun playlistIdOrNull(mediaId: String): Long? =
        mediaId.takeIf { it.startsWith(PLAYLIST_PREFIX) }?.removePrefix(PLAYLIST_PREFIX)?.toLongOrNull()

    fun rootChildren(): List<AutoNode> = listOf(
        AutoNode(CATEGORY_SONGS, "Songs", browsable = true, playable = false),
        AutoNode(CATEGORY_FAVORITES, "Favorites", browsable = true, playable = false),
        AutoNode(CATEGORY_RECENT, "Recently Played", browsable = true, playable = false),
        AutoNode(CATEGORY_PLAYLISTS, "Playlists", browsable = true, playable = false),
    )

    fun songNodes(songs: List<Song>): List<AutoNode> =
        songs.take(MAX_ITEMS_PER_CATEGORY).map { it.toAutoNode() }

    fun playlistNodes(playlists: List<Playlist>): List<AutoNode> =
        playlists.take(MAX_ITEMS_PER_CATEGORY).map { it.toAutoNode() }

    private fun Song.toAutoNode(): AutoNode = AutoNode(
        mediaId = songMediaId(id),
        title = title,
        subtitle = artist,
        browsable = false,
        playable = true,
    )

    private fun Playlist.toAutoNode(): AutoNode = AutoNode(
        mediaId = playlistMediaId(id),
        title = name,
        browsable = true,
        playable = false,
    )
}
