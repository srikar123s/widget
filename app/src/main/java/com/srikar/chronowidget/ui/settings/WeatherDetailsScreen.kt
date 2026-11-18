package com.srikar.chronowidget.ui.settings

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srikar.chronowidget.data.models.WidgetPreferences
import com.srikar.chronowidget.weather.WeatherData
import com.srikar.chronowidget.weather.WeatherRepository
import com.srikar.chronowidget.weather.XiaomiInvestigationReport
import com.srikar.chronowidget.weather.XiaomiWeatherProvider
import com.srikar.chronowidget.widget.WidgetUpdater
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailsScreen(
    prefs: WidgetPreferences,
    weather: WeatherData,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Run dynamic investigation of the installed Xiaomi Weather app
    val xiaomiReport: XiaomiInvestigationReport = remember {
        XiaomiWeatherProvider(context).performDetailedInvestigation()
    }

    val formattedTime = remember(weather.timestampMillis) {
        val sdf = SimpleDateFormat("hh:mm:ss a, EEEE, dd MMMM yyyy", Locale.getDefault())
        sdf.format(Date(weather.timestampMillis))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Weather Details",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
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
                .padding(16.dp)
        ) {
            // Main Weather Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF131823)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val iconRes = WidgetUpdater.getWeatherIconRes(weather.weatherCode)
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = "Condition Icon",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = weather.getFormattedTemperature(prefs.isCelsius),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = weather.condition,
                        fontSize = 18.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = weather.locationName,
                        fontSize = 14.sp,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Feels Like",
                    value = if (prefs.isCelsius) "${Math.round(weather.apparentTemperatureCelsius)}°C" else "${Math.round((weather.apparentTemperatureCelsius * 9/5) + 32)}°F"
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Humidity",
                    value = "${weather.relativeHumidity}%"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Wind Speed",
                    value = "${weather.windSpeedKmh} km/h"
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "WMO Code",
                    value = "Code ${weather.weatherCode}"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // XIAOMI WEATHER APP INVESTIGATION CARD
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
                        Text(
                            text = "Xiaomi Weather Investigation",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B),
                            fontSize = 15.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (xiaomiReport.isAppInstalled) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFF374151)
                        ) {
                            Text(
                                text = if (xiaomiReport.isAppInstalled) "Installed" else "Not Detected",
                                color = if (xiaomiReport.isAppInstalled) Color(0xFF38BDF8) else Color(0xFF9CA3AF),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    DiagnosticRow("Target Package", xiaomiReport.targetPackage)
                    DiagnosticRow("Version Name", xiaomiReport.appVersionName ?: "N/A (Target: G-17.0.3.26)")
                    DiagnosticRow("Provider Authority", xiaomiReport.providerAuthority)
                    DiagnosticRow("Provider Found", if (xiaomiReport.isProviderFound) "Yes" else "No")
                    DiagnosticRow("Exported Status", if (xiaomiReport.isProviderExported) "Exported (true)" else "Not Exported (false)")
                    DiagnosticRow("Permission Required", xiaomiReport.readPermission ?: "Signature")
                    DiagnosticRow("Test Query Result", xiaomiReport.queryTestResult)
                    DiagnosticRow("Fallback Status", xiaomiReport.fallbackStatus, valueColor = Color(0xFF10B981))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Source Diagnostics Card
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
                        text = "Active Weather Diagnostics",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticRow("Active Provider", weather.source.displayName)
                    DiagnosticRow("Configured Preference", prefs.weatherSourcePreference)
                    DiagnosticRow("Refresh Interval", "${prefs.weatherRefreshIntervalMinutes} min (Battery-Saving)")
                    DiagnosticRow("Data Freshness", if (weather.isStale) "Stale (Exceeded 3h)" else "Fresh")
                    DiagnosticRow("Last Refreshed", formattedTime)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Refresh Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        isRefreshing = true
                        try {
                            WeatherRepository.getInstance(context).refreshWeather(triggerWidgetUpdate = true)
                            Toast.makeText(context, "Weather updated!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Refresh failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isRefreshing = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7)
                ),
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh Weather Now", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131823)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = Color(0xFF94A3B8), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFFE2E8F0)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 13.sp, modifier = Modifier.weight(0.4f))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
