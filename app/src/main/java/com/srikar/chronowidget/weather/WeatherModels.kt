package com.srikar.chronowidget.weather

enum class WeatherSource(val displayName: String) {
    AUTO("Auto (Xiaomi / Open-Meteo)"),
    XIAOMI("Xiaomi Weather Provider"),
    OPEN_METEO("Open-Meteo API"),
    CACHED("Cached Weather")
}

data class WeatherData(
    val temperatureCelsius: Double,
    val condition: String,
    val weatherCode: Int,
    val relativeHumidity: Int = 0,
    val apparentTemperatureCelsius: Double = temperatureCelsius,
    val windSpeedKmh: Double = 0.0,
    val locationName: String = "Current Location",
    val timestampMillis: Long = System.currentTimeMillis(),
    val source: WeatherSource = WeatherSource.AUTO,
    val isStale: Boolean = false
) {
    fun getFormattedTemperature(isCelsius: Boolean): String {
        return if (isCelsius) {
            "${Math.round(temperatureCelsius)}°C"
        } else {
            val fahrenheit = (temperatureCelsius * 9.0 / 5.0) + 32.0
            "${Math.round(fahrenheit)}°F"
        }
    }

    fun getFormattedHumidity(): String {
        return if (relativeHumidity > 0) "$relativeHumidity%" else ""
    }
}
