package dev.focusloop.spike.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.focusloop.spike.domain.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiIntegrationStorageTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun now(ms: Long) = ClockMark(1_000_000 + ms, ms, 7)

    @Test fun versionOneMigrationKeepsExistingSessionAndRevisionPersists() {
        val name = "synthetic-ui-migration-${System.nanoTime()}.db"
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("dev.focusloop.spike.data.CoreDatabase/1.json").bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val path = context.getDatabasePath(name); path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i); val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("${'$'}{TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices")
                for (j in 0 until (indices?.length() ?: 0)) old.execSQL(indices!!.getJSONObject(j).getString("createSql").replace("${'$'}{TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO sessions(id,title,originalPlanMs,wallMs,elapsedAnchorMs,boot,activeMs,elapsedMs,status,unknownGap,activeSlot,targetPackage,reminderPreferred) VALUES(7,'Synthetic migration',1500000,1001000,1000,7,0,0,'RUNNING',0,1,NULL,0)")
            old.execSQL("INSERT INTO session_events(sessionId,position,kind,wallMs,addedMs) VALUES(7,0,'START',1001000,0)")
            old.version = 1
        }
        val db = CoreDatabase.open(context, name)
        try {
            val repo = CoreRepository(db)
            val initial = repo.load(now(2000)).current!!
            assertEquals("Synthetic migration", initial.session.title)
            assertEquals(1500000L, initial.session.originalPlanMs)
            assertNull(initial.session.events.first().note)
            repo.revise(7, "Revised synthetic purpose", now(3000))
            val revised = repo.find(7)!!
            assertEquals("Synthetic migration", revised.session.title)
            assertEquals("Revised synthetic purpose", revised.session.currentTitle)
            assertEquals(initial.session.events.first(), revised.session.events.first())
            assertEquals(2, revised.session.events.size)
            assertEquals(2, db.openHelper.writableDatabase.version)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun startingAndSavingIsAtomicWhenAnotherSessionAlreadyExists() {
        val name = "synthetic-ui-atomic-${System.nanoTime()}.db"
        val db = CoreDatabase.open(context, name)
        try {
            val repo = CoreRepository(db)
            repo.start(Intention(title = "Synthetic first", plannedMinutes = 10), now(1000), saveAsTemplate = true)
            assertThrows(IllegalArgumentException::class.java) {
                repo.start(Intention(title = "Synthetic second", plannedMinutes = 5), now(2000), saveAsTemplate = true)
            }
            assertEquals(listOf("Synthetic first"), repo.load(now(3000)).intentions.map { it.title })
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun deletingAllPlansKeepsSessionsAndDeletingHistoryClearsReflections() {
        val name = "synthetic-ui-deletion-${System.nanoTime()}.db"
        val db = CoreDatabase.open(context, name)
        try {
            val repo = CoreRepository(db)
            val id = repo.start(Intention(title = "Synthetic retained session", plannedMinutes = 10), now(1000), true)
            repo.clearIntentions()
            assertTrue(repo.load(now(2000)).intentions.isEmpty())
            assertEquals("Synthetic retained session", repo.load(now(2000)).current!!.session.title)
            repo.act(id, EventKind.FINISH, now(3000))
            repo.reflect(id, ReportedOutcome.SKIPPED, now(4000))
            repo.clearHistory()
            assertNull(db.core().reflection(id))
            assertTrue(db.core().events(id).isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
