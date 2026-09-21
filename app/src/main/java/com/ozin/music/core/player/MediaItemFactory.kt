package com.ozin.music.core.player

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.ozin.music.R
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.ArtworkResolver
import com.ozin.music.core.settings.AppSettings
import com.ozin.music.core.settings.LockScreenPrivacy

/**
 * Single, shared place that turns a [Song] into a fully-described
 * [MediaItem] — including real artwork ([ArtworkResolver]) and lock-screen
 * privacy enforcement — used by both [PlayerController] (phone-side queue)
 * and [OzinLibrarySessionCallback] (Android Auto) so the system-facing
 * MediaMetadata (lock screen, notification, car display) is built exactly
 * once, the same way, everywhere.
 */
object MediaItemFactory {

    suspend fun buildMediaItem(
        context: Context,
        song: Song,
        artworkResolver: ArtworkResolver,
        settings: AppSettings,
        mediaId: String = song.id.toString(),
        browsable: Boolean = false,
        playable: Boolean = true,
    ): MediaItem {
        val artwork = artworkResolver.resolve(
            song = song,
            quality = settings.artworkQuality,
            privacy = settings.lockScreenPrivacy,
            showArtwork = settings.lockScreenShowArtwork,
        )

        val hideMetadata = !settings.lockScreenShowMediaInfo ||
            settings.lockScreenPrivacy == LockScreenPrivacy.HIDE_METADATA ||
            settings.lockScreenPrivacy == LockScreenPrivacy.PRIVATE

        val genericTitle = context.getString(R.string.app_name)
        val genericArtist = context.getString(R.string.privacy_generic_artist)

        val title = if (hideMetadata) genericTitle else song.title
        val artist = if (hideMetadata) genericArtist else song.artist
        val album = if (hideMetadata) genericTitle else song.album

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setIsBrowsable(browsable)
            .setIsPlayable(playable)

        if (song.trackNumber > 0) {
            metadataBuilder.setTrackNumber(song.trackNumber)
        }

        when {
            artwork.data != null -> metadataBuilder.setArtworkData(artwork.data, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            artwork.uri != null -> metadataBuilder.setArtworkUri(artwork.uri)
        }

        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(contentUriFor(song.id))
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    fun contentUriFor(songId: Long) =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
}
