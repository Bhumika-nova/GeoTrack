package com.example.geotrack.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.geotrack.MainActivity
import com.example.geotrack.R

/**
 * Manages notification channels, foreground service notifications, and user alerts.
 */
object NotificationHelper {

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Low-importance silent channel for ongoing foreground service (no constant buzzing)
            val trackingChannel = NotificationChannel(
                Constants.CHANNEL_ID_TRACKING,
                Constants.CHANNEL_NAME_TRACKING,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification displayed while employee is inside office geofence"
                setShowBadge(false)
            }

            // High-importance alert channel for check-in/out popups and warnings
            val alertsChannel = NotificationChannel(
                Constants.CHANNEL_ID_ALERTS,
                Constants.CHANNEL_NAME_ALERTS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical attendance event alerts and check-in confirmation"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(trackingChannel, alertsChannel))
        }
    }

    fun getForegroundNotification(context: Context, checkInTime: String? = null): Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (checkInTime != null) {
            "Checked-In: $checkInTime • Active In-Office Session"
        } else {
            "Active in-office attendance monitoring"
        }

        return NotificationCompat.Builder(context, Constants.CHANNEL_ID_TRACKING)
            .setContentTitle("GeoTracker • In Office (Verified)")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun showCheckInAlert(context: Context, officeName: String, time: String) {
        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ID_ALERTS)
            .setContentTitle("Checked In Successfully")
            .setContentText("You are marked IN at $officeName ($time)")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notifySafely(context, Constants.NOTIFICATION_ID_ALERT, notification)
    }

    fun showCheckOutAlert(context: Context, officeName: String, time: String) {
        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ID_ALERTS)
            .setContentTitle("Checked Out")
            .setContentText("Completed shift at $officeName ($time). Syncing record...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notifySafely(context, Constants.NOTIFICATION_ID_ALERT, notification)
    }

    fun showWifiWarningNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ID_ALERTS)
            .setContentTitle("Office Wi-Fi Disconnected")
            .setContentText("Please reconnect to ${Constants.OFFICE_WIFI_SSID} to ensure attendance verification.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notifySafely(context, 1003, notification)
    }

    private fun notifySafely(context: Context, notificationId: Int, notification: Notification) {
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Handled when POST_NOTIFICATIONS is not granted
        }
    }
}
