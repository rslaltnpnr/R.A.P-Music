package com.ozin.music.feature.stats

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.core.domain.NamedTotal
import com.ozin.music.core.domain.SongTotal
import com.ozin.music.core.domain.StatsRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val range by viewModel.range.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("İstatistikler") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            },
            actions = {
                IconButton(onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, viewModel.shareText())
                    }
                    ContextCompat.startActivity(context, Intent.createChooser(sendIntent, "Paylaş"), null)
                }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share")
                }
            },
        )

        ScrollableTabRow(selectedTabIndex = StatsRange.entries.indexOf(range)) {
            StatsRange.entries.forEach { r ->
                Tab(
                    selected = range == r,
                    onClick = { viewModel.selectRange(r) },
                    text = { Text(rangeLabel(r)) },
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Text(
                    text = "Toplam dinleme: ${formatDuration(summary.totalListenedMs)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            item { SectionHeader("En aktif saatler") }
            item { HourlyHistogram(summary.hourlyHistogramMs) }
            item { SectionHeader("En çok dinlenen şarkılar") }
            items(summary.topSongs, key = { it.song.id }) { entry -> SongTotalRow(entry) }
            if (summary.topSongs.isEmpty()) {
                item { EmptyRow() }
            }
            item { SectionHeader("En çok dinlenen sanatçılar") }
            items(summary.topArtists, key = { "artist_${it.name}" }) { NamedTotalRow(it) }
            item { SectionHeader("En çok dinlenen albümler") }
            items(summary.topAlbums, key = { "album_${it.name}" }) { NamedTotalRow(it) }
            item { SectionHeader("En çok dinlenen türler") }
            items(summary.topGenres, key = { "genre_${it.name}" }) { NamedTotalRow(it) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun EmptyRow() {
    Text("Bu aralıkta veri yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SongTotalRow(entry: SongTotal) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.song.title, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
            Text(entry.song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Text(formatDuration(entry.totalMs), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NamedTotalRow(entry: NamedTotal) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(entry.name, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, modifier = Modifier.weight(1f))
        Text(formatDuration(entry.totalMs), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HourlyHistogram(buckets: List<Long>) {
    val max = buckets.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        buckets.forEach { value ->
            val fraction = (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((80 * fraction).dp.coerceAtLeast(2.dp))
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
            )
        }
    }
}

private fun rangeLabel(range: StatsRange): String = when (range) {
    StatsRange.TODAY -> "Bugün"
    StatsRange.WEEK -> "Bu hafta"
    StatsRange.MONTH -> "Bu ay"
    StatsRange.YEAR -> "Bu yıl"
    StatsRange.ALL_TIME -> "Tüm zamanlar"
}

private fun formatDuration(ms: Long): String {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}sa ${minutes}dk" else "${minutes}dk"
}
