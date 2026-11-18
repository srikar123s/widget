package com.srikar.chronowidget

import com.srikar.chronowidget.weather.OpenMeteoWeatherProvider
import com.srikar.chronowidget.weather.WeatherData
import com.srikar.chronowidget.weather.WeatherSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherModelsTest {

    @Test
    fun testCelsiusFormatting() {
        val weather = WeatherData(
            temperatureCelsius = 28.4,
            condition = "Partly Cloudy",
            weatherCode = 2
        )
        assertEquals("28°C", weather.getFormattedTemperature(isCelsius = true))
    }

    @Test
    fun testFahrenheitFormatting() {
        val weather = WeatherData(
            temperatureCelsius = 20.0,
            condition = "Clear",
            weatherCode = 0
        )
        // 20 * 9/5 + 32 = 68.0
        assertEquals("68°F", weather.getFormattedTemperature(isCelsius = false))
    }

    @Test
    fun testWmoCodeMapping() {
        val provider = OpenMeteoWeatherProvider()
        assertEquals("Clear Sky", provider.mapWmoCodeToCondition(0))
        assertEquals("Partly Cloudy", provider.mapWmoCodeToCondition(2))
        assertEquals("Overcast", provider.mapWmoCodeToCondition(3))
        assertEquals("Rain", provider.mapWmoCodeToCondition(61))
        assertEquals("Thunderstorm", provider.mapWmoCodeToCondition(95))
    }

    @Test
    fun testHumidityFormatting() {
        val weatherWithHumidity = WeatherData(
            temperatureCelsius = 25.0,
            condition = "Sunny",
            weatherCode = 0,
            relativeHumidity = 65
        )
        assertEquals("65%", weatherWithHumidity.getFormattedHumidity())

        val weatherWithoutHumidity = WeatherData(
            temperatureCelsius = 25.0,
            condition = "Sunny",
            weatherCode = 0,
            relativeHumidity = 0
        )
        assertEquals("", weatherWithoutHumidity.getFormattedHumidity())
    }
}
