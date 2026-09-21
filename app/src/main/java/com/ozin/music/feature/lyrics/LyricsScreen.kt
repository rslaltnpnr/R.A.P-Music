package com.ozin.music.feature.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LyricsScreen(onBack: () -> Unit, viewModel: LyricsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("Lyrics", style = MaterialTheme.typography.titleMedium)
                    if (state.songTitle.isNotBlank()) {
                        Text(state.songTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (!state.isEditing) {
                IconButton(onClick = viewModel::startEditing) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit lyrics")
                }
            }
        }

        if (state.isEditing) {
            LyricsEditor(state, viewModel)
            return@Column
        }

        if (state.hasSyncedLyrics) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Sync offset: ${state.offsetMs} ms", style = MaterialTheme.typography.labelMedium)
                Button(onClick = { viewModel.adjustOffset(-100) }, modifier = Modifier.padding(start = 12.dp)) { Text("-100ms") }
                Button(onClick = { viewModel.adjustOffset(100) }, modifier = Modifier.padding(start = 8.dp)) { Text("+100ms") }
            }

            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            val currentIndex = state.lines.indexOf(state.currentLine).coerceAtLeast(0)
            LaunchedEffect(currentIndex) {
                listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0))
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(state.lines) { line ->
                    val isCurrent = line == state.currentLine
                    Text(
                        text = line.text.ifBlank { "…" },
                        style = if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    )
                }
            }
        } else if (state.hasAnyLyrics) {
            Text(
                text = "Unsynchronized lyrics",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.rawText.lines()) { line ->
                    Text(line, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No lyrics found for this song.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = viewModel::startEditing, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Add lyrics")
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsEditor(state: LyricsUiState, viewModel: LyricsViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Edit the raw .lrc text (e.g. [00:12.30] line). Plain lines with no timestamp are treated as unsynchronized lyrics.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = state.editText,
            onValueChange = viewModel::onEditTextChanged,
            modifier = Modifier.fillMaxWidth().height(360.dp),
        )
        if (state.saveError != null) {
            Text(state.saveError, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = viewModel::saveLyrics) { Text("Save to .lrc") }
            Button(onClick = viewModel::cancelEditing) { Text("Cancel") }
        }
    }
}
