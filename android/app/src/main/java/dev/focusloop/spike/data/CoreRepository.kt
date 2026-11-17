package dev.focusloop.spike.data

import dev.focusloop.spike.domain.*
import java.util.concurrent.Callable

/** Synchronous transaction boundary; ViewModel always calls it on Dispatchers.IO. */
class CoreRepository(private val db: CoreDatabase, private val legacy: SessionStore? = null) {
    private val dao get() = db.core()
    private fun <T> transaction(block: () -> T): T = db.runInTransaction(Callable { block() })

    private fun initialize() {
        if (dao.metadata(IMPORT_KEY) != null) return
        legacy?.read()?.let { insert(it, null, false) }
        dao.saveMetadata(MetadataRow(IMPORT_KEY, "done"))
    }

    fun load(now: ClockMark, historyLimit: Int = 100): CoreSnapshot = transaction {
        initialize()
        val current = (dao.active() ?: dao.latest())?.let(::record)?.let { saved ->
            val projected = SessionRules.project(saved.session, now)
            if (projected.status == SessionStatus.RECOVERY_REQUIRED && projected != saved.session) persist(saved, projected)
            saved.copy(session = projected)
        }
        CoreSnapshot(current, dao.intentions().map { Intention(it.id, it.title, it.plannedMinutes, it.targetPackage, it.reminderPreferred) },
            dao.history(historyLimit).map(::record), dao.historyCount())
    }

    fun saveIntention(value: Intention): Long = transaction {
        initialize()
        val valid = IntentionRules.validate(value)
        val row = IntentionRow(valid.id, valid.title, valid.plannedMinutes, valid.targetPackage, valid.reminderPreferred)
        if (valid.id == 0L) dao.insertIntention(row) else {
            require(dao.updateIntention(row) == 1) { "Saved intention no longer exists." }
            valid.id
        }
    }

    fun deleteIntention(id: Long) = transaction { initialize(); dao.deleteIntention(id) }
    fun clearIntentions() = transaction { initialize(); dao.clearIntentions() }

    fun start(value: Intention, now: ClockMark, saveAsTemplate: Boolean = false): Long = transaction {
        initialize()
        val valid = IntentionRules.validate(value)
        val existing = dao.active()?.let(::record)?.session
        val session = SessionRules.start(existing, valid.title, valid.plannedMinutes, now)
        if (saveAsTemplate) saveIntention(valid)
        insert(session, valid.targetPackage, valid.reminderPreferred)
    }

    fun revise(id: Long, title: String, now: ClockMark) = transaction {
        initialize()
        val saved = record(requireNotNull(dao.session(id)))
        persist(saved, SessionRules.revise(saved.session, title, now))
    }

    fun find(id: Long): SessionRecord? = transaction { initialize(); dao.session(id)?.let(::record) }

    fun act(id: Long, kind: EventKind, now: ClockMark, addedMs: Long = 0) = transaction {
        initialize()
        val saved = record(requireNotNull(dao.session(id)) { "Session no longer exists." })
        val updated = SessionRules.transition(saved.session, kind, now, addedMs)
        persist(saved, updated)
    }

    fun reflect(id: Long, outcome: ReportedOutcome, now: ClockMark) = transaction {
        initialize()
        val saved = requireNotNull(dao.session(id)) { "Session no longer exists." }
        require(saved.status == SessionStatus.FINISHED.name) { "Finish the session before reflecting." }
        dao.saveReflection(ReflectionRow(id, outcome.name, now.wallMs))
    }

    fun clearHistory() {
        transaction {
            // Preserve the tombstone even if a corrupt legacy source could not be imported.
            dao.saveMetadata(MetadataRow(IMPORT_KEY, "done"))
            dao.clearSessions()
        }
        // An explicit, confirmed deletion clears both copies; failure is surfaced and retryable.
        legacy?.clear()
    }

    /** Save only recovery transitions; ticking does not write SQLite every second. */
    fun saveRecovery(id: Long, now: ClockMark) = transaction {
        initialize()
        val saved = record(requireNotNull(dao.session(id)))
        val updated = SessionRules.project(saved.session, now)
        if (updated.status == SessionStatus.RECOVERY_REQUIRED && updated != saved.session) persist(saved, updated)
    }

    private fun insert(session: Session, target: String?, reminder: Boolean): Long {
        val id = dao.insertSession(row(0, session, target, reminder))
        dao.insertEvents(session.events.mapIndexed { index, event -> eventRow(id, index, event) })
        return id
    }

    private fun persist(saved: SessionRecord, updated: Session) {
        require(updated.originalPlanMs == saved.session.originalPlanMs && updated.title == saved.session.title)
        require(updated.events.take(saved.session.events.size) == saved.session.events) { "Existing events cannot be overwritten." }
        check(dao.updateSession(row(saved.id, updated, saved.targetPackage, saved.reminderPreferred)) == 1)
        dao.insertEvents(updated.events.drop(saved.session.events.size).mapIndexed { index, event ->
            eventRow(saved.id, saved.session.events.size + index, event)
        })
    }

    private fun row(id: Long, session: Session, target: String?, reminder: Boolean) = SessionRow(id, session.title,
        session.originalPlanMs, session.anchor.wallMs, session.anchor.elapsedMs, session.anchor.boot,
        session.activeMs, session.elapsedMs, session.status.name, session.hasUnknownGap,
        if (session.isOpen) 1 else null, target, reminder)

    private fun eventRow(id: Long, index: Int, event: SessionEvent) = EventRow(id, index, event.kind.name, event.wallMs, event.addedMs, event.note)
    private fun record(row: SessionRow): SessionRecord {
        val events = dao.events(row.id).map { SessionEvent(EventKind.valueOf(it.kind), it.wallMs, it.addedMs, it.note) }
        val reflection = dao.reflection(row.id)?.let { Reflection(ReportedOutcome.valueOf(it.outcome), it.recordedWallMs) }
        return SessionRecord(row.id, Session(row.title, row.originalPlanMs, ClockMark(row.wallMs, row.elapsedAnchorMs, row.boot),
            row.activeMs, row.elapsedMs, SessionStatus.valueOf(row.status), row.unknownGap, events), row.targetPackage, row.reminderPreferred, reflection)
    }
    companion object { const val IMPORT_KEY = "legacy-phase1-import-v1" }
}
