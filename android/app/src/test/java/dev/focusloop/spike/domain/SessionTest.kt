package dev.focusloop.spike.domain

import org.junit.Assert.*
import org.junit.Test

class SessionTest {
    private fun at(elapsed: Long, boot: Int? = 7, wall: Long = 1_000_000 + elapsed) = ClockMark(wall, elapsed, boot)
    private fun started() = SessionRules.start(null, "Study", 25, at(1000))
    private class MemoryStore : SessionStore {
        var saved: Session? = null
        override fun read() = saved
        override fun write(session: Session) { saved = session }
        override fun clear() { saved = null }
    }

    @Test fun onlyOneOpenSession() {
        assertThrows(IllegalArgumentException::class.java) { SessionRules.start(started(), "Other", 5, at(2000)) }
    }
    @Test fun pausedSessionStillOwnsTheSlot() {
        val paused = SessionRules.transition(started(), EventKind.PAUSE, at(2000))
        assertThrows(IllegalArgumentException::class.java) { SessionRules.start(paused, "Other", 5, at(3000)) }
    }
    @Test fun validatesTitleAndDuration() {
        assertThrows(IllegalArgumentException::class.java) { SessionRules.start(null, " ", 25, at(0)) }
        for (minutes in listOf(0, -1, 1441)) assertThrows(IllegalArgumentException::class.java) {
            SessionRules.start(null, "Study", minutes, at(0))
        }
    }
    @Test fun activeExcludesPauseAndElapsedIncludesIt() {
        val pause = SessionRules.transition(started(), EventKind.PAUSE, at(6000))
        val resume = SessionRules.transition(pause, EventKind.RESUME, at(16000))
        val finish = SessionRules.transition(resume, EventKind.FINISH, at(20000))
        assertEquals(9000L, finish.activeMs)
        assertEquals(19000L, finish.elapsedMs)
        assertEquals(finish, SessionRules.project(finish, at(90000)))
    }
    @Test fun wallClockJumpsDoNotChangeDuration() {
        assertEquals(4000L, SessionRules.project(started(), at(5000, wall = -9000000)).activeMs)
        assertEquals(4000L, SessionRules.project(started(), at(5000, wall = 900000000)).activeMs)
    }
    @Test fun sleepAndBackgroundIntervalsAreIncludedFromMonotonicClock() {
        assertEquals(3_600_000L, SessionRules.project(started(), at(3_601_000)).activeMs)
    }
    @Test fun extensionAppendsAndPreservesOriginalPlan() {
        val original = started()
        val one = SessionRules.transition(original, EventKind.EXTEND, at(2000), 300000)
        val two = SessionRules.transition(one, EventKind.EXTEND, at(3000), 600000)
        assertEquals(original.originalPlanMs, two.originalPlanMs)
        assertEquals(original.originalPlanMs + 900000, two.targetMs)
        assertEquals(listOf(300000L, 600000L), two.events.filter { it.kind == EventKind.EXTEND }.map { it.addedMs })
    }
    @Test fun invalidExtensionRejected() {
        assertThrows(IllegalArgumentException::class.java) { SessionRules.transition(started(), EventKind.EXTEND, at(2000), -1) }
    }
    @Test fun targetDoesNotForceCompletion() {
        val past = SessionRules.project(started(), at(4_000_000))
        assertEquals(SessionStatus.RUNNING, past.status)
        assertTrue(past.activeMs > past.targetMs)
    }
    @Test fun newRepositoryRestoresFromPersistedAnchor() {
        val store = MemoryStore()
        SessionExperiment(store).start("Study", 25, at(1000))
        val restored = SessionExperiment(store).restore(at(11000))!!
        assertEquals(10000L, restored.activeMs)
        assertEquals(SessionStatus.RUNNING, restored.status)
    }
    @Test fun rebootEvenWithGreaterUptimeDoesNotInventDuration() {
        val pause = SessionRules.transition(started(), EventKind.PAUSE, at(6000))
        val recovered = SessionRules.project(pause, at(999999, boot = 8))
        assertEquals(SessionStatus.RECOVERY_REQUIRED, recovered.status)
        assertEquals(5000L, recovered.activeMs)
        assertEquals(5000L, recovered.elapsedMs)
        assertTrue(recovered.hasUnknownGap)
    }
    @Test fun runningRebootUsesOnlyPersistedCheckpoint() {
        val store = MemoryStore()
        val experiment = SessionExperiment(store)
        experiment.start("Study", 25, at(1000))
        experiment.act(EventKind.EXTEND, at(5000), 300000)
        val recovered = SessionExperiment(store).restore(at(10000, boot = 8))!!
        assertEquals(4000L, recovered.activeMs)
        assertEquals(recovered, store.saved)
        assertEquals(recovered, SessionExperiment(store).restore(at(11000, boot = 8)))
    }
    @Test fun recoveryResumesOnlyFromExplicitNewAnchor() {
        val interrupted = SessionRules.project(started(), at(10000, boot = 8))
        val resumed = SessionRules.transition(interrupted, EventKind.RESUME, at(20000, boot = 8))
        val timed = SessionRules.project(resumed, at(25000, boot = 8))
        assertEquals(5000L, timed.activeMs)
        assertEquals(5000L, timed.elapsedMs)
        assertTrue(timed.hasUnknownGap)
    }
    @Test fun unavailableBootIdentityIsExplicit() {
        val recovered = SessionRules.project(started(), at(10000, boot = null))
        assertEquals(SessionStatus.RECOVERY_REQUIRED, recovered.status)
        assertThrows(IllegalArgumentException::class.java) {
            SessionRules.transition(recovered, EventKind.RESUME, at(12000, boot = null))
        }
    }
    @Test fun backwardsMonotonicClockRequiresRecovery() {
        assertEquals(SessionStatus.RECOVERY_REQUIRED, SessionRules.project(started(), at(100)).status)
    }
    @Test fun finishAndCancelAreTerminalAndAllowAnotherExperiment() {
        for (kind in listOf(EventKind.FINISH, EventKind.CANCEL)) {
            val ended = SessionRules.transition(started(), kind, at(3000))
            assertFalse(ended.isOpen)
            assertThrows(IllegalArgumentException::class.java) { SessionRules.transition(ended, EventKind.RESUME, at(5000)) }
            assertEquals("Next", SessionRules.start(ended, "Next", 10, at(6000)).title)
        }
    }
    @Test fun invalidPauseResumeSequenceRejected() {
        assertThrows(IllegalArgumentException::class.java) { SessionRules.transition(started(), EventKind.RESUME, at(3000)) }
        val paused = SessionRules.transition(started(), EventKind.PAUSE, at(3000))
        assertThrows(IllegalArgumentException::class.java) { SessionRules.transition(paused, EventKind.PAUSE, at(5000)) }
    }
    @Test fun writeFailureDoesNotReportANewSession() {
        val store = object : SessionStore {
            override fun read(): Session? = null
            override fun write(session: Session) { error("disk full") }
            override fun clear() = Unit
        }
        assertThrows(IllegalStateException::class.java) { SessionExperiment(store).start("Study", 5, at(1000)) }
    }
}
