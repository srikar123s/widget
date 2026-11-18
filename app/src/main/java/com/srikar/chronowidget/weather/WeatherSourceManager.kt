package com.srikar.chronowidget.weather

import android.content.Context
import android.util.Log

class WeatherSourceManager(
    private val context: Context,
    private val xiaomiProvider: XiaomiWeatherProvider = XiaomiWeatherProvider(context),
    private val openMeteoProvider: OpenMeteoWeatherProvider = OpenMeteoWeatherProvider(),
    private val cache: WeatherCache = WeatherCache(context)
) {

    companion object {
        private const val TAG = "WeatherSourceManager"
    }

    suspend fun resolveWeather(
        sourcePreference: String,
        latitude: Double,
        longitude: Double,
        cityName: String
    ): WeatherData {
        Log.d(TAG, "Resolving weather with preference: $sourcePreference, lat: $latitude, lon: $longitude, city: $cityName")

        when (sourcePreference.uppercase()) {
            "XIAOMI" -> {
                val xiaomiData = xiaomiProvider.queryWeatherData()
                if (xiaomiData != null) {
                    cache.saveWeather(xiaomiData)
                    return xiaomiData
                }
                Log.w(TAG, "Xiaomi provider requested but failed; trying cache")
                return cache.getCachedWeather() ?: cache.getDefaultFallbackWeather()
            }

            "OPEN_METEO" -> {
                val openMeteoData = openMeteoProvider.fetchWeather(latitude, longitude, cityName)
                if (openMeteoData != null) {
                    cache.saveWeather(openMeteoData)
                    return openMeteoData
                }
                Log.w(TAG, "Open-Meteo requested but failed; trying cache")
                return cache.getCachedWeather() ?: cache.getDefaultFallbackWeather()
            }

            else -> { // AUTO mode
                // 1. First priority: Check Xiaomi / OEM Weather Provider
                try {
                    val xiaomiData = xiaomiProvider.queryWeatherData()
                    if (xiaomiData != null && !xiaomiData.isStale) {
                        Log.i(TAG, "Successfully loaded fresh weather directly from Xiaomi Weather Provider")
                        cache.saveWeather(xiaomiData)
                        return xiaomiData
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Xiaomi query error in AUTO mode: ${e.message}")
                }

                // 2. Second priority: Open-Meteo API fallback using exact device coordinates and detected city
                Log.d(TAG, "Xiaomi provider restricted or unavailable; requesting Open-Meteo API for $cityName")
                val openMeteoData = openMeteoProvider.fetchWeather(latitude, longitude, cityName)
                if (openMeteoData != null) {
                    Log.i(TAG, "Successfully loaded fresh weather from Open-Meteo API for $cityName")
                    cache.saveWeather(openMeteoData)
                    return openMeteoData
                }

                // 3. Third priority: Cached Weather
                Log.w(TAG, "Network unavailable; falling back to cached weather")
                return cache.getCachedWeather() ?: cache.getDefaultFallbackWeather()
            }
        }
    }

    fun isXiaomiAvailable(): Boolean = xiaomiProvider.isProviderAvailable()
}
