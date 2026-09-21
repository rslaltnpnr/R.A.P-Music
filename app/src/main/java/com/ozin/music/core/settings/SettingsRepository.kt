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
 * default view (Phase 9). */
enum class NowPlayingVisualMode { DEFAULT, VINYL, CASSETTE, VISUALIZER }

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
    // Personalization (Phase 9)
    val themePreset: ThemePreset = ThemePreset.DEFAULT_DARK,
    val accentColorOption: AccentColorOption = AccentColorOption.PURPLE,
    val nowPlayingVisualMode: NowPlayingVisualMode = NowPlayingVisualMode.DEFAULT,
    val languageOption: LanguageOption = LanguageOption.SYSTEM,
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
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val NOW_PLAYING_VISUAL_MODE = stringPreferencesKey("now_playing_visual_mode")
        val LANGUAGE_OPTION = stringPreferencesKey("language_option")
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
            themePreset = runCatching {
                ThemePreset.valueOf(prefs[Keys.THEME_PRESET] ?: ThemePreset.DEFAULT_DARK.name)
            }.getOrDefault(ThemePreset.DEFAULT_DARK),
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
        )
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

    suspend fun setThemePreset(preset: ThemePreset) {
        context.dataStore.edit { it[Keys.THEME_PRESET] = preset.name }
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
}
