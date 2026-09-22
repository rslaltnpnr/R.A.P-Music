package com.ozin.music.core.data.network

import android.util.Log
import java.io.IOException
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin client for the free, keyless LRCLIB API (https://lrclib.net), used to
 * automatically fetch synced lyrics when no local `.lrc` file exists next to
 * a song (see [com.ozin.music.feature.lyrics.LyricsViewModel]).
 *
 * Uses LRCLIB's `GET /api/get` endpoint, which does an exact-match lookup by
 * track/artist/album/duration and is documented to return a single JSON
 * object (404 when there is no exact match) - this is the right endpoint
 * here since we already know the exact track/artist/album/duration from the
 * local library scan, rather than `/api/search`, which returns a list meant
 * for a user to disambiguate.
 *
 * Reuses this app's existing `org.json` convention (see
 * [com.ozin.music.core.backup.BackupManager]) rather than adding a second
 * JSON library, and the singleton [OkHttpClient] already provided by
 * [com.ozin.music.core.remote.RemoteModule] for the WebDAV remote-library
 * feature, rather than standing up a second HTTP client or adding Retrofit.
 */
@Singleton
class LrcLibClient @Inject constructor(
    private val httpClient: OkHttpClient,
) {

    /** Result of a lookup: the raw, unmodified LRC-format text of the
     * synced lyrics (`syncedLyrics` field), or null when LRCLIB has no exact
     * match, the track has no synced lyrics, or the request failed for any
     * reason (network, timeout, malformed response). Never throws. */
    suspend fun fetchSyncedLyrics(
        trackName: String,
        artistName: String,
        albumName: String,
        durationMs: Long,
    ): String? = withContext(Dispatchers.IO) {
        if (trackName.isBlank() || artistName.isBlank()) return@withContext null
        try {
            val durationSeconds = (durationMs / 1000L).coerceAtLeast(1L)
            val url = buildString {
                append("https://lrclib.net/api/get")
                append("?track_name=").append(urlEncode(trackName))
                append("&artist_name=").append(urlEncode(artistName))
                if (albumName.isNotBlank()) {
                    append("&album_name=").append(urlEncode(albumName))
                }
                append("&duration=").append(durationSeconds)
            }
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "R.A.P-Music Android app (https://github.com/rslaltnpnr/R.A.P-Music)")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val synced = json.optString("syncedLyrics", "")
                synced.takeIf { it.isNotBlank() }
            }
        } catch (e: IOException) {
            Log.w(TAG, "LRCLIB lookup failed (network)", e)
            null
        } catch (e: JSONException) {
            Log.w(TAG, "LRCLIB lookup failed (malformed response)", e)
            null
        } catch (e: Exception) {
            // Defensive catch-all: an auto-download lookup must never crash
            // the caller or block playback.
            Log.w(TAG, "LRCLIB lookup failed", e)
            null
        }
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val TAG = "LrcLibClient"
    }
}
