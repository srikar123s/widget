package com.srikar.chronowidget.weather

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenMeteoWeatherProvider {

    companion object {
        private const val TAG = "OpenMeteoProvider"
        private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchWeather(latitude: Double, longitude: Double, cityName: String = "Current Location"): WeatherData? = withContext(Dispatchers.IO) {
        val url = "$BASE_URL?latitude=$latitude&longitude=$longitude&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "ChronoWidget-Android/1.0")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Open-Meteo request failed with code: ${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val rootJson = JSONObject(body)
            val current = rootJson.getJSONObject("current")

            val temp = current.getDouble("temperature_2m")
            val apparentTemp = current.optDouble("apparent_temperature", temp)
            val humidity = current.optInt("relative_humidity_2m", 0)
            val weatherCode = current.optInt("weather_code", 0)
            val windSpeed = current.optDouble("wind_speed_10m", 0.0)

            val condition = mapWmoCodeToCondition(weatherCode)

            WeatherData(
                temperatureCelsius = temp,
                condition = condition,
                weatherCode = weatherCode,
                relativeHumidity = humidity,
                apparentTemperatureCelsius = apparentTemp,
                windSpeedKmh = windSpeed,
                locationName = cityName,
                timestampMillis = System.currentTimeMillis(),
                source = WeatherSource.OPEN_METEO,
                isStale = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Network exception fetching Open-Meteo weather: ${e.message}")
            null
        }
    }

    fun mapWmoCodeToCondition(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75, 77 -> "Snow"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Partly Cloudy"
        }
    }
}
