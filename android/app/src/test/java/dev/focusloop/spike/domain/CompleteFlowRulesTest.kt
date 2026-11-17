package dev.focusloop.spike.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class CompleteFlowRulesTest {
    @Test fun revisionKeepsOriginalPlanAndEvents() {
        val started = SessionRules.start(null, "Read", 25, ClockMark(1000, 1000, 1))
        val revised = SessionRules.revise(started, "Review notes", ClockMark(5000, 5000, 1))
        assertEquals("Read", revised.title)
        assertEquals("Review notes", revised.currentTitle)
        assertEquals(started.originalPlanMs, revised.originalPlanMs)
        assertEquals(started.targetMs, revised.targetMs)
        assertEquals(started.events, revised.events.take(1))
        assertEquals(4000L, revised.activeMs)
        assertEquals(EventKind.REVISE, revised.events.last().kind)
        assertThrows(IllegalArgumentException::class.java) { SessionRules.revise(revised, " ", ClockMark(6000, 6000, 1)) }
    }
    @Test fun customDatesUseLocalCalendarAndClampCurrentDay() {
        val now = Instant.parse("2026-11-17T08:00:00Z")
        val zone = ZoneId.of("Asia/Manila")
        val range = usageRange(UsagePeriod.CUSTOM, now, zone, CustomDates(LocalDate.parse("2026-11-15"), LocalDate.parse("2026-11-17")))
        assertEquals(Instant.parse("2026-11-14T16:00:00Z").toEpochMilli(), range.startMs)
        assertEquals(now.toEpochMilli(), range.endMs)
        assertThrows(IllegalArgumentException::class.java) { usageRange(UsagePeriod.CUSTOM, now, zone, CustomDates(LocalDate.parse("2026-11-17"), LocalDate.parse("2026-11-18"))) }
        assertThrows(IllegalArgumentException::class.java) { usageRange(UsagePeriod.CUSTOM, now, zone, CustomDates(LocalDate.parse("2026-11-17"), LocalDate.parse("2026-11-16"))) }
        assertThrows(IllegalArgumentException::class.java) { usageRange(UsagePeriod.CUSTOM, now, zone, CustomDates(LocalDate.parse("2020-01-01"), LocalDate.parse("2026-11-17"))) }
    }
    @Test fun missingChartDayIsNotManufacturedZero() {
        val start = Instant.parse("2026-11-01T00:00:00Z").toEpochMilli()
        val range = TimeRange(start, start + 3 * 86400000L)
        val days = dailyUsage(listOf(UsageBucket("org.example.reader", 0, start, start + 86400000),
            UsageBucket("org.example.reader", 60000, start + 2 * 86400000, start + 3 * 86400000)), range, ZoneOffset.UTC)
        assertEquals(0L, days[0].foregroundMs)
        assertNull(days[1].foregroundMs)
        assertEquals(60000L, days[2].foregroundMs)
        assertTrue(dailyUsage(emptyList(), range, ZoneOffset.UTC).all { it.foregroundMs == null })
    }
    @Test fun longerWindowsCountCalendarDays() {
        val now = Instant.parse("2026-11-17T08:00:00Z")
        assertEquals(Instant.parse("2026-11-04T00:00:00Z").toEpochMilli(), usageRange(UsagePeriod.TWO_WEEKS, now, ZoneOffset.UTC).startMs)
        assertEquals(Instant.parse("2026-10-21T00:00:00Z").toEpochMilli(), usageRange(UsagePeriod.FOUR_WEEKS, now, ZoneOffset.UTC).startMs)
    }
    @Test fun preferencesStayOptionalAndValidateDuration() {
        assertTrue(validPreferences(UserPreferences()).goals.isEmpty())
        assertEquals(10, validPreferences(UserPreferences(defaultMinutes = 10)).defaultMinutes)
        assertThrows(IllegalArgumentException::class.java) { validPreferences(UserPreferences(defaultMinutes = 0)) }
        assertThrows(IllegalArgumentException::class.java) { validPreferences(UserPreferences(goals = setOf("Invented goal"))) }
    }
    @Test fun remindersRespectOptInPauseEndAndUnknownRebootGap() {
        val now = ClockMark(1000, 1000, 1)
        val record = SessionRecord(1, SessionRules.start(null, "Sample", 1, now), null, true, null)
        assertEquals(30_000L, reminderDelayMs(record, ClockMark(31_000, 31_000, 1)))
        assertEquals(0L, reminderDelayMs(record, ClockMark(71_000, 71_000, 1)))
        assertNull(reminderDelayMs(record.copy(reminderPreferred = false), now))
        assertNull(reminderDelayMs(record, ClockMark(71_000, 100, 2)))
        for (event in listOf(EventKind.PAUSE, EventKind.FINISH, EventKind.CANCEL)) {
            assertNull(reminderDelayMs(record.copy(session = SessionRules.transition(record.session, event, now)), now))
        }
    }
    @Test fun extensionAndResumeMoveReminderWithoutCountingPausedTime() {
        val record = SessionRecord(1, SessionRules.start(null, "Sample", 1, ClockMark(1000, 1000, 1)), null, true, null)
        val paused = SessionRules.transition(record.session, EventKind.PAUSE, ClockMark(31_000, 31_000, 1))
        val resumed = SessionRules.transition(paused, EventKind.RESUME, ClockMark(91_000, 91_000, 1))
        val extended = SessionRules.transition(resumed, EventKind.EXTEND, ClockMark(91_000, 91_000, 1), 60_000)
        assertEquals(90_000L, reminderDelayMs(record.copy(session = extended), ClockMark(91_000, 91_000, 1)))
        assertEquals(60_000L, record.session.originalPlanMs)
    }
}
