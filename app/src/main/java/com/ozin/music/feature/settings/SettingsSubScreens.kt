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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.R
import com.ozin.music.core.settings.ArtworkQuality
import com.ozin.music.core.settings.LanguageOption
import com.ozin.music.core.settings.LockScreenPrivacy
import com.ozin.music.core.settings.RepeatMode
import com.ozin.music.core.ui.components.ChoiceChip
import com.ozin.music.core.ui.components.SettingsCard
import com.ozin.music.core.ui.components.SettingsSubScreen
import com.ozin.music.core.ui.theme.AccentColorOption
import com.ozin.music.core.ui.theme.Spacing
import com.ozin.music.core.ui.theme.ThemeMode
import com.ozin.music.core.ui.theme.ThemePreset

@Composable
fun PlaybackSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()
    val loudnessProgress by viewModel.loudnessAnalysisProgress.collectAsState()

    // Real, working cancellation: if the user navigates away from this
    // screen mid-analysis, the background Job driving it is cancelled here
    // rather than left running unattended.
    DisposableEffect(Unit) {
        onDispose { viewModel.stopLoudnessAnalysis() }
    }

    SettingsSubScreen(
        title = stringResource(R.string.settings_section_playback),
        onBack = onBack,
        backContentDescription = stringResource(R.string.settings_back),
    ) {
        SettingsCard {
            SettingRow(stringResource(R.string.settings_crossfade), settings.crossfadeEnabled, viewModel::toggleCrossfade)
            if (settings.crossfadeEnabled) {
                Text(stringResource(R.string.settings_crossfade_duration), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    listOf(2, 3, 5, 8).forEach { seconds ->
                        ChoiceChip(
                            label = stringResource(R.string.settings_seconds_format, seconds),
                            selected = settings.crossfadeSeconds == seconds,
                            onClick = { viewModel.setCrossfadeSeconds(seconds) },
                        )
                    }
                }
                SettingRow(stringResource(R.string.settings_smart_crossfade), settings.smartCrossfadeEnabled, viewModel::toggleSmartCrossfade)
            }
            SettingRow(stringResource(R.string.settings_fade_in_out), settings.fadeInOutEnabled, viewModel::toggleFadeInOut)
            SettingRow(stringResource(R.string.settings_audio_normalization), settings.normalizationEnabled, viewModel::toggleNormalization)
            SettingRow(stringResource(R.string.settings_shuffle_default), settings.shuffleDefault, viewModel::toggleShuffleDefault)
            SettingRow(stringResource(R.string.settings_compact_mini_player), settings.miniPlayerCompact, viewModel::toggleMiniPlayerCompact)

            Text(stringResource(R.string.settings_default_repeat_mode), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                RepeatMode.entries.forEach { mode ->
                    ChoiceChip(
                        label = repeatModeLabel(mode),
                        selected = settings.repeatDefault == mode,
                        onClick = { viewModel.setRepeatDefault(mode) },
                    )
                }
            }
        }

        SettingsCard {
            Text(
                stringResource(R.string.settings_analyze_loudness),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                stringResource(R.string.settings_analyze_loudness_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val progress = loudnessProgress
            if (progress != null && progress.running) {
                Text(
                    stringResource(R.string.settings_analyze_loudness_progress, progress.processed, progress.total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { viewModel.stopLoudnessAnalysis() }) {
                    Text(stringResource(R.string.settings_analyze_loudness_stop))
                }
            } else {
                Button(onClick = { viewModel.startLoudnessAnalysis() }) {
                    Text(stringResource(R.string.settings_analyze_loudness_start))
                }
            }
        }
    }
}

@Composable
fun LibrarySettingsScreen(
    onBack: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenProblemFiles: () -> Unit,
    onOpenDuplicates: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    var folderInput by remember { mutableStateOf("") }
    SettingsSubScreen(
        title = stringResource(R.string.settings_section_library),
        onBack = onBack,
        backContentDescription = stringResource(R.string.settings_back),
    ) {
        SettingsCard {
            Text(stringResource(R.string.settings_excluded_folders), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            settings.excludedFolders.forEach { folder ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(folder, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = Spacing.xs))
                    Button(onClick = { viewModel.removeExcludedFolder(folder) }) { Text(stringResource(R.string.settings_remove)) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = folderInput,
                    onValueChange = { folderInput = it },
                    placeholder = { Text(stringResource(R.string.settings_folder_path_placeholder)) },
                    modifier = Modifier.weight(1f, fill = true),
                )
                Button(onClick = { viewModel.addExcludedFolder(folderInput); folderInput = "" }) { Text(stringResource(R.string.settings_exclude)) }
            }
            Button(onClick = onOpenFolders) { Text(stringResource(R.string.settings_manage_folders)) }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(onClick = onOpenProblemFiles) { Text(stringResource(R.string.settings_problem_files)) }
                Button(onClick = onOpenDuplicates) { Text(stringResource(R.string.settings_find_duplicates)) }
            }
            Button(onClick = { viewModel.rescanLibrary() }) { Text(stringResource(R.string.settings_rescan_library)) }
        }
    }
}

@Composable
fun PersonalizationSettingsScreen(
    onBack: () -> Unit,
    onOpenStats: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    SettingsSubScreen(
        title = stringResource(R.string.settings_section_personalization),
        onBack = onBack,
        backContentDescription = stringResource(R.string.settings_back),
    ) {
        SettingsCard {
            Button(onClick = onOpenStats) { Text(stringResource(R.string.settings_listening_statistics)) }
            Column {
                Button(onClick = { viewModel.computeMoodTags() }) { Text(stringResource(R.string.settings_compute_mood_tags)) }
                Text(
                    stringResource(R.string.settings_mood_tags_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ThemePreset.entries.forEach { preset ->
                    ChoiceChip(
                        label = themePresetLabel(preset),
                        selected = settings.themePreset == preset,
                        onClick = { viewModel.setThemePreset(preset) },
                    )
                }
            }

            Text(stringResource(R.string.settings_theme_mode), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ThemeMode.entries.forEach { mode ->
                    ChoiceChip(
                        label = themeModeLabel(mode),
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                    )
                }
            }

            Text(stringResource(R.string.settings_accent_color), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                LanguageOption.entries.forEach { option ->
                    ChoiceChip(
                        label = languageOptionLabel(option),
                        selected = settings.languageOption == option,
                        onClick = { viewModel.setLanguageOption(option) },
                    )
                }
            }
        }
    }
}

@Composable
fun LockScreenSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()
    SettingsSubScreen(
        title = stringResource(R.string.settings_lock_screen),
        onBack = onBack,
        backContentDescription = stringResource(R.string.settings_back),
    ) {
        SettingsCard {
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

            Text(stringResource(R.string.settings_artwork_quality), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ArtworkQuality.entries.forEach { quality ->
                    ChoiceChip(
                        label = artworkQualityLabel(quality),
                        selected = settings.artworkQuality == quality,
                        onClick = { viewModel.setArtworkQuality(quality) },
                    )
                }
            }

            Text(stringResource(R.string.settings_lock_screen_privacy), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                LockScreenPrivacy.entries.forEach { privacy ->
                    ChoiceChip(
                        label = lockScreenPrivacyLabel(privacy),
                        selected = settings.lockScreenPrivacy == privacy,
                        onClick = { viewModel.setLockScreenPrivacy(privacy) },
                    )
                }
            }
        }
    }
}

@Composable
fun NowPlayingSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()
    SettingsSubScreen(
        title = stringResource(R.string.settings_section_now_playing),
        onBack = onBack,
        backContentDescription = stringResource(R.string.settings_back),
    ) {
        SettingsCard {
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
        }
    }
}

@Composable
fun BackupRestoreSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
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

    SettingsSubScreen(
        title = stringResource(R.string.settings_backup_restore),
        onBack = onBack,
        backContentDescription = stringResource(R.string.settings_back),
    ) {
        SettingsCard {
            Text(stringResource(R.string.settings_backup_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { exportLauncher.launch("rap_music_backup.json") }) {
                Text(stringResource(R.string.settings_backup))
            }
            Text(stringResource(R.string.settings_restore_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                Text(stringResource(R.string.settings_restore))
            }
        }
    }
}

@Composable
fun AboutSettingsScreen(onBack: () -> Unit, onOpenDebugInfo: () -> Unit) {
    SettingsSubScreen(
        title = stringResource(R.string.settings_about),
        onBack = onBack,
        backContentDescription = stringResource(R.string.settings_back),
    ) {
        SettingsCard {
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
            Button(onClick = onOpenDebugInfo) { Text(stringResource(R.string.settings_debug_info)) }
        }
    }
}

@Composable
internal fun SettingRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onBackground)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
internal fun repeatModeLabel(mode: RepeatMode): String = when (mode) {
    RepeatMode.OFF -> stringResource(R.string.settings_repeat_off)
    RepeatMode.ALL -> stringResource(R.string.settings_repeat_all)
    RepeatMode.ONE -> stringResource(R.string.settings_repeat_one)
}

@Composable
internal fun themePresetLabel(preset: ThemePreset): String = when (preset) {
    ThemePreset.DEFAULT_DARK -> stringResource(R.string.settings_theme_default_dark)
    ThemePreset.AMOLED -> stringResource(R.string.settings_theme_amoled)
    ThemePreset.NEON -> stringResource(R.string.settings_theme_neon)
    ThemePreset.RETRO -> stringResource(R.string.settings_theme_retro)
    ThemePreset.MINIMAL -> stringResource(R.string.settings_theme_minimal)
}

@Composable
internal fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.DARK -> stringResource(R.string.settings_theme_mode_dark)
    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_mode_light)
    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_mode_system)
}

