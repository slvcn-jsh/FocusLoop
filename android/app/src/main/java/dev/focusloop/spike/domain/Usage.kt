package dev.focusloop.spike.domain

import java.time.Instant
import java.time.ZoneId

enum class UsageAccess { GRANTED, NOT_GRANTED, REVOKED, UNAVAILABLE }
fun usageAccess(allowed: Boolean?, wasGranted: Boolean): UsageAccess = when {
    allowed == null -> UsageAccess.UNAVAILABLE
    allowed -> UsageAccess.GRANTED
    wasGranted -> UsageAccess.REVOKED
    else -> UsageAccess.NOT_GRANTED
}

enum class UsagePeriod(val label: String) { TODAY("Today"), YESTERDAY("Yesterday"), WEEK("7 days") }
data class TimeRange(val startMs: Long, val endMs: Long) {
    init { require(startMs <= endMs) }
}

fun usageRange(period: UsagePeriod, now: Instant, zone: ZoneId): TimeRange {
    val today = now.atZone(zone).toLocalDate()
    val start = today.minusDays(when (period) {
        UsagePeriod.TODAY -> 0; UsagePeriod.YESTERDAY -> 1; UsagePeriod.WEEK -> 6
    })
    val end = if (period == UsagePeriod.YESTERDAY) today.atStartOfDay(zone).toInstant() else now
    return TimeRange(start.atStartOfDay(zone).toInstant().toEpochMilli(), end.toEpochMilli())
}

data class UsageBucket(val packageName: String, val foregroundMs: Long, val startMs: Long, val endMs: Long)
data class UsageRow(val packageName: String, val foregroundMs: Long)
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
