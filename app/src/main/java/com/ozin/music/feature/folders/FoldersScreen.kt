package com.ozin.music.feature.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun FoldersScreen(viewModel: FoldersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Text("Folders", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Uncheck a folder to exclude its songs from your library.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (state.suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(12.dp),
            ) {
                Text("These folders look like voice notes, not music:", style = MaterialTheme.typography.labelLarge)
                state.suggestions.forEach { folder ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(folder, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Button(onClick = { viewModel.dismissSuggestion(folder) }) { Text("Exclude") }
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(state.folders, key = { it.path }) { folder ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folder.path, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text("${folder.songCount} songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Checkbox(
                        checked = !folder.excluded,
                        onCheckedChange = { included -> viewModel.toggleExcluded(folder.path, excluded = !included) },
                    )
                }
            }
        }
    }
}
