package dev.focusloop.spike.platform

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import dev.focusloop.spike.domain.ClockMark

class AndroidClock(private val context: Context) {
    fun read(): ClockMark = ClockMark(System.currentTimeMillis(), SystemClock.elapsedRealtime(),
        try { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }
        catch (_: Settings.SettingNotFoundException) { null }
        catch (_: SecurityException) { null })
}
