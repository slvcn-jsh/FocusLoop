package dev.focusloop.spike

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.focusloop.spike.domain.*
import dev.focusloop.spike.platform.*
import dev.focusloop.spike.data.CoreDatabase
import dev.focusloop.spike.data.CoreRepository
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
    val reminder: String = "Reminders are optional. No probe scheduled by this launch.",
    val restored: Boolean = false,
) { val session: Session? get() = core.current?.session }

class SpikeViewModel(app: Application) : AndroidViewModel(app) {
    private val database = CoreDatabase.open(app)
    private val legacy = if (app.getDatabasePath("feasibility.db").exists()) SessionDatabase(app) else null
    private val sessions = CoreRepository(database, legacy)
    private val clock = AndroidClock(app)
    private val usage = AndroidUsage(app)
    private val lock = Mutex()
    private var queryJob: Job? = null
    private val mutable = MutableStateFlow(SpikeState())
    val state = mutable.asStateFlow()

    fun foreground() {
        reloadSession()
        refreshUsage()
        reminderStatus()
    }

    fun hideUsage() {
        queryJob?.cancel()
        mutable.update { it.copy(usage = null) }
    }

    fun refreshUsage(period: UsagePeriod = state.value.period) {
        queryJob?.cancel()
        mutable.update { it.copy(period = period, usage = null) }
        queryJob = viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) { usage.query(period) }
            mutable.update { it.copy(usage = snapshot) }
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

    private fun change(operation: () -> Unit) {
        viewModelScope.launch {
            lock.withLock {
                mutable.update { it.copy(busy = true) }
                try {
                    val snapshot = withContext(Dispatchers.IO) { operation(); sessions.load(clock.read()) }
                    mutable.update { it.copy(core = snapshot, sessionLoaded = true, sessionError = null, storageFailed = false) }
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
                            sessions.load(now)
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
                ReminderProbe(getApplication()).let { "Notifications ${if (it.permitted()) "enabled" else "disabled or not granted"}. ${it.deliveryInfo()}" }
            }
            mutable.update { it.copy(reminder = info) }
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
