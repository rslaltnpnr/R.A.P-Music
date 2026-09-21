package com.ozin.music.feature.metadata

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MetadataEditScreen(onBack: () -> Unit, viewModel: MetadataEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.onConsentGranted() else viewModel.onConsentDenied()
    }
    LaunchedEffect(state.pendingConsent) {
        state.pendingConsent?.let { sender ->
            consentLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            if (bytes != null) viewModel.onAlbumArtPicked(bytes)
        }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text("Edit song info", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp, top = 12.dp))
        }

        EditField("Title", state.title) { viewModel.onFieldChange(MetadataField.TITLE, it) }
        EditField("Artist", state.artist) { viewModel.onFieldChange(MetadataField.ARTIST, it) }
        EditField("Album", state.album) { viewModel.onFieldChange(MetadataField.ALBUM, it) }
        EditField("Album artist", state.albumArtist) { viewModel.onFieldChange(MetadataField.ALBUM_ARTIST, it) }
        EditField("Genre", state.genre) { viewModel.onFieldChange(MetadataField.GENRE, it) }
        EditField("Year", state.year) { viewModel.onFieldChange(MetadataField.YEAR, it) }
        EditField("Track number", state.trackNumber) { viewModel.onFieldChange(MetadataField.TRACK, it) }
        EditField("Disc number", state.discNumber) { viewModel.onFieldChange(MetadataField.DISC, it) }
        EditField("Comment", state.comment) { viewModel.onFieldChange(MetadataField.COMMENT, it) }

        Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.padding(top = 12.dp)) {
            Text("Replace album art")
        }

        if (state.error != null) {
            Text(state.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }
        val path = state.song?.path.orEmpty()
        if (!path.endsWith(".mp3", ignoreCase = true) && path.isNotBlank()) {
            Text(
                "Note: only MediaStore fields (title/artist/album/genre/year/track) are written for this file format. " +
                    "Embedded on-disk tag writing in this app is currently MP3-only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = viewModel::save, enabled = !state.isSaving) {
                Text(if (state.isSaving) "Saving…" else "Save")
            }
        }
    }
}

@Composable
private fun EditField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true,
    )
}
