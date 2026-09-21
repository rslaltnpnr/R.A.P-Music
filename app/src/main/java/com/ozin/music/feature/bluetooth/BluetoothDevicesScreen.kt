package com.ozin.music.feature.bluetooth

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import com.ozin.music.core.domain.EqPresetId

@Composable
fun BluetoothDevicesScreen(viewModel: BluetoothDevicesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var editingAddress by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onPermissionResult(granted) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Bluetooth devices", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Set an EQ preset, crossfade and autoplay for a paired device. Applied automatically when it connects.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!state.permissionGranted && Build.VERSION.SDK_INT >= 31) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bluetooth permission needed to list paired devices.")
                    Button(onClick = { permissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT) }) {
                        Text("Grant permission")
                    }
                }
            }
        }

        if (state.permissionGranted && state.devices.isEmpty()) {
            Text("No paired devices found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        state.devices.forEach { row ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(row.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            row.profile?.let { "EQ ${it.eqPresetId} • Crossfade ${it.crossfadeSeconds}s • Autoplay ${if (it.autoplayEnabled) "on" else "off"}" }
                                ?: "No profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = { editingAddress = row.address }) { Text("Edit") }
                }
            }
        }
    }

    val editingRow = state.devices.firstOrNull { it.address == editingAddress }
    if (editingRow != null) {
        DeviceProfileDialog(
            row = editingRow,
            onDismiss = { editingAddress = null },
            onSave = { preset, crossfade, autoplay ->
                viewModel.saveProfile(editingRow.address, editingRow.name, preset, crossfade, autoplay)
                editingAddress = null
            },
            onRemove = {
                viewModel.deleteProfile(editingRow.address)
                editingAddress = null
            },
        )
    }
}

@Composable
private fun DeviceProfileDialog(
    row: BluetoothDeviceRow,
    onDismiss: () -> Unit,
    onSave: (EqPresetId, Int, Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    var preset by remember {
        mutableStateOf(row.profile?.let { p -> runCatching { EqPresetId.valueOf(p.eqPresetId) }.getOrNull() } ?: EqPresetId.NORMAL)
    }
    var crossfadeSeconds by remember { mutableStateOf(row.profile?.crossfadeSeconds ?: 0) }
    var autoplay by remember { mutableStateOf(row.profile?.autoplayEnabled ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(row.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("EQ preset", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EqPresetId.entries.filter { it != EqPresetId.CUSTOM }.forEach { candidate ->
                        Button(onClick = { preset = candidate }) {
                            Text(candidate.name + if (preset == candidate) " ✓" else "")
                        }
                    }
                }
                Text("Crossfade", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 2, 3, 5, 8).forEach { seconds ->
                        Button(onClick = { crossfadeSeconds = seconds }) {
                            Text("${seconds}s" + if (crossfadeSeconds == seconds) " ✓" else "")
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Autoplay on connect")
                    Switch(checked = autoplay, onCheckedChange = { autoplay = it })
                }
                Button(onClick = onRemove) { Text("Remove profile") }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(preset, crossfadeSeconds, autoplay) }) { Text("Save") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
