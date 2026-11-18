package com.srikar.chronowidget.weather

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

data class XiaomiInvestigationReport(
    val targetPackage: String,
    val isAppInstalled: Boolean,
    val appVersionName: String?,
    val providerAuthority: String,
    val isProviderFound: Boolean,
    val isProviderExported: Boolean,
    val readPermission: String?,
    val queryTestResult: String,
    val availableFields: List<String>,
    val fallbackStatus: String
)

class XiaomiWeatherProvider(private val context: Context) {

    companion object {
        private const val TAG = "XiaomiWeatherProvider"

        // All known Xiaomi HyperOS / MIUI Weather ContentProvider endpoints as strings
        val CANDIDATE_URI_STRINGS = listOf(
            "content://weather/actualWeatherData/1",
            "content://weather/weather",
            "content://weather/actualWeatherData",
            "content://weather/all",
            "content://com.miui.weather2.provider/weather",
            "content://com.miui.weather2.provider/actualWeatherData/1",
            "content://com.miui.weather.provider/actualWeatherData/1",
            "content://com.miui.weather2/weather"
        )

        val CANDIDATE_URIS: List<Uri>
            get() = CANDIDATE_URI_STRINGS.mapNotNull {
                try { Uri.parse(it) } catch (_: Exception) { null }
            }

        val XIAOMI_WEATHER_URI: Uri?
            get() = try { Uri.parse(CANDIDATE_URI_STRINGS[0]) } catch (_: Exception) { null }

        private const val MAX_FRESHNESS_WINDOW_MS = 4 * 60 * 60 * 1000L // 4 hours
        private const val PROBE_FAILURE_BACKOFF_MS = 60 * 60 * 1000L // 1 hour backoff on non-Xiaomi devices
        val KNOWN_WEATHER_PACKAGES = listOf("com.miui.weather2", "com.xiaomi.weather")

        // In-memory cache for probe result to eliminate repetitive overhead on non-Xiaomi devices
        @Volatile private var cachedWorkingUri: Uri? = null
        @Volatile private var lastProbeFailedTimestamp: Long = 0L
    }

