package com.example.geotrack

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.geotrack.utils.NotificationHelper
import com.google.android.gms.security.ProviderInstaller
import org.osmdroid.config.Configuration

/**
 * Application class for GeoTracker.
 * Initializes notification channels, Osmdroid configuration, and security provider.
 */
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Force consistent Dark/Night mode matching UI theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // Initialize Notification Channels
        NotificationHelper.createNotificationChannels(this)

        // Initialize Osmdroid Map Configuration
        Configuration.getInstance().load(
            this,
            getSharedPreferences("${packageName}_osm_preferences", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName

        // Install Google Play Services TLS Security Provider patch
        installSecurityProvider()
    }

    private fun installSecurityProvider() {
        try {
            ProviderInstaller.installIfNeededAsync(this, object : ProviderInstaller.ProviderInstallListener {
                override fun onProviderInstalled() {
                    // TLS Security Provider updated successfully
                }

                override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: android.content.Intent?) {
                    // Handled gracefully if Play Services is outdated
                }
            })
        } catch (_: Exception) {
            // Ignored on emulators/unsupported environments
        }
    }
}
