package com.srikar.chronowidget

import com.srikar.chronowidget.actions.WeatherLauncher
import com.srikar.chronowidget.data.models.WidgetPreferences
import com.srikar.chronowidget.location.LocationHelper
import com.srikar.chronowidget.location.LocationResult
import com.srikar.chronowidget.location.LocationSourceType
import com.srikar.chronowidget.weather.WeatherData
import com.srikar.chronowidget.weather.WeatherSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationAndClickActionsTest {

    @Test
    fun testPopularCityPresetsSanity() {
        val cities = LocationHelper.POPULAR_CITIES
        assertTrue("Popular city presets must not be empty", cities.isNotEmpty())
        assertTrue("Must contain at least 5 cities", cities.size >= 5)

        for (city in cities) {
            assertTrue("City name must not be blank", city.cityName.isNotBlank())
            assertTrue("Latitude must be within [-90, 90]", city.latitude in -90.0..90.0)
            assertTrue("Longitude must be within [-180, 180]", city.longitude in -180.0..180.0)
        }

        // Verify key cities
        assertTrue(cities.any { it.cityName == "Bangalore" })
        assertTrue(cities.any { it.cityName == "Tokyo" })
        assertTrue(cities.any { it.cityName == "New York" })
        assertTrue(cities.any { it.cityName == "London" })
    }

    @Test
    fun testWeatherAndCityLocationSynchronization() {
        val resolvedCity = "Tokyo"
        val weather = WeatherData(
            temperatureCelsius = 22.0,
            condition = "Sunny",
            weatherCode = 0,
            locationName = resolvedCity,
            source = WeatherSource.AUTO
        )

        assertEquals("Tokyo", weather.locationName)
        assertNotEquals("Local Area", weather.locationName)
        assertEquals("22°C", weather.getFormattedTemperature(isCelsius = true))
    }

    @Test
    fun testLocationResultModel() {
        val loc = LocationResult(
            latitude = 35.6762,
            longitude = 139.6503,
            cityName = "Tokyo",
            sourceType = LocationSourceType.PASSIVE,
            isApproximate = false
        )

        assertEquals("Tokyo", loc.cityName)
        assertEquals(35.6762, loc.latitude, 0.0001)
        assertEquals(139.6503, loc.longitude, 0.0001)
        assertEquals(LocationSourceType.PASSIVE, loc.sourceType)
        assertEquals("Passive (Zero Battery)", loc.sourceType.displayName)
    }

    @Test
    fun testDefaultTapActionsSeparation() {
        val prefs = WidgetPreferences()

        // Time, Date, and Weather must have separate default tap actions
        assertEquals("CLOCK", prefs.timeTapAction)
        assertEquals("CALENDAR", prefs.dateTapAction)
        assertEquals("XIAOMI_OR_SYSTEM", prefs.weatherTapAction)

        // All three actions are distinct
        assertNotEquals(prefs.timeTapAction, prefs.dateTapAction)
        assertNotEquals(prefs.timeTapAction, prefs.weatherTapAction)
    }

    @Test
    fun testWeatherLauncherConstants() {
        assertEquals("extra_destination", WeatherLauncher.EXTRA_DESTINATION)
        assertEquals("destination_weather_details", WeatherLauncher.DESTINATION_WEATHER_DETAILS)
    }

    @Test
    fun testClockLauncherPackagesPriority() {
        val packages = com.srikar.chronowidget.actions.ClockLauncher.CLOCK_PACKAGES
        assertTrue(packages.isNotEmpty())
        // Xiaomi MIUI / HyperOS DeskClock must be top priority
        assertEquals("com.miui.deskclock", packages[0])
        assertTrue(packages.contains("com.google.android.deskclock"))
        assertTrue(packages.contains("com.sec.android.app.clockpackage"))
        assertTrue(packages.contains("com.oneplus.deskclock"))
    }

    @Test
    fun testXiaomiCandidateUris() {
        val uris = com.srikar.chronowidget.weather.XiaomiWeatherProvider.CANDIDATE_URI_STRINGS
        assertTrue(uris.size >= 5)
        assertTrue(uris.contains("content://weather/actualWeatherData/1"))
        assertTrue(uris.contains("content://weather/weather"))
        assertTrue(uris.any { it.contains("com.miui.weather2.provider") })
    }
}
