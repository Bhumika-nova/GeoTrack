package com.example.geotrack.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log

/**
 * Dual-layer anti-spoofing Wi-Fi validator.
 * Validates both SSID and BSSID pair to ensure the device is physically connected
 * to the authorized office router/hotspot rather than relying solely on GPS coordinates.
 *
 * Emits detailed Logcat outputs (Tag: "WifiValidator") during testing so developers
 * can inspect the detected SSID & BSSID in Android Studio and configure Constants.kt.
 */
object WifiValidator {

    private const val TAG = "WifiValidator"

    data class WifiPair(
        val ssid: String?,
        val bssid: String?
    )

    /**
     * Resolves the current WifiInfo from NetworkCapabilities or WifiManager.
     */
    @Suppress("DEPRECATION")
    private fun getWifiInfo(context: Context): WifiInfo? {
        val appContext = context.applicationContext
        val connectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return null

        val activeNetwork = connectivityManager.activeNetwork ?: return null
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null

        if (!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return null
        }

        // Try getting WifiInfo from NetworkCapabilities (Android 10+ / API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val transportInfo = networkCapabilities.transportInfo
            if (transportInfo is WifiInfo) {
                val ssid = transportInfo.ssid
                if (!ssid.isNullOrEmpty() && ssid != "<unknown ssid>") {
                    return transportInfo
                }
            }
        }

        // Fallback / standard retrieval via WifiManager
        val wifiManager =
            appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        return wifiManager.connectionInfo
    }

    /**
     * Retrieves the sanitized SSID of the currently connected Wi-Fi network.
     * Returns null if not connected, if SSID is masked, or if location permission is missing.
     */
    fun getConnectedWifiSsid(context: Context): String? {
        val wifiInfo = getWifiInfo(context) ?: return null
        val rawSsid = wifiInfo.ssid ?: return null
        val cleanSsid = rawSsid.replace("\"", "").trim()

        return if (cleanSsid.isEmpty() || cleanSsid.equals("<unknown ssid>", ignoreCase = true)) {
            null
        } else {
            cleanSsid
        }
    }

    /**
     * Retrieves the sanitized BSSID (MAC address) of the currently connected Wi-Fi access point.
     * Returns null if unavailable or if Android masks it (02:00:00:00:00:00).
     */
    fun getConnectedWifiBssid(context: Context): String? {
        val wifiInfo = getWifiInfo(context) ?: return null
        val rawBssid = wifiInfo.bssid ?: return null
        val cleanBssid = rawBssid.trim()

        return if (cleanBssid.isEmpty() ||
            cleanBssid.equals("02:00:00:00:00:00", ignoreCase = true) ||
            cleanBssid.equals("00:00:00:00:00:00", ignoreCase = true)
        ) {
            null
        } else {
            cleanBssid
        }
    }

    /**
     * Convenience method to retrieve both SSID and BSSID as a pair.
     */
    fun getConnectedWifiPair(context: Context): WifiPair {
        return WifiPair(
            ssid = getConnectedWifiSsid(context),
            bssid = getConnectedWifiBssid(context)
        )
    }

    /**
     * Validates whether the device is connected to the authorized Office Wi-Fi.
     * Dual validation: Both SSID and BSSID must match the configured constants.
     *
     * Also outputs diagnostic information to Android Studio Logcat for easy testing.
     */
    fun isConnectedToOfficeWifi(context: Context): Boolean {
        val wifiInfo = getWifiInfo(context)
        val rawBssid = wifiInfo?.bssid
        val currentSsid = getConnectedWifiSsid(context)
        val currentBssid = getConnectedWifiBssid(context)

        // Diagnostic Logcat banner for testing and configuring BSSID in Android Studio
        Log.i(TAG, "────────────────────────────────────────────────────────")
        Log.i(TAG, "📡 [WIFI DEBUG] Current Connected Network:")
        Log.i(TAG, "   SSID : \"${currentSsid ?: "<Unavailable / Not Connected>"}\"")
        Log.i(TAG, "   BSSID: \"${currentBssid ?: "<Unavailable / Masked>"}\"")
        Log.i(TAG, "🎯 [WIFI DEBUG] Configured Office Target:")
        Log.i(TAG, "   Primary Target : SSID=\"${Constants.OFFICE_WIFI_SSID}\", BSSID=\"${Constants.OFFICE_WIFI_BSSID}\"")
        if (Constants.OFFICE_WIFI_FALLBACK_SSID.isNotBlank()) {
            Log.i(TAG, "   Fallback Target: SSID=\"${Constants.OFFICE_WIFI_FALLBACK_SSID}\", BSSID=\"${Constants.OFFICE_WIFI_FALLBACK_BSSID}\"")
        }

        // Detect Android location masking
        if (rawBssid.equals("02:00:00:00:00:00", ignoreCase = true)) {
            Log.w(TAG, "⚠️ Android masked the BSSID (02:00:00:00:00:00).")
            Log.w(TAG, "   Ensure Location (GPS) is turned ON and Location permission (Fine Location) is granted.")
        }

        if (currentSsid == null || currentBssid == null) {
            Log.w(TAG, "❌ Wi-Fi validation failed: Incomplete Wi-Fi info (SSID or BSSID unavailable).")
            Log.i(TAG, "────────────────────────────────────────────────────────")
            return false
        }

        // Primary pair validation (SSID + BSSID)
        val primarySsidMatch = currentSsid.equals(Constants.OFFICE_WIFI_SSID, ignoreCase = true)
        val primaryBssidMatch = Constants.OFFICE_WIFI_BSSID.isNotBlank() &&
                currentBssid.equals(Constants.OFFICE_WIFI_BSSID, ignoreCase = true)

        // Fallback pair validation (SSID + BSSID)
        val fallbackSsidMatch = Constants.OFFICE_WIFI_FALLBACK_SSID.isNotBlank() &&
                currentSsid.equals(Constants.OFFICE_WIFI_FALLBACK_SSID, ignoreCase = true)
        val fallbackBssidMatch = Constants.OFFICE_WIFI_FALLBACK_BSSID.isNotBlank() &&
                currentBssid.equals(Constants.OFFICE_WIFI_FALLBACK_BSSID, ignoreCase = true)

        val isPrimaryMatch = primarySsidMatch && primaryBssidMatch
        val isFallbackMatch = fallbackSsidMatch && fallbackBssidMatch
        val isValid = isPrimaryMatch || isFallbackMatch

        if (isValid) {
            Log.i(TAG, "✅ [WIFI DEBUG] VALIDATION PASSED: Device verified on office Wi-Fi pair!")
        } else {
            Log.w(TAG, "❌ [WIFI DEBUG] VALIDATION FAILED: Wi-Fi pair does not match authorized credentials.")
            if (Constants.OFFICE_WIFI_BSSID.isBlank()) {
                Log.i(TAG, "👉 [COPY & PASTE TO Constants.kt]")
                Log.i(TAG, "   const val OFFICE_WIFI_SSID = \"$currentSsid\"")
                Log.i(TAG, "   const val OFFICE_WIFI_BSSID = \"$currentBssid\"")
            } else if (primarySsidMatch && !primaryBssidMatch) {
                Log.w(TAG, "⚠️ SSID matched ('$currentSsid'), but BSSID mismatch!")
                Log.w(TAG, "   Expected BSSID: ${Constants.OFFICE_WIFI_BSSID}")
                Log.w(TAG, "   Detected BSSID: $currentBssid")
            }
        }
        Log.i(TAG, "────────────────────────────────────────────────────────")

        return isValid
    }

    /**
     * Checks if general internet connectivity is available and validated.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
