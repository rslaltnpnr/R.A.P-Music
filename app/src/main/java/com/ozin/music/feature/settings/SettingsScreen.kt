package com.ozin.music.feature.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.R
import com.ozin.music.core.settings.ArtworkQuality
import com.ozin.music.core.settings.LanguageOption
import com.ozin.music.core.settings.LockScreenPrivacy
import com.ozin.music.core.settings.RepeatMode
import com.ozin.music.core.ui.theme.AccentColorOption
import com.ozin.music.core.ui.theme.ThemeMode
import com.ozin.music.core.ui.theme.ThemePreset

@Composable
fun SettingsScreen(
    onOpenFolders: () -> Unit = {},
    onOpenProblemFiles: () -> Unit = {},
    onOpenDuplicates: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenBluetoothDevices: () -> Unit = {},
    onOpenRemoteServers: () -> Unit = {},
    onOpenSmartSearch: () -> Unit = {},
    onOpenDjMode: () -> Unit = {},
    onOpenDebugInfo: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    var folderInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val backupResult by viewModel.backupResult.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    LaunchedEffect(backupResult) {
        val result = backupResult ?: return@LaunchedEffect
        val message = when (result) {
            is BackupResult.ExportSuccess -> context.getString(R.string.backup_export_success)
            is BackupResult.ExportFailure -> context.getString(R.string.backup_export_failure, result.reason)
            is BackupResult.ImportSuccess -> context.getString(R.string.backup_import_success)
            is BackupResult.ImportFailure -> context.getString(R.string.backup_import_failure, result.reason)
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        viewModel.consumeBackupResult()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)

        SettingRow(stringResource(R.string.settings_crossfade), settings.crossfadeEnabled, viewModel::toggleCrossfade)
        if (settings.crossfadeEnabled) {
            Text(stringResource(R.string.settings_crossfade_duration), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 3, 5, 8).forEach { seconds ->
                    Button(onClick = { viewModel.setCrossfadeSeconds(seconds) }) {
                        Text(
                            if (settings.crossfadeSeconds == seconds) {
                                stringResource(R.string.settings_seconds_selected_format, seconds)
                            } else {
                                stringResource(R.string.settings_seconds_format, seconds)
                            }
                        )
                    }
                }
            }
            SettingRow(stringResource(R.string.settings_smart_crossfade), settings.smartCrossfadeEnabled, viewModel::toggleSmartCrossfade)
        }
        SettingRow(stringResource(R.string.settings_fade_in_out), settings.fadeInOutEnabled, viewModel::toggleFadeInOut)
        SettingRow(stringResource(R.string.settings_audio_normalization), settings.normalizationEnabled, viewModel::toggleNormalization)
        SettingRow(stringResource(R.string.settings_shuffle_default), settings.shuffleDefault, viewModel::toggleShuffleDefault)
        SettingRow(stringResource(R.string.settings_compact_mini_player), settings.miniPlayerCompact, viewModel::toggleMiniPlayerCompact)

        Text(stringResource(R.string.settings_default_repeat_mode), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RepeatMode.entries.forEach { mode ->
                val label = repeatModeLabel(mode)
                Button(onClick = { viewModel.setRepeatDefault(mode) }) {
                    Text(
                        if (settings.repeatDefault == mode) {
                            stringResource(R.string.settings_repeat_mode_selected_format, label)
                        } else {
                            label
                        }
                    )
                }
            }
        }

        Text(stringResource(R.string.settings_excluded_folders), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        settings.excludedFolders.forEach { folder ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(folder, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                Button(onClick = { viewModel.removeExcludedFolder(folder) }) { Text(stringResource(R.string.settings_remove)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = folderInput,
                onValueChange = { folderInput = it },
                placeholder = { Text(stringResource(R.string.settings_folder_path_placeholder)) },
                modifier = Modifier.weight(1f, fill = true),
            )
            Button(onClick = { viewModel.addExcludedFolder(folderInput); folderInput = "" }) { Text(stringResource(R.string.settings_exclude)) }
        }
        Button(onClick = onOpenFolders) { Text(stringResource(R.string.settings_manage_folders)) }

        Text(stringResource(R.string.settings_library_management), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenProblemFiles) { Text(stringResource(R.string.settings_problem_files)) }
            Button(onClick = onOpenDuplicates) { Text(stringResource(R.string.settings_find_duplicates)) }
        }

        Button(onClick = { viewModel.rescanLibrary() }) { Text(stringResource(R.string.settings_rescan_library)) }

        Text(stringResource(R.string.settings_personalization), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Button(onClick = onOpenStats) { Text(stringResource(R.string.settings_listening_statistics)) }
        Column {
            Button(onClick = { viewModel.computeMoodTags() }) { Text(stringResource(R.string.settings_compute_mood_tags)) }
            Text(
                stringResource(R.string.settings_mood_tags_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(stringResource(R.string.settings_smart_search), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Button(onClick = onOpenSmartSearch) { Text(stringResource(R.string.settings_search_by_keyword)) }

        Text(stringResource(R.string.settings_car_bluetooth), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Button(onClick = onOpenBluetoothDevices) { Text(stringResource(R.string.settings_bluetooth_device_profiles)) }

        Text(stringResource(R.string.nav_dj_mode), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Button(onClick = onOpenDjMode) { Text(stringResource(R.string.settings_open_dj_mode)) }

        Text(stringResource(R.string.settings_network), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Button(onClick = onOpenRemoteServers) { Text(stringResource(R.string.settings_network_servers)) }

        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemePreset.entries.forEach { preset ->
                val label = themePresetLabel(preset)
                Button(onClick = { viewModel.setThemePreset(preset) }) {
                    Text(
                        if (settings.themePreset == preset) {
                            stringResource(R.string.settings_theme_selected_format, label)
                        } else {
                            label
                        }
                    )
                }
            }
        }

        Text(stringResource(R.string.settings_theme_mode), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                val label = themeModeLabel(mode)
                Button(onClick = { viewModel.setThemeMode(mode) }) {
                    Text(
                        if (settings.themeMode == mode) {
                            stringResource(R.string.settings_theme_mode_selected_format, label)
                        } else {
                            label
                        }
                    )
                }
            }
        }

        Text(stringResource(R.string.settings_accent_color), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentColorOption.entries.forEach { option ->
                val selected = settings.accentColorOption == option
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(option.color)
                        .border(
                            width = if (selected) 3.dp else 0.dp,
                            color = if (selected) Color.White else Color.Transparent,
                            shape = CircleShape,
                        )
                        .clickable { viewModel.setAccentColorOption(option) },
                )
            }
        }

        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LanguageOption.entries.forEach { option ->
                val selected = settings.languageOption == option
                Button(onClick = { viewModel.setLanguageOption(option) }) {
                    Text(if (selected) "✓ ${languageOptionLabel(option)}" else languageOptionLabel(option))
                }
            }
        }

        Text(stringResource(R.string.settings_lock_screen), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        SettingRow(
            stringResource(R.string.settings_lock_screen_show_artwork),
            settings.lockScreenShowArtwork,
            viewModel::toggleLockScreenShowArtwork,
        )
        SettingRow(
            stringResource(R.string.settings_lock_screen_show_media_info),
            settings.lockScreenShowMediaInfo,
            viewModel::toggleLockScreenShowMediaInfo,
        )
        SettingRow(
            stringResource(R.string.settings_now_playing_gestures),
            settings.nowPlayingGesturesEnabled,
            viewModel::toggleNowPlayingGestures,
        )
        SettingRow(
            stringResource(R.string.settings_shake_to_pause),
            settings.shakeToPauseEnabled,
            viewModel::toggleShakeToPause,
        )

        Text(stringResource(R.string.settings_artwork_quality), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArtworkQuality.entries.forEach { quality ->
                val selected = settings.artworkQuality == quality
                Button(onClick = { viewModel.setArtworkQuality(quality) }) {
                    Text(if (selected) "✓ ${artworkQualityLabel(quality)}" else artworkQualityLabel(quality))
                }
            }
        }

        Text(stringResource(R.string.settings_lock_screen_privacy), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LockScreenPrivacy.entries.forEach { privacy ->
                val selected = settings.lockScreenPrivacy == privacy
                Button(onClick = { viewModel.setLockScreenPrivacy(privacy) }) {
                    Text(if (selected) "✓ ${lockScreenPrivacyLabel(privacy)}" else lockScreenPrivacyLabel(privacy))
                }
            }
        }

        Button(onClick = onOpenDebugInfo) { Text(stringResource(R.string.settings_debug_info)) }

        Text(stringResource(R.string.settings_backup_restore), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.settings_backup_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { exportLauncher.launch("rap_music_backup.json") }) {
                Text(stringResource(R.string.settings_backup))
            }
            Text(stringResource(R.string.settings_restore_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                Text(stringResource(R.string.settings_restore))
            }
        }

        Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                stringResource(R.string.settings_about_version_format, com.ozin.music.BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.settings_about_developer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onBackground)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun repeatModeLabel(mode: RepeatMode): String = when (mode) {
    RepeatMode.OFF -> stringResource(R.string.settings_repeat_off)
    RepeatMode.ALL -> stringResource(R.string.settings_repeat_all)
    RepeatMode.ONE -> stringResource(R.string.settings_repeat_one)
}

@Composable
private fun themePresetLabel(preset: ThemePreset): String = when (preset) {
    ThemePreset.DEFAULT_DARK -> stringResource(R.string.settings_theme_default_dark)
    ThemePreset.AMOLED -> stringResource(R.string.settings_theme_amoled)
    ThemePreset.NEON -> stringResource(R.string.settings_theme_neon)
    ThemePreset.RETRO -> stringResource(R.string.settings_theme_retro)
    ThemePreset.MINIMAL -> stringResource(R.string.settings_theme_minimal)
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.DARK -> stringResource(R.string.settings_theme_mode_dark)
    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_mode_light)
    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_mode_system)
}

@Composable
private fun languageOptionLabel(option: LanguageOption): String = when (option) {
    LanguageOption.SYSTEM -> stringResource(R.string.settings_language_system)
    LanguageOption.ENGLISH -> stringResource(R.string.settings_language_english)
    LanguageOption.TURKISH -> stringResource(R.string.settings_language_turkish)
}

@Composable
private fun artworkQualityLabel(quality: ArtworkQuality): String = when (quality) {
    ArtworkQuality.AUTO -> stringResource(R.string.settings_artwork_quality_auto)
    ArtworkQuality.HIGH -> stringResource(R.string.settings_artwork_quality_high)
    ArtworkQuality.BALANCED -> stringResource(R.string.settings_artwork_quality_balanced)
}

@Composable
private fun lockScreenPrivacyLabel(privacy: LockScreenPrivacy): String = when (privacy) {
    LockScreenPrivacy.NORMAL -> stringResource(R.string.settings_privacy_normal)
    LockScreenPrivacy.HIDE_ARTWORK -> stringResource(R.string.settings_privacy_hide_artwork)
    LockScreenPrivacy.HIDE_METADATA -> stringResource(R.string.settings_privacy_hide_metadata)
    LockScreenPrivacy.PRIVATE -> stringResource(R.string.settings_privacy_private)
}
