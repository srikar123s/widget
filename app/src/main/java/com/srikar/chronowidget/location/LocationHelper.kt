package com.srikar.chronowidget.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.srikar.chronowidget.data.models.WidgetPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

enum class LocationPermissionStatus {
    GRANTED_FINE,
    GRANTED_COARSE,
    DENIED
}

enum class LocationSourceType(val displayName: String) {
    PASSIVE("Passive (Zero Battery)"),
    NETWORK("Network / Cell Tower"),
    GPS("GPS Sensor"),
    MANUAL("Manual City Selection")
}

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val sourceType: LocationSourceType,
    val isApproximate: Boolean,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class CityPreset(
    val cityName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
)

object LocationHelper {

    private const val TAG = "LocationHelper"
    private const val SIGNIFICANT_DISTANCE_METERS = 5000f // 5 km movement threshold

    val POPULAR_CITIES = listOf(
        CityPreset("Bangalore", "India", 12.9716, 77.5946),
        CityPreset("New York", "USA", 40.7128, -74.0060),
        CityPreset("London", "UK", 51.5074, -0.1278),
        CityPreset("Tokyo", "Japan", 35.6762, 139.6503),
        CityPreset("San Francisco", "USA", 37.7749, -122.4194),
        CityPreset("Paris", "France", 48.8566, 2.3522),
        CityPreset("Sydney", "Australia", -33.8688, 151.2093),
        CityPreset("Dubai", "UAE", 25.2048, 55.2708),
        CityPreset("Singapore", "Singapore", 1.3521, 103.8198),
        CityPreset("Mumbai", "India", 19.0760, 72.8777)
    )

    fun checkPermission(context: Context): LocationPermissionStatus {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return when {
            fineGranted -> LocationPermissionStatus.GRANTED_FINE
            coarseGranted -> LocationPermissionStatus.GRANTED_COARSE
            else -> LocationPermissionStatus.DENIED
        }
    }

