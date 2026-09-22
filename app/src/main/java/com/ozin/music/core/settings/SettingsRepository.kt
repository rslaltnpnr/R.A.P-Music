package com.ozin.music.core.settings

import android.content.Context
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ozin.music.core.domain.EqPresetId
import com.ozin.music.core.ui.theme.AccentColorOption
import com.ozin.music.core.ui.theme.ThemeMode
import com.ozin.music.core.ui.theme.ThemePreset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "ozin_settings")

enum class RepeatMode { OFF, ALL, ONE }

/** User-selectable in-app language override. [SYSTEM] follows the device
 * language (the default); the others force a specific app language via
 * [androidx.appcompat.app.AppCompatDelegate.setApplicationLocales]. */
enum class LanguageOption { SYSTEM, ENGLISH, TURKISH }

/** Maps a [LanguageOption] to the [LocaleListCompat] that should be passed to
 * [androidx.appcompat.app.AppCompatDelegate.setApplicationLocales]. [LanguageOption.SYSTEM]
 * maps to an empty list, which tells AppCompat to defer to the device/system locale. */
fun LanguageOption.toLocaleListCompat(): LocaleListCompat = when (this) {
    LanguageOption.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
    LanguageOption.ENGLISH -> LocaleListCompat.forLanguageTags("en")
    LanguageOption.TURKISH -> LocaleListCompat.forLanguageTags("tr")
}

/** Alternate Now Playing visual modes selectable alongside the original
 * default view (Phase 9). FULL_ART, BLUR and MINIMAL were added alongside
 * the originals; VINYL/CASSETTE/VISUALIZER internals are untouched. */
enum class NowPlayingVisualMode { DEFAULT, VINYL, CASSETTE, VISUALIZER, FULL_ART, BLUR, MINIMAL }

/** How much artwork resolution/downsampling [com.ozin.music.core.domain.ArtworkResolver]
 * applies before handing a bitmap/URI to the system MediaMetadata (lock
 * screen/notification). HIGH uses the original embedded/MediaStore art
 * directly; BALANCED and AUTO downsample it first (see ArtworkResolver for
 * the exact target sizes of each tier). */
enum class ArtworkQuality { AUTO, HIGH, BALANCED }

/** Lock-screen/notification privacy tier enforced in the real MediaMetadata-
 * building path ([com.ozin.music.core.player.MediaItemFactory]), not a
 * cosmetic no-op: HIDE_ARTWORK forces the default artwork, HIDE_METADATA
 * forces a generic title (artist is also generalized, see MediaItemFactory),
 * PRIVATE combines both. */
enum class LockScreenPrivacy { NORMAL, HIDE_ARTWORK, HIDE_METADATA, PRIVATE }

