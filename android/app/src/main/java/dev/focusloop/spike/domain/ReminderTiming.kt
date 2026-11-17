package dev.focusloop.spike.domain

/** Null means ineligible; zero means due. A reboot never reconstructs an unknown interval. */
fun reminderDelayMs(record: SessionRecord?, now: ClockMark): Long? {
    if (record == null || !record.reminderPreferred) return null
    val session = SessionRules.project(record.session, now)
    if (session.status != SessionStatus.RUNNING) return null
    return (session.targetMs - session.activeMs).coerceAtLeast(0)
}
