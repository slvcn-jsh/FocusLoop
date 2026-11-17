package dev.focusloop.spike.platform

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import dev.focusloop.spike.MainActivity
import dev.focusloop.spike.R

/** Independent 60-second probe, not the production session reminder system. */
class ReminderProbe(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val prefs = context.getSharedPreferences("reminder_probe", Context.MODE_PRIVATE)
    init {
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Feasibility reminder", NotificationManager.IMPORTANCE_DEFAULT))
    }
    fun permitted(): Boolean =
        (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            manager.areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    private fun pending(): PendingIntent = PendingIntent.getBroadcast(context, 1,
        Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun schedule(): String {
        if (!permitted()) return "Notifications unavailable or disabled. Session timing is unaffected."
        val due = SystemClock.elapsedRealtime() + 60_000
        val boot = AndroidClock(context).read().boot ?: return "Boot identity unavailable; probe not scheduled."
        prefs.edit().clear().putLong("due", due).putInt("boot", boot).apply()
        context.getSystemService(AlarmManager::class.java).set(AlarmManager.ELAPSED_REALTIME_WAKEUP, due, pending())
        return "Inexact probe scheduled for no earlier than 60 seconds; delivery may be delayed."
    }
    fun cancel(): String {
        context.getSystemService(AlarmManager::class.java).cancel(pending())
        manager.cancel(1)
        prefs.edit().clear().apply()
        return "Probe cancelled and notification dismissed."
    }
    fun deliveryInfo(): String {
        if (prefs.contains("due") && AndroidClock(context).read().boot != prefs.getInt("boot", -1)) {
            return "Previous probe belongs to another boot; delivery is not established. Schedule again if wanted."
        }
        val lag = prefs.getLong("delivery_lag", Long.MIN_VALUE)
        return if (lag == Long.MIN_VALUE) "No receiver delivery recorded for the latest probe."
        else "Latest receiver ran ${lag / 1000}s after the requested time; notification visibility is not verified."
    }
    companion object { const val CHANNEL = "feasibility_reminder" }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("reminder_probe", Context.MODE_PRIVATE)
        val due = prefs.getLong("due", -1)
        if (due < 0) return
        prefs.edit().putLong("delivery_lag", SystemClock.elapsedRealtime() - due).apply()
        val probe = ReminderProbe(context)
        if (!probe.permitted()) return
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(context, ReminderProbe.CHANNEL)
            .setSmallIcon(R.drawable.ic_reminder).setContentTitle("FocusLoop reminder experiment")
            .setContentText("A voluntary timing check. You can dismiss this reminder.")
            .setContentIntent(open).setAutoCancel(true).build()
        try { context.getSystemService(NotificationManager::class.java).notify(1, notification) }
        catch (_: SecurityException) { /* Permission can be revoked between check and delivery. */ }
    }
}
