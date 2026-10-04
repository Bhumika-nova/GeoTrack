package com.example.geotrack.data

import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for AttendanceRecord database operations.
 * Supports both suspend functions for single queries and Flow for reactive UI observation.
 */
interface AttendanceDao {

    suspend fun insert(record: AttendanceRecord): Long

    suspend fun update(record: AttendanceRecord)

    suspend fun getActiveRecord(): AttendanceRecord?

    fun getActiveRecordFlow(): Flow<AttendanceRecord?>

    suspend fun getUnsyncedCompletedRecords(): List<AttendanceRecord>

    suspend fun getCompletedRecords(): List<AttendanceRecord>

    fun getAllRecordsFlow(): Flow<List<AttendanceRecord>>

    suspend fun getAllRecords(): List<AttendanceRecord>

    fun getCompletedShiftsCountFlow(): Flow<Int>

    suspend fun getRecordById(id: Int): AttendanceRecord?

    suspend fun delete(record: AttendanceRecord)

    suspend fun deleteAll()
}
