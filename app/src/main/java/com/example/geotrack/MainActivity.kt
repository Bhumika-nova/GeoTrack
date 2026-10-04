package com.example.geotrack

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatDelegate
import com.example.geotrack.ui.AttendanceLogsScreen
import com.example.geotrack.ui.AuthScreen
import com.example.geotrack.ui.DashboardScreen
import com.example.geotrack.ui.ProfileScreen
import com.example.geotrack.ui.theme.GeoTrackTheme
import com.example.geotrack.utils.AutoStartHelper
import com.example.geotrack.utils.Constants
import com.example.geotrack.utils.GeofenceHelper
import com.example.geotrack.utils.NotificationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.auth.FirebaseAuth
import org.osmdroid.config.Configuration

enum class Screen {
    AUTH,
    DASHBOARD,
    LOGS,
    PROFILE
}

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation = mutableStateOf<Location?>(null)
    private var currentScreen = mutableStateOf(Screen.AUTH)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                currentLocation.value = location
            }
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineLocationGranted) {
            startLocationUpdates()
            requestBackgroundLocationIfNeeded()
            GeofenceHelper.registerOfficeGeofence(this)
        }
    }

    private val requestBackgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            GeofenceHelper.registerOfficeGeofence(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Force consistent Dark/Night mode matching UI theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // Initialize Notification Channels
        NotificationHelper.createNotificationChannels(this)

        // Initialize Osmdroid Map Configuration with Internal App Cache
        try {
            val basePath = java.io.File(cacheDir, "osmdroid")
            if (!basePath.exists()) basePath.mkdirs()
            val tileCache = java.io.File(basePath, "tiles")
            if (!tileCache.exists()) tileCache.mkdirs()

            Configuration.getInstance().osmdroidBasePath = basePath
            Configuration.getInstance().osmdroidTileCache = tileCache
            Configuration.getInstance().load(
                this,
                getSharedPreferences("${packageName}_osm_preferences", Context.MODE_PRIVATE)
            )
            Configuration.getInstance().userAgentValue = "GeoTracker/1.0 (Android; ${packageName})"
        } catch (_: Exception) {}

        // Install Google Play Services TLS Security Provider patch
        try {
            ProviderInstaller.installIfNeededAsync(this, object : ProviderInstaller.ProviderInstallListener {
                override fun onProviderInstalled() {}
                override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: Intent?) {}
            })
        } catch (_: Exception) {}

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check active session
        val auth = FirebaseAuth.getInstance()
        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val savedUserId = prefs.getString(Constants.KEY_USER_ID, null)

        if (auth.currentUser != null || !savedUserId.isNullOrEmpty()) {
            currentScreen.value = Screen.DASHBOARD
            checkAndRequestPermissions()
        } else {
            currentScreen.value = Screen.AUTH
        }

        // AutoStart prompt for aggressive OEM devices
        val hasPromptedAutoStart = prefs.getBoolean(Constants.KEY_AUTO_START_PROMPTED, false)
        if (!hasPromptedAutoStart && AutoStartHelper.isAggressiveOemDevice()) {
            prefs.edit().putBoolean(Constants.KEY_AUTO_START_PROMPTED, true).apply()
            AutoStartHelper.openAutoStartSetting(this)
        }

        setContent {
            GeoTrackTheme {
                MainAppNavHost(
                    currentScreen = currentScreen.value,
                    userLocation = currentLocation.value,
                    onNavigate = { screen -> currentScreen.value = screen },
                    onAuthSuccess = {
                        currentScreen.value = Screen.DASHBOARD
                        checkAndRequestPermissions()
                        GeofenceHelper.registerOfficeGeofence(this@MainActivity)
                    },
                    onSignOut = {
                        currentScreen.value = Screen.AUTH
                    }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startLocationUpdates()
            requestBackgroundLocationIfNeeded()
            GeofenceHelper.registerOfficeGeofence(this)
        }
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            15_000L
        ).setMinUpdateIntervalMillis(10_000L).build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation.value = location
                }
            }
        } catch (_: SecurityException) {
            // Handled when permissions are not yet granted
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

@Composable
fun MainAppNavHost(
    currentScreen: Screen,
    userLocation: Location?,
    onNavigate: (Screen) -> Unit,
    onAuthSuccess: () -> Unit,
    onSignOut: () -> Unit
) {
    when (currentScreen) {
        Screen.AUTH -> {
            AuthScreen(onAuthSuccess = onAuthSuccess)
        }
        Screen.DASHBOARD -> {
            DashboardScreen(
                userLocation = userLocation,
                onNavigateToLogs = { onNavigate(Screen.LOGS) },
                onNavigateToProfile = { onNavigate(Screen.PROFILE) }
            )
        }
        Screen.LOGS -> {
            AttendanceLogsScreen(
                onNavigateBack = { onNavigate(Screen.DASHBOARD) }
            )
        }
        Screen.PROFILE -> {
            ProfileScreen(
                onNavigateBack = { onNavigate(Screen.DASHBOARD) },
                onSignOut = onSignOut
            )
        }
    }
}
