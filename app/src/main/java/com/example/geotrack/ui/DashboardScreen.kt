package com.example.geotrack.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.location.Location
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.geotrack.R
import com.example.geotrack.data.AppDatabase
import com.example.geotrack.data.AttendanceRecord
import com.example.geotrack.data.FirestoreSyncHelper
import com.example.geotrack.receiver.GeofenceBroadcastReceiver
import com.example.geotrack.service.LocationForegroundService
import com.example.geotrack.service.SyncWorker
import com.example.geotrack.ui.theme.CardBorder
import com.example.geotrack.ui.theme.CardSurface
import com.example.geotrack.ui.theme.CoralRed
import com.example.geotrack.ui.theme.CyberCyan
import com.example.geotrack.ui.theme.DarkBackground
import com.example.geotrack.ui.theme.DarkRedBackground
import com.example.geotrack.ui.theme.NeonGreen
import com.example.geotrack.ui.theme.TextMuted
import com.example.geotrack.ui.theme.TextPrimary
import com.example.geotrack.ui.theme.TextSecondary
import com.example.geotrack.utils.Constants
import com.example.geotrack.utils.MapTilerTileSource
import com.example.geotrack.utils.NotificationHelper
import com.example.geotrack.utils.WifiValidator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom High-Performance Overlay for 150m Geofence Circle.
 * Avoids any InfoWindow / MapViewRepository null pointer exceptions.
 */
