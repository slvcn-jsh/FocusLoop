package dev.focusloop.spike.domain

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class UsageTest {
    @Test fun grantedDeniedRevokedAndUnavailableAreDistinct() {
        assertEquals(UsageAccess.GRANTED, usageAccess(true, false))
        assertEquals(UsageAccess.NOT_GRANTED, usageAccess(false, false))
        assertEquals(UsageAccess.REVOKED, usageAccess(false, true))
        assertEquals(UsageAccess.UNAVAILABLE, usageAccess(null, true))
    }
    @Test fun emptyNullAndMeasuredZeroAreDistinct() {
        val range = TimeRange(100, 200)
        assertEquals(UsageResult.Empty, aggregateUsage(emptyList(), range))
        assertTrue(aggregateUsage(null, range) is UsageResult.Unavailable)
        val zero = aggregateUsage(listOf(UsageBucket("app", 0, 100, 200)), range) as UsageResult.Records
        assertEquals(0L, zero.rows.single().foregroundMs)
    }
    @Test fun dailyBucketExpansionIsNotClippedOrHidden() {
        val result = aggregateUsage(listOf(UsageBucket("app", 5000, 0, 86400000)), TimeRange(20000, 80000)) as UsageResult.Records
        assertTrue(result.exceedsRequest)
        assertEquals(5000L, result.rows.single().foregroundMs)
        assertEquals(TimeRange(0, 86400000), result.returnedRange)
    }
    @Test fun perPackageBucketsAreSummedAndRanked() {
        val rows = listOf(UsageBucket("A", 100, 0, 10), UsageBucket("B", 300, 0, 10), UsageBucket("A", 400, 10, 20))
        val result = aggregateUsage(rows, TimeRange(0, 20)) as UsageResult.Records
        assertEquals(listOf(UsageRow("A", 500), UsageRow("B", 300)), result.rows)
        assertFalse(result.exceedsRequest)
    }
    @Test fun invalidRecordsAreNotShownAsValidUsage() {
        assertTrue(aggregateUsage(listOf(UsageBucket("A", -1, 0, 10)), TimeRange(0, 10)) is UsageResult.Unavailable)
    }
    @Test fun localDayRangesFollowManilaCalendar() {
        val now = Instant.parse("2026-11-17T08:00:00Z")
        val zone = ZoneId.of("Asia/Manila")
        assertEquals(TimeRange(Instant.parse("2026-11-16T16:00:00Z").toEpochMilli(), now.toEpochMilli()), usageRange(UsagePeriod.TODAY, now, zone))
        assertEquals(TimeRange(Instant.parse("2026-11-15T16:00:00Z").toEpochMilli(), Instant.parse("2026-11-16T16:00:00Z").toEpochMilli()), usageRange(UsagePeriod.YESTERDAY, now, zone))
        assertEquals(Instant.parse("2026-11-10T16:00:00Z").toEpochMilli(), usageRange(UsagePeriod.WEEK, now, zone).startMs)
    }
    @Test fun calendarDayIsNotAlways24Hours() {
        val zone = ZoneId.of("America/New_York")
        val range = usageRange(UsagePeriod.YESTERDAY, Instant.parse("2026-03-09T16:00:00Z"), zone)
        assertEquals(23 * 3600000L, range.endMs - range.startMs)
    }
    @Test fun midnightDoesNotCreateFakeTodayUsage() {
        val now = Instant.parse("2026-11-16T16:00:00Z")
        val range = usageRange(UsagePeriod.TODAY, now, ZoneId.of("Asia/Manila"))
        assertEquals(range.startMs, range.endMs)
    }
}
