package com.ozin.music.core.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.ozin.music.core.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the on-device MediaStore audio table into [Song] rows. Runs entirely
 * off the main thread and never throws: any per-row failure is logged and the
 * row skipped so a single corrupt entry cannot break the whole scan.
 */
@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun scan(excludedFolders: Set<String>): List<Song> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    try {
                        val path = cursor.getString(dataCol) ?: continue
                        val parentFolder = File(path).parent ?: ""
                        if (excludedFolders.any { parentFolder.startsWith(it) }) continue

                        result += Song(
                            id = cursor.getLong(idCol),
                            title = cursor.getString(titleCol) ?: File(path).nameWithoutExtension,
                            artist = cursor.getString(artistCol) ?: "Unknown Artist",
                            album = cursor.getString(albumCol) ?: "Unknown Album",
                            albumId = cursor.getLong(albumIdCol),
                            durationMs = cursor.getLong(durationCol),
                            path = path,
                            sizeBytes = cursor.getLong(sizeCol),
                            year = cursor.getInt(yearCol),
                            trackNumber = cursor.getInt(trackCol) % 1000,
                            dateAdded = cursor.getLong(dateAddedCol) * 1000L,
                        )
                    } catch (rowError: Exception) {
                        Log.w(TAG, "Skipping unreadable media row", rowError)
                    }
                }
            }
        } catch (scanError: Exception) {
            // Revoked permission, provider crash, etc. — return whatever we
            // managed to gather rather than crashing the scan.
            Log.e(TAG, "MediaStore scan failed", scanError)
        }
        result
    }

    fun albumArtUri(albumId: Long): Uri =
        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

    companion object {
        private const val TAG = "MediaStoreScanner"
    }
}
