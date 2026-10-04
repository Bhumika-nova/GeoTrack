package com.example.geotrack.data

import android.content.Context
import com.example.geotrack.receiver.GeofenceBroadcastReceiver
import com.example.geotrack.utils.Constants
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Helper class for synchronizing offline Room SQLite attendance records with Cloud Firebase Firestore.
 */
object FirestoreSyncHelper {

    suspend fun syncUnsyncedRecords(context: Context): Int {
        val database = AppDatabase.getDatabase(context)
        val dao = database.attendanceDao()
        val unsyncedRecords = dao.getUnsyncedCompletedRecords()

        if (unsyncedRecords.isEmpty()) {
            return 0
        }

        val firestore = FirebaseFirestore.getInstance()
        val attendanceCollection = firestore.collection("attendance")
        var syncedCount = 0

        for (record in unsyncedRecords) {
            try {
                val payload = hashMapOf<String, Any?>(
                    "officeName" to record.officeName,
                    "checkInTime" to record.checkInTime,
                    "checkInTimestamp" to record.checkInTimestamp,
                    "checkOutTime" to record.checkOutTime,
                    "checkOutTimestamp" to record.checkOutTimestamp,
                    "userId" to record.userId,
                    "status" to record.status,
                    "wifiSsid" to record.wifiSsid,
                    "syncedAt" to FieldValue.serverTimestamp()
                )

                if (record.firestoreId.isNullOrEmpty()) {
                    val documentRef = attendanceCollection.add(payload).await()
                    record.firestoreId = documentRef.id
                    record.synced = true
                } else {
                    attendanceCollection.document(record.firestoreId!!).set(payload).await()
                    record.synced = true
                }

                dao.update(record)
                syncedCount++
            } catch (e: Exception) {
                android.util.Log.e("FirestoreSyncHelper", "Failed to sync attendance record #${record.id} to Firestore: ${e.message}", e)
                // Keep synced = false so it will be retried on next connection
            }
        }

        if (syncedCount > 0) {
            GeofenceBroadcastReceiver.sendUpdateUiBroadcast(context)
        }

        return syncedCount
    }
}
