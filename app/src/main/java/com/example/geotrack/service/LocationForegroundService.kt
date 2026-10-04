package com.example.geotrack.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.geotrack.utils.Constants
import com.example.geotrack.utils.NotificationHelper
import com.example.geotrack.utils.WifiValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service for active in-office presence monitoring.
 * Elevates process priority (oom_adj: 100-200) to protect against OEM LowMemoryKiller.
 * Runs lightweight 5-minute Wi-Fi anti-spoofing verification loop without battery-draining GPS polling.
 */
class LocationForegroundService : Service() {

    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var isTracking = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: Constants.ACTION_START_TRACKING
        val checkInTime = intent?.getStringExtra("EXTRA_CHECK_IN_TIME")

        when (action) {
            Constants.ACTION_START_TRACKING -> {
                startTracking(checkInTime)
            }
            Constants.ACTION_STOP_TRACKING -> {
                stopTracking()
            }
        }

        return START_STICKY
    }

    private fun startTracking(checkInTime: String?) {
        if (isTracking) return
        isTracking = true

        val notification = NotificationHelper.getForegroundNotification(this, checkInTime)
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            Constants.NOTIFICATION_ID_FOREGROUND,
            notification,
            foregroundServiceType
        )

        // Launch periodic 5-minute Wi-Fi validation loop
        serviceJob?.cancel()
        serviceJob = serviceScope.launch {
            while (isActive && isTracking) {
                delay(Constants.WIFI_CHECK_INTERVAL_MS)
                if (!isActive || !isTracking) break

                val isConnected = WifiValidator.isConnectedToOfficeWifi(this@LocationForegroundService)
                if (!isConnected) {
                    NotificationHelper.showWifiWarningNotification(this@LocationForegroundService)
                }
            }
        }
    }

    private fun stopTracking() {
        isTracking = false
        serviceJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        isTracking = false
        serviceJob?.cancel()
        super.onDestroy()
    }

    /**
     * Called when the user swipes the app off from Recents on Samsung/OEM devices.
     * We restart the service immediately so background geofence monitoring continues.
     * The geofence receiver (OS-level) still fires EXIT transitions regardless,
     * but this keeps the persistent notification and Wi-Fi loop alive.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (isTracking) {
            // Reschedule our own restart — the OS will call onStartCommand again with null intent
            val restartIntent = Intent(applicationContext, LocationForegroundService::class.java).apply {
                action = Constants.ACTION_START_TRACKING
                setPackage(packageName)
            }
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_ONE_SHOT
            }
            val restartPendingIntent = android.app.PendingIntent.getService(
                applicationContext, 1, restartIntent, pendingIntentFlags
            )
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.set(
                android.app.AlarmManager.ELAPSED_REALTIME,
                android.os.SystemClock.elapsedRealtime() + 1000L,
                restartPendingIntent
            )
        }
    }

    companion object {
        fun start(context: Context, checkInTime: String? = null) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = Constants.ACTION_START_TRACKING
                putExtra("EXTRA_CHECK_IN_TIME", checkInTime)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Ignore ForegroundServiceStartNotAllowedException on Android 12+ if in background without permissions
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = Constants.ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }
}
