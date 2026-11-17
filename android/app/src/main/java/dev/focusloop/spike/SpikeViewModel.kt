package dev.focusloop.spike

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.focusloop.spike.domain.*
import dev.focusloop.spike.platform.*
import dev.focusloop.spike.data.CoreDatabase
import dev.focusloop.spike.data.CoreRepository
import dev.focusloop.spike.ui.AppActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class SpikeState(
    val core: CoreSnapshot = CoreSnapshot(),
    val sessionLoaded: Boolean = false,
    val sessionError: String? = null,
    val storageFailed: Boolean = false,
    val busy: Boolean = false,
    val period: UsagePeriod = UsagePeriod.TODAY,
    val usage: UsageSnapshot? = null,
    val todayUsage: UsageSnapshot? = null,
    val recentUsage: UsageSnapshot? = null,
    val customDates: CustomDates? = null,
    val preferences: UserPreferences = UserPreferences(),
    val preferencesLoaded: Boolean = false,
    val selectedRecords: Map<Long, SessionRecord> = emptyMap(),
    val notificationsAllowed: Boolean = false,
    val reminder: String = "Reminders are optional. No test reminder requested yet.",
    val restored: Boolean = false,
) { val session: Session? get() = core.current?.session }

class SpikeViewModel(app: Application) : AndroidViewModel(app), AppActions {
    private val localPreferences = LocalPreferences(app)
    private val database = CoreDatabase.open(app)
    private val legacy = if (app.getDatabasePath("feasibility.db").exists()) SessionDatabase(app) else null
    private val sessions = CoreRepository(database, legacy)
    private val clock = AndroidClock(app)
    private val usage = AndroidUsage(app)
    private val lock = Mutex()
    private var queryJob: Job? = null
    private var historyLimit = 100
    private val mutable = MutableStateFlow(SpikeState())
    val state = mutable.asStateFlow()

    fun foreground() {
        reloadSession()
        refreshUsage()
        reminderStatus()
    }

    fun hideUsage() {
        queryJob?.cancel()
        mutable.update { it.copy(usage = null, todayUsage = null, recentUsage = null) }
    }

    fun refreshUsage() = refreshUsage(state.value.period, state.value.customDates)
    override fun refreshUsage(period: UsagePeriod, custom: CustomDates?) {
        queryJob?.cancel()
        mutable.update { it.copy(period = period, customDates = custom, usage = null, todayUsage = null, recentUsage = null) }
        queryJob = viewModelScope.launch {
            val (snapshot, today, recent) = withContext(Dispatchers.IO) {
                val selected = usage.query(period, custom)
                Triple(selected, if (period == UsagePeriod.TODAY) selected else usage.query(UsagePeriod.TODAY),
                    if (period == UsagePeriod.WEEK) selected else usage.query(UsagePeriod.WEEK))
            }
            mutable.update { it.copy(usage = snapshot, todayUsage = today, recentUsage = recent) }
        }
    }

    fun reloadSession() = change { mutable.update { it.copy(restored = true) } }

    fun start(title: String, minutes: String, target: String?, reminderPreferred: Boolean) = change {
        sessions.start(Intention(title = title, plannedMinutes = minutes.toIntOrNull() ?: 0,
            targetPackage = target, reminderPreferred = reminderPreferred), clock.read())
        mutable.update { it.copy(restored = false) }
    }

    fun act(event: EventKind) = change {
        sessions.act(requireNotNull(state.value.core.current).id, event, clock.read(), if (event == EventKind.EXTEND) 300_000 else 0)
    }

    fun saveIntention(value: Intention) = change { sessions.saveIntention(value) }
    fun deleteIntention(id: Long) = change { sessions.deleteIntention(id) }
    fun reflect(id: Long, outcome: ReportedOutcome) = change { sessions.reflect(id, outcome, clock.read()) }
    fun clear() = change { sessions.clearHistory(); mutable.update { it.copy(restored = false) } }

