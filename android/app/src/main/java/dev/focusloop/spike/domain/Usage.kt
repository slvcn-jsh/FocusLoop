package dev.focusloop.spike.domain

import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class UsageAccess { GRANTED, NOT_GRANTED, REVOKED, UNAVAILABLE }
fun usageAccess(allowed: Boolean?, wasGranted: Boolean): UsageAccess = when {
    allowed == null -> UsageAccess.UNAVAILABLE
    allowed -> UsageAccess.GRANTED
    wasGranted -> UsageAccess.REVOKED
    else -> UsageAccess.NOT_GRANTED
}

enum class UsagePeriod(val label: String) { TODAY("Today"), YESTERDAY("Yesterday"), WEEK("7 days"), TWO_WEEKS("14 days"), FOUR_WEEKS("28 days"), CUSTOM("Custom") }
data class CustomDates(val start: LocalDate, val endInclusive: LocalDate)
data class TimeRange(val startMs: Long, val endMs: Long) {
    init { require(startMs <= endMs) }
}

fun usageRange(period: UsagePeriod, now: Instant, zone: ZoneId, custom: CustomDates? = null): TimeRange {
    val today = now.atZone(zone).toLocalDate()
    if (period == UsagePeriod.CUSTOM) {
        require(custom != null) { "Choose a start and end date." }
        require(!custom.start.isAfter(custom.endInclusive) && !custom.endInclusive.isAfter(today)) { "Choose a valid range ending today or earlier." }
        require(ChronoUnit.DAYS.between(custom.start, custom.endInclusive) < 366) { "Choose up to 366 days." }
        return TimeRange(custom.start.atStartOfDay(zone).toInstant().toEpochMilli(),
            minOf(now.toEpochMilli(), custom.endInclusive.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()))
    }
    val start = today.minusDays(when (period) {
        UsagePeriod.TODAY -> 0; UsagePeriod.YESTERDAY -> 1; UsagePeriod.WEEK -> 6
        UsagePeriod.TWO_WEEKS -> 13; UsagePeriod.FOUR_WEEKS -> 27; UsagePeriod.CUSTOM -> error("Handled above")
    })
    val end = if (period == UsagePeriod.YESTERDAY) today.atStartOfDay(zone).toInstant() else now
    return TimeRange(start.atStartOfDay(zone).toInstant().toEpochMilli(), end.toEpochMilli())
}

data class UsageBucket(val packageName: String, val foregroundMs: Long, val startMs: Long, val endMs: Long)
data class UsageRow(val packageName: String, val foregroundMs: Long)
data class UsageDay(val date: LocalDate, val foregroundMs: Long?)

/** Missing calendar days remain null. Raw bucket dates do not imply continuous or exact coverage. */
fun dailyUsage(buckets: List<UsageBucket>, range: TimeRange, zone: ZoneId, packageName: String? = null): List<UsageDay> {
    val grouped = buckets.filter { packageName == null || it.packageName == packageName }
        .groupBy { Instant.ofEpochMilli(it.startMs).atZone(zone).toLocalDate() }
        .mapValues { (_, rows) -> rows.sumOf { it.foregroundMs } }
    val first = Instant.ofEpochMilli(range.startMs).atZone(zone).toLocalDate()
    val last = Instant.ofEpochMilli(maxOf(range.startMs, range.endMs - 1)).atZone(zone).toLocalDate()
    return (0..ChronoUnit.DAYS.between(first, last).coerceAtMost(365)).map { offset ->
        val date = first.plusDays(offset); UsageDay(date, grouped[date])
    }
}
sealed interface UsageResult {
    data class Unavailable(val reason: String) : UsageResult
    data object Empty : UsageResult
    data class Records(val rows: List<UsageRow>, val returnedRange: TimeRange, val exceedsRequest: Boolean) : UsageResult
}

/** Aggregate raw daily buckets, retaining their actual boundaries. No invented clipping. */
fun aggregateUsage(buckets: List<UsageBucket>?, requested: TimeRange): UsageResult {
    if (buckets == null) return UsageResult.Unavailable("Android returned no accessible usage result.")
    if (buckets.isEmpty()) return UsageResult.Empty
    if (buckets.any { it.foregroundMs < 0 || it.startMs > it.endMs || it.packageName.isBlank() }) {
        return UsageResult.Unavailable("Android returned invalid usage records.")
    }
    val bounds = TimeRange(buckets.minOf { it.startMs }, buckets.maxOf { it.endMs })
    val rows = buckets.groupBy { it.packageName }.map { (name, values) ->
        UsageRow(name, values.sumOf { it.foregroundMs })
    }.sortedByDescending { it.foregroundMs }
    return UsageResult.Records(rows, bounds,
        bounds.startMs < requested.startMs || bounds.endMs > requested.endMs)
}
