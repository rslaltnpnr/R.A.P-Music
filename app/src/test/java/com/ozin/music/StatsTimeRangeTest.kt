package com.ozin.music

import com.ozin.music.core.domain.StatsRange
import com.ozin.music.core.domain.StatsTimeRange
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class StatsTimeRangeTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    // Wednesday 2024-01-10 15:30 UTC.
    private val now = ZonedDateTime.of(2024, 1, 10, 15, 30, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `today boundary excludes a timestamp from yesterday`() {
        val yesterday = ZonedDateTime.of(2024, 1, 9, 23, 59, 0, 0, zone).toInstant().toEpochMilli()
        assertFalse(StatsTimeRange.isWithinRange(yesterday, StatsRange.TODAY, now, zone))
    }

    @Test
    fun `today boundary includes a timestamp from earlier today`() {
        val earlierToday = ZonedDateTime.of(2024, 1, 10, 0, 1, 0, 0, zone).toInstant().toEpochMilli()
        assertTrue(StatsTimeRange.isWithinRange(earlierToday, StatsRange.TODAY, now, zone))
    }

    @Test
    fun `week boundary starts on monday`() {
        // Monday 2024-01-08 00:00 UTC is within the week containing "now".
        val monday = ZonedDateTime.of(2024, 1, 8, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertTrue(StatsTimeRange.isWithinRange(monday, StatsRange.WEEK, now, zone))

        // The previous Sunday is not.
        val previousSunday = ZonedDateTime.of(2024, 1, 7, 23, 59, 0, 0, zone).toInstant().toEpochMilli()
        assertFalse(StatsTimeRange.isWithinRange(previousSunday, StatsRange.WEEK, now, zone))
    }

    @Test
    fun `month boundary starts on the first of the month`() {
        val firstOfMonth = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertTrue(StatsTimeRange.isWithinRange(firstOfMonth, StatsRange.MONTH, now, zone))

        val lastMonth = ZonedDateTime.of(2023, 12, 31, 23, 59, 0, 0, zone).toInstant().toEpochMilli()
        assertFalse(StatsTimeRange.isWithinRange(lastMonth, StatsRange.MONTH, now, zone))
    }

    @Test
    fun `year boundary starts on january first`() {
        val firstOfYear = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertTrue(StatsTimeRange.isWithinRange(firstOfYear, StatsRange.YEAR, now, zone))

        val lastYear = ZonedDateTime.of(2023, 12, 31, 23, 59, 0, 0, zone).toInstant().toEpochMilli()
        assertFalse(StatsTimeRange.isWithinRange(lastYear, StatsRange.YEAR, now, zone))
    }

    @Test
    fun `all time includes everything from epoch`() {
        assertTrue(StatsTimeRange.isWithinRange(0L, StatsRange.ALL_TIME, now, zone))
        assertTrue(StatsTimeRange.isWithinRange(now, StatsRange.ALL_TIME, now, zone))
    }

    @Test
    fun `a timestamp after now is never within range`() {
        val future = now + 60_000
        assertFalse(StatsTimeRange.isWithinRange(future, StatsRange.TODAY, now, zone))
    }
}
