package com.ozin.music.feature.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.core.domain.EqPresetId

@Composable
fun EqualizerScreen(viewModel: EqualizerViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()
    val sleepRemaining by viewModel.sleepRemainingMs.collectAsState()
    val bandCount = viewModel.bandCount()
    val range = viewModel.bandLevelRange()

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
            Switch(checked = settings.eqEnabled, onCheckedChange = viewModel::setEnabled)
        }

        Text("Presets", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(EqPresetId.entries, key = { it.name }) { preset ->
                Button(onClick = { viewModel.applyPreset(preset) }) {
                    Text(preset.name.replace('_', ' ') + if (settings.eqPreset == preset) " ✓" else "")
                }
            }
        }

        if (bandCount > 0) {
            Text("Bands", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            for (band in 0 until bandCount) {
                val freq = viewModel.centerFrequencyHz(band)
                val level = viewModel.currentBandLevel(band)
                Column {
                    Text("${freq} Hz", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = level.toFloat(),
                        onValueChange = { viewModel.setBandLevel(band, it.toInt().toShort()) },
                        valueRange = range[0].toFloat()..range[1].toFloat(),
                    )
                }
            }
        } else {
            Text(
                "Equalizer bands appear once playback has started.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text("Preamp", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Slider(
            value = settings.eqPreampMb.toFloat(),
            onValueChange = { viewModel.setPreamp(it.toInt()) },
            valueRange = -1500f..1500f,
        )

        Text("Bass boost", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Slider(
            value = settings.bassBoostStrength.toFloat(),
            onValueChange = { viewModel.setBassBoost(it.toInt()) },
            valueRange = 0f..1000f,
        )

        Text("Virtualizer (surround)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Slider(
            value = settings.virtualizerStrength.toFloat(),
            onValueChange = { viewModel.setVirtualizer(it.toInt()) },
            valueRange = 0f..1000f,
        )

        Text("Loudness gain", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Slider(
            value = settings.loudnessGainMb.toFloat(),
            onValueChange = { viewModel.setLoudnessGain(it.toInt()) },
            valueRange = 0f..2000f,
        )

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
