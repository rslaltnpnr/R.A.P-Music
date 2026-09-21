package com.ozin.music.feature.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.R
import com.ozin.music.core.data.model.CustomEqPreset
import com.ozin.music.core.domain.EqPresetId
import com.ozin.music.core.ui.components.ChoiceChip
import com.ozin.music.core.ui.components.SectionHeader
import com.ozin.music.core.ui.components.SettingsCard
import com.ozin.music.core.ui.theme.Spacing

@Composable
fun EqualizerScreen(viewModel: EqualizerViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()
    val sleepRemaining by viewModel.sleepRemainingMs.collectAsState()
    val customPresets by viewModel.customPresets.collectAsState()
    val bandCount = viewModel.bandCount()
    val range = viewModel.bandLevelRange()
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            stringResource(R.string.eq_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.eq_enabled), color = MaterialTheme.colorScheme.onBackground)
                Switch(checked = settings.eqEnabled, onCheckedChange = viewModel::setEnabled)
            }
        }

        SectionHeader(title = stringResource(R.string.eq_presets))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(EqPresetId.entries, key = { it.name }) { preset ->
                ChoiceChip(
                    label = preset.name.replace('_', ' '),
                    selected = settings.eqPreset == preset,
                    onClick = { viewModel.applyPreset(preset) },
                )
            }
        }

        SectionHeader(title = stringResource(R.string.eq_bands))
        if (bandCount > 0) {
            SettingsCard {
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
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    stringResource(R.string.eq_bands_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.lg),
                )
            }
        }

        SettingsCard {
            Text(stringResource(R.string.eq_preamp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Slider(
                value = settings.eqPreampMb.toFloat(),
                onValueChange = { viewModel.setPreamp(it.toInt()) },
                valueRange = -1500f..1500f,
            )

            Text(stringResource(R.string.eq_bass_boost), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Slider(
                value = settings.bassBoostStrength.toFloat(),
                onValueChange = { viewModel.setBassBoost(it.toInt()) },
                valueRange = 0f..1000f,
            )

            Text(stringResource(R.string.eq_virtualizer), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Slider(
                value = settings.virtualizerStrength.toFloat(),
                onValueChange = { viewModel.setVirtualizer(it.toInt()) },
                valueRange = 0f..1000f,
            )

            Text(stringResource(R.string.eq_loudness_gain), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Slider(
                value = settings.loudnessGainMb.toFloat(),
                onValueChange = { viewModel.setLoudnessGain(it.toInt()) },
                valueRange = 0f..2000f,
            )
        }

        SectionHeader(title = stringResource(R.string.eq_custom_presets))
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.eq_custom_presets), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Button(onClick = { showSaveDialog = true }) { Text(stringResource(R.string.eq_save_as)) }
            }
            if (customPresets.isEmpty()) {
                Text(stringResource(R.string.eq_no_custom_presets), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                for (preset in customPresets) {
                    CustomPresetRow(
                        preset = preset,
                        onLoad = { viewModel.loadCustomPreset(preset) },
                        onDelete = { viewModel.deleteCustomPreset(preset) },
                    )
                }
            }
        }

        SectionHeader(title = stringResource(R.string.eq_sleep_timer))
        SettingsCard {
            if (sleepRemaining > 0) {
                Text(
                    stringResource(
                        R.string.eq_sleep_timer_remaining_format,
                        sleepRemaining / 60000,
                        (sleepRemaining / 1000) % 60,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf(15, 30, 45, 60).forEach { minutes ->
                    ChoiceChip(
                        label = stringResource(R.string.eq_sleep_timer_minutes_format, minutes),
                        selected = false,
                        onClick = { viewModel.startSleepTimer(minutes) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(onClick = { viewModel.startSleepTimerEndOfTrack() }) { Text(stringResource(R.string.eq_sleep_timer_end_of_track)) }
                Button(onClick = { viewModel.cancelSleepTimer() }) { Text(stringResource(R.string.eq_sleep_timer_cancel)) }
            }
        }
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.eq_save_preset_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.eq_preset_name_placeholder)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveCurrentAsCustomPreset(name)
                        showSaveDialog = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text(stringResource(R.string.eq_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text(stringResource(R.string.eq_cancel)) }
            },
        )
    }
}

@Composable
private fun CustomPresetRow(
    preset: CustomEqPreset,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Button(onClick = onLoad, modifier = Modifier.weight(1f)) {
            Text(preset.name)
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.eq_delete_preset),
            )
        }
    }
}