data class AppSettings(
    val excludedFolders: Set<String> = emptySet(),
    val shuffleDefault: Boolean = false,
    val repeatDefault: RepeatMode = RepeatMode.OFF,
    val crossfadeEnabled: Boolean = false,
    val crossfadeSeconds: Int = 4,
    val smartCrossfadeEnabled: Boolean = false,
    val normalizationEnabled: Boolean = false,
    val miniPlayerCompact: Boolean = false,
    val fadeInOutEnabled: Boolean = false,
    // Equalizer
    val eqEnabled: Boolean = false,
    val eqPreset: EqPresetId = EqPresetId.NORMAL,
    val eqCustomBands: List<Int> = emptyList(),
    val eqPreampMb: Int = 0,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val lyricsOffsetMs: Int = 0,
    /** Whether Now Playing shows the one-time, dismissible "try this EQ
     * preset?" banner (item 2) when a newly-played song's mood tags map to a
     * preset different from the currently-active one. Defaults to ON since
     * it is non-blocking, never auto-applies, and can be turned off here at
     * any time from Settings > Playback. */
    val eqSuggestionEnabled: Boolean = true,
    /** Whether the lyrics screen is allowed to automatically look up synced
     * lyrics from the LRCLIB web API (https://lrclib.net) when no local
     * `.lrc` file is found next to the current song. This sends the song's
     * title/artist/album/duration to that third-party service, so it
     * defaults to OFF and is clearly labeled in Settings. On a successful
     * lookup the result is cached to a real local `.lrc` file so future
     * plays of that song never hit the network again. */
    val autoDownloadLyricsEnabled: Boolean = false,
    // Personalization (Phase 9)
    val themePreset: ThemePreset = ThemePreset.DEFAULT_DARK,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColorOption: AccentColorOption = AccentColorOption.PURPLE,
    val nowPlayingVisualMode: NowPlayingVisualMode = NowPlayingVisualMode.DEFAULT,
    val languageOption: LanguageOption = LanguageOption.SYSTEM,
    // Lock screen (Phase 11)
    val lockScreenShowArtwork: Boolean = true,
    val lockScreenShowMediaInfo: Boolean = true,
    val artworkQuality: ArtworkQuality = ArtworkQuality.AUTO,
    val lockScreenPrivacy: LockScreenPrivacy = LockScreenPrivacy.NORMAL,
    val nowPlayingGesturesEnabled: Boolean = true,
    val shakeToPauseEnabled: Boolean = false,
    /** The last of the 5 main bottom-nav routes the user viewed (Home/
     * Library/Lists/EQ/Settings route constants from [com.ozin.music.Routes]),
     * used as the NavHost start destination on next launch so the app resumes
     * where the user left off. Empty means "no preference yet" (fresh
     * install), in which case the caller falls back to Home. */
    val lastViewedRoute: String = "",
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
        val SHUFFLE_DEFAULT = booleanPreferencesKey("shuffle_default")
        val REPEAT_DEFAULT = stringPreferencesKey("repeat_default")
        val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        val CROSSFADE_SECONDS = stringPreferencesKey("crossfade_seconds")
        val NORMALIZATION_ENABLED = booleanPreferencesKey("normalization_enabled")
        val MINI_PLAYER_COMPACT = booleanPreferencesKey("mini_player_compact")
        val SMART_CROSSFADE_ENABLED = booleanPreferencesKey("smart_crossfade_enabled")
        val FADE_IN_OUT_ENABLED = booleanPreferencesKey("fade_in_out_enabled")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_CUSTOM_BANDS = stringPreferencesKey("eq_custom_bands")
        val EQ_PREAMP_MB = intPreferencesKey("eq_preamp_mb")
        val BASS_BOOST_STRENGTH = intPreferencesKey("bass_boost_strength")
        val VIRTUALIZER_STRENGTH = intPreferencesKey("virtualizer_strength")
        val LOUDNESS_GAIN_MB = intPreferencesKey("loudness_gain_mb")
        val LYRICS_OFFSET_MS = intPreferencesKey("lyrics_offset_ms")
        val EQ_SUGGESTION_ENABLED = booleanPreferencesKey("eq_suggestion_enabled")
        val AUTO_DOWNLOAD_LYRICS_ENABLED = booleanPreferencesKey("auto_download_lyrics_enabled")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val NOW_PLAYING_VISUAL_MODE = stringPreferencesKey("now_playing_visual_mode")
        val LANGUAGE_OPTION = stringPreferencesKey("language_option")
        val LOCK_SCREEN_SHOW_ARTWORK = booleanPreferencesKey("lock_screen_show_artwork")
        val LOCK_SCREEN_SHOW_MEDIA_INFO = booleanPreferencesKey("lock_screen_show_media_info")
        val ARTWORK_QUALITY = stringPreferencesKey("artwork_quality")
        val LOCK_SCREEN_PRIVACY = stringPreferencesKey("lock_screen_privacy")
        val NOW_PLAYING_GESTURES_ENABLED = booleanPreferencesKey("now_playing_gestures_enabled")
        val SHAKE_TO_PAUSE_ENABLED = booleanPreferencesKey("shake_to_pause_enabled")
        val LAST_VIEWED_ROUTE = stringPreferencesKey("last_viewed_route")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            excludedFolders = prefs[Keys.EXCLUDED_FOLDERS] ?: emptySet(),
            shuffleDefault = prefs[Keys.SHUFFLE_DEFAULT] ?: false,
            repeatDefault = runCatching {
                RepeatMode.valueOf(prefs[Keys.REPEAT_DEFAULT] ?: RepeatMode.OFF.name)
            }.getOrDefault(RepeatMode.OFF),
            crossfadeEnabled = prefs[Keys.CROSSFADE_ENABLED] ?: false,
            crossfadeSeconds = prefs[Keys.CROSSFADE_SECONDS]?.toIntOrNull() ?: 4,
            smartCrossfadeEnabled = prefs[Keys.SMART_CROSSFADE_ENABLED] ?: false,
            normalizationEnabled = prefs[Keys.NORMALIZATION_ENABLED] ?: false,
            miniPlayerCompact = prefs[Keys.MINI_PLAYER_COMPACT] ?: false,
            fadeInOutEnabled = prefs[Keys.FADE_IN_OUT_ENABLED] ?: false,
            eqEnabled = prefs[Keys.EQ_ENABLED] ?: false,
            eqPreset = runCatching {
                EqPresetId.valueOf(prefs[Keys.EQ_PRESET] ?: EqPresetId.NORMAL.name)
            }.getOrDefault(EqPresetId.NORMAL),
            eqCustomBands = prefs[Keys.EQ_CUSTOM_BANDS]?.takeIf { it.isNotBlank() }
                ?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList(),
            eqPreampMb = prefs[Keys.EQ_PREAMP_MB] ?: 0,
            bassBoostStrength = prefs[Keys.BASS_BOOST_STRENGTH] ?: 0,
            virtualizerStrength = prefs[Keys.VIRTUALIZER_STRENGTH] ?: 0,
            loudnessGainMb = prefs[Keys.LOUDNESS_GAIN_MB] ?: 0,
            lyricsOffsetMs = prefs[Keys.LYRICS_OFFSET_MS] ?: 0,
            eqSuggestionEnabled = prefs[Keys.EQ_SUGGESTION_ENABLED] ?: true,
            autoDownloadLyricsEnabled = prefs[Keys.AUTO_DOWNLOAD_LYRICS_ENABLED] ?: false,
            themePreset = runCatching {
                ThemePreset.valueOf(prefs[Keys.THEME_PRESET] ?: ThemePreset.DEFAULT_DARK.name)
            }.getOrDefault(ThemePreset.DEFAULT_DARK),
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name)
            }.getOrDefault(ThemeMode.DARK),
            accentColorOption = runCatching {
                AccentColorOption.valueOf(prefs[Keys.ACCENT_COLOR] ?: AccentColorOption.PURPLE.name)
            }.getOrDefault(AccentColorOption.PURPLE),
            nowPlayingVisualMode = runCatching {
                NowPlayingVisualMode.valueOf(
                    prefs[Keys.NOW_PLAYING_VISUAL_MODE] ?: NowPlayingVisualMode.DEFAULT.name
                )
            }.getOrDefault(NowPlayingVisualMode.DEFAULT),
            languageOption = runCatching {
                LanguageOption.valueOf(prefs[Keys.LANGUAGE_OPTION] ?: LanguageOption.SYSTEM.name)
            }.getOrDefault(LanguageOption.SYSTEM),
            lockScreenShowArtwork = prefs[Keys.LOCK_SCREEN_SHOW_ARTWORK] ?: true,
            lockScreenShowMediaInfo = prefs[Keys.LOCK_SCREEN_SHOW_MEDIA_INFO] ?: true,
            artworkQuality = runCatching {
                ArtworkQuality.valueOf(prefs[Keys.ARTWORK_QUALITY] ?: ArtworkQuality.AUTO.name)
            }.getOrDefault(ArtworkQuality.AUTO),
            lockScreenPrivacy = runCatching {
                LockScreenPrivacy.valueOf(prefs[Keys.LOCK_SCREEN_PRIVACY] ?: LockScreenPrivacy.NORMAL.name)
            }.getOrDefault(LockScreenPrivacy.NORMAL),
            nowPlayingGesturesEnabled = prefs[Keys.NOW_PLAYING_GESTURES_ENABLED] ?: true,
            shakeToPauseEnabled = prefs[Keys.SHAKE_TO_PAUSE_ENABLED] ?: false,
            lastViewedRoute = prefs[Keys.LAST_VIEWED_ROUTE] ?: "",
        )
    }

    suspend fun setLastViewedRoute(route: String) {
        context.dataStore.edit { it[Keys.LAST_VIEWED_ROUTE] = route }
    }

    suspend fun setShakeToPauseEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHAKE_TO_PAUSE_ENABLED] = enabled }
    }

    suspend fun addExcludedFolder(path: String) {
        context.dataStore.edit { it[Keys.EXCLUDED_FOLDERS] = (it[Keys.EXCLUDED_FOLDERS] ?: emptySet()) + path }
    }

    suspend fun removeExcludedFolder(path: String) {
        context.dataStore.edit { it[Keys.EXCLUDED_FOLDERS] = (it[Keys.EXCLUDED_FOLDERS] ?: emptySet()) - path }
    }

    suspend fun setShuffleDefault(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHUFFLE_DEFAULT] = enabled }
    }

    suspend fun setRepeatDefault(mode: RepeatMode) {
        context.dataStore.edit { it[Keys.REPEAT_DEFAULT] = mode.name }
    }

    suspend fun setCrossfadeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CROSSFADE_ENABLED] = enabled }
    }

    suspend fun setCrossfadeSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.CROSSFADE_SECONDS] = seconds.toString() }
    }

    suspend fun setNormalizationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NORMALIZATION_ENABLED] = enabled }
    }

    suspend fun setMiniPlayerCompact(compact: Boolean) {
        context.dataStore.edit { it[Keys.MINI_PLAYER_COMPACT] = compact }
    }

    suspend fun setSmartCrossfadeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SMART_CROSSFADE_ENABLED] = enabled }
    }

    suspend fun setFadeInOutEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FADE_IN_OUT_ENABLED] = enabled }
    }

    suspend fun setEqEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EQ_ENABLED] = enabled }
    }

    suspend fun setEqPreset(preset: EqPresetId) {
        context.dataStore.edit { it[Keys.EQ_PRESET] = preset.name }
    }

    suspend fun setEqCustomBands(bands: List<Int>) {
        context.dataStore.edit { it[Keys.EQ_CUSTOM_BANDS] = bands.joinToString(",") }
    }

    suspend fun setEqPreampMb(mb: Int) {
        context.dataStore.edit { it[Keys.EQ_PREAMP_MB] = mb }
    }

    suspend fun setBassBoostStrength(strength: Int) {
        context.dataStore.edit { it[Keys.BASS_BOOST_STRENGTH] = strength }
    }

    suspend fun setVirtualizerStrength(strength: Int) {
        context.dataStore.edit { it[Keys.VIRTUALIZER_STRENGTH] = strength }
    }

    suspend fun setLoudnessGainMb(mb: Int) {
        context.dataStore.edit { it[Keys.LOUDNESS_GAIN_MB] = mb }
    }

    suspend fun adjustLyricsOffset(deltaMs: Int) {
        context.dataStore.edit {
            it[Keys.LYRICS_OFFSET_MS] = (it[Keys.LYRICS_OFFSET_MS] ?: 0) + deltaMs
        }
    }

    suspend fun addExcludedFolders(paths: Collection<String>) {
        if (paths.isEmpty()) return
        context.dataStore.edit { it[Keys.EXCLUDED_FOLDERS] = (it[Keys.EXCLUDED_FOLDERS] ?: emptySet()) + paths }
    }

    /** Absolute setters used by backup/restore ([com.ozin.music.core.backup.BackupManager]),
     * which needs to replace a value outright rather than adjust it. */
    suspend fun replaceExcludedFolders(paths: Set<String>) {
        context.dataStore.edit { it[Keys.EXCLUDED_FOLDERS] = paths }
    }

    suspend fun setLyricsOffsetMs(offsetMs: Int) {
        context.dataStore.edit { it[Keys.LYRICS_OFFSET_MS] = offsetMs }
    }

    suspend fun setThemePreset(preset: ThemePreset) {
        context.dataStore.edit { it[Keys.THEME_PRESET] = preset.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentColorOption(option: AccentColorOption) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = option.name }
    }

    suspend fun setNowPlayingVisualMode(mode: NowPlayingVisualMode) {
        context.dataStore.edit { it[Keys.NOW_PLAYING_VISUAL_MODE] = mode.name }
    }

    suspend fun setLanguageOption(option: LanguageOption) {
        context.dataStore.edit { it[Keys.LANGUAGE_OPTION] = option.name }
    }

    suspend fun setLockScreenShowArtwork(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOCK_SCREEN_SHOW_ARTWORK] = enabled }
    }

    suspend fun setLockScreenShowMediaInfo(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOCK_SCREEN_SHOW_MEDIA_INFO] = enabled }
    }

    suspend fun setArtworkQuality(quality: ArtworkQuality) {
        context.dataStore.edit { it[Keys.ARTWORK_QUALITY] = quality.name }
    }

    suspend fun setLockScreenPrivacy(privacy: LockScreenPrivacy) {
        context.dataStore.edit { it[Keys.LOCK_SCREEN_PRIVACY] = privacy.name }
    }

    suspend fun setNowPlayingGesturesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOW_PLAYING_GESTURES_ENABLED] = enabled }
    }

    suspend fun setEqSuggestionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EQ_SUGGESTION_ENABLED] = enabled }
    }

    suspend fun setAutoDownloadLyricsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_DOWNLOAD_LYRICS_ENABLED] = enabled }
    }
}
