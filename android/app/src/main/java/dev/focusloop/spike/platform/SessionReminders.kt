package dev.focusloop.spike.platform

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import dev.focusloop.spike.MainActivity
import dev.focusloop.spike.R
import dev.focusloop.spike.data.CoreDatabase
import dev.focusloop.spike.data.CoreRepository
import dev.focusloop.spike.domain.*
import kotlinx.coroutines.*

/** One voluntary inexact reminder for the current target, with no foreground/background timer service. */
class SessionReminders(private val context: Context) {
    private val prefs = context.getSharedPreferences("session_reminders", Context.MODE_PRIVATE)
    private val manager = context.getSystemService(NotificationManager::class.java)
    init { manager.createNotificationChannel(NotificationChannel(CHANNEL, "Session checkpoints", NotificationManager.IMPORTANCE_DEFAULT)) }

    fun permitted(): Boolean = (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        manager.areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    private fun pending() = PendingIntent.getBroadcast(context, 100, Intent(context, SessionReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    fun cancel() { context.getSystemService(AlarmManager::class.java).cancel(pending()); manager.cancel(100) }
    fun sync(record: SessionRecord?) {
        cancel()
        if (record == null || !permitted()) return
        val delay = reminderDelayMs(record, AndroidClock(context).read()) ?: return
        val session = record.session
        if (delay == 0L) return
        val key = "${record.id}:${session.targetMs}"
        if (prefs.getString("delivered", null) == key) return
        prefs.edit().putLong("session", record.id).putLong("target", session.targetMs).apply()
        val due = SystemClock.elapsedRealtime() + delay
        context.getSystemService(AlarmManager::class.java).set(AlarmManager.ELAPSED_REALTIME_WAKEUP, due, pending())
    }
    internal fun deliver() {
        val id = prefs.getLong("session", -1)
        if (id < 0) return
        val db = CoreDatabase.open(context)
        try {
            val record = CoreRepository(db).find(id) ?: return
            val now = AndroidClock(context).read()
            val delay = reminderDelayMs(record, now) ?: return
            val session = SessionRules.project(record.session, now)
            if (!permitted()) return
            if (delay > 0) { sync(record.copy(session = session)); return }
            val key = "${record.id}:${session.targetMs}"
            if (prefs.getString("delivered", null) == key) return
            val open = PendingIntent.getActivity(context, 100, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val notification = Notification.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_reminder)
                .setContentTitle("A moment to check in")
                .setContentText("Your planned time has arrived. Finish, extend, or keep going.")
                .setContentIntent(open).setAutoCancel(true).build()
            try {
                manager.notify(100, notification)
                prefs.edit().putString("delivered", key).apply()
            } catch (_: SecurityException) { /* Revocation remains effective at delivery time. */ }
        } finally { db.close() }
    }
    companion object { const val CHANNEL = "focusloop_checkpoints" }
}

class SessionReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { SessionReminders(context).deliver() } catch (_: RuntimeException) {
                // A failed/delayed reminder never changes the session or reports an outcome.
            } finally { pending.finish() }
        }
    }
}
