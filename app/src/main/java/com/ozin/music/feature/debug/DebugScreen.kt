package com.ozin.music.feature.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.R
import com.ozin.music.core.domain.ArtworkSource
import com.ozin.music.core.domain.AudioOutputKind

@Composable
fun DebugScreen(
    onBack: () -> Unit = {},
    viewModel: DebugViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val playback = state.playback
    val song = playback.currentSong

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.debug_title), style = MaterialTheme.typography.headlineSmall)

        DebugRow(stringResource(R.string.debug_current_song), song?.let { "${it.title} (id=${it.id})" } ?: stringResource(R.string.debug_no_song))
        DebugRow(
            stringResource(R.string.debug_playback_state),
            if (playback.isPlaying) stringResource(R.string.debug_playing) else stringResource(R.string.debug_paused),
        )
        DebugRow(stringResource(R.string.debug_artwork_source), artworkSourceLabel(playback.currentArtworkSource))
        DebugRow(
            stringResource(R.string.debug_session_connected),
            if (playback.isConnected) stringResource(R.string.debug_yes) else stringResource(R.string.debug_no),
        )
        DebugRow(
            stringResource(R.string.debug_notification_active),
            if (song != null && playback.isPlaying) stringResource(R.string.debug_yes) else stringResource(R.string.debug_no),
        )
        Text(
            stringResource(R.string.debug_notification_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DebugRow(stringResource(R.string.debug_audio_output), audioOutputLabel(state.audioOutput))
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun artworkSourceLabel(source: ArtworkSource?): String = when (source) {
    ArtworkSource.EMBEDDED -> stringResource(R.string.debug_artwork_source_embedded)
    ArtworkSource.MEDIA_STORE -> stringResource(R.string.debug_artwork_source_media_store)
    ArtworkSource.DEFAULT -> stringResource(R.string.debug_artwork_source_default)
    null -> stringResource(R.string.debug_artwork_source_unknown)
}

@Composable
fun audioOutputLabel(kind: AudioOutputKind): String = when (kind) {
    AudioOutputKind.THIS_DEVICE -> stringResource(R.string.output_this_device)
    AudioOutputKind.BLUETOOTH -> stringResource(R.string.output_bluetooth)
    AudioOutputKind.WIRED_HEADPHONES -> stringResource(R.string.output_wired)
    AudioOutputKind.CAR -> stringResource(R.string.output_car)
}
