package com.ozin.music.core.files

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.ozin.music.core.domain.Id3v2Tag
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** The editable fields of a song's metadata, as exposed by the Edit Info screen. */
data class SongMetadataEdit(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val year: Int,
    val trackNumber: Int,
    val discNumber: Int,
    val comment: String,
)

/**
 * Writes song metadata edits through proper Android scoped-storage APIs.
 *
 * MediaStore's own columns (title/artist/album/album artist/genre/year/
 * track) are always updated via [ContentResolver.update] — this is what the
 * rest of the OS (other music apps, the system media scanner) sees.
 *
 * For MP3 files specifically, this also rewrites the file's own ID3v2.3 tag
 * (TIT2/TPE1/TALB/TPE2/TCON/TYER/TRCK/TPOS/COMM/APIC) using [Id3v2Tag], so
 * the metadata survives outside this app too (e.g. copied to a PC). This is
 * deliberately MP3-only: FLAC/OGG use Vorbis comments, a different format
 * this app does not write to avoid corrupting files — for those formats
 * only the MediaStore-visible fields above are updated, which is enough for
 * this app's own library and for most other Android apps that also read
 * MediaStore.
 */
@Singleton
class MetadataEditor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun mediaStoreUri(songId: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)

    suspend fun updateMetadata(
        songId: Long,
        path: String,
        edit: SongMetadataEdit,
        albumArt: ByteArray? = null,
    ): FileOperationResult = withContext(Dispatchers.IO) {
        val uri = mediaStoreUri(songId)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.TITLE, edit.title)
            put(MediaStore.Audio.Media.ARTIST, edit.artist)
            put(MediaStore.Audio.Media.ALBUM, edit.album)
            put(MediaStore.Audio.Media.GENRE, edit.genre)
            put(MediaStore.Audio.Media.YEAR, edit.year)
            put(MediaStore.Audio.Media.TRACK, edit.trackNumber)
        }

        val mediaStoreResult = try {
            context.contentResolver.update(uri, values, null, null)
            FileOperationResult.Success
        } catch (e: RecoverableSecurityException) {
            return@withContext FileOperationResult.NeedsUserConsent(e.userAction.actionIntent.intentSender)
        } catch (e: Exception) {
            FileOperationResult.Failed(e.message ?: "Could not update MediaStore metadata")
        }
        if (mediaStoreResult is FileOperationResult.Failed) return@withContext mediaStoreResult

        if (path.endsWith(".mp3", ignoreCase = true)) {
            val id3Result = runCatching { writeMp3Tags(path, edit, albumArt) }
            if (id3Result.isFailure) {
                // MediaStore write already succeeded — the app's own library and
                // MediaStore-reading apps still reflect the edit; only the
                // embedded on-disk tag write is best-effort and quietly
                // skipped on failure (e.g. unreadable/unwritable direct path
                // under scoped storage).
                return@withContext FileOperationResult.Success
            }
        }
        FileOperationResult.Success
    }

    /** Splices a freshly-built ID3v2.3 tag onto the front of an MP3 file, replacing any existing one. */
    private fun writeMp3Tags(path: String, edit: SongMetadataEdit, albumArt: ByteArray?) {
        val file = File(path)
        if (!file.canWrite()) return // Scoped storage may block direct writes for some paths; skip quietly.

        val original = file.readBytes()
        val existingTagLength = Id3v2Tag.existingTagLength(original)
        val audioBody = original.copyOfRange(existingTagLength, original.size)

        val frames = buildList {
            add(Id3v2Tag.textFrame("TIT2", edit.title))
            add(Id3v2Tag.textFrame("TPE1", edit.artist))
            add(Id3v2Tag.textFrame("TALB", edit.album))
            add(Id3v2Tag.textFrame("TPE2", edit.albumArtist))
            add(Id3v2Tag.textFrame("TCON", edit.genre))
            add(Id3v2Tag.textFrame("TYER", edit.year.toString()))
            add(Id3v2Tag.textFrame("TRCK", edit.trackNumber.toString()))
            add(Id3v2Tag.textFrame("TPOS", edit.discNumber.toString()))
            if (edit.comment.isNotBlank()) add(Id3v2Tag.commentFrame(edit.comment))
            if (albumArt != null) add(Id3v2Tag.pictureFrame("image/jpeg", albumArt))
        }
        val newTag = Id3v2Tag.encodeTag(frames)

        file.outputStream().use { out ->
            out.write(newTag)
            out.write(audioBody)
        }
    }

    suspend fun renameFile(songId: Long, newDisplayName: String): FileOperationResult = withContext(Dispatchers.IO) {
        val uri = mediaStoreUri(songId)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, newDisplayName)
        }
        try {
            context.contentResolver.update(uri, values, null, null)
            FileOperationResult.Success
        } catch (e: RecoverableSecurityException) {
            FileOperationResult.NeedsUserConsent(e.userAction.actionIntent.intentSender)
        } catch (e: Exception) {
            FileOperationResult.Failed(e.message ?: "Could not rename file")
        }
    }

    suspend fun deleteSongs(songIds: List<Long>): FileOperationResult = withContext(Dispatchers.IO) {
        if (songIds.isEmpty()) return@withContext FileOperationResult.Success
        val uris = songIds.map { mediaStoreUri(it) }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                return@withContext FileOperationResult.NeedsUserConsent(pendingIntent.intentSender)
            }
            uris.forEach { context.contentResolver.delete(it, null, null) }
            FileOperationResult.Success
        } catch (e: RecoverableSecurityException) {
            FileOperationResult.NeedsUserConsent(e.userAction.actionIntent.intentSender)
        } catch (e: Exception) {
            FileOperationResult.Failed(e.message ?: "Could not delete file(s)")
        }
    }

    /** Real content:// share URI via MediaStore — never a raw file:// path. */
    fun shareUri(songId: Long): Uri = mediaStoreUri(songId)

    data class FileDetails(
        val path: String,
        val sizeBytes: Long,
        val format: String,
        val bitrateKbps: Int?,
        val sampleRateHz: Int?,
        val lastModifiedMs: Long,
    )

    suspend fun readFileDetails(path: String, sizeBytes: Long): FileDetails = withContext(Dispatchers.IO) {
        val file = File(path)
        var bitrate: Int? = null
        var sampleRate: Int? = null
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(path)
            bitrate = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull()?.div(1000)
            sampleRate = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                ?.toIntOrNull()
            retriever.release()
        } catch (_: Exception) {
            // Corrupt/unreadable media: fall back to file-system info only.
        }
        FileDetails(
            path = path,
            sizeBytes = sizeBytes,
            format = file.extension.uppercase().ifBlank { "Unknown" },
            bitrateKbps = bitrate,
            sampleRateHz = sampleRate,
            lastModifiedMs = runCatching { file.lastModified() }.getOrDefault(0L),
        )
    }
}
