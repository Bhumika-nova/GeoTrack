package com.example.geotrack.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.example.geotrack.data.AppDatabase
import com.example.geotrack.service.LocationForegroundService
import com.example.geotrack.service.SyncWorker
import com.example.geotrack.utils.Constants
import com.example.geotrack.utils.GeofenceHelper
import com.example.geotrack.utils.NotificationHelper
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BootReceiver: Restores 150m Office Geofences and recovers interrupted sessions
 * after phone reboot or shutdown (android.intent.action.BOOT_COMPLETED).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Re-register 150m Office Geofence with Android OS Location Framework
                GeofenceHelper.registerOfficeGeofence(context)

                // 2. Query Room DB for any active dangling session from before the reboot
                val database = AppDatabase.getDatabase(context)
                val dao = database.attendanceDao()
                val activeRecord = dao.getActiveRecord()

                if (activeRecord != null) {
                    handleDanglingSessionRecovery(context, activeRecord, dao)
                }
            } catch (_: Exception) {
                // Ignore unexpected boot errors
            } finally {
                pendingResult.finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun handleDanglingSessionRecovery(
        context: Context,
        record: com.example.geotrack.data.AttendanceRecord,
        dao: com.example.geotrack.data.AttendanceDao
    ) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        var isInsideOffice = false

        try {
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null) {
                val distanceResults = FloatArray(1)
                Location.distanceBetween(
                    lastLocation.latitude,
                    lastLocation.longitude,
                    Constants.GEOFENCE_LAT,
                    Constants.GEOFENCE_LON,
                    distanceResults
                )
                val distanceMeters = distanceResults[0]
                isInsideOffice = distanceMeters <= Constants.GEOFENCE_RADIUS_METERS
            }
        } catch (_: Exception) {
            // Default to outside if location unavailable
            isInsideOffice = false
        }

        if (isInsideOffice) {
            // Resume live in-office monitoring
            LocationForegroundService.start(context, record.checkInTime)
        } else {
            // Auto-close session as employee is no longer in the office
            val now = System.currentTimeMillis()
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val checkOutTimeString = timeFormat.format(Date(now))

            record.checkOutTime = checkOutTimeString
            record.checkOutTimestamp = now
            record.completed = true
            record.status = "CHECKED_OUT"

            dao.update(record)

            // Enqueue guaranteed cloud sync
            SyncWorker.enqueueSync(context)

            // Notify user
            NotificationHelper.showCheckOutAlert(context, record.officeName, checkOutTimeString)
            GeofenceBroadcastReceiver.sendUpdateUiBroadcast(context)
        }
    }
}