@Composable
internal fun languageOptionLabel(option: LanguageOption): String = when (option) {
    LanguageOption.SYSTEM -> stringResource(R.string.settings_language_system)
    LanguageOption.ENGLISH -> stringResource(R.string.settings_language_english)
    LanguageOption.TURKISH -> stringResource(R.string.settings_language_turkish)
}

@Composable
internal fun artworkQualityLabel(quality: ArtworkQuality): String = when (quality) {
    ArtworkQuality.AUTO -> stringResource(R.string.settings_artwork_quality_auto)
    ArtworkQuality.HIGH -> stringResource(R.string.settings_artwork_quality_high)
    ArtworkQuality.BALANCED -> stringResource(R.string.settings_artwork_quality_balanced)
}

@Composable
internal fun lockScreenPrivacyLabel(privacy: LockScreenPrivacy): String = when (privacy) {
    LockScreenPrivacy.NORMAL -> stringResource(R.string.settings_privacy_normal)
    LockScreenPrivacy.HIDE_ARTWORK -> stringResource(R.string.settings_privacy_hide_artwork)
    LockScreenPrivacy.HIDE_METADATA -> stringResource(R.string.settings_privacy_hide_metadata)
    LockScreenPrivacy.PRIVATE -> stringResource(R.string.settings_privacy_private)
}
