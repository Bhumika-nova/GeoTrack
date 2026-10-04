package com.example.geotrack.data

/**
 * Represents a single employee attendance session.
 * Single source of truth stored locally in SQLite database.
 */
data class AttendanceRecord(
    val id: Int = 0,
    val officeName: String = "Headquarters",
    val checkInTime: String,                  // Formatted string (e.g., "02:52 PM")
    val checkInTimestamp: Long = System.currentTimeMillis(), // Epoch milliseconds for duration calculation
    var checkOutTime: String? = null,         // Null while session is active
    var checkOutTimestamp: Long? = null,
    var synced: Boolean = false,              // true once uploaded to Firestore
    var firestoreId: String? = null,          // Cloud Firestore Document ID
    var userId: String = "",                  // Firebase User UID
    var completed: Boolean = false,           // true once checked out
    var status: String = "IN_OFFICE_VERIFIED", // e.g., "IN_OFFICE_VERIFIED", "CHECKED_OUT"
    var distanceMeters: Float = 0f,
    var wifiSsid: String? = null
)
