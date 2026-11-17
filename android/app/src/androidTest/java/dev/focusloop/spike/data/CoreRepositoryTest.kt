package dev.focusloop.spike.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.focusloop.spike.domain.*
import dev.focusloop.spike.platform.SessionDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CoreRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun now(ms: Long, boot: Int? = 7) = ClockMark(1_000_000 + ms, ms, boot)
    private val intention get() = Intention(title = "Synthetic study", plannedMinutes = 25)

    private fun isolated(block: (CoreDatabase, CoreRepository, String) -> Unit) {
        val name = "core-synthetic-${System.nanoTime()}.db"
        val db = CoreDatabase.open(context, name)
        try { block(db, CoreRepository(db), name) } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun twoSessionsSurviveReopenWithOriginalPlanAndExtensionEvents() = isolated { db, repo, name ->
        val first = repo.start(intention, now(1000))
        repo.act(first, EventKind.EXTEND, now(3000), 300000)
        repo.act(first, EventKind.PAUSE, now(4000))
        repo.act(first, EventKind.RESUME, now(9000))
        repo.act(first, EventKind.FINISH, now(12000))
        val second = repo.start(intention.copy(title = "Synthetic rest"), now(13000))
        db.close()
        val reopened = CoreDatabase.open(context, name)
        try {
            val snapshot = CoreRepository(reopened).load(now(18000))
            assertEquals(second, snapshot.current!!.id)
            assertEquals(5000L, snapshot.current!!.session.activeMs)
            val past = snapshot.history.single()
            assertEquals(first, past.id)
            assertEquals(1500000L, past.session.originalPlanMs)
            assertEquals(1800000L, past.session.targetMs)
            assertEquals(6000L, past.session.activeMs)
            assertEquals(11000L, past.session.elapsedMs)
            assertEquals(listOf(EventKind.START, EventKind.EXTEND, EventKind.PAUSE, EventKind.RESUME, EventKind.FINISH), past.session.events.map { it.kind })
            assertNull(past.reflection)
        } finally { reopened.close() }
    }

    @Test fun concurrentStartHasOnlyOneWinnerAndOneOpenRow() = isolated { _, repo, _ ->
        val executor = Executors.newFixedThreadPool(2)
        try {
            repo.load(now(0))
            val gate = CountDownLatch(1)
            val tasks = (1..2).map { executor.submit(Callable {
                gate.await(5, TimeUnit.SECONDS)
                try { repo.start(intention, now(1000)); true } catch (_: IllegalArgumentException) { false }
            }) }
            gate.countDown()
            assertEquals(1, tasks.count { it.get(15, TimeUnit.SECONDS) })
            assertTrue(repo.load(now(2000)).current!!.session.isOpen)
        } finally { executor.shutdownNow() }
    }

    @Test fun databaseUniqueIndexRejectsASecondOpenSlot() = isolated { db, repo, _ ->
        repo.start(intention, now(1000))
        val row = requireNotNull(db.core().active()).copy(id = 0)
        assertThrows(Exception::class.java) { db.core().insertSession(row) }
        assertNotNull(db.core().active())
    }

    @Test fun reflectionRequiresFinishAndNeverFollowsAutomaticallyFromTiming() = isolated { _, repo, _ ->
        val id = repo.start(intention, now(1000))
        assertThrows(IllegalArgumentException::class.java) { repo.reflect(id, ReportedOutcome.COMPLETED, now(2000)) }
        repo.act(id, EventKind.FINISH, now(3000))
        assertNull(repo.load(now(4000)).history.single().reflection)
        repo.reflect(id, ReportedOutcome.SKIPPED, now(5000))
        assertEquals(ReportedOutcome.SKIPPED, repo.load(now(6000)).history.single().reflection!!.outcome)
        repo.reflect(id, ReportedOutcome.PARTIAL, now(7000))
        assertEquals(ReportedOutcome.PARTIAL, repo.load(now(8000)).history.single().reflection!!.outcome)
        assertEquals(1, repo.load(now(8000)).history.size)
    }

    @Test fun templateEditsCannotAlterRecordedSessionPlanOrTarget() = isolated { _, repo, _ ->
        val original = intention.copy(targetPackage = "org.example.reader", reminderPreferred = true)
        val templateId = repo.saveIntention(original)
        assertNull(repo.load(now(1000)).current)
        val sessionId = repo.start(repo.load(now(1000)).intentions.single(), now(2000))
        repo.saveIntention(original.copy(id = templateId, title = "Changed", plannedMinutes = 10, targetPackage = null))
        val snapshot = repo.load(now(3000))
        assertEquals("Synthetic study", snapshot.current!!.session.title)
        assertEquals(1500000L, snapshot.current!!.session.originalPlanMs)
        assertEquals("org.example.reader", snapshot.current!!.targetPackage)
        assertTrue(snapshot.current!!.reminderPreferred)
        repo.deleteIntention(templateId)
        assertEquals(sessionId, repo.load(now(4000)).current!!.id)
        assertTrue(repo.load(now(4000)).intentions.isEmpty())
    }

    @Test fun invalidTransitionRollsBackStateAndEventWrites() = isolated { _, repo, _ ->
        val id = repo.start(intention, now(1000))
        val before = repo.load(now(1000))
        assertThrows(IllegalArgumentException::class.java) { repo.act(id, EventKind.RESUME, now(3000)) }
        assertEquals(before, repo.load(now(1000)))
        repo.act(id, EventKind.CANCEL, now(4000))
        assertNull(repo.load(now(5000)).history.single().reflection)
    }

    @Test fun legacyImportIsIdempotentPreservesSourceAndDeletionDoesNotResurrect() {
        val suffix = System.nanoTime()
        val oldName = "legacy-synthetic-$suffix.db"
        val coreName = "import-synthetic-$suffix.db"
        val legacy = SessionDatabase(context, oldName)
        val db = CoreDatabase.open(context, coreName)
        try {
            val original = SessionRules.transition(SessionRules.start(null, "Synthetic legacy", 10, now(1000)), EventKind.EXTEND, now(2000), 300000)
            legacy.write(original)
            val repo = CoreRepository(db, legacy)
            val first = repo.load(now(3000))
            assertEquals(original, legacy.read())
            assertEquals(first, repo.load(now(3000)))
            assertEquals(900000L, first.current!!.session.targetMs)
            assertEquals(2, db.core().events(first.current!!.id).size)
            repo.saveIntention(intention)
            repo.clearHistory()
            assertNull(legacy.read())
            val empty = repo.load(now(4000))
            assertNull(empty.current)
            assertTrue(empty.history.isEmpty())
            assertEquals(1, empty.intentions.size)
            assertEquals("done", db.core().metadata(CoreRepository.IMPORT_KEY))
        } finally { db.close(); legacy.close(); context.deleteDatabase(oldName); context.deleteDatabase(coreName) }
    }

    @Test fun importFailureLeavesNoMarkerAndCanRetry() = isolated { db, _, _ ->
        var fail = true
        val legacy = object : SessionStore {
            override fun read(): Session? {
                if (fail) error("Synthetic read error")
                return SessionRules.start(null, "Synthetic retry", 5, now(1000))
            }
            override fun write(session: Session) = Unit
            override fun clear() = Unit
        }
        val repo = CoreRepository(db, legacy)
        assertThrows(IllegalStateException::class.java) { repo.load(now(2000)) }
        assertNull(db.core().metadata(CoreRepository.IMPORT_KEY))
        assertNull(db.core().latest())
        fail = false
        assertEquals("Synthetic retry", repo.load(now(3000)).current!!.session.title)
    }

    @Test fun rebootRecoveryStaysUncertainInHistory() = isolated { _, repo, _ ->
        val id = repo.start(intention, now(1000))
        repo.act(id, EventKind.EXTEND, now(3000), 300000)
        val recovered = repo.load(now(9000, boot = 8))
        assertEquals(SessionStatus.RECOVERY_REQUIRED, recovered.current!!.session.status)
        assertEquals(2000L, recovered.current!!.session.activeMs)
        repo.act(id, EventKind.RESUME, now(10000, boot = 8))
        repo.act(id, EventKind.FINISH, now(12000, boot = 8))
        val past = repo.load(now(15000, boot = 8)).history.single()
        assertTrue(past.session.hasUnknownGap)
        assertEquals(4000L, past.session.activeMs)
    }

    @Test fun corruptDatabaseIsReportedWithoutAutomaticDeletion() {
        val name = "corrupt-synthetic-${System.nanoTime()}.db"
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        val original = "Synthetic invalid SQLite file".toByteArray()
        file.writeBytes(original)
        val db = CoreDatabase.open(context, name)
        try {
            assertThrows(Exception::class.java) { CoreRepository(db).load(now(1000)) }
            assertArrayEquals(original, file.readBytes())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun sourceDeletionFailureIsRetryableAndCannotResurrectHistory() = isolated { db, _, _ ->
        var value: Session? = SessionRules.start(null, "Synthetic deletion retry", 5, now(1000))
        var fail = true
        val legacy = object : SessionStore {
            override fun read() = value
            override fun write(session: Session) { value = session }
            override fun clear() { if (fail) error("Synthetic clear error") else value = null }
        }
        val repo = CoreRepository(db, legacy)
        repo.load(now(2000))
        assertThrows(IllegalStateException::class.java) { repo.clearHistory() }
        assertNotNull(value)
        assertNull(repo.load(now(3000)).current)
        fail = false
        repo.clearHistory()
        assertNull(value)
        assertTrue(repo.load(now(4000)).history.isEmpty())
    }
}
