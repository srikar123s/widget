package com.srikar.chronowidget.weather

import android.content.Context
import android.content.SharedPreferences

class WeatherCache(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("chrono_weather_cache", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TEMP = "key_temp"
        private const val KEY_CONDITION = "key_condition"
        private const val KEY_CODE = "key_code"
        private const val KEY_HUMIDITY = "key_humidity"
        private const val KEY_APPARENT_TEMP = "key_apparent_temp"
        private const val KEY_WIND_SPEED = "key_wind_speed"
        private const val KEY_LOCATION = "key_location"
        private const val KEY_TIMESTAMP = "key_timestamp"
        private const val KEY_SOURCE = "key_source"
    }

    fun saveWeather(data: WeatherData) {
        prefs.edit().apply {
            putFloat(KEY_TEMP, data.temperatureCelsius.toFloat())
            putString(KEY_CONDITION, data.condition)
            putInt(KEY_CODE, data.weatherCode)
            putInt(KEY_HUMIDITY, data.relativeHumidity)
            putFloat(KEY_APPARENT_TEMP, data.apparentTemperatureCelsius.toFloat())
            putFloat(KEY_WIND_SPEED, data.windSpeedKmh.toFloat())
            putString(KEY_LOCATION, data.locationName)
            putLong(KEY_TIMESTAMP, data.timestampMillis)
            putString(KEY_SOURCE, data.source.name)
            apply()
        }
    }

    fun getCachedWeather(): WeatherData? {
        if (!prefs.contains(KEY_TEMP)) {
            return null
        }
        val temp = prefs.getFloat(KEY_TEMP, 28f).toDouble()
        val condition = prefs.getString(KEY_CONDITION, "Partly Cloudy") ?: "Partly Cloudy"
        val code = prefs.getInt(KEY_CODE, 2)
        val humidity = prefs.getInt(KEY_HUMIDITY, 50)
        val apparent = prefs.getFloat(KEY_APPARENT_TEMP, temp.toFloat()).toDouble()
        val wind = prefs.getFloat(KEY_WIND_SPEED, 0f).toDouble()
        val location = prefs.getString(KEY_LOCATION, "Current Location") ?: "Current Location"
        val timestamp = prefs.getLong(KEY_TIMESTAMP, System.currentTimeMillis())
        val sourceStr = prefs.getString(KEY_SOURCE, WeatherSource.CACHED.name) ?: WeatherSource.CACHED.name
        val source = try {
            WeatherSource.valueOf(sourceStr)
        } catch (e: Exception) {
            WeatherSource.CACHED
        }

        val ageMs = System.currentTimeMillis() - timestamp
        val isStale = ageMs > (3 * 60 * 60 * 1000L) // Stale after 3 hours

        return WeatherData(
            temperatureCelsius = temp,
            condition = condition,
            weatherCode = code,
            relativeHumidity = humidity,
            apparentTemperatureCelsius = apparent,
            windSpeedKmh = wind,
            locationName = location,
            timestampMillis = timestamp,
            source = source,
            isStale = isStale
        )
    }

    fun getDefaultFallbackWeather(): WeatherData {
        return WeatherData(
            temperatureCelsius = 28.0,
            condition = "Partly Cloudy",
            weatherCode = 2,
            relativeHumidity = 55,
            apparentTemperatureCelsius = 29.0,
            windSpeedKmh = 12.0,
            locationName = "Local Weather",
            timestampMillis = System.currentTimeMillis(),
            source = WeatherSource.CACHED,
            isStale = false
        )
    }
}
