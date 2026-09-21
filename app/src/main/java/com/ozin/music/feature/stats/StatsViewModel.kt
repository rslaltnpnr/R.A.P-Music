package com.ozin.music.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.repository.ListeningStatsRepository
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.domain.StatsAggregator
import com.ozin.music.core.domain.StatsRange
import com.ozin.music.core.domain.StatsSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val listeningStatsRepository: ListeningStatsRepository,
) : ViewModel() {

    private val selectedRange = MutableStateFlow(StatsRange.TODAY)
    val range: StateFlow<StatsRange> = selectedRange

    val summary: StateFlow<StatsSummary> = combine(
        songRepository.songs,
        listeningStatsRepository.events,
        selectedRange,
    ) { songs, events, range ->
        StatsAggregator.summarize(songs, events, range)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StatsAggregator.summarize(emptyList(), emptyList(), StatsRange.TODAY),
    )

    fun selectRange(range: StatsRange) {
        selectedRange.value = range
    }

    /** A short, real, formatted plain-text summary of the current range,
     * suitable for Intent.ACTION_SEND sharing. */
    fun shareText(): String {
        val s = summary.value
        val minutes = s.totalListenedMs / 60_000
        val builder = StringBuilder()
        builder.append("OZIN Music — ${s.range.name} dinleme özeti\n")
        builder.append("Toplam dinleme: ${minutes} dk\n")
        if (s.topSongs.isNotEmpty()) {
            builder.append("En çok dinlenenler:\n")
            s.topSongs.take(5).forEachIndexed { index, songTotal ->
                builder.append("${index + 1}. ${songTotal.song.title} — ${songTotal.song.artist}\n")
            }
        }
        if (s.topArtists.isNotEmpty()) {
            builder.append("En çok dinlenen sanatçılar:\n")
            s.topArtists.take(5).forEach { builder.append("- ${it.name}\n") }
        }
        val busiestHour = s.hourlyHistogramMs.withIndex().maxByOrNull { it.value }?.index
        if (busiestHour != null && s.totalListenedMs > 0) {
            builder.append("En aktif saat: $busiestHour:00\n")
        }
        return builder.toString()
    }
}
