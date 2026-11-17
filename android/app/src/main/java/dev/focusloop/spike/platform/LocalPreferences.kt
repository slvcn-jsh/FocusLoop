package dev.focusloop.spike.platform

import android.content.Context
import dev.focusloop.spike.domain.*

class LocalPreferences(context: Context, name: String = "focusloop_preferences",
    existingCoreInstall: Boolean = context.getDatabasePath("focusloop-core.db").exists()) {
    private val store = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    // Existing core users enter Home with their current session; fresh installs see onboarding.
    private val upgrading = existingCoreInstall
    fun read(): UserPreferences {
        if (!store.contains("onboarded")) check(store.edit().putBoolean("onboarded", upgrading).commit())
        return UserPreferences(store.getBoolean("onboarded", false), store.getStringSet("goals", emptySet())!!.toSet(),
            store.getInt("minutes", 25), store.getBoolean("reminders", false))
    }
    fun write(value: UserPreferences) {
        validPreferences(value)
        check(store.edit().putBoolean("onboarded", value.onboarded).putStringSet("goals", value.goals)
            .putInt("minutes", value.defaultMinutes).putBoolean("reminders", value.remindersEnabled).commit()) {
            "Preferences could not be saved."
        }
    }
}
