package com.ozin.music.core.domain

import com.ozin.music.core.data.model.ListeningEvent
import com.ozin.music.core.data.model.Song
import java.time.Instant
import java.time.ZoneId

data class NamedTotal(val name: String, val totalMs: Long)
data class SongTotal(val song: Song, val totalMs: Long)

data class StatsSummary(
    val range: StatsRange,
    val totalListenedMs: Long,
    val topSongs: List<SongTotal>,
    val topArtists: List<NamedTotal>,
    val topAlbums: List<NamedTotal>,
    val topGenres: List<NamedTotal>,
    /** 24 buckets, index = hour-of-day (0-23) in the local time zone, value = total ms listened in that hour. */
    val hourlyHistogramMs: List<Long>,
)

/** Pure aggregation over the real listening-event log + song library. Kept
 * side-effect free so it can be unit tested without Room/Flow. */
object StatsAggregator {

    fun summarize(
        songs: List<Song>,
        events: List<ListeningEvent>,
        range: StatsRange,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
        topCount: Int = 10,
    ): StatsSummary {
        val start = StatsTimeRange.startOfRangeMs(range, nowMs, zone)
        val inRange = events.filter { it.timestampMs in start..nowMs }
        val songMap = songs.associateBy { it.id }

        val totalMs = inRange.sumOf { it.durationMs }

        val bySong = inRange.groupBy { it.songId }.mapValues { (_, list) -> list.sumOf { it.durationMs } }
        val topSongs = bySong.entries
            .sortedByDescending { it.value }
            .mapNotNull { (id, dur) -> songMap[id]?.let { SongTotal(it, dur) } }
            .take(topCount)

        val topArtists = topByAttribute(inRange, songMap, topCount) { it.artist }
        val topAlbums = topByAttribute(inRange, songMap, topCount) { it.album }
        val topGenres = topByAttribute(inRange, songMap, topCount) { it.genre }

        val histogram = LongArray(24)
        inRange.forEach { event ->
            val hour = Instant.ofEpochMilli(event.timestampMs).atZone(zone).hour
            histogram[hour] += event.durationMs
        }

        return StatsSummary(
            range = range,
            totalListenedMs = totalMs,
            topSongs = topSongs,
            topArtists = topArtists,
            topAlbums = topAlbums,
            topGenres = topGenres,
            hourlyHistogramMs = histogram.toList(),
        )
    }

    private fun topByAttribute(
        events: List<ListeningEvent>,
        songMap: Map<Long, Song>,
        topCount: Int,
        attribute: (Song) -> String,
    ): List<NamedTotal> =
        events
            .mapNotNull { event -> songMap[event.songId]?.let { attribute(it) to event.durationMs } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.sum() }
            .entries
            .sortedByDescending { it.value }
            .take(topCount)
            .map { NamedTotal(it.key, it.value) }
}