    /**
     * Resolves the device location and city name.
     * Prioritizes cached locations to eliminate battery consumption.
     * Uses reverse-geocoding only when necessary.
     */
    suspend fun resolveLocationAndCity(
        context: Context,
        prefs: WidgetPreferences,
        cachedResult: LocationResult? = null
    ): LocationResult = withContext(Dispatchers.IO) {
        val permissionStatus = checkPermission(context)

        // If permission is denied or manual location is forced by user
        if (permissionStatus == LocationPermissionStatus.DENIED || !prefs.useGpsLocation) {
            return@withContext LocationResult(
                latitude = prefs.manualLatitude,
                longitude = prefs.manualLongitude,
                cityName = prefs.manualCityName.ifBlank { "Bangalore" },
                sourceType = LocationSourceType.MANUAL,
                isApproximate = false,
                timestampMillis = System.currentTimeMillis()
            )
        }

        // 1. Obtain current or cached location
        val (location, sourceType) = getDeviceLocation(context)

        if (location == null) {
            // Location could not be resolved from sensors; fallback to manual city
            Log.w(TAG, "No device location available; falling back to manual city: ${prefs.manualCityName}")
            return@withContext LocationResult(
                latitude = prefs.manualLatitude,
                longitude = prefs.manualLongitude,
                cityName = prefs.manualCityName.ifBlank { "Bangalore" },
                sourceType = LocationSourceType.MANUAL,
                isApproximate = false,
                timestampMillis = System.currentTimeMillis()
            )
        }

        val isApproximate = permissionStatus == LocationPermissionStatus.GRANTED_COARSE ||
                sourceType == LocationSourceType.NETWORK

        // 2. Check significant movement threshold before hitting Geocoder
        if (cachedResult != null &&
            cachedResult.sourceType != LocationSourceType.MANUAL &&
            cachedResult.cityName.isNotBlank() &&
            cachedResult.cityName != "Local Area"
        ) {
            val results = FloatArray(1)
            Location.distanceBetween(
                location.latitude,
                location.longitude,
                cachedResult.latitude,
                cachedResult.longitude,
                results
            )
            val distanceMeters = results[0]
            if (distanceMeters < SIGNIFICANT_DISTANCE_METERS) {
                Log.d(TAG, "Device moved ${distanceMeters.toInt()}m (< 5km); reusing cached city: ${cachedResult.cityName}")
                return@withContext LocationResult(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    cityName = cachedResult.cityName,
                    sourceType = sourceType,
                    isApproximate = isApproximate,
                    timestampMillis = location.time.takeIf { it > 0 } ?: System.currentTimeMillis()
                )
            }
        }

        // 3. Reverse Geocode to obtain real city / locality name
        val detectedCity = reverseGeocodeCity(
            context = context,
            latitude = location.latitude,
            longitude = location.longitude,
            fallback = prefs.manualCityName.ifBlank { "Current City" }
        )

        Log.i(TAG, "Resolved current location: lat=${location.latitude}, lon=${location.longitude}, city=$detectedCity ($sourceType)")

        LocationResult(
            latitude = location.latitude,
            longitude = location.longitude,
            cityName = detectedCity,
            sourceType = sourceType,
            isApproximate = isApproximate,
            timestampMillis = location.time.takeIf { it > 0 } ?: System.currentTimeMillis()
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun getDeviceLocation(context: Context): Pair<Location?, LocationSourceType> {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return Pair(null, LocationSourceType.MANUAL)

        try {
            // Step A: Passive provider check (Zero battery cost, locations collected by other apps)
            val passiveLoc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (passiveLoc != null && isLocationRecent(passiveLoc)) {
                return Pair(passiveLoc, LocationSourceType.PASSIVE)
            }

            // Step B: Network provider check (Low battery, cell towers & Wi-Fi)
            val networkLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (networkLoc != null && isLocationRecent(networkLoc)) {
                return Pair(networkLoc, LocationSourceType.NETWORK)
            }

            // Step C: GPS provider check
            val gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (gpsLoc != null && isLocationRecent(gpsLoc)) {
                return Pair(gpsLoc, LocationSourceType.GPS)
            }

            // If any cached location exists (even if older than 3 hours), prefer it over turning on GPS sensor
            val anyCached = passiveLoc ?: networkLoc ?: gpsLoc
            if (anyCached != null) {
                return Pair(anyCached, LocationSourceType.PASSIVE)
            }

            // Step D: Only if NO cached location exists whatsoever, request a single quick fix (max 5s timeout)
            val freshLocation = withTimeoutOrNull(5000L) {
                requestSingleFix(locationManager)
            }
            if (freshLocation != null) {
                return Pair(freshLocation, LocationSourceType.NETWORK)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining device location: ${e.message}")
        }

        return Pair(null, LocationSourceType.MANUAL)
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleFix(locationManager: LocationManager): Location? =
        suspendCancellableCoroutine { cont ->
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> null
            }

            if (provider == null) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    try {
                        locationManager.removeUpdates(this)
                    } catch (_: Exception) {}
                    if (cont.isActive) cont.resume(loc)
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    if (cont.isActive) cont.resume(null)
                }
            }

            try {
                locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            } catch (e: Exception) {
                Log.w(TAG, "Could not request single update: ${e.message}")
                if (cont.isActive) cont.resume(null)
            }

            cont.invokeOnCancellation {
                try {
                    locationManager.removeUpdates(listener)
                } catch (_: Exception) {}
            }
        }

    private fun isLocationRecent(location: Location): Boolean {
        // Considered recent if under 3 hours old
        val ageMillis = System.currentTimeMillis() - location.time
        return ageMillis < 3 * 3600 * 1000L
    }

    /**
     * Resolves latitude and longitude to a human-readable city name using Android Geocoder.
     */
    fun reverseGeocodeCity(
        context: Context,
        latitude: Double,
        longitude: Double,
        fallback: String
    ): String {
        if (!Geocoder.isPresent()) {
            Log.w(TAG, "Geocoder service is not present on this device")
            return fallback
        }

        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val city = address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: address.subLocality
                    ?: address.featureName

                if (!city.isNullOrBlank()) {
                    return city.trim()
                }
            }
            fallback
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder reverse-lookup failed: ${e.message}")
            fallback
        }
    }
}