    /**
     * Conducts a safe, non-intrusive investigation of the Xiaomi Weather app and its ContentProvider.
     * Respects Android sandbox rules, avoids reflection and root, and logs full diagnostics.
     */
    fun performDetailedInvestigation(): XiaomiInvestigationReport {
        val pm = context.packageManager
        var foundPackage: String? = null
        var versionName: String? = null
        var isInstalled = false

        for (pkg in KNOWN_WEATHER_PACKAGES) {
            try {
                val pInfo = pm.getPackageInfo(pkg, PackageManager.GET_PROVIDERS)
                foundPackage = pkg
                versionName = pInfo.versionName
                isInstalled = true
                break
            } catch (e: PackageManager.NameNotFoundException) {
                // Not found under this package name
            } catch (e: Exception) {
                Log.d(TAG, "Error checking package $pkg: ${e.message}")
            }
        }

        val targetPkg = foundPackage ?: "com.miui.weather2"
        var isProviderFound = false
        var isExported = false
        var readPermission: String? = null

        val authorities = listOf("weather", "com.miui.weather2.provider", "com.miui.weather.provider")
        for (auth in authorities) {
            try {
                val providerInfo = pm.resolveContentProvider(auth, 0)
                if (providerInfo != null) {
                    isProviderFound = true
                    isExported = providerInfo.exported
                    readPermission = providerInfo.readPermission
                    break
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error resolving '$auth' authority: ${e.message}")
            }
        }

        // Test querying candidate URIs safely
        var queryResult = "Untested"
        val fields = mutableListOf<String>()

        for (uri in CANDIDATE_URIS) {
            try {
                val cursor = context.contentResolver.query(
                    uri,
                    null,
                    null,
                    null,
                    null
                )
                if (cursor != null) {
                    cursor.use { c ->
                        fields.addAll(c.columnNames)
                        val rowCount = c.count
                        queryResult = if (rowCount > 0) {
                            cachedWorkingUri = uri
                            lastProbeFailedTimestamp = 0L
                            "Success via $uri (${rowCount} row(s) returned)"
                        } else {
                            "Accessible via $uri, but cursor is empty (0 rows)"
                        }
                    }
                    break
                }
            } catch (se: SecurityException) {
                queryResult = "SecurityException: Permission denied (requires system signature/privilege)"
                Log.w(TAG, "SecurityException querying $uri: ${se.message}")
            } catch (iae: IllegalArgumentException) {
                // Unknown URL / Provider authority not registered on this ROM
            } catch (e: Exception) {
                queryResult = "Error (${e.javaClass.simpleName}): ${e.message}"
            }
        }

        val fallback = if (queryResult.startsWith("Success")) {
            "Xiaomi Weather provider data is active"
        } else {
            "Open-Meteo API (Universal GPS/Network Fallback active)"
        }

        return XiaomiInvestigationReport(
            targetPackage = targetPkg,
            isAppInstalled = isInstalled,
            appVersionName = versionName,
            providerAuthority = "weather",
            isProviderFound = isProviderFound,
            isProviderExported = isExported,
            readPermission = readPermission ?: "signatureOrSystem",
            queryTestResult = queryResult,
            availableFields = fields,
            fallbackStatus = fallback
        )
    }

    /**
     * Safely checks if the Xiaomi Weather Content Provider is present and accessible.
     */
    fun isProviderAvailable(): Boolean {
        // If recent probe failed completely within backoff window, skip checking
        if (cachedWorkingUri == null && lastProbeFailedTimestamp > 0 &&
            System.currentTimeMillis() - lastProbeFailedTimestamp < PROBE_FAILURE_BACKOFF_MS) {
            return false
        }

        return try {
            val packageManager = context.packageManager
            val authorities = listOf("weather", "com.miui.weather2.provider", "com.miui.weather.provider")
            authorities.any { auth ->
                val providerInfo = packageManager.resolveContentProvider(auth, 0)
                providerInfo != null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Provider check failed: ${e.message}")
            false
        }
    }

    /**
     * Attempts to query Xiaomi Weather content providers safely across all known endpoints.
     * Caches the working URI or backoff failure to avoid overhead on non-Xiaomi devices.
     * Returns valid WeatherData if successful and fresh; null otherwise.
     */
    suspend fun queryWeatherData(): WeatherData? {
        val now = System.currentTimeMillis()

        // Short-circuit on non-Xiaomi devices if previous probe failed within backoff window
        if (cachedWorkingUri == null && lastProbeFailedTimestamp > 0 &&
            now - lastProbeFailedTimestamp < PROBE_FAILURE_BACKOFF_MS) {
            return null
        }

        val contentResolver = context.contentResolver

        // Prioritize known cached working URI if available
        val urisToTry = cachedWorkingUri?.let { listOf(it) + CANDIDATE_URIS.filter { u -> u != it } } ?: CANDIDATE_URIS

        for (uri in urisToTry) {
            try {
                val cursor = contentResolver.query(uri, null, null, null, null) ?: continue
                cursor.use { c ->
                    if (!c.moveToFirst()) return@use

                    val tempCol = getColumnIndexAny(c, "temp", "temperature", "temp_current", "curr_temp", "current_temp")
                    val descCol = getColumnIndexAny(c, "description", "weather_type_name", "weather_description", "weather_text", "weather")
                    val cityCol = getColumnIndexAny(c, "city_name", "cityName", "city", "location", "area_name")
                    val humidityCol = getColumnIndexAny(c, "humidity", "relative_humidity", "humidity_val")
                    val timeCol = getColumnIndexAny(c, "publish_time", "time", "timestamp", "update_time", "pubTime")
                    val codeCol = getColumnIndexAny(c, "weather_type", "type", "weather_code", "weatherCode")

                    if (tempCol == -1) return@use

                    val tempString = c.getString(tempCol) ?: return@use
                    val tempVal = parseTemperature(tempString) ?: return@use

                    if (tempVal < -60 || tempVal > 60) return@use

                    val description = if (descCol != -1) c.getString(descCol) ?: "Partly Cloudy" else "Partly Cloudy"
                    val cityName = if (cityCol != -1) c.getString(cityCol) ?: "Current Location" else "Current Location"
                    val humidity = if (humidityCol != -1) c.getInt(humidityCol) else 0
                    val weatherCode = if (codeCol != -1) c.getInt(codeCol) else 2

                    var isStale = false
                    var timestamp = System.currentTimeMillis()
                    if (timeCol != -1) {
                        val rawTime = c.getLong(timeCol)
                        if (rawTime > 0) {
                            timestamp = if (rawTime < 2_000_000_000L) rawTime * 1000L else rawTime
                            val age = System.currentTimeMillis() - timestamp
                            if (age > MAX_FRESHNESS_WINDOW_MS || age < -60_000L) {
                                isStale = true
                            }
                        }
                    }

                    // Save working URI for fast future lookups
                    cachedWorkingUri = uri
                    lastProbeFailedTimestamp = 0L

                    Log.i(TAG, "Successfully extracted weather from Xiaomi provider ($uri): $cityName, $tempVal°C, $description")

                    return WeatherData(
                        temperatureCelsius = tempVal,
                        condition = description,
                        weatherCode = weatherCode,
                        relativeHumidity = humidity,
                        apparentTemperatureCelsius = tempVal,
                        locationName = cityName,
                        timestampMillis = timestamp,
                        source = WeatherSource.XIAOMI,
                        isStale = isStale
                    )
                }
            } catch (se: SecurityException) {
                Log.d(TAG, "Access denied to $uri (system signature required): ${se.message}")
            } catch (e: Exception) {
                Log.d(TAG, "Cannot query $uri: ${e.message}")
            }
        }

        // If all candidate URIs failed, record failure timestamp to avoid polling on non-Xiaomi devices
        if (cachedWorkingUri == null) {
            lastProbeFailedTimestamp = now
        }

        return null
    }

    private fun getColumnIndexAny(cursor: android.database.Cursor, vararg columnNames: String): Int {
        for (name in columnNames) {
            val idx = cursor.getColumnIndex(name)
            if (idx != -1) return idx
        }
        return -1
    }

    private fun parseTemperature(raw: String): Double? {
        val clean = raw.replace("°C", "").replace("°F", "").replace("°", "").trim()
        return clean.toDoubleOrNull()
    }
}
