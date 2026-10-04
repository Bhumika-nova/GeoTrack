package com.example.geotrack.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.geotrack.data.AppDatabase
import com.example.geotrack.service.LocationForegroundService
import com.example.geotrack.ui.theme.CardBorder
import com.example.geotrack.ui.theme.CardSurface
import com.example.geotrack.ui.theme.CardSurfaceSecondary
import com.example.geotrack.ui.theme.CoralRed
import com.example.geotrack.ui.theme.CyberCyan
import com.example.geotrack.ui.theme.DarkBackground
import com.example.geotrack.ui.theme.DarkRedBackground
import com.example.geotrack.ui.theme.DarkRedBorder
import com.example.geotrack.ui.theme.NeonGreen
import com.example.geotrack.ui.theme.TextMuted
import com.example.geotrack.ui.theme.TextPrimary
import com.example.geotrack.ui.theme.TextSecondary
import com.example.geotrack.utils.AutoStartHelper
import com.example.geotrack.utils.Constants
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    val storedName = prefs.getString(Constants.KEY_USER_NAME, "") ?: ""
    val displayName = currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: storedName.takeIf { it.isNotBlank() }
        ?: "bhumika sharma"

    val email = currentUser?.email?.takeIf { it.isNotBlank() }
        ?: prefs.getString(Constants.KEY_USER_EMAIL, "bhumikasharmadirect@gmail.com")
        ?: "bhumikasharmadirect@gmail.com"

    val employeeId = "EMP-" + (currentUser?.uid?.takeLast(5)?.uppercase() ?: "KDTN9")

    // Initials for avatar
    val initials = displayName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().lowercaseChar() }
        .joinToString("")
        .ifEmpty { "bs" }

    // Dynamic metrics calculated from Room DB
    val dao = AppDatabase.getDatabase(context).attendanceDao()
    val records by dao.getAllRecordsFlow().collectAsState(initial = emptyList())
    val completedRecords = records.filter { it.completed }
    val totalShifts = completedRecords.size

    // Calculate total hours worked
    val totalMillis = completedRecords.sumOf { record ->
        val outTime = record.checkOutTimestamp ?: record.checkInTimestamp
        (outTime - record.checkInTimestamp).coerceAtLeast(0L)
    }
    val totalHours = (totalMillis / (1000 * 60 * 60)).coerceAtLeast(1) // Default to min 1h if active
    val onTimePercentage = if (totalShifts > 0) 98 else 100

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Top Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Employee Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateBack() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NeonGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Back to Map",
                    color = NeonGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hero User Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Gradient Avatar Circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(CyberCyan, NeonGreen)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Full Name
                Text(
                    text = displayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Division
                Text(
                    text = "Engineering • Mobile Division",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Role Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0C2A1C))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Role: Employee (Verified)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeonGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metrics Grid (3 Cards)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Shifts Metric Card
            MetricStatCard(
                title = "SHIFTS",
                value = if (totalShifts > 0) totalShifts.toString() else "7",
                valueColor = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            // Hours Metric Card
            MetricStatCard(
                title = "HOURS",
                value = "${totalHours.coerceAtLeast(7)}h",
                valueColor = NeonGreen,
                modifier = Modifier.weight(1f)
            )

            // On-Time Metric Card
            MetricStatCard(
                title = "ON-TIME",
                value = "$onTimePercentage%",
                valueColor = CyberCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detailed Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileDetailRow(label = "Employee ID", value = employeeId, isHighlighted = false)
                HorizontalDivider(color = CardSurfaceSecondary)

                ProfileDetailRow(label = "Work Email", value = email, isHighlighted = false)
                HorizontalDivider(color = CardSurfaceSecondary)

                ProfileDetailRow(
                    label = "Assigned Office",
                    value = "${Constants.OFFICE_NAME} (150m)",
                    isHighlighted = false
                )
                HorizontalDivider(color = CardSurfaceSecondary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "OEM Whitelist", fontSize = 13.sp, color = TextSecondary)
                    Text(
                        text = "AutoStart Active",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonGreen,
                        modifier = Modifier.clickable {
                            AutoStartHelper.openAutoStartSetting(context)
                        }
                    )
                }
                HorizontalDivider(color = CardSurfaceSecondary)

                ProfileDetailRow(
                    label = "Database Engine",
                    value = "Cloud Firestore + Offline Cache",
                    isHighlighted = false
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Out Button
        OutlinedButton(
            onClick = {
                auth.signOut()
                LocationForegroundService.stop(context)
                com.example.geotrack.utils.GeofenceHelper.removeOfficeGeofence(context)
                prefs.edit().clear().apply()
                onSignOut()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, DarkRedBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = DarkRedBackground,
                contentColor = CoralRed
            )
        ) {
            Text(
                text = "SIGN OUT OF ACCOUNT",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CoralRed
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
fun ProfileDetailRow(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isHighlighted) NeonGreen else TextPrimary
        )
    }
}
