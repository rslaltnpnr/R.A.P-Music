package com.ozin.music.feature.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.R
import com.ozin.music.core.settings.LanguageOption
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
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    var folderInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
        Button(onClick = onOpenDjMode) { Text(stringResource(R.string.nav_dj_mode)) }

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