class GeofenceCircleOverlay(
    val center: GeoPoint,
    val radiusMeters: Double
) : Overlay() {
    private val fillPaint = Paint().apply {
        color = android.graphics.Color.argb(45, 255, 82, 82)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val outlinePaint = Paint().apply {
        color = android.graphics.Color.rgb(255, 82, 82)
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val projection = mapView.projection ?: return
        val centerPoint = projection.toPixels(center, null) ?: return
        val radiusPixels = projection.metersToPixels(radiusMeters.toFloat()).coerceAtLeast(20f)
        canvas.drawCircle(centerPoint.x.toFloat(), centerPoint.y.toFloat(), radiusPixels, fillPaint)
        canvas.drawCircle(centerPoint.x.toFloat(), centerPoint.y.toFloat(), radiusPixels, outlinePaint)
    }
}

/**
 * Custom High-Performance Overlay for Office Red Marker.
 * Draws a clean vector cross (+) symbol inside the pin — no emoji, no text.
 */
class OfficeMarkerOverlay(
    val geoPoint: GeoPoint
) : Overlay() {
    private val redPaint = Paint().apply {
        color = android.graphics.Color.rgb(255, 82, 82)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val whitePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val crossPaint = Paint().apply {
        color = android.graphics.Color.rgb(255, 82, 82)
        style = Paint.Style.STROKE
        strokeWidth = 4.5f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val projection = mapView.projection ?: return
        val point = projection.toPixels(geoPoint, null) ?: return
        val x = point.x.toFloat()
        val y = point.y.toFloat()

        // Outer red circular head
        canvas.drawCircle(x, y - 30f, 26f, redPaint)
        // Inner white circle
        canvas.drawCircle(x, y - 30f, 18f, whitePaint)

        // Triangular pin pointer tip
        val path = Path().apply {
            moveTo(x - 16f, y - 20f)
            lineTo(x + 16f, y - 20f)
            lineTo(x, y + 4f)
            close()
        }
        canvas.drawPath(path, redPaint)

        // Cross (+) symbol — horizontal bar
        canvas.drawLine(x - 9f, y - 30f, x + 9f, y - 30f, crossPaint)
        // Cross (+) symbol — vertical bar
        canvas.drawLine(x, y - 39f, x, y - 21f, crossPaint)
    }
}

/**
 * Custom High-Performance Overlay for User Glowing Radar Marker.
 */
class UserPulseOverlay(
    var geoPoint: GeoPoint
) : Overlay() {
    private val outerPaint = Paint().apply {
        color = android.graphics.Color.argb(90, 0, 230, 118) // Glowing outer ring
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val innerPaint = Paint().apply {
        color = android.graphics.Color.rgb(0, 230, 118) // Solid neon green center
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val whiteStroke = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val projection = mapView.projection ?: return
        val point = projection.toPixels(geoPoint, null) ?: return
        val x = point.x.toFloat()
        val y = point.y.toFloat()

        // Outer radar pulse
        canvas.drawCircle(x, y, 26f, outerPaint)
        // Inner green core
        canvas.drawCircle(x, y, 11f, innerPaint)
        // White border ring
        canvas.drawCircle(x, y, 11f, whiteStroke)
    }
}

@Composable
fun DashboardScreen(
    userLocation: Location?,
    onNavigateToLogs: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dao = AppDatabase.getDatabase(context).attendanceDao()
    val activeRecord by dao.getActiveRecordFlow().collectAsState(initial = null)

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    val storedName = prefs.getString(Constants.KEY_USER_NAME, "") ?: ""
    val displayName = currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: storedName.takeIf { it.isNotBlank() }
        ?: "bhumika sharma"

    val initials = displayName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().lowercaseChar() }
        .joinToString("")
        .ifEmpty { "bs" }

    // Live distance calculation — safe "outside" defaults until real GPS arrives
    var distanceMeters by remember { mutableFloatStateOf(9999f) }
    var isInsideFence by remember { mutableStateOf(false) }

    LaunchedEffect(userLocation) {
        if (userLocation != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude,
                userLocation.longitude,
                Constants.GEOFENCE_LAT,
                Constants.GEOFENCE_LON,
                results
            )
            distanceMeters = results[0]
            isInsideFence = distanceMeters <= Constants.GEOFENCE_RADIUS_METERS
        }
    }

    // Auto check-in on app launch — mirrors Java checkInitialState():
    // Waits for real GPS, then queries DB directly (not Compose state which starts as null).
    // This prevents duplicate sessions when reopening the app while a session is already active.
    var hasRunInitialCheck by remember { mutableStateOf(false) }
    LaunchedEffect(userLocation) {
        if (userLocation != null && !hasRunInitialCheck) {
            hasRunInitialCheck = true

            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude,
                userLocation.longitude,
                Constants.GEOFENCE_LAT,
                Constants.GEOFENCE_LON,
                results
            )

            if (results[0] <= Constants.GEOFENCE_RADIUS_METERS) {
                // Direct DB read — never rely on collectAsState(initial = null)
                // which is always null on first frame even when a record exists
                val existingActive = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    dao.getActiveRecord()
                }
                if (existingActive == null && WifiValidator.isConnectedToOfficeWifi(context)) {
                    val now = System.currentTimeMillis()
                    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))
                    val userId = currentUser?.uid?.takeIf { it.isNotBlank() }
                        ?: prefs.getString(Constants.KEY_USER_ID, "")
                        ?: ""
                    val newRec = AttendanceRecord(
                        officeName = Constants.OFFICE_NAME,
                        checkInTime = timeStr,
                        checkInTimestamp = now,
                        userId = userId,
                        completed = false,
                        status = "IN_OFFICE_VERIFIED",
                        wifiSsid = WifiValidator.getConnectedWifiSsid(context)
                    )
                    dao.insert(newRec)
                    LocationForegroundService.start(context, timeStr)
                    NotificationHelper.showCheckInAlert(context, Constants.OFFICE_NAME, timeStr)
                    GeofenceBroadcastReceiver.sendUpdateUiBroadcast(context)
                } else if (existingActive != null) {
                    // Session already active — just restart the FGS to restore the notification
                    // (service may have been killed when app was removed from recents)
                    LocationForegroundService.start(context, existingActive.checkInTime)
                }
            }
        }
    }

    // Live Wi-Fi SSID
    var currentWifiSsid by remember { mutableStateOf<String?>(Constants.OFFICE_WIFI_SSID) }
    var isWifiConnected by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (isActive) {
            currentWifiSsid = WifiValidator.getConnectedWifiSsid(context) ?: "Unknown SSID"
            isWifiConnected = WifiValidator.isConnectedToOfficeWifi(context)
            delay(10_000L)
        }
    }

    // Live duration counter for active session
    var liveSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(activeRecord) {
        while (isActive && activeRecord != null) {
            val checkInTimestamp = activeRecord?.checkInTimestamp ?: System.currentTimeMillis()
            liveSeconds = ((System.currentTimeMillis() - checkInTimestamp) / 1000L).coerceAtLeast(0L)
            delay(1000L)
        }
    }

    val hours = liveSeconds / 3600
    val minutes = (liveSeconds % 3600) / 60
    val seconds = liveSeconds % 60
    val durationText = String.format(Locale.getDefault(), "%02dh %02dm %02ds", hours, minutes, seconds)

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var userOverlayRef by remember { mutableStateOf<UserPulseOverlay?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    DisposableEffect(mapViewRef) {
        mapViewRef?.onResume()
        onDispose {
            mapViewRef?.onPause()
            // CRITICAL FIX: Do not call onDetach(), as it permanently shuts down the Osmdroid tile download thread pool
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Map Container (Osmdroid Inverted Dark Mode)
        AndroidView(
            factory = { ctx ->
                org.osmdroid.config.Configuration.getInstance().userAgentValue = ctx.packageName
                
                MapView(ctx).apply {
                    setTileSource(MapTilerTileSource.createDarkTileSource())
                    setMultiTouchControls(true)
                    setUseDataConnection(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    isTilesScaledToDpi = true
                    minZoomLevel = 4.0
                    maxZoomLevel = 20.0
                    isClickable = true
                    isFocusable = true
                    
                    onResume()

                    val officePoint = GeoPoint(Constants.GEOFENCE_LAT, Constants.GEOFENCE_LON)

                    // 150m Geofence Circle Overlay (Coral red stroke with semi-transparent fill)
                    val circle = GeofenceCircleOverlay(officePoint, Constants.GEOFENCE_RADIUS_METERS.toDouble())
                    overlays.add(circle)

                    // Geodesic Polygon for high-precision vector rendering
                    val circlePolygon = Polygon(this).apply {
                        points = Polygon.pointsAsCircle(officePoint, Constants.GEOFENCE_RADIUS_METERS.toDouble())
                        fillPaint.color = android.graphics.Color.argb(35, 255, 82, 82)
                        outlinePaint.color = android.graphics.Color.rgb(255, 82, 82)
                        outlinePaint.strokeWidth = 4f
                    }
                    overlays.add(circlePolygon)

                    // Office Pin Custom Overlay
                    val officeMarker = OfficeMarkerOverlay(officePoint)
                    overlays.add(officeMarker)

                    // User Location Pulse Custom Overlay
                    val initialUserPoint = if (userLocation != null) {
                        GeoPoint(userLocation.latitude, userLocation.longitude)
                    } else {
                        GeoPoint(Constants.GEOFENCE_LAT + 0.0003, Constants.GEOFENCE_LON + 0.0004)
                    }
                    val userOverlay = UserPulseOverlay(initialUserPoint)
                    overlays.add(userOverlay)
                    userOverlayRef = userOverlay

                    // Ensure map centers on office coordinates after layout pass
                    controller.setZoom(17.5)
                    controller.setCenter(officePoint)

                    addOnFirstLayoutListener { _, _, _, _, _ ->
                        controller.setZoom(17.5)
                        controller.setCenter(officePoint)
                        postInvalidate()
                    }

                    post {
                        controller.setZoom(17.5)
                        controller.setCenter(officePoint)
                        postInvalidate()
                    }

                    mapViewRef = this
                }
            },
            update = { view ->
                if (userLocation != null) {
                    val userPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
                    userOverlayRef?.geoPoint = userPoint
                    
                    if (view.zoomLevelDouble < 5.0) {
                        view.controller.setZoom(17.5)
                        view.controller.setCenter(userPoint)
                    } else if (distanceMeters > Constants.GEOFENCE_RADIUS_METERS && distanceMeters < 10000) {
                        // Slowly pan towards user if they are outside the fence but still in range
                        view.controller.animateTo(userPoint)
                    }
                    view.invalidate()
                } else if (view.zoomLevelDouble < 5.0) {
                    val officePoint = GeoPoint(Constants.GEOFENCE_LAT, Constants.GEOFENCE_LON)
                    view.controller.setZoom(17.5)
                    view.controller.setCenter(officePoint)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top App Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 36.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.95f)),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attendance Logs Button
                IconButton(
                    onClick = { onNavigateToLogs() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkBackground)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Attendance Logs",
                        tint = NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title & Logo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GeoTracker",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Profile Avatar Initial Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(CyberCyan, NeonGreen)
                            )
                        )
                        .clickable { onNavigateToProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        // Floating Status Badges (Top Center below bar)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 104.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Status Pill (Java Style: Based on Session state)
            val pillText = if (activeRecord != null) {
                if (isWifiConnected) "IN OFFICE (Verified)" else "IN OFFICE (Unverified)"
            } else {
                if (isInsideFence) "INSIDE AREA" else "OUTSIDE AREA"
            }
            
            val pillBgColor = if (activeRecord != null) {
                if (isWifiConnected) CyberCyan.copy(alpha = 0.95f) else Color(0xFFFF9800)
            } else {
                if (isInsideFence) Color(0xFF607D8B) else Color(0xFF263238)
            }
            
            val pillTextColor = if (activeRecord != null && isWifiConnected) Color.Black else TextSecondary

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(pillBgColor)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pillText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = pillTextColor
                    )
                }
            }

            // Sub Status Badges Row (Inside Fence & Wi-Fi)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Fence Distance Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardSurface.copy(alpha = 0.92f))
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isInsideFence) NeonGreen else CoralRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isInsideFence) "INSIDE FENCE (${distanceMeters.toInt()}m)" else "OUTSIDE (${distanceMeters.toInt()}m)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isInsideFence) NeonGreen else TextSecondary
                        )
                    }
                }

                // Wi-Fi SSID Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardSurface.copy(alpha = 0.92f))
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = "Wi-Fi",
                            tint = if (isWifiConnected) NeonGreen else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentWifiSsid ?: "No Wi-Fi",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isWifiConnected) NeonGreen else TextSecondary
                        )
                    }
                }
            }
        }

        // Map Float Controls (Right Side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Zoom In Button
            IconButton(
                onClick = { mapViewRef?.controller?.zoomIn() },
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = TextPrimary)
            }

            // Zoom Out Button
            IconButton(
                onClick = { mapViewRef?.controller?.zoomOut() },
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = TextPrimary)
            }

            // Recenter Button
            IconButton(
                onClick = {
                    val target = if (userLocation != null) {
                        GeoPoint(userLocation.latitude, userLocation.longitude)
                    } else {
                        GeoPoint(Constants.GEOFENCE_LAT, Constants.GEOFENCE_LON)
                    }
                    mapViewRef?.controller?.animateTo(target)
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.LocationSearching,
                    contentDescription = "Recenter",
                    tint = CoralRed
                )
            }
        }

        // Bottom Active Session Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top Row: Checked In indicator + Date & Sync Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (activeRecord != null) NeonGreen else TextMuted)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeRecord != null) "Checked-In" else "Not Checked In",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (activeRecord != null) NeonGreen else TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeRecord?.checkInTime ?: SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    // Refresh / Sync Circle Button
                    IconButton(
                        onClick = {
                            isSyncing = true
                            coroutineScope.launch {
                                SyncWorker.enqueueSync(context)
                                FirestoreSyncHelper.syncUnsyncedRecords(context)
                                delay(600L)
                                isSyncing = false
                            }
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(CyberCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Duration Counter Row — read-only, no manual buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = "Duration",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeRecord != null) durationText else "00h 00m 00s",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (activeRecord != null) "counting live" else "auto-tracking active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (activeRecord != null) NeonGreen else TextMuted
                        )
                    }

                    // Auto-status pill (replaces manual buttons)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (activeRecord != null) DarkRedBackground
                                else Color(0xFF1A2A1A)
                            )
                            .border(
                                1.dp,
                                if (activeRecord != null) CoralRed.copy(alpha = 0.6f)
                                else NeonGreen.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (activeRecord != null) "● TRACKING" else "◎ STANDBY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeRecord != null) CoralRed else NeonGreen.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Geofence Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isInsideFence) "${Constants.OFFICE_NAME}: Inside 150m Geofence" else "${Constants.OFFICE_NAME}: Outside 150m Geofence",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
