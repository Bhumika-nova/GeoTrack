package com.example.geotrack.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Native SQLite Database Open Helper for GeoTracker.
 * Ensures instant, deterministic compilation without KSP JVM signature incompatibilities.
 */
class AppDatabaseOpenHelper(context: Context) :
    SQLiteOpenHelper(context, "geotracker_db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS attendance_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                officeName TEXT NOT NULL,
                checkInTime TEXT NOT NULL,
                checkInTimestamp INTEGER NOT NULL,
                checkOutTime TEXT,
                checkOutTimestamp INTEGER,
                synced INTEGER NOT NULL DEFAULT 0,
                firestoreId TEXT,
                userId TEXT NOT NULL,
                completed INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL,
                distanceMeters REAL NOT NULL DEFAULT 0,
                wifiSsid TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS attendance_records")
        onCreate(db)
    }
}

/**
 * Thread-safe DAO implementation backed by SQLite with reactive Coroutines Flow support.
 */
class AttendanceDaoImpl(private val dbHelper: AppDatabaseOpenHelper) : AttendanceDao {

    // Reactive trigger flow that emits whenever database records change
    private val changeTrigger = MutableStateFlow(System.currentTimeMillis())

    private fun notifyDataChanged() {
        changeTrigger.value = System.currentTimeMillis()
    }

    override suspend fun insert(record: AttendanceRecord): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            if (record.id > 0) put("id", record.id)
            put("officeName", record.officeName)
            put("checkInTime", record.checkInTime)
            put("checkInTimestamp", record.checkInTimestamp)
            put("checkOutTime", record.checkOutTime)
            put("checkOutTimestamp", record.checkOutTimestamp)
            put("synced", if (record.synced) 1 else 0)
            put("firestoreId", record.firestoreId)
            put("userId", record.userId)
            put("completed", if (record.completed) 1 else 0)
            put("status", record.status)
            put("distanceMeters", record.distanceMeters)
            put("wifiSsid", record.wifiSsid)
        }
        val rowId = db.insertWithOnConflict(
            "attendance_records",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        notifyDataChanged()
        rowId
    }

    override suspend fun update(record: AttendanceRecord): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("officeName", record.officeName)
            put("checkInTime", record.checkInTime)
            put("checkInTimestamp", record.checkInTimestamp)
            put("checkOutTime", record.checkOutTime)
            put("checkOutTimestamp", record.checkOutTimestamp)
            put("synced", if (record.synced) 1 else 0)
            put("firestoreId", record.firestoreId)
            put("userId", record.userId)
            put("completed", if (record.completed) 1 else 0)
            put("status", record.status)
            put("distanceMeters", record.distanceMeters)
            put("wifiSsid", record.wifiSsid)
        }
        db.update("attendance_records", values, "id = ?", arrayOf(record.id.toString()))
        notifyDataChanged()
    }

    override suspend fun getActiveRecord(): AttendanceRecord? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM attendance_records WHERE completed = 0 AND checkOutTime IS NULL ORDER BY id DESC LIMIT 1",
            null
        )
        cursor.use {
            if (it.moveToFirst()) cursorToRecord(it) else null
        }
    }

    override fun getActiveRecordFlow(): Flow<AttendanceRecord?> {
        return changeTrigger.map {
            getActiveRecord()
        }
    }

    override suspend fun getUnsyncedCompletedRecords(): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<AttendanceRecord>()
        val cursor = db.rawQuery(
            "SELECT * FROM attendance_records WHERE completed = 1 AND synced = 0 ORDER BY id ASC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToRecord(it))
            }
        }
        list
    }

    override suspend fun getCompletedRecords(): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<AttendanceRecord>()
        val cursor = db.rawQuery(
            "SELECT * FROM attendance_records WHERE completed = 1 ORDER BY id DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToRecord(it))
            }
        }
        list
    }

    override fun getAllRecordsFlow(): Flow<List<AttendanceRecord>> {
        return changeTrigger.map {
            getAllRecords()
        }
    }

    override suspend fun getAllRecords(): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<AttendanceRecord>()
        val cursor = db.rawQuery(
            "SELECT * FROM attendance_records ORDER BY id DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToRecord(it))
            }
        }
        list
    }

    override fun getCompletedShiftsCountFlow(): Flow<Int> {
        return changeTrigger.map {
            withContext(Dispatchers.IO) {
                val db = dbHelper.readableDatabase
                val cursor = db.rawQuery("SELECT COUNT(*) FROM attendance_records WHERE completed = 1", null)
                cursor.use {
                    if (it.moveToFirst()) it.getInt(0) else 0
                }
            }
        }
    }

    override suspend fun getRecordById(id: Int): AttendanceRecord? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM attendance_records WHERE id = ? LIMIT 1",
            arrayOf(id.toString())
        )
        cursor.use {
            if (it.moveToFirst()) cursorToRecord(it) else null
        }
    }

    override suspend fun delete(record: AttendanceRecord): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("attendance_records", "id = ?", arrayOf(record.id.toString()))
        notifyDataChanged()
    }

    override suspend fun deleteAll(): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("attendance_records", null, null)
        notifyDataChanged()
    }

    private fun cursorToRecord(cursor: Cursor): AttendanceRecord {
        val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
        val officeName = cursor.getString(cursor.getColumnIndexOrThrow("officeName"))
        val checkInTime = cursor.getString(cursor.getColumnIndexOrThrow("checkInTime"))
        val checkInTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow("checkInTimestamp"))
        val checkOutTime = getNullableString(cursor, "checkOutTime")
        val checkOutTimestamp = getNullableLong(cursor, "checkOutTimestamp")
        val synced = cursor.getInt(cursor.getColumnIndexOrThrow("synced")) == 1
        val firestoreId = getNullableString(cursor, "firestoreId")
        val userId = cursor.getString(cursor.getColumnIndexOrThrow("userId"))
        val completed = cursor.getInt(cursor.getColumnIndexOrThrow("completed")) == 1
        val status = cursor.getString(cursor.getColumnIndexOrThrow("status"))
        val distanceMeters = cursor.getFloat(cursor.getColumnIndexOrThrow("distanceMeters"))
        val wifiSsid = getNullableString(cursor, "wifiSsid")

        return AttendanceRecord(
            id = id,
            officeName = officeName,
            checkInTime = checkInTime,
            checkInTimestamp = checkInTimestamp,
            checkOutTime = checkOutTime,
            checkOutTimestamp = checkOutTimestamp,
            synced = synced,
            firestoreId = firestoreId,
            userId = userId,
            completed = completed,
            status = status,
            distanceMeters = distanceMeters,
            wifiSsid = wifiSsid
        )
    }

    private fun getNullableString(cursor: Cursor, columnName: String): String? {
        val index = cursor.getColumnIndexOrThrow(columnName)
        return if (cursor.isNull(index)) null else cursor.getString(index)
    }

    private fun getNullableLong(cursor: Cursor, columnName: String): Long? {
        val index = cursor.getColumnIndexOrThrow(columnName)
        return if (cursor.isNull(index)) null else cursor.getLong(index)
    }
}

/**
 * Singleton database access for GeoTracker.
 */
abstract class AppDatabase {

    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val helper = AppDatabaseOpenHelper(context.applicationContext)
                val dao = AttendanceDaoImpl(helper)
                val instance = object : AppDatabase() {
                    override fun attendanceDao(): AttendanceDao = dao
                }
                INSTANCE = instance
                instance
            }
        }
    }
}
