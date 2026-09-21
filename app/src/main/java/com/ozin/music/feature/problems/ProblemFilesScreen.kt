package com.ozin.music.feature.problems

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProblemFilesScreen(viewModel: ProblemFilesViewModel = hiltViewModel()) {
    val problems by viewModel.problemFiles.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Text("Problem files", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Songs that failed to play or read. Ignore to dismiss, Remove to also delete the file, or Rescan to retry.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        if (problems.isEmpty()) {
            Text("No problem files.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn {
                items(problems, key = { it.path }) { problem ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(problem.title, style = MaterialTheme.typography.titleSmall)
                        Text(problem.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            Button(onClick = { viewModel.ignore(problem) }) { Text("Ignore") }
                            Button(onClick = { viewModel.rescan(problem) }) { Text("Rescan") }
                            Button(onClick = { viewModel.remove(problem, alsoDeleteFile = true) }) { Text("Remove") }
                        }
                    }
                }
            }
        }
    }
}
