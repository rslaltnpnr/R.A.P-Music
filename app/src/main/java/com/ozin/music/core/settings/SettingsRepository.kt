package com.ozin.music.core.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "ozin_settings")

enum class RepeatMode { OFF, ALL, ONE }

data class AppSettings(
    val excludedFolders: Set<String> = emptySet(),
    val shuffleDefault: Boolean = false,
    val repeatDefault: RepeatMode = RepeatMode.OFF,
    val crossfadeEnabled: Boolean = false,
    val crossfadeSeconds: Int = 4,
    val normalizationEnabled: Boolean = false,
    val miniPlayerCompact: Boolean = false,
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
            normalizationEnabled = prefs[Keys.NORMALIZATION_ENABLED] ?: false,
            miniPlayerCompact = prefs[Keys.MINI_PLAYER_COMPACT] ?: false,
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
}
