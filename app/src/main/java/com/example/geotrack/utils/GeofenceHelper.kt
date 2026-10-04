package com.example.geotrack.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.geotrack.receiver.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * Helper class for creating, registering, and removing Google Play Services Geofences.
 */
object GeofenceHelper {

    fun getOfficeGeofence(): Geofence {
        return Geofence.Builder()
            .setRequestId(Constants.GEOFENCE_ID)
            .setCircularRegion(
                Constants.GEOFENCE_LAT,
                Constants.GEOFENCE_LON,
                Constants.GEOFENCE_RADIUS_METERS
            )
            .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_DURATION)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .setNotificationResponsiveness(5000) // 5 seconds responsiveness
            .build()
    }

    fun getGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
    }

    fun getGeofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    @SuppressLint("MissingPermission")
    fun registerOfficeGeofence(
        context: Context,
        geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context),
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        val geofence = getOfficeGeofence()
        val request = getGeofencingRequest(geofence)
        val pendingIntent = getGeofencePendingIntent(context)

        try {
            geofencingClient.addGeofences(request, pendingIntent)
                .addOnSuccessListener {
                    onSuccess?.invoke()
                }
                .addOnFailureListener { exception ->
                    onFailure?.invoke(exception)
                }
        } catch (e: Exception) {
            onFailure?.invoke(e)
        }
    }

    fun removeOfficeGeofence(
        context: Context,
        geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context),
        onComplete: (() -> Unit)? = null
    ) {
        val pendingIntent = getGeofencePendingIntent(context)
        geofencingClient.removeGeofences(pendingIntent)
            .addOnCompleteListener {
                onComplete?.invoke()
            }
    }
}
