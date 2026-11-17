package dev.focusloop.spike

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.focusloop.spike.domain.*
import dev.focusloop.spike.platform.*
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
    val session: Session? = null,
    val sessionLoaded: Boolean = false,
    val sessionError: String? = null,
    val busy: Boolean = false,
    val period: UsagePeriod = UsagePeriod.TODAY,
    val usage: UsageSnapshot? = null,
    val reminder: String = "Reminders are optional. No probe scheduled by this launch.",
    val restored: Boolean = false,
)

class SpikeViewModel(app: Application) : AndroidViewModel(app) {
    private val database = SessionDatabase(app)
    private val sessions = SessionExperiment(database)
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

    fun reloadSession() = change {
        val restored = sessions.restore(clock.read())
        mutable.update { it.copy(restored = restored != null) }
        restored
    }

    fun start(title: String, minutes: String) = change {
        sessions.start(title, minutes.toIntOrNull() ?: 0, clock.read()).also {
            mutable.update { current -> current.copy(restored = false) }
        }
    }

    fun act(event: EventKind) = change {
        sessions.act(event, clock.read(), if (event == EventKind.EXTEND) 300_000 else 0)
    }

    fun clear() = change { sessions.clear(); mutable.update { it.copy(restored = false) }; null }

    private fun change(operation: () -> Session?) {
        viewModelScope.launch {
            lock.withLock {
                mutable.update { it.copy(busy = true) }
                try {
                    val session = withContext(Dispatchers.IO) { operation() }
                    mutable.update { it.copy(session = session, sessionLoaded = true, sessionError = null) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    mutable.update { it.copy(sessionError = if (e is IllegalArgumentException) e.message
                        else "Could not read or save the session. Existing record retained; retry or explicitly clear it.") }
                } finally { mutable.update { it.copy(busy = false) } }
            }
        }
    }

    /** Only while STARTED. No timer service, wake lock, or per-second database writes. */
    suspend fun tick() {
        lock.withLock {
            val saved = state.value.session
            if (saved != null && state.value.sessionError == null) {
                val now = withContext(Dispatchers.IO) { clock.read() }
                val projected = SessionRules.project(saved, now)
                try {
                    if (projected.status == SessionStatus.RECOVERY_REQUIRED && projected != saved) {
                        withContext(Dispatchers.IO) { database.write(projected) }
                    }
                    mutable.update { it.copy(session = projected) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    mutable.update { it.copy(sessionError = "Recovery could not be saved. Existing record retained.") }
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

    override fun onCleared() { database.close() }
}
