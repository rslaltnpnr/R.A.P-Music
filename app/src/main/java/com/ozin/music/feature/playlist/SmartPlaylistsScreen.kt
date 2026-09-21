package com.ozin.music.feature.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.core.data.model.SmartPlaylist
import com.ozin.music.core.domain.SmartRuleField
import com.ozin.music.core.domain.SmartRuleOperator
import com.ozin.music.core.domain.SmartPlaylistRule

@Composable
fun SmartPlaylistsScreen(
    onSmartPlaylistClick: (Long) -> Unit,
    viewModel: SmartPlaylistViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New smart playlist")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (playlists.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("No smart playlists yet. Tap + to create one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(playlists, key = { it.id }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSmartPlaylistClick(playlist.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(playlist.name, color = MaterialTheme.colorScheme.onBackground)
                            if (playlist.isBuiltIn) {
                                Text("Built-in", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        IconButton(onClick = { viewModel.delete(playlist) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateSmartPlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, rules -> viewModel.create(name, rules); showCreateDialog = false },
        )
    }
}

@Composable
private fun CreateSmartPlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String, List<SmartPlaylistRule>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf(listOf<SmartPlaylistRule>()) }
    var field by remember { mutableStateOf(SmartRuleField.GENRE) }
    var operator by remember { mutableStateOf(SmartRuleOperator.CONTAINS) }
    var value by remember { mutableStateOf("") }
    var fieldMenuOpen by remember { mutableStateOf(false) }
    var operatorMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New smart playlist") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    "Rules (all must match)",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                rules.forEachIndexed { index, rule ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${rule.field.name} ${rule.operator.name} \"${rule.value}\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { rules = rules.toMutableList().also { it.removeAt(index) } }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove rule")
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Text(
                            text = field.name,
                            modifier = Modifier.clickable { fieldMenuOpen = true }.padding(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        DropdownMenu(expanded = fieldMenuOpen, onDismissRequest = { fieldMenuOpen = false }) {
                            SmartRuleField.entries.forEach { f ->
                                DropdownMenuItem(text = { Text(f.name) }, onClick = { field = f; fieldMenuOpen = false })
                            }
                        }
                    }
                    Box {
                        Text(
                            text = operator.name,
                            modifier = Modifier.clickable { operatorMenuOpen = true }.padding(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        DropdownMenu(expanded = operatorMenuOpen, onDismissRequest = { operatorMenuOpen = false }) {
                            SmartRuleOperator.entries.forEach { op ->
                                DropdownMenuItem(text = { Text(op.name) }, onClick = { operator = op; operatorMenuOpen = false })
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = { Text("Value") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        if (value.isNotBlank()) {
                            rules = rules + SmartPlaylistRule(field, operator, value.trim())
                            value = ""
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Add rule") }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, rules) }, enabled = name.isNotBlank() && rules.isNotEmpty()) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun SmartPlaylistBanner(playlist: SmartPlaylist?) {
    if (playlist?.isBuiltIn == true) {
        Text(
            "Built-in smart playlist",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}
