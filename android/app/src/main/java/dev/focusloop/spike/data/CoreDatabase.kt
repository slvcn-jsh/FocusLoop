package dev.focusloop.spike.data

import android.content.Context
import android.database.sqlite.SQLiteDatabaseCorruptException
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.room.migration.Migration

@Entity(tableName = "intentions")
data class IntentionRow(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String,
    val plannedMinutes: Int, val targetPackage: String?, val reminderPreferred: Boolean)

@Entity(tableName = "sessions", indices = [Index(value = ["activeSlot"], unique = true)])
data class SessionRow(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String,
    val originalPlanMs: Long, val wallMs: Long, val elapsedAnchorMs: Long, val boot: Int?,
    val activeMs: Long, val elapsedMs: Long, val status: String, val unknownGap: Boolean,
    val activeSlot: Int?, val targetPackage: String?, val reminderPreferred: Boolean)

@Entity(tableName = "session_events", primaryKeys = ["sessionId", "position"], foreignKeys = [
    ForeignKey(entity = SessionRow::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)])
data class EventRow(val sessionId: Long, val position: Int, val kind: String, val wallMs: Long, val addedMs: Long, val note: String? = null)

@Entity(tableName = "reflections", foreignKeys = [
    ForeignKey(entity = SessionRow::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)])
data class ReflectionRow(@PrimaryKey val sessionId: Long, val outcome: String, val recordedWallMs: Long)

@Entity(tableName = "core_metadata")
data class MetadataRow(@PrimaryKey val key: String, val value: String)

@Dao
interface CoreDao {
    @Query("SELECT * FROM sessions WHERE activeSlot = 1 LIMIT 1") fun active(): SessionRow?
    @Query("SELECT * FROM sessions ORDER BY id DESC LIMIT 1") fun latest(): SessionRow?
    @Query("SELECT * FROM sessions WHERE id = :id") fun session(id: Long): SessionRow?
    @Query("SELECT * FROM sessions WHERE activeSlot IS NULL ORDER BY id DESC LIMIT :limit") fun history(limit: Int = 100): List<SessionRow>
    @Query("SELECT COUNT(*) FROM sessions WHERE activeSlot IS NULL") fun historyCount(): Int
    @Query("SELECT * FROM session_events WHERE sessionId = :id ORDER BY position") fun events(id: Long): List<EventRow>
    @Query("SELECT * FROM reflections WHERE sessionId = :id") fun reflection(id: Long): ReflectionRow?
    @Query("SELECT * FROM intentions ORDER BY id DESC") fun intentions(): List<IntentionRow>
    @Query("SELECT value FROM core_metadata WHERE `key` = :key") fun metadata(key: String): String?
    @Insert fun insertSession(value: SessionRow): Long
    @Update fun updateSession(value: SessionRow): Int
    @Insert fun insertEvents(values: List<EventRow>)
    @Insert fun insertIntention(value: IntentionRow): Long
    @Update fun updateIntention(value: IntentionRow): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun saveReflection(value: ReflectionRow)
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun saveMetadata(value: MetadataRow)
    @Query("DELETE FROM sessions") fun clearSessions()
    @Query("DELETE FROM intentions WHERE id = :id") fun deleteIntention(id: Long)
    @Query("DELETE FROM intentions") fun clearIntentions()
}

@Database(entities = [IntentionRow::class, SessionRow::class, EventRow::class, ReflectionRow::class, MetadataRow::class],
    version = 2, exportSchema = true)
abstract class CoreDatabase : RoomDatabase() {
    abstract fun core(): CoreDao
    companion object {
        fun open(context: Context, name: String = "focusloop-core.db"): CoreDatabase =
            Room.databaseBuilder(context.applicationContext, CoreDatabase::class.java, name)
                .openHelperFactory(PreservingOpenHelperFactory).addMigrations(MIGRATION_1_2).build()
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE session_events ADD COLUMN note TEXT") }
        }
        // No destructive migration or main-thread-query fallback.
    }
}

/** Keep Android's default corruption handler from silently deleting app-owned records. */
private object PreservingOpenHelperFactory : SupportSQLiteOpenHelper.Factory {
    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val delegate = configuration.callback
        val callback = object : SupportSQLiteOpenHelper.Callback(delegate.version) {
            override fun onCreate(db: SupportSQLiteDatabase) = delegate.onCreate(db)
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = delegate.onUpgrade(db, oldVersion, newVersion)
            override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = delegate.onDowngrade(db, oldVersion, newVersion)
            override fun onConfigure(db: SupportSQLiteDatabase) = delegate.onConfigure(db)
            override fun onOpen(db: SupportSQLiteDatabase) = delegate.onOpen(db)
            override fun onCorruption(db: SupportSQLiteDatabase) {
                throw SQLiteDatabaseCorruptException("Local records are corrupt; automatic deletion is disabled.")
            }
        }
        return FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
            .name(configuration.name).callback(callback).build())
    }
}
