package com.example.geotrack.utils

import com.google.android.gms.location.Geofence

/**
 * Global Constants for GeoTracker Application.
 */
object Constants {
    // Office Geofence Configuration
    const val GEOFENCE_ID = "OFFICE_HEADQUARTERS_GEOFENCE"
    const val GEOFENCE_LAT = 28.7161082  // Restored from Java reference project
    const val GEOFENCE_LON = 77.1183015   // Restored from Java reference project
    const val GEOFENCE_RADIUS_METERS = 150f
    const val GEOFENCE_EXPIRATION_DURATION = Geofence.NEVER_EXPIRE
    const val OFFICE_NAME = "Headquarters"

    // MapTiler Cloud Tile Configuration
    var MAPTILER_API_KEY = "Y8ZzwJf2weqMmPAy72DR"
    const val MAPTILER_DARK_STYLE_ID = "basic-v2-dark"
    const val MAPTILER_STREETS_STYLE_ID = "streets-v2"

    // Firebase Auth Configuration
    const val DEFAULT_WEB_CLIENT_ID = "237321261644-as6smuo2l2bg6suj5ksvo85jtoh6g73d.apps.googleusercontent.com"

    // Wi-Fi Anti-Spoofing Credentials (SSID + BSSID pair validation)
    // TEST SSID & BSSID — check Android Studio Logcat (tag: WifiValidator) for detected credentials
    const val OFFICE_WIFI_SSID = "Tanishq_5GHz"
    const val OFFICE_WIFI_BSSID = "06:25:e0:93:66:4d" // Populated from Android Studio Logcat during testing (e.g. "xx:xx:xx:xx:xx:xx")

    // Fallback Wi-Fi Pair (Secondary router band or backup AP)
    const val OFFICE_WIFI_FALLBACK_SSID = "Office_WiFi"
    const val OFFICE_WIFI_FALLBACK_BSSID = ""

    // Notification Channels & IDs
    const val CHANNEL_ID_TRACKING = "geotracker_tracking_channel"
    const val CHANNEL_NAME_TRACKING = "Live In-Office Tracking"
    const val CHANNEL_ID_ALERTS = "geotracker_alerts_channel"
    const val CHANNEL_NAME_ALERTS = "Attendance Alerts"
    const val NOTIFICATION_ID_FOREGROUND = 1001
    const val NOTIFICATION_ID_ALERT = 1002

    // Broadcast Actions & Service Commands
    const val ACTION_UPDATE_UI = "com.example.geotrack.UPDATE_UI"
    const val ACTION_START_TRACKING = "com.example.geotrack.ACTION_START_TRACKING"
    const val ACTION_STOP_TRACKING = "com.example.geotrack.ACTION_STOP_TRACKING"
    const val ACTION_MANUAL_CHECKOUT = "com.example.geotrack.ACTION_MANUAL_CHECKOUT"

    // Shared Preferences Keys
    const val PREFS_NAME = "GeoTrackerPrefs"
    const val KEY_USER_ID = "key_user_id"
    const val KEY_USER_EMAIL = "key_user_email"
    const val KEY_USER_NAME = "key_user_name"
    const val KEY_USER_ROLE = "key_user_role"
    const val KEY_AUTO_START_PROMPTED = "key_auto_start_prompted"
    const val KEY_MAPTILER_API_KEY = "key_maptiler_api_key"

    // Periodic Check Intervals
    const val WIFI_CHECK_INTERVAL_MS = 300_000L // 5 minutes
}
