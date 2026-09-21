package com.ozin.music.feature.files

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

/** Rename / Delete / Share / File Details for one song, via proper scoped-storage APIs. */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun FileManagementSheet(onDismiss: () -> Unit, viewModel: FileManagementViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.onConsentGranted() else viewModel.onConsentDenied()
    }
    LaunchedEffect(state.pendingConsent) {
        state.pendingConsent?.let { consentLauncher.launch(IntentSenderRequest.Builder(it).build()) }
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) onDismiss()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("File management", style = MaterialTheme.typography.titleMedium)

            val details = state.details
            if (details != null) {
                Text("Path: ${details.path}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                Text("Format: ${details.format}", style = MaterialTheme.typography.bodySmall)
                Text("Size: ${details.sizeBytes / 1024} KB", style = MaterialTheme.typography.bodySmall)
                if (details.bitrateKbps != null) Text("Bitrate: ${details.bitrateKbps} kbps", style = MaterialTheme.typography.bodySmall)
                if (details.sampleRateHz != null) Text("Sample rate: ${details.sampleRateHz} Hz", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Last modified: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(details.lastModifiedMs))}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            OutlinedTextField(
                value = state.newDisplayName,
                onValueChange = viewModel::onNewNameChanged,
                label = { Text("File name") },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                singleLine = true,
            )

            val errorMessage = state.error
            if (errorMessage != null) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::rename, enabled = !state.isBusy) { Text("Rename") }
                Button(
                    onClick = {
                        val song = state.song ?: return@Button
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", File(song.path),
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "audio/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share song"))
                    },
                ) { Text("Share") }
                Button(onClick = { showDeleteConfirm = true }, enabled = !state.isBusy) { Text("Delete") }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this file?") },
            text = { Text("This permanently deletes the audio file from your device.") },
            confirmButton = {
                Button(onClick = { showDeleteConfirm = false; viewModel.delete() }) { Text("Delete") }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
