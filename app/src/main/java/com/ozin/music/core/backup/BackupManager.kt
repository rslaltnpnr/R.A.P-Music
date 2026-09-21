package com.ozin.music.core.backup

import android.content.Context
import android.net.Uri
import com.ozin.music.core.data.repository.CustomEqPresetRepository
import com.ozin.music.core.data.repository.PlaylistRepository
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.domain.EqPresetId
import com.ozin.music.core.ui.theme.AccentColorOption
import com.ozin.music.core.settings.ArtworkQuality
import com.ozin.music.core.settings.LanguageOption
import com.ozin.music.core.settings.LockScreenPrivacy
import com.ozin.music.core.settings.NowPlayingVisualMode
import com.ozin.music.core.settings.RepeatMode
import com.ozin.music.core.settings.SettingsRepository
import com.ozin.music.core.ui.theme.ThemeMode
import com.ozin.music.core.ui.theme.ThemePreset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports/imports a single JSON backup file (via Storage Access Framework
 * `Uri`s handed in by the caller from `CreateDocument`/`OpenDocument`, the
 * same SAF pattern `FileManagementViewModel` already uses elsewhere in this
 * app) covering: playlists + their song membership, favorites, ratings,
 * custom EQ presets, and every field in [SettingsRepository.settings].
 *
 * Songs are matched across devices/rescans by [com.ozin.music.core.data.model.Song.path]
 * (a MediaStore row id is not stable across a rescan), so a restore onto a
 * library missing some of the backed-up files simply skips those entries
 * rather than crashing or fabricating placeholder songs.
 *
 * Import behavior is a MERGE, not a wholesale replace: existing playlists,
 * favorites, ratings and EQ presets are kept, and the backup's playlists are
 * added as new playlists (renamed with a numeric suffix on a name clash) and
 * its favorites/ratings are applied on top of the current library. Settings
 * are the one exception - they are a single coherent configuration, so a
 * restore replaces them outright with the backup's values. This choice (documented
 * here rather than silently either way) avoids the far riskier alternative of
 * wiping the user's existing library-derived data on every restore.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val eqPresetRepository: CustomEqPresetRepository,
) {
    companion object {
        private const val SCHEMA_VERSION = 1
    }

    suspend fun export(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = buildBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toString(2).toByteArray(Charsets.UTF_8))
            } ?: error("Could not open the selected file for writing")
        }
    }

    /** @return Result.success on a valid backup applied (even if some
     * entries inside it had to be skipped), Result.failure with a message
     * safe to show the user otherwise. Never throws. */
    suspend fun import(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("Could not open the selected file for reading")
            val json = try {
                JSONObject(text)
            } catch (e: JSONException) {
                throw IllegalArgumentException("not valid JSON", e)
            }
            if (!json.has("settings") && !json.has("playlists") && !json.has("favorites")) {
                throw IllegalArgumentException("missing expected backup fields")
            }
            applyBackupJson(json)
        }
    }

    private suspend fun buildBackupJson(): JSONObject {
        val settings = settingsRepository.settings.first()
        val songs = songRepository.songs.first()
        val playlists = playlistRepository.songsInAllPlaylistsSnapshot()
        val eqPresets = eqPresetRepository.presets.first()

        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val settingsJson = JSONObject().apply {
            put("excludedFolders", JSONArray(settings.excludedFolders.toList()))
            put("shuffleDefault", settings.shuffleDefault)
            put("repeatDefault", settings.repeatDefault.name)
            put("crossfadeEnabled", settings.crossfadeEnabled)
            put("crossfadeSeconds", settings.crossfadeSeconds)
            put("smartCrossfadeEnabled", settings.smartCrossfadeEnabled)
            put("normalizationEnabled", settings.normalizationEnabled)
            put("miniPlayerCompact", settings.miniPlayerCompact)
            put("fadeInOutEnabled", settings.fadeInOutEnabled)
            put("eqEnabled", settings.eqEnabled)
            put("eqPreset", settings.eqPreset.name)
            put("eqCustomBands", JSONArray(settings.eqCustomBands))
            put("eqPreampMb", settings.eqPreampMb)
            put("bassBoostStrength", settings.bassBoostStrength)
            put("virtualizerStrength", settings.virtualizerStrength)
            put("loudnessGainMb", settings.loudnessGainMb)
            put("lyricsOffsetMs", settings.lyricsOffsetMs)
            put("themePreset", settings.themePreset.name)
            put("themeMode", settings.themeMode.name)
            put("accentColorOption", settings.accentColorOption.name)
            put("nowPlayingVisualMode", settings.nowPlayingVisualMode.name)
            put("languageOption", settings.languageOption.name)
            put("lockScreenShowArtwork", settings.lockScreenShowArtwork)
            put("lockScreenShowMediaInfo", settings.lockScreenShowMediaInfo)
            put("artworkQuality", settings.artworkQuality.name)
            put("lockScreenPrivacy", settings.lockScreenPrivacy.name)
            put("nowPlayingGesturesEnabled", settings.nowPlayingGesturesEnabled)
            put("shakeToPauseEnabled", settings.shakeToPauseEnabled)
        }
        root.put("settings", settingsJson)

        val favoritesJson = JSONArray()
        val ratingsJson = JSONObject()
        songs.forEach { song ->
            if (song.isFavorite) favoritesJson.put(song.path)
            if (song.rating > 0) ratingsJson.put(song.path, song.rating)
        }
        root.put("favorites", favoritesJson)
        root.put("ratings", ratingsJson)

        val eqPresetsJson = JSONArray()
        eqPresets.forEach { preset ->
            eqPresetsJson.put(
                JSONObject().apply {
                    put("name", preset.name)
                    put("bands", preset.bands)
                    put("preampMb", preset.preampMb)
                    put("bassBoostStrength", preset.bassBoostStrength)
                    put("virtualizerStrength", preset.virtualizerStrength)
                    put("loudnessGainMb", preset.loudnessGainMb)
                }
            )
        }
        root.put("eqPresets", eqPresetsJson)

        val playlistsJson = JSONArray()
        playlists.forEach { (name, songPaths) ->
            playlistsJson.put(
                JSONObject().apply {
                    put("name", name)
                    put("songPaths", JSONArray(songPaths))
                }
            )
        }
        root.put("playlists", playlistsJson)

        return root
    }

    private suspend fun applyBackupJson(json: JSONObject) {
        json.optJSONObject("settings")?.let { s ->
            restoreSettings(s)
        }

        val pathToId = songRepository.songs.first().associate { it.path to it.id }

        json.optJSONArray("favorites")?.let { favorites ->
            for (i in 0 until favorites.length()) {
                val path = favorites.optString(i, null) ?: continue
                pathToId[path]?.let { id -> songRepository.setFavorite(id, true) }
            }
        }

        json.optJSONObject("ratings")?.let { ratings ->
            ratings.keys().forEach { path ->
                pathToId[path]?.let { id -> songRepository.setRating(id, ratings.optInt(path, 0)) }
            }
        }

        json.optJSONArray("eqPresets")?.let { presets ->
            for (i in 0 until presets.length()) {
                val preset = presets.optJSONObject(i) ?: continue
                val bands = preset.optString("bands", "")
                    .takeIf { it.isNotBlank() }
                    ?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
                eqPresetRepository.save(
                    name = preset.optString("name", "Restored preset"),
                    bands = bands,
                    preampMb = preset.optInt("preampMb", 0),
                    bassBoostStrength = preset.optInt("bassBoostStrength", 0),
                    virtualizerStrength = preset.optInt("virtualizerStrength", 0),
                    loudnessGainMb = preset.optInt("loudnessGainMb", 0),
                )
            }
        }

        json.optJSONArray("playlists")?.let { playlists ->
            val existingNames = playlistRepository.playlistNamesSnapshot()
            for (i in 0 until playlists.length()) {
                val entry = playlists.optJSONObject(i) ?: continue
                val baseName = entry.optString("name", "Restored playlist")
                var name = baseName
                var suffix = 2
                while (name in existingNames) {
                    name = "$baseName ($suffix)"
                    suffix++
                }
                val playlistId = playlistRepository.create(name)
                val paths = entry.optJSONArray("songPaths")
                if (paths != null) {
                    for (j in 0 until paths.length()) {
                        val path = paths.optString(j, null) ?: continue
                        pathToId[path]?.let { songId -> playlistRepository.addSong(playlistId, songId) }
                    }
                }
            }
        }
    }

    private suspend fun restoreSettings(s: JSONObject) {
        s.optJSONArray("excludedFolders")?.let { arr ->
            val set = (0 until arr.length()).mapNotNull { arr.optString(it, null) }.toSet()
            settingsRepository.replaceExcludedFolders(set)
        }
        settingsRepository.setShuffleDefault(s.optBoolean("shuffleDefault", false))
        runCatching { RepeatMode.valueOf(s.optString("repeatDefault")) }.getOrNull()
            ?.let { settingsRepository.setRepeatDefault(it) }
        settingsRepository.setCrossfadeEnabled(s.optBoolean("crossfadeEnabled", false))
        settingsRepository.setCrossfadeSeconds(s.optInt("crossfadeSeconds", 4))
        settingsRepository.setSmartCrossfadeEnabled(s.optBoolean("smartCrossfadeEnabled", false))
        settingsRepository.setNormalizationEnabled(s.optBoolean("normalizationEnabled", false))
        settingsRepository.setMiniPlayerCompact(s.optBoolean("miniPlayerCompact", false))
        settingsRepository.setFadeInOutEnabled(s.optBoolean("fadeInOutEnabled", false))
        settingsRepository.setEqEnabled(s.optBoolean("eqEnabled", false))
        runCatching { EqPresetId.valueOf(s.optString("eqPreset")) }.getOrNull()
            ?.let { settingsRepository.setEqPreset(it) }
        s.optJSONArray("eqCustomBands")?.let { arr ->
            val bands = (0 until arr.length()).map { arr.optInt(it, 0) }
            settingsRepository.setEqCustomBands(bands)
        }
        settingsRepository.setEqPreampMb(s.optInt("eqPreampMb", 0))
        settingsRepository.setBassBoostStrength(s.optInt("bassBoostStrength", 0))
        settingsRepository.setVirtualizerStrength(s.optInt("virtualizerStrength", 0))
        settingsRepository.setLoudnessGainMb(s.optInt("loudnessGainMb", 0))
        settingsRepository.setLyricsOffsetMs(s.optInt("lyricsOffsetMs", 0))
        runCatching { ThemePreset.valueOf(s.optString("themePreset")) }.getOrNull()
            ?.let { settingsRepository.setThemePreset(it) }
        runCatching { ThemeMode.valueOf(s.optString("themeMode")) }.getOrNull()
            ?.let { settingsRepository.setThemeMode(it) }
        runCatching { AccentColorOption.valueOf(s.optString("accentColorOption")) }.getOrNull()
            ?.let { settingsRepository.setAccentColorOption(it) }
        runCatching { NowPlayingVisualMode.valueOf(s.optString("nowPlayingVisualMode")) }.getOrNull()
            ?.let { settingsRepository.setNowPlayingVisualMode(it) }
        runCatching { LanguageOption.valueOf(s.optString("languageOption")) }.getOrNull()
            ?.let { settingsRepository.setLanguageOption(it) }
        settingsRepository.setLockScreenShowArtwork(s.optBoolean("lockScreenShowArtwork", true))
        settingsRepository.setLockScreenShowMediaInfo(s.optBoolean("lockScreenShowMediaInfo", true))
        runCatching { ArtworkQuality.valueOf(s.optString("artworkQuality")) }.getOrNull()
            ?.let { settingsRepository.setArtworkQuality(it) }
        runCatching { LockScreenPrivacy.valueOf(s.optString("lockScreenPrivacy")) }.getOrNull()
            ?.let { settingsRepository.setLockScreenPrivacy(it) }
        settingsRepository.setNowPlayingGesturesEnabled(s.optBoolean("nowPlayingGesturesEnabled", true))
        settingsRepository.setShakeToPauseEnabled(s.optBoolean("shakeToPauseEnabled", false))
    }
}
