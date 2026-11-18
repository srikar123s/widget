package com.srikar.chronowidget.weather

import android.content.Context
import android.util.Log
import com.srikar.chronowidget.data.preferences.PreferencesManager
import com.srikar.chronowidget.location.LocationHelper
import com.srikar.chronowidget.location.LocationResult
import com.srikar.chronowidget.location.LocationSourceType
import com.srikar.chronowidget.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherRepository(
    private val context: Context,
    private val preferencesManager: PreferencesManager = PreferencesManager(context),
    private val sourceManager: WeatherSourceManager = WeatherSourceManager(context),
    private val cache: WeatherCache = WeatherCache(context)
) {

    companion object {
        private const val TAG = "WeatherRepository"

        @Volatile
        private var INSTANCE: WeatherRepository? = null

        fun getInstance(context: Context): WeatherRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WeatherRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _weatherState = MutableStateFlow(
        cache.getCachedWeather() ?: cache.getDefaultFallbackWeather()
    )
    val weatherState: StateFlow<WeatherData> = _weatherState.asStateFlow()

    private var _lastLocation: LocationResult? = null
    private val _locationState = MutableStateFlow(
        LocationResult(
            latitude = 12.9716,
            longitude = 77.5946,
            cityName = "Bangalore",
            sourceType = LocationSourceType.MANUAL,
            isApproximate = false
        )
    )
    val locationState: StateFlow<LocationResult> = _locationState.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        // Load initial weather and location in background
        repositoryScope.launch {
            refreshWeather(false)
        }
    }

    /**
     * Executes the location-based weather flow:
     * 1. Check permission / preferences
     * 2. Obtain current coordinates (prioritizing cached coordinates for battery efficiency)
     * 3. Resolve coordinates into accurate city/locality name via reverse geocoding
     * 4. Fetch weather for those EXACT coordinates
     * 5. Save last successful weather in cache
     * 6. Trigger widget updates with synchronized city and weather
     */
    suspend fun refreshWeather(triggerWidgetUpdate: Boolean = true): WeatherData {
        val prefs = preferencesManager.getPreferencesSnapshot()

        // 1 & 2 & 3: Resolve location and actual city name
        val locResult = LocationHelper.resolveLocationAndCity(
            context = context,
            prefs = prefs,
            cachedResult = _lastLocation
        )
        _lastLocation = locResult
        _locationState.value = locResult

        Log.d(TAG, "Fetching weather for city: ${locResult.cityName} (${locResult.latitude}, ${locResult.longitude})")

        // 4 & 5: Fetch weather for exact coordinates using AUTO source (Xiaomi -> Open-Meteo -> Cache)
        val freshWeather = sourceManager.resolveWeather(
            sourcePreference = prefs.weatherSourcePreference,
            latitude = locResult.latitude,
            longitude = locResult.longitude,
            cityName = locResult.cityName
        )

        _weatherState.value = freshWeather

        // 6: Update all widgets
        if (triggerWidgetUpdate) {
            try {
                WidgetUpdater.updateAllWidgets(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widgets after weather refresh: ${e.message}")
            }
        }

        return freshWeather
    }
}
