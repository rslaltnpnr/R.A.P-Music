package com.ozin.music.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.core.settings.RepeatMode

@Composable
fun SettingsScreen(
    onOpenFolders: () -> Unit = {},
    onOpenProblemFiles: () -> Unit = {},
    onOpenDuplicates: () -> Unit = {},
    onOpenStats: () -> Unit = {},
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
        Text("Settings", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)

        SettingRow("Crossfade", settings.crossfadeEnabled, viewModel::toggleCrossfade)
        if (settings.crossfadeEnabled) {
            Text("Crossfade duration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 3, 5, 8).forEach { seconds ->
                    Button(onClick = { viewModel.setCrossfadeSeconds(seconds) }) {
                        Text("${seconds}s" + if (settings.crossfadeSeconds == seconds) " ✓" else "")
                    }
                }
            }
            SettingRow("Smart crossfade (skip same album)", settings.smartCrossfadeEnabled, viewModel::toggleSmartCrossfade)
        }
        SettingRow("Fade in/out on play-pause", settings.fadeInOutEnabled, viewModel::toggleFadeInOut)
        SettingRow("Audio normalization", settings.normalizationEnabled, viewModel::toggleNormalization)
        SettingRow("Shuffle by default", settings.shuffleDefault, viewModel::toggleShuffleDefault)
        SettingRow("Compact mini player", settings.miniPlayerCompact, viewModel::toggleMiniPlayerCompact)

        Text("Default repeat mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RepeatMode.entries.forEach { mode ->
                Button(onClick = { viewModel.setRepeatDefault(mode) }) {
                    Text(mode.name + if (settings.repeatDefault == mode) " ✓" else "")
                }
            }
        }

        Text("Excluded folders", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        settings.excludedFolders.forEach { folder ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(folder, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                Button(onClick = { viewModel.removeExcludedFolder(folder) }) { Text("Remove") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = folderInput,
                onValueChange = { folderInput = it },
                placeholder = { Text("/storage/emulated/0/...") },
                modifier = Modifier.weight(1f, fill = true),
            )
            Button(onClick = { viewModel.addExcludedFolder(folderInput); folderInput = "" }) { Text("Exclude") }
        }
        Button(onClick = onOpenFolders) { Text("Manage folders") }

        Text("Library management", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenProblemFiles) { Text("Problem files") }
            Button(onClick = onOpenDuplicates) { Text("Find duplicates") }
        }

        Button(onClick = { viewModel.rescanLibrary() }) { Text("Rescan library") }

        Text("Personalization", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Button(onClick = onOpenStats) { Text("Listening statistics") }
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
