package com.example.geotrack.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.geotrack.data.AppDatabase
import com.example.geotrack.data.AttendanceRecord
import com.example.geotrack.service.LocationForegroundService
import com.example.geotrack.utils.Constants
import com.example.geotrack.utils.NotificationHelper
import com.example.geotrack.utils.WifiValidator
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BroadcastReceiver triggered by Android OS when device crosses the 150m Office Geofence boundary.
 * Executes zero-battery transition handling and updates the offline Room SQLite database.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            return
        }

        val transition = geofencingEvent.geofenceTransition
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (transition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        handleEnterTransition(context)
                    }
                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        handleExitTransition(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleEnterTransition(context: Context) {
        if (!WifiValidator.isConnectedToOfficeWifi(context)) {
            NotificationHelper.showWifiWarningNotification(context)
            return
        }

        val database = AppDatabase.getDatabase(context)
        val dao = database.attendanceDao()

        // Prevent duplicate active sessions
        val activeRecord = dao.getActiveRecord()
        if (activeRecord != null) {
            return
        }

        val now = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val checkInTimeString = timeFormat.format(Date(now))

        val currentSsid = WifiValidator.getConnectedWifiSsid(context)
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val userId = prefs.getString(Constants.KEY_USER_ID, "") ?: ""

        val newRecord = AttendanceRecord(
            officeName = Constants.OFFICE_NAME,
            checkInTime = checkInTimeString,
            checkInTimestamp = now,
            userId = userId,
            completed = false,
            status = "IN_OFFICE_VERIFIED",
            wifiSsid = currentSsid
        )

        dao.insert(newRecord)

        // Start Ongoing Foreground Service & Show Alert
        LocationForegroundService.start(context, checkInTimeString)
        NotificationHelper.showCheckInAlert(context, Constants.OFFICE_NAME, checkInTimeString)

        // Notify UI to update Status Pill & Active Card
        sendUpdateUiBroadcast(context)
    }

    private suspend fun handleExitTransition(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val dao = database.attendanceDao()

        val activeRecord = dao.getActiveRecord() ?: return

        val now = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val checkOutTimeString = timeFormat.format(Date(now))

        activeRecord.checkOutTime = checkOutTimeString
        activeRecord.checkOutTimestamp = now
        activeRecord.completed = true
        activeRecord.status = "CHECKED_OUT"

        dao.update(activeRecord)

        // Stop Ongoing Foreground Service
        LocationForegroundService.stop(context)

        // Enqueue Guaranteed Cloud Sync via WorkManager (NetworkType.CONNECTED)
        com.example.geotrack.service.SyncWorker.enqueueSync(context)

        // Show checkout alert
        NotificationHelper.showCheckOutAlert(context, Constants.OFFICE_NAME, checkOutTimeString)

        // Notify UI
        sendUpdateUiBroadcast(context)
    }

    companion object {
        fun sendUpdateUiBroadcast(context: Context) {
            val intent = Intent(Constants.ACTION_UPDATE_UI).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        }
    }
}
