package dev.focusloop.spike.platform

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteOpenHelper
import dev.focusloop.spike.domain.*
import org.json.JSONArray
import org.json.JSONObject

/** Single experimental record; production Room history is deliberately deferred. */
class SessionDatabase(private val context: Context, private val name: String = "feasibility.db") :
    SQLiteOpenHelper(context, name, null, 1, {
        throw SQLiteDatabaseCorruptException("Experimental database corrupt; automatic deletion is disabled.")
    }), SessionStore {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE session (id INTEGER PRIMARY KEY CHECK(id=1), payload TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("No destructive migration allowed; add an explicit migration.")
    }
    override fun read(): Session? = readableDatabase.rawQuery("SELECT payload FROM session WHERE id=1", null).use {
        if (it.moveToFirst()) decodeSession(it.getString(0)) else null
    }
    override fun write(session: Session) {
        val values = ContentValues().apply { put("id", 1); put("payload", encodeSession(session)) }
        check(writableDatabase.insertWithOnConflict("session", null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L)
    }
    override fun clear() {
        try { writableDatabase.delete("session", null, null) }
        catch (_: SQLiteDatabaseCorruptException) {
            // This path is reached only after the user explicitly confirms clearing.
            close()
            check(context.deleteDatabase(name)) { "Could not clear corrupt experimental database." }
        }
    }
}

internal fun encodeSession(s: Session): String = JSONObject().apply {
    put("version", 1); put("title", s.title); put("plan", s.originalPlanMs)
    put("wall", s.anchor.wallMs); put("clock", s.anchor.elapsedMs); put("boot", s.anchor.boot ?: JSONObject.NULL)
    put("active", s.activeMs); put("elapsed", s.elapsedMs); put("status", s.status.name)
    put("unknown", s.hasUnknownGap)
    put("events", JSONArray().apply {
        s.events.forEach { e -> put(JSONObject().apply {
            put("kind", e.kind.name); put("wall", e.wallMs); put("added", e.addedMs)
        }) }
    })
}.toString()

internal fun decodeSession(raw: String): Session {
    val j = JSONObject(raw)
    check(j.getInt("version") == 1) { "Unsupported session format. Record retained." }
    val events = j.getJSONArray("events")
    return Session(j.getString("title"), j.getLong("plan"),
        ClockMark(j.getLong("wall"), j.getLong("clock"), if (j.isNull("boot")) null else j.getInt("boot")),
        j.getLong("active"), j.getLong("elapsed"), SessionStatus.valueOf(j.getString("status")),
        j.getBoolean("unknown"), List(events.length()) { index ->
            events.getJSONObject(index).let {
                SessionEvent(EventKind.valueOf(it.getString("kind")), it.getLong("wall"), it.getLong("added"))
            }
        }).also {
        check(it.originalPlanMs in 60_000L..86_400_000L && it.activeMs >= 0 && it.elapsedMs >= it.activeMs)
        check(it.events.isNotEmpty() && it.events.first().kind == EventKind.START)
        check(it.events.all { e -> e.addedMs >= 0 && (e.kind == EventKind.EXTEND || e.addedMs == 0L) })
    }
}