    override fun startIntention(value: Intention, saveTemplate: Boolean, onSuccess: (Long) -> Unit) =
        change(onSuccess = { onSuccess(requireNotNull(it.current).id) }) {
            sessions.start(value, clock.read(), saveTemplate)
            mutable.update { it.copy(restored = false) }
        }
    override fun savePlan(value: Intention, onSuccess: () -> Unit) = change(onSuccess = { onSuccess() }) { sessions.saveIntention(value) }
    override fun deletePlan(id: Long, onSuccess: () -> Unit) = change(onSuccess = { onSuccess() }) { sessions.deleteIntention(id) }
    override fun sessionAction(event: EventKind, addedMinutes: Int, onSuccess: (Long) -> Unit) = change(onSuccess = { onSuccess(requireNotNull(it.current).id) }) {
        sessions.act(requireNotNull(state.value.core.current).id, event, clock.read(), if (event == EventKind.EXTEND) addedMinutes * 60_000L else 0)
    }
    override fun reviseIntention(title: String, onSuccess: () -> Unit) = change(onSuccess = { onSuccess() }) {
        sessions.revise(requireNotNull(state.value.core.current).id, title, clock.read())
    }
    override fun saveReflection(id: Long, outcome: ReportedOutcome, onSuccess: () -> Unit) = change(onSuccess = { onSuccess() }) { sessions.reflect(id, outcome, clock.read()) }
    override fun savePreferences(value: UserPreferences, onSuccess: () -> Unit) = change(onSuccess = { onSuccess() }) { localPreferences.write(value) }
    override fun clearHistory(onSuccess: () -> Unit) = change(onSuccess = { onSuccess() }) { sessions.clearHistory() }
    override fun clearPlans(onSuccess: () -> Unit) = change(onSuccess = { onSuccess() }) { sessions.clearIntentions() }
    override fun loadMoreHistory() { historyLimit += 100; reloadSession() }
    override fun loadRecord(id: Long) {
        viewModelScope.launch {
            lock.withLock {
                try {
                    val record = withContext(Dispatchers.IO) { sessions.find(id) }
                    mutable.update { current -> current.copy(selectedRecords = if (record == null) current.selectedRecords - id else current.selectedRecords + (id to record)) }
                } catch (_: RuntimeException) { mutable.update { it.copy(sessionError = "This session could not be loaded. Try again.") } }
            }
        }
    }
    override fun retry() { foreground() }
    override fun dismissError() { if (!state.value.storageFailed) mutable.update { it.copy(sessionError = null) } }
    override fun testReminder() = reminder(true)

    private fun change(onSuccess: (CoreSnapshot) -> Unit = {}, operation: () -> Unit) {
        viewModelScope.launch {
            lock.withLock {
                mutable.update { it.copy(busy = true) }
                try {
                    val result = withContext(Dispatchers.IO) {
                        localPreferences.read() // Persist fresh-install state before the first Room open.
                        operation()
                        val snapshot = sessions.load(clock.read(), historyLimit)
                        val records = state.value.selectedRecords.keys.mapNotNull { id -> sessions.find(id)?.let { id to it } }.toMap()
                        try { SessionReminders(getApplication()).sync(snapshot.current) } catch (_: RuntimeException) { /* Timing remains independent of reminder availability. */ }
                        Triple(snapshot, localPreferences.read(), records)
                    }
                    mutable.update { it.copy(core = result.first, preferences = result.second, selectedRecords = result.third,
                        preferencesLoaded = true, sessionLoaded = true, sessionError = null, storageFailed = false) }
                    onSuccess(result.first)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    mutable.update { it.copy(storageFailed = e !is IllegalArgumentException, sessionError = if (e is IllegalArgumentException) e.message
                        else "Could not read or save local records. Retry, or explicitly clear history. No success was recorded.") }
                } finally { mutable.update { it.copy(busy = false) } }
            }
        }
    }

    /** Only while STARTED. No timer service, wake lock, or per-second database writes. */
    suspend fun tick() {
        lock.withLock {
            val savedRecord = state.value.core.current
            val saved = savedRecord?.session
            if (saved != null && !state.value.storageFailed) {
                val now = withContext(Dispatchers.IO) { clock.read() }
                val projected = SessionRules.project(saved, now)
                try {
                    if (projected.status == SessionStatus.RECOVERY_REQUIRED && projected != saved) {
                        val recovered = withContext(Dispatchers.IO) {
                            sessions.saveRecovery(savedRecord.id, now)
                            sessions.load(now, historyLimit)
                        }
                        mutable.update { it.copy(core = recovered) }
                    } else {
                        mutable.update { it.copy(core = it.core.copy(current = savedRecord.copy(session = projected))) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    mutable.update { it.copy(storageFailed = true, sessionError = "Recovery could not be saved. Existing record retained.") }
                }
            }
        }
        val access = withContext(Dispatchers.IO) { usage.access() }
        val snapshot = state.value.usage
        if (snapshot != null && access != snapshot.access) refreshUsage()
    }

    fun reminderStatus() {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                SessionReminders(getApplication()).permitted()
            }
            mutable.update { it.copy(notificationsAllowed = info) }
        }
    }

    fun reminder(schedule: Boolean) {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                try { ReminderProbe(getApplication()).let { if (schedule) it.schedule() else it.cancel() } }
                catch (_: RuntimeException) { "Reminder service unavailable. Session timing is unaffected." }
            }
            mutable.update { it.copy(reminder = info) }
        }
    }

    override fun onCleared() { database.close(); legacy?.close() }
}
