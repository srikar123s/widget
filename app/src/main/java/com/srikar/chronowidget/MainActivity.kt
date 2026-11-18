package com.srikar.chronowidget

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.srikar.chronowidget.actions.WeatherLauncher
import com.srikar.chronowidget.data.models.WidgetPreferences
import com.srikar.chronowidget.data.preferences.PreferencesManager
import com.srikar.chronowidget.ui.customization.CustomizationScreen
import com.srikar.chronowidget.ui.dashboard.DashboardScreen
import com.srikar.chronowidget.ui.settings.AboutScreen
import com.srikar.chronowidget.ui.settings.WeatherDetailsScreen
import com.srikar.chronowidget.ui.theme.ChronoWidgetTheme
import com.srikar.chronowidget.weather.WeatherData
import com.srikar.chronowidget.weather.WeatherRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var weatherRepository: WeatherRepository

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocationGranted || coarseLocationGranted) {
            lifecycleScope.launch {
                val currentPrefs = preferencesManager.getPreferencesSnapshot()
                preferencesManager.updatePreferences(currentPrefs.copy(useGpsLocation = true))
                weatherRepository.refreshWeather(triggerWidgetUpdate = true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesManager = PreferencesManager(this)
        weatherRepository = WeatherRepository.getInstance(this)

        val initialDestination = parseIntentDestination(intent)

        setContent {
            ChronoWidgetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0E1117)
                ) {
                    val prefs by preferencesManager.widgetPreferencesFlow.collectAsState(
                        initial = WidgetPreferences()
                    )
                    val weather by weatherRepository.weatherState.collectAsState()
                    val location by weatherRepository.locationState.collectAsState()

                    var currentScreen by remember { mutableStateOf(initialDestination) }

                    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            "dashboard" -> DashboardScreen(
                                prefs = prefs,
                                weather = weather,
                                location = location,
                                onRequestLocationPermission = { requestLocationPermissions() },
                                onOpenAppSettings = { openAppSettings() },
                                onOpenLocationSettings = { openLocationSettings() },
                                onSelectCity = { cityName, lat, lon ->
                                    lifecycleScope.launch {
                                        val updated = prefs.copy(
                                            useGpsLocation = false,
                                            manualCityName = cityName,
                                            manualLatitude = lat,
                                            manualLongitude = lon
                                        )
                                        preferencesManager.updatePreferences(updated)
                                        weatherRepository.refreshWeather(triggerWidgetUpdate = true)
                                    }
                                },
                                onNavigateToCustomization = { currentScreen = "customization" },
                                onNavigateToWeatherDetails = { currentScreen = "weather_details" },
                                onNavigateToAbout = { currentScreen = "about" }
                            )

                            "customization" -> CustomizationScreen(
                                initialPrefs = prefs,
                                weather = weather,
                                onBack = { currentScreen = "dashboard" }
                            )

                            "weather_details" -> WeatherDetailsScreen(
                                prefs = prefs,
                                weather = weather,
                                onBack = { currentScreen = "dashboard" }
                            )

                            "about" -> AboutScreen(
                                onBack = { currentScreen = "dashboard" }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun parseIntentDestination(intent: Intent?): String {
        return if (intent?.getStringExtra(WeatherLauncher.EXTRA_DESTINATION) == WeatherLauncher.DESTINATION_WEATHER_DETAILS) {
            "weather_details"
        } else {
            "dashboard"
        }
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}
