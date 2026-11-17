package dev.focusloop.spike.platform

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserManager
import dev.focusloop.spike.domain.*
import java.time.Instant
import java.time.ZoneId

data class UsageSnapshot(
    val access: UsageAccess,
    val requested: TimeRange,
    val result: UsageResult,
)

class AndroidUsage(private val context: Context) {
    private val prefs = context.getSharedPreferences("usage_access_state", Context.MODE_PRIVATE)

    @Suppress("DEPRECATION")
    fun access(): UsageAccess {
        val allowed = try {
            context.getSystemService(AppOpsManager::class.java)?.let { manager ->
                val mode = manager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
                mode == AppOpsManager.MODE_ALLOWED || (mode == AppOpsManager.MODE_DEFAULT &&
                    context.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED)
            }
        } catch (_: RuntimeException) { null }
        val previousGrant = prefs.getBoolean("ever_granted", false)
        val state = usageAccess(allowed, previousGrant)
        if (state == UsageAccess.GRANTED && !previousGrant) prefs.edit().putBoolean("ever_granted", true).apply()
        return state
    }

    fun query(period: UsagePeriod): UsageSnapshot {
        val range = usageRange(period, Instant.now(), ZoneId.systemDefault())
        val state = access()
        if (state != UsageAccess.GRANTED) return UsageSnapshot(state, range,
            UsageResult.Unavailable("Usage Access is optional. Manual sessions remain available."))
        val result = try {
            if (context.getSystemService(UserManager::class.java)?.isUserUnlocked != true) {
                UsageResult.Unavailable("Usage data unavailable before the user is unlocked.")
            } else {
                val records = context.getSystemService(UsageStatsManager::class.java)
                    ?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, range.startMs, range.endMs)
                    ?.map { UsageBucket(it.packageName, it.totalTimeInForeground, it.firstTimeStamp, it.lastTimeStamp) }
                aggregateUsage(records, range)
            }
        } catch (_: SecurityException) {
            UsageResult.Unavailable("Usage Access changed during the query. Refresh or check Settings.")
        } catch (_: RuntimeException) {
            UsageResult.Unavailable("The Android usage service could not return records. Try Refresh.")
        }
        // Recheck after the query so a revocation does not publish a just-read result.
        val after = access()
        return UsageSnapshot(after, range, if (after == UsageAccess.GRANTED) result else
            UsageResult.Unavailable("Usage Access changed. No cached usage is shown as current."))
    }
}
