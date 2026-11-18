package com.srikar.chronowidget.ui.dashboard

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.srikar.chronowidget.data.models.WidgetPreferences
import com.srikar.chronowidget.location.CityPreset
import com.srikar.chronowidget.location.LocationHelper
import com.srikar.chronowidget.location.LocationPermissionStatus
import com.srikar.chronowidget.location.LocationResult
import com.srikar.chronowidget.location.LocationSourceType
import com.srikar.chronowidget.ui.preview.WidgetLivePreview
import com.srikar.chronowidget.weather.WeatherData
import com.srikar.chronowidget.weather.WeatherRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    prefs: WidgetPreferences,
    weather: WeatherData,
    location: LocationResult,
    onRequestLocationPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onSelectCity: (cityName: String, lat: Double, lon: Double) -> Unit,
    onNavigateToCustomization: () -> Unit,
    onNavigateToWeatherDetails: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var showCustomCityDialog by remember { mutableStateOf(false) }

    // Check if standalone Screen Lock app is installed
    val lifecycleOwner = LocalLifecycleOwner.current
    var isStandaloneScreenLockInstalled by remember {
        mutableStateOf(checkStandaloneScreenLockInstalled(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isStandaloneScreenLockInstalled = checkStandaloneScreenLockInstalled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionStatus = remember(location) {
        LocationHelper.checkPermission(context)
    }

    val formattedUpdateTime = remember(weather.timestampMillis) {
        val sdf = SimpleDateFormat("hh:mm:ss a, dd MMM", Locale.getDefault())
        sdf.format(Date(weather.timestampMillis))
    }

    val formattedLocationTime = remember(location.timestampMillis) {
        val sdf = SimpleDateFormat("hh:mm:ss a, dd MMM", Locale.getDefault())
        sdf.format(Date(location.timestampMillis))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ChronoWidget",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = "4×2",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About ChronoWidget",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0E1117)
                )
            )
        },
        containerColor = Color(0xFF0E1117)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. LIVE WIDGET PREVIEW CARD (Floating Transparent Text Preview)
            WidgetLivePreview(
                prefs = prefs,
                weather = weather,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. LOCATION PERMISSION RATIONALE & PROMPT (If not yet granted)
            if (permissionStatus == LocationPermissionStatus.DENIED) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F2338)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Automatic Location & Weather",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "ChronoWidget uses your location to display your current city and provide accurate local weather.",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onRequestLocationPermission,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Grant Permission", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = onOpenAppSettings,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155))
                                )
                            ) {
                                Text("App Settings", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. PRIMARY ACTION BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateToCustomization,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Customize",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Customize", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            isRefreshing = true
                            try {
                                WeatherRepository.getInstance(context).refreshWeather(triggerWidgetUpdate = true)
                                Toast.makeText(context, "Weather & location refreshed!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Refresh failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isRefreshing = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF8FAFC)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = !isRefreshing).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155))
                    ),
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF38BDF8)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isRefreshing) "Updating..." else "Refresh")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. LOCATION SETTINGS & CITY DETECTION CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF131823)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Location & Current City",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        // Permission status chip
                        val statusText = when (permissionStatus) {
                            LocationPermissionStatus.GRANTED_FINE -> "Precise Location"
                            LocationPermissionStatus.GRANTED_COARSE -> "Approximate"
                            LocationPermissionStatus.DENIED -> "Permission Denied"
                        }
                        val statusColor = when (permissionStatus) {
                            LocationPermissionStatus.GRANTED_FINE -> Color(0xFF10B981)
                            LocationPermissionStatus.GRANTED_COARSE -> Color(0xFF38BDF8)
                            LocationPermissionStatus.DENIED -> Color(0xFFF59E0B)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    StatusRow(label = "Current City", value = location.cityName, valueColor = Color(0xFF38BDF8))
                    StatusRow(label = "Detection Source", value = location.sourceType.displayName)
                    StatusRow(
                        label = "Coordinates",
                        value = "${String.format(Locale.US, "%.4f", location.latitude)}, ${String.format(Locale.US, "%.4f", location.longitude)}"
                    )
                    StatusRow(label = "Location Timestamp", value = formattedLocationTime)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isRefreshing = true
                                    try {
                                        WeatherRepository.getInstance(context).refreshWeather(triggerWidgetUpdate = true)
                                        Toast.makeText(context, "Location refreshed: ${location.cityName}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isRefreshing = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                        ) {
                            Text("Refresh Location", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenLocationSettings,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                        ) {
                            Text("System GPS Settings", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Manual City Selection Fallback
                    Text(
                        text = "Quick City Selection (Fallback):",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick-pick city chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val firstRow = LocationHelper.POPULAR_CITIES.take(4)
                        firstRow.forEach { city ->
                            FilterChip(
                                selected = location.cityName.equals(city.cityName, ignoreCase = true),
                                onClick = {
                                    onSelectCity(city.cityName, city.latitude, city.longitude)
                                },
                                label = { Text(city.cityName, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val secondRow = LocationHelper.POPULAR_CITIES.drop(4).take(3)
                        secondRow.forEach { city ->
                            FilterChip(
                                selected = location.cityName.equals(city.cityName, ignoreCase = true),
                                onClick = {
                                    onSelectCity(city.cityName, city.latitude, city.longitude)
                                },
                                label = { Text(city.cityName, fontSize = 11.sp) }
                            )
                        }

                        FilterChip(
                            selected = false,
                            onClick = { showCustomCityDialog = true },
                            label = { Text("More / Custom...", fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. WEATHER SOURCE & STATUS CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF131823)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Weather Status",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        TextButton(onClick = onNavigateToWeatherDetails) {
                            Text(
                                text = "Details →",
                                color = Color(0xFF38BDF8),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    StatusRow(label = "Active Source", value = weather.source.displayName)
                    StatusRow(label = "Condition", value = "${weather.getFormattedTemperature(prefs.isCelsius)} | ${weather.condition}")
                    StatusRow(label = "City / Locality", value = weather.locationName, valueColor = Color(0xFF38BDF8))
                    StatusRow(label = "Last Updated", value = formattedUpdateTime)
                    StatusRow(
                        label = "Data Freshness",
                        value = if (weather.isStale) "Stale (Using cache)" else "Fresh (Active)",
                        valueColor = if (weather.isStale) Color(0xFFF59E0B) else Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. DEDICATED SCREEN LOCK WIDGET (1×1) CARD — Delegates to standalone Screen Lock app
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF131823)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF808080),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Screen Lock (Dock Icon & 1×1 Widget)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        val statusText = if (isStandaloneScreenLockInstalled) "App Installed" else "Install Required"
                        val statusColor = if (isStandaloneScreenLockInstalled) Color(0xFF10B981) else Color(0xFFF59E0B)

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "1-tap screen lock via the standalone 'Screen Lock' app (AccessibilityService). Works on POCO/HyperOS with fingerprint/face unlock preserved.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isStandaloneScreenLockInstalled) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("market://details?id=com.srikar.screenlock")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open Play Store: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Install Screen Lock App", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = context.packageManager.getLaunchIntentForPackage("com.srikar.screenlock")
                                            ?: Intent(Intent.ACTION_MAIN).apply {
                                                setComponent(android.content.ComponentName("com.srikar.screenlock", "com.srikar.screenlock.MainActivity"))
                                            }
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not launch app: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Lock", fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:com.srikar.screenlock")
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open settings: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF90CAF9))
                            ) {
                                Text("App Settings", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7. ADD WIDGET INSTRUCTIONS CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF131823)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "How to add widgets to Home Screen",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    InstructionStep(number = "1", text = "Return to your phone's home screen.")
                    InstructionStep(number = "2", text = "Touch and hold an empty space on the home screen.")
                    InstructionStep(number = "3", text = "Tap 'Widgets' from the launcher menu.")
                    InstructionStep(number = "4", text = "Choose 'ChronoWidget (4×2)' for Clock/Weather or 'Screen Lock (1×1)' for 1-tap lock.")
                    InstructionStep(number = "5", text = "Drag your preferred widget to your home screen.")

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "POCO / HyperOS Tip: In Settings > Apps > Manage Apps > ChronoWidget, enable 'Autostart' and set Battery Saver to 'No restrictions' for uninterrupted background updates.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Custom City Input Dialog
    if (showCustomCityDialog) {
        var inputCity by remember { mutableStateOf(prefs.manualCityName) }
        var inputLat by remember { mutableStateOf(prefs.manualLatitude.toString()) }
        var inputLon by remember { mutableStateOf(prefs.manualLongitude.toString()) }

        AlertDialog(
            onDismissRequest = { showCustomCityDialog = false },
            title = { Text("Set Custom City Coordinates", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputCity,
                        onValueChange = { inputCity = it },
                        label = { Text("City Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputLat,
                        onValueChange = { inputLat = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputLon,
                        onValueChange = { inputLon = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = inputLat.toDoubleOrNull() ?: prefs.manualLatitude
                        val lon = inputLon.toDoubleOrNull() ?: prefs.manualLongitude
                        onSelectCity(inputCity.ifBlank { "Custom City" }, lat, lon)
                        showCustomCityDialog = false
                    }
                ) {
                    Text("Save & Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCityDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1A2234)
        )
    }
}

private fun checkStandaloneScreenLockInstalled(context: android.content.Context): Boolean {
    return try {
        context.packageManager.getPackageInfo("com.srikar.screenlock", android.content.pm.PackageManager.GET_ACTIVITIES)
        true
    } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
        false
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFFE2E8F0)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun InstructionStep(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0284C7)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Color(0xFFCBD5E1),
            fontSize = 13.sp
        )
    }
}
