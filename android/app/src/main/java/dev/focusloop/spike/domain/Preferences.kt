package dev.focusloop.spike.domain

val PERSONAL_GOALS = listOf("Reduce distractions", "Understand screen time", "Study or work",
    "Reduce unwanted scrolling", "Build better habits", "Other")
data class UserPreferences(val onboarded: Boolean = false, val goals: Set<String> = emptySet(),
    val defaultMinutes: Int = 25, val remindersEnabled: Boolean = false)

fun validPreferences(value: UserPreferences): UserPreferences {
    require(value.defaultMinutes in 1..1440) { "Use a duration from 1 to 1440 minutes." }
    require(value.goals.all { it in PERSONAL_GOALS }) { "Choose one of the available goals." }
    return value
}
