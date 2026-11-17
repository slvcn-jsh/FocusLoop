package dev.focusloop.spike.ui

import dev.focusloop.spike.domain.*

/** UI commands are injectable so navigation tests never touch the user's real records or permissions. */
interface AppActions {
    fun startIntention(value: Intention, saveTemplate: Boolean, onSuccess: (Long) -> Unit)
    fun savePlan(value: Intention, onSuccess: () -> Unit)
    fun deletePlan(id: Long, onSuccess: () -> Unit)
    fun sessionAction(event: EventKind, addedMinutes: Int = 5, onSuccess: (Long) -> Unit = {})
    fun reviseIntention(title: String, onSuccess: () -> Unit)
    fun saveReflection(id: Long, outcome: ReportedOutcome, onSuccess: () -> Unit)
    fun savePreferences(value: UserPreferences, onSuccess: () -> Unit = {})
    fun clearHistory(onSuccess: () -> Unit)
    fun clearPlans(onSuccess: () -> Unit)
    fun loadMoreHistory()
    fun loadRecord(id: Long)
    fun refreshUsage(period: UsagePeriod, custom: CustomDates? = null)
    fun retry()
    fun dismissError()
    fun testReminder()
}
