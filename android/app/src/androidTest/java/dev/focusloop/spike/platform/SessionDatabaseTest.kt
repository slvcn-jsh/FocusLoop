package dev.focusloop.spike.platform

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.focusloop.spike.domain.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun sqliteReopenRetainsPlanEventsAndMonotonicAnchor() {
        val name = "session-persistence-test.db"
        context.deleteDatabase(name)
        try {
            val first = SessionDatabase(context, name)
            val experiment = SessionExperiment(first)
            experiment.start("Synthetic persistence test", 25, ClockMark(100000, 1000, 7))
            experiment.act(EventKind.EXTEND, ClockMark(102000, 3000, 7), 300000)
            first.close()
            val second = SessionDatabase(context, name)
            val restored = SessionExperiment(second).restore(ClockMark(109000, 10000, 7))!!
            assertEquals(9000L, restored.activeMs)
            assertEquals(1500000L, restored.originalPlanMs)
            assertEquals(1800000L, restored.targetMs)
            assertEquals(2, restored.events.size)
            second.clear()
            assertNull(second.read())
            second.close()
        } finally { context.deleteDatabase(name) }
    }

    @Test fun codecPreservesNullableBootAndUnknownGap() {
        val interrupted = SessionRules.project(SessionRules.start(null, "Synthetic", 1, ClockMark(1, 1, 7)), ClockMark(5, 2, null))
        assertEquals(interrupted, decodeSession(encodeSession(interrupted)))
    }

    @Test fun malformedRecordIsNotSilentlyReplaced() {
        assertThrows(Exception::class.java) { decodeSession("not json") }
    }
}
