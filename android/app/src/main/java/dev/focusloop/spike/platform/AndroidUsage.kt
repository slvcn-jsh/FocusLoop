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
import android.graphics.Bitmap
import android.graphics.Canvas
import java.util.concurrent.ConcurrentHashMap

data class UsageSnapshot(
    val access: UsageAccess,
    val requested: TimeRange,
    val result: UsageResult,
    val buckets: List<UsageBucket> = emptyList(),
    val identities: Map<String, AppIdentity> = emptyMap(),
)
data class AppIdentity(val name: String, val icon: Bitmap? = null)

class AndroidUsage(private val context: Context) {
    private val prefs = context.getSharedPreferences("usage_access_state", Context.MODE_PRIVATE)
    private val identityCache = ConcurrentHashMap<String, AppIdentity>()

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

    fun query(period: UsagePeriod, custom: CustomDates? = null): UsageSnapshot {
        val range = try { usageRange(period, Instant.now(), ZoneId.systemDefault(), custom) }
        catch (e: IllegalArgumentException) { return UsageSnapshot(access(), TimeRange(0, 0), UsageResult.Unavailable(e.message ?: "Choose valid dates.")) }
        val state = access()
        if (state != UsageAccess.GRANTED) return UsageSnapshot(state, range,
            UsageResult.Unavailable("Usage Access is optional. Manual sessions remain available."))
        var buckets: List<UsageBucket> = emptyList()
        val result = try {
            if (context.getSystemService(UserManager::class.java)?.isUserUnlocked != true) {
                UsageResult.Unavailable("Usage data unavailable before the user is unlocked.")
            } else {
                val records = context.getSystemService(UsageStatsManager::class.java)
                    ?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, range.startMs, range.endMs)
                    ?.map { UsageBucket(it.packageName, it.totalTimeInForeground, it.firstTimeStamp, it.lastTimeStamp) }
                buckets = records.orEmpty()
                aggregateUsage(records, range)
            }
        } catch (_: SecurityException) {
            UsageResult.Unavailable("Usage Access changed during the query. Refresh or check Settings.")
        } catch (_: RuntimeException) {
            UsageResult.Unavailable("The Android usage service could not return records. Try Refresh.")
        }
        // Recheck after the query so a revocation does not publish a just-read result.
        val after = access()
        if (after != UsageAccess.GRANTED) return UsageSnapshot(after, range,
            UsageResult.Unavailable("Usage Access changed. No cached usage is shown as current."))
        val identities = if (result is UsageResult.Records) result.rows.associate { row -> row.packageName to identity(row.packageName) } else emptyMap()
        return UsageSnapshot(after, range, result, buckets, identities)
    }

    @Suppress("DEPRECATION")
    private fun identity(name: String): AppIdentity = identityCache.getOrPut(name) {
        try {
            val info = context.packageManager.getApplicationInfo(name, 0)
            val drawable = context.packageManager.getApplicationIcon(info)
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, 64, 64); drawable.draw(Canvas(bitmap))
            AppIdentity(context.packageManager.getApplicationLabel(info).toString(), bitmap)
        } catch (_: Exception) { AppIdentity(name) }
    }
}
