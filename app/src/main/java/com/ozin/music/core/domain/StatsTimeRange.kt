package com.ozin.music.core.domain

import java.time.Instant
import java.time.ZoneId

/** Time ranges the listening-stats dashboard can be filtered by. */
enum class StatsRange { TODAY, WEEK, MONTH, YEAR, ALL_TIME }

/** Pure boundary math for [StatsRange], kept separate from any DB/Flow code
 * so it is trivially unit-testable. Week starts on Monday. */
object StatsTimeRange {

    fun startOfRangeMs(range: StatsRange, nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        if (range == StatsRange.ALL_TIME) return 0L
        val now = Instant.ofEpochMilli(nowMs).atZone(zone)
        val startDate = when (range) {
            StatsRange.TODAY -> now.toLocalDate()
            StatsRange.WEEK -> now.toLocalDate().minusDays((now.dayOfWeek.value - 1).toLong())
            StatsRange.MONTH -> now.toLocalDate().withDayOfMonth(1)
            StatsRange.YEAR -> now.toLocalDate().withDayOfYear(1)
            StatsRange.ALL_TIME -> now.toLocalDate()
        }
        return startDate.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun isWithinRange(timestampMs: Long, range: StatsRange, nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        timestampMs in startOfRangeMs(range, nowMs, zone)..nowMs
}
