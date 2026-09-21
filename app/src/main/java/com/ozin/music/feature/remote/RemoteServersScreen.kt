package com.ozin.music.feature.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.core.data.model.RemoteServer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteServersScreen(
    onBack: () -> Unit,
    onOpenServer: (Long) -> Unit,
    viewModel: RemoteServersViewModel = hiltViewModel(),
) {
    val servers by viewModel.servers.collectAsState()
    val testResults by viewModel.testResults.collectAsState()
    var showAddForm by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf<RemoteServer?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network servers") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Add a WebDAV server to browse and play music stored on your local network. " +
                    "SMB and Chromecast are not supported yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(servers, key = { it.id }) { server ->
                    RemoteServerRow(
                        server = server,
                        testStatus = testResults[server.id],
                        onOpen = { onOpenServer(server.id) },
                        onTest = { viewModel.testConnection(server.id) },
                        onEdit = { editingServer = server },
                        onDelete = { viewModel.deleteServer(server.id) },
                    )
                }
            }

            if (showAddForm) {
                ServerForm(
                    title = "Add server",
                    initial = null,
                    onCancel = { showAddForm = false },
                    onSave = { name, address, username, password ->
                        viewModel.addServer(name, address, username, password)
                        showAddForm = false
                    },
                )
            } else {
                Button(onClick = { showAddForm = true }) { Text("Add WebDAV server") }
            }

            editingServer?.let { server ->
                ServerForm(
                    title = "Edit server",
                    initial = server,
                    onCancel = { editingServer = null },
                    onSave = { name, address, username, password ->
                        viewModel.updateServer(server.id, name, address, username, password.ifBlank { null })
                        editingServer = null
                    },
                )
            }
        }
    }
}

@Composable
private fun RemoteServerRow(
    server: RemoteServer,
    testStatus: ConnectionTestStatus?,
    onOpen: () -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Cloud, contentDescription = null)
                Text(server.name, style = MaterialTheme.typography.titleMedium)
            }
            Text(server.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (testStatus) {
                ConnectionTestStatus.TESTING -> Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Text("Testing connection…", style = MaterialTheme.typography.bodySmall)
                }
                ConnectionTestStatus.SUCCESS -> Text(
                    "Connection successful",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
                ConnectionTestStatus.FAILURE -> Text(
                    "Connection failed",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                null -> {}
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpen) { Text("Browse") }
                Button(onClick = onTest) { Text("Test") }
                Button(onClick = onEdit) { Text("Edit") }
                Button(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun ServerForm(
    title: String,
    initial: RemoteServer?,
    onCancel: () -> Unit,
    onSave: (name: String, address: String, username: String, password: String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var address by remember { mutableStateOf(initial?.address.orEmpty()) }
    var username by remember { mutableStateOf(initial?.username.orEmpty()) }
    var password by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address (https://host:port/dav)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (initial != null) "Password (leave blank to keep)" else "Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSave(name, address, username, password) }) { Text("Save") }
                Button(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}
