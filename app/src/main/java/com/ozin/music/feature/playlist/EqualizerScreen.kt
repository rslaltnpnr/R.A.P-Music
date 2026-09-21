package com.ozin.music.feature.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.core.player.EqPreset

@Composable
fun EqualizerScreen(viewModel: EqualizerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val sleepRemaining by viewModel.sleepRemainingMs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Equalizer", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Enabled", color = MaterialTheme.colorScheme.onBackground)
            Switch(checked = state.enabled, onCheckedChange = viewModel::setEnabled)
        }

        Text("Presets", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EqPreset.entries.forEach { preset ->
                Button(onClick = { viewModel.applyPreset(preset) }) {
                    Text(preset.name.replace('_', ' '))
                }
            }
        }

        Text("Sleep timer", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        if (sleepRemaining > 0) {
            Text("Stopping in ${sleepRemaining / 60000}m ${(sleepRemaining / 1000) % 60}s", color = MaterialTheme.colorScheme.primary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 30, 45, 60).forEach { minutes ->
                Button(onClick = { viewModel.startSleepTimer(minutes) }) { Text("${minutes}m") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.startSleepTimerEndOfTrack() }) { Text("End of track") }
            Button(onClick = { viewModel.cancelSleepTimer() }) { Text("Cancel") }
        }
    }
}
