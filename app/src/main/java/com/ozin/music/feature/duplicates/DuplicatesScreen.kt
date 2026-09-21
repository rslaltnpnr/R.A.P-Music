package com.ozin.music.feature.duplicates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.ozin.music.core.domain.DuplicateGroup

@Composable
fun DuplicatesScreen(viewModel: DuplicatesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var groupPendingConfirm by remember { mutableStateOf<DuplicateGroup?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Text("Duplicate songs", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = viewModel::scan, modifier = Modifier.padding(vertical = 12.dp)) {
            Text(if (state.isScanning) "Scanning…" else "Scan for duplicates")
        }

        if (state.hasScanned && state.groups.isEmpty()) {
            Text("No likely duplicates found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        LazyColumn {
            items(state.groups) { group ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(group.songs.first().title, style = MaterialTheme.typography.titleSmall)
                    group.songs.forEach { song ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(song.path, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Button(onClick = { viewModel.deleteOne(song.id) }) { Text("Delete") }
                        }
                    }
                    Button(onClick = { groupPendingConfirm = group }, modifier = Modifier.padding(top = 4.dp)) {
                        Text("Keep first, delete rest")
                    }
                }
            }
        }
    }

    val pending = groupPendingConfirm
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { groupPendingConfirm = null },
            title = { Text("Delete ${pending.songs.size - 1} duplicate(s)?") },
            text = { Text("Keeps \"${pending.songs.first().path}\" and permanently deletes the rest.") },
            confirmButton = {
                Button(onClick = { viewModel.keepFirstDeleteRest(pending); groupPendingConfirm = null }) { Text("Delete") }
            },
            dismissButton = { Button(onClick = { groupPendingConfirm = null }) { Text("Cancel") } },
        )
    }
}
