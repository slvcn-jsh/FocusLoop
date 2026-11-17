package dev.focusloop.spike.domain

data class ClockMark(val wallMs: Long, val elapsedMs: Long, val boot: Int?)
enum class SessionStatus { RUNNING, PAUSED, RECOVERY_REQUIRED, FINISHED, CANCELLED }
enum class EventKind { START, PAUSE, RESUME, EXTEND, RECOVER, FINISH, CANCEL }
data class SessionEvent(val kind: EventKind, val wallMs: Long, val addedMs: Long = 0)

/** App-owned experiment data. Neither device usage nor a self-reported outcome. */
data class Session(
    val title: String,
    val originalPlanMs: Long,
    val anchor: ClockMark,
    val activeMs: Long,
    val elapsedMs: Long,
    val status: SessionStatus,
    val hasUnknownGap: Boolean,
    val events: List<SessionEvent>,
) {
    val isOpen: Boolean get() = status !in setOf(SessionStatus.FINISHED, SessionStatus.CANCELLED)
    val targetMs: Long get() = originalPlanMs + events.sumOf { it.addedMs }
}

object SessionRules {
    fun start(existing: Session?, title: String, minutes: Int, now: ClockMark): Session {
        require(existing?.isOpen != true) { "Finish or cancel the current session first." }
        require(title.isNotBlank() && title.length <= 120) { "Enter a title (1-120 characters)." }
        require(minutes in 1..1440) { "Use a duration from 1 to 1440 minutes." }
        require(now.boot != null) { "Boot identity unavailable; reliable timing cannot start." }
        return Session(title.trim(), minutes * 60_000L, now, 0, 0, SessionStatus.RUNNING,
            false, listOf(SessionEvent(EventKind.START, now.wallMs)))
    }

    /** Project from persisted monotonic anchors, never from a wall-clock subtraction. */
    fun project(session: Session, now: ClockMark): Session {
        if (!session.isOpen || session.status == SessionStatus.RECOVERY_REQUIRED) return session
        if (now.boot == null || session.anchor.boot == null || now.boot != session.anchor.boot ||
            now.elapsedMs < session.anchor.elapsedMs) {
            return session.copy(anchor = now, status = SessionStatus.RECOVERY_REQUIRED,
                hasUnknownGap = true, events = session.events + SessionEvent(EventKind.RECOVER, now.wallMs))
        }
        val delta = now.elapsedMs - session.anchor.elapsedMs
        return session.copy(anchor = now, elapsedMs = session.elapsedMs + delta,
            activeMs = session.activeMs + if (session.status == SessionStatus.RUNNING) delta else 0)
    }

    fun transition(session: Session, event: EventKind, now: ClockMark, addedMs: Long = 0): Session {
        val current = project(session, now)
        require(current.isOpen) { "This session has already ended." }
        val status = when (event) {
            EventKind.PAUSE -> {
                require(current.status == SessionStatus.RUNNING) { "Only a running session can pause." }
                SessionStatus.PAUSED
            }
            EventKind.RESUME -> {
                require(current.status in setOf(SessionStatus.PAUSED, SessionStatus.RECOVERY_REQUIRED))
                require(now.boot != null) { "Boot identity unavailable; timing cannot resume." }
                SessionStatus.RUNNING
            }
            EventKind.EXTEND -> {
                require(current.status in setOf(SessionStatus.RUNNING, SessionStatus.PAUSED))
                require(addedMs in 1..86_400_000L && current.targetMs + addedMs <= 604_800_000L)
                current.status
            }
            EventKind.FINISH -> SessionStatus.FINISHED
            EventKind.CANCEL -> SessionStatus.CANCELLED
            else -> error("Unsupported transition")
        }
        require(event == EventKind.EXTEND || addedMs == 0L)
        return current.copy(anchor = now, status = status,
            events = current.events + SessionEvent(event, now.wallMs, addedMs))
    }
}

interface SessionStore {
    fun read(): Session?
    fun write(session: Session)
    fun clear()
}

/** Caller serializes actions and executes storage off the UI thread. */
class SessionExperiment(private val store: SessionStore) {
    fun restore(now: ClockMark): Session? = store.read()?.let { saved ->
        val projected = SessionRules.project(saved, now)
        if (projected.status == SessionStatus.RECOVERY_REQUIRED && projected != saved) store.write(projected)
        projected
    }
    fun start(title: String, minutes: Int, now: ClockMark): Session =
        SessionRules.start(store.read(), title, minutes, now).also(store::write)
    fun act(event: EventKind, now: ClockMark, addedMs: Long = 0): Session =
        SessionRules.transition(requireNotNull(store.read()), event, now, addedMs).also(store::write)
    fun clear() = store.clear()
}
