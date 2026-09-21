package com.ozin.music.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.core.data.model.Song

/**
 * "Smart search": a plain keyword search over the local library (see
 * [SmartSearchViewModel] / [com.ozin.music.core.domain.QueryIntentParser]).
 * It recognizes a small, fixed set of Turkish/English phrases (favorite,
 * mood words, "most played", "last 30 days") plus free text - it is not
 * natural language understanding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSearchScreen(
    onBack: () -> Unit,
    onSongClick: () -> Unit,
    viewModel: SmartSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = { Text("Smart search") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
        Text(
            "Keyword search, not AI: try \"favori\", \"enerjik\", \"gece\", \"en çok dinlenen\" or \"son 30 gün\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("e.g. \"enerjik antrenman\"") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
        )

        if (!state.hasSearched) {
            Text(
                "Type a search and press enter.",
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (state.results.isEmpty()) {
            Text(
                "No matches.",
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn {
                items(state.results, key = { it.id }) { song ->
                    SearchResultRow(song, onClick = { viewModel.playSong(song); onSongClick() })
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(song.title, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
            Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
