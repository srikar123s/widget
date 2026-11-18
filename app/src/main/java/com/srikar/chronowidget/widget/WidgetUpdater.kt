package com.srikar.chronowidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import com.srikar.chronowidget.R
import com.srikar.chronowidget.actions.CalendarLauncher
import com.srikar.chronowidget.actions.ClockLauncher
import com.srikar.chronowidget.actions.WeatherLauncher
import com.srikar.chronowidget.data.models.ColorPalettes
import com.srikar.chronowidget.data.models.WidgetPreferences
import com.srikar.chronowidget.data.preferences.PreferencesManager
import com.srikar.chronowidget.datetime.DateTimeUtils
import com.srikar.chronowidget.weather.WeatherCache
import com.srikar.chronowidget.weather.WeatherData
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetUpdater {

    private const val TAG = "WidgetUpdater"

    fun getWeatherIconRes(code: Int): Int {
        return when (code) {
            0 -> R.drawable.ic_weather_sunny
            1, 2 -> R.drawable.ic_weather_partly_cloudy
            3 -> R.drawable.ic_weather_cloudy
            45, 48 -> R.drawable.ic_weather_fog
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> R.drawable.ic_weather_rain
            71, 73, 75, 77, 85, 86 -> R.drawable.ic_weather_snow
            95, 96, 99 -> R.drawable.ic_weather_thunder
            else -> R.drawable.ic_weather_partly_cloudy
        }
    }

    suspend fun updateAllWidgets(context: Context) = withContext(Dispatchers.IO) {
        try {
            val prefsManager = PreferencesManager(context)
            val prefs = prefsManager.getPreferencesSnapshot()
            val weatherCache = WeatherCache(context)
            val weatherData = weatherCache.getCachedWeather() ?: weatherCache.getDefaultFallbackWeather()

            // 1. Update Native RemoteViews Widgets
            updateNativeWidgets(context, prefs, weatherData)

            // 2. Update Glance Widgets
            try {
                ChronoWidget().updateAll(context)
            } catch (ge: Throwable) {
                Log.d(TAG, "Glance update info: ${ge.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widgets: ${e.message}", e)
        }
    }

    fun updateNativeWidgets(context: Context, prefs: WidgetPreferences, weather: WeatherData) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        val componentName = ComponentName(context, ChronoNativeWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (appWidgetIds.isEmpty()) {
            return
        }

        for (appWidgetId in appWidgetIds) {
            val remoteViews = buildRemoteViews(context, prefs, weather)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    fun buildRemoteViews(context: Context, prefs: WidgetPreferences, weather: WeatherData): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_chrono_layout)

        // Parse granular independent element colors safely
        val timeDigitsColor = parseColorSafe(prefs.timeDigitsColorHex, Color.WHITE)
        val amPmColor = parseColorSafe(prefs.amPmTextColorHex, Color.parseColor("#E2E8F0"))
        val weekdayColor = parseColorSafe(prefs.weekdayTextColorHex, Color.WHITE)
        val dateColor = parseColorSafe(prefs.dateTextColorHex, Color.WHITE)
        val monthYearColor = parseColorSafe(prefs.monthYearTextColorHex, Color.parseColor("#E2E8F0"))
        val tempColor = parseColorSafe(prefs.tempTextColorHex, Color.WHITE)
        val conditionColor = parseColorSafe(prefs.conditionTextColorHex, Color.parseColor("#E2E8F0"))
        val cityColor = parseColorSafe(prefs.cityTextColorHex, Color.parseColor("#E2E8F0"))

        // --- 0. BACKGROUND & TRANSPARENCY ---
        if (prefs.backgroundOpacity <= 0.01f) {
            views.setInt(R.id.widget_root, "setBackgroundColor", Color.TRANSPARENT)
        } else {
            val baseColor = parseColorSafe(prefs.backgroundColorHex, Color.BLACK)
            val alpha = (prefs.backgroundOpacity * 255).toInt().coerceIn(0, 255)
            val effectiveColor = Color.argb(
                alpha,
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor)
            )
            views.setInt(R.id.widget_root, "setBackgroundColor", effectiveColor)
        }

        // --- 1. TIME SECTION ---
        if (!prefs.showTime) {
            views.setViewVisibility(R.id.widget_time_container, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_time_container, View.VISIBLE)

            // Digits pattern (e.g. "hh:mm" or "HH:mm")
            val digitsPattern = if (prefs.timeFormatOverride.isNotBlank()) {
                prefs.timeFormatOverride
            } else if (prefs.is24Hour) {
                if (prefs.showSeconds) "HH:mm:ss" else "HH:mm"
            } else {
                if (prefs.showSeconds) "hh:mm:ss" else "hh:mm"
            }

            views.setCharSequence(R.id.widget_time, "setFormat12Hour", digitsPattern)
            views.setCharSequence(R.id.widget_time, "setFormat24Hour", digitsPattern)
            views.setBoolean(R.id.widget_time, "setAllCaps", true)
            views.setTextColor(R.id.widget_time, timeDigitsColor)
            views.setTextViewTextSize(
                R.id.widget_time,
                TypedValue.COMPLEX_UNIT_SP,
                prefs.timeFontSizeSp.coerceIn(24, 64).toFloat()
            )

            // AM/PM TextClock
            if (prefs.showAmPm && !prefs.is24Hour) {
                views.setViewVisibility(R.id.widget_ampm, View.VISIBLE)
                views.setCharSequence(R.id.widget_ampm, "setFormat12Hour", "a")
                views.setCharSequence(R.id.widget_ampm, "setFormat24Hour", "")
                views.setBoolean(R.id.widget_ampm, "setAllCaps", true)
                views.setTextColor(R.id.widget_ampm, amPmColor)
                views.setTextViewTextSize(
                    R.id.widget_ampm,
                    TypedValue.COMPLEX_UNIT_SP,
                    prefs.amPmFontSizeSp.coerceIn(10, 30).toFloat()
                )
            } else {
                views.setViewVisibility(R.id.widget_ampm, View.GONE)
            }

            // Time Click Action (Opens Clock app)
            if (prefs.timeTapAction != "NONE") {
                val timePendingIntent = ClockLauncher.getPendingIntent(context)
                views.setOnClickPendingIntent(R.id.widget_time_container, timePendingIntent)
                views.setOnClickPendingIntent(R.id.widget_time, timePendingIntent)
                views.setOnClickPendingIntent(R.id.widget_ampm, timePendingIntent)
            }
        }

        // --- 2. DATE SECTION ---
        if (!prefs.showWeekday && !prefs.showDate && !prefs.showMonth && !prefs.showYear) {
            views.setViewVisibility(R.id.widget_date_container, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_date_container, View.VISIBLE)

            // Weekday
            if (prefs.showWeekday) {
                views.setViewVisibility(R.id.widget_date_weekday, View.VISIBLE)
                views.setTextViewText(R.id.widget_date_weekday, DateTimeUtils.getWeekday())
                views.setTextColor(R.id.widget_date_weekday, weekdayColor)
                views.setTextViewTextSize(
                    R.id.widget_date_weekday,
                    TypedValue.COMPLEX_UNIT_SP,
                    prefs.dateFontSizeSp.coerceIn(10, 28).toFloat()
                )
            } else {
                views.setViewVisibility(R.id.widget_date_weekday, View.GONE)
            }

            // Day
            if (prefs.showDate) {
                views.setViewVisibility(R.id.widget_date_day, View.VISIBLE)
                views.setTextViewText(R.id.widget_date_day, DateTimeUtils.getDay())
                views.setTextColor(R.id.widget_date_day, dateColor)
                views.setTextViewTextSize(
                    R.id.widget_date_day,
                    TypedValue.COMPLEX_UNIT_SP,
                    prefs.dateFontSizeSp.coerceIn(10, 28).toFloat()
                )
            } else {
                views.setViewVisibility(R.id.widget_date_day, View.GONE)
            }

            // Month & Year
            if (prefs.showMonth || prefs.showYear) {
                views.setViewVisibility(R.id.widget_date_month_year, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_date_month_year,
                    DateTimeUtils.getMonthAndYear(prefs.showMonth, prefs.showYear)
                )
                views.setTextColor(R.id.widget_date_month_year, monthYearColor)
                views.setTextViewTextSize(
                    R.id.widget_date_month_year,
                    TypedValue.COMPLEX_UNIT_SP,
                    prefs.dateFontSizeSp.coerceIn(10, 28).toFloat()
                )
            } else {
                views.setViewVisibility(R.id.widget_date_month_year, View.GONE)
            }

            // Date Click Action (Opens Calendar app)
            if (prefs.dateTapAction != "NONE") {
                val datePendingIntent = CalendarLauncher.getPendingIntent(context)
                views.setOnClickPendingIntent(R.id.widget_date_container, datePendingIntent)
                views.setOnClickPendingIntent(R.id.widget_date_weekday, datePendingIntent)
                views.setOnClickPendingIntent(R.id.widget_date_day, datePendingIntent)
                views.setOnClickPendingIntent(R.id.widget_date_month_year, datePendingIntent)
                views.setOnClickPendingIntent(R.id.widget_date, datePendingIntent)
            }
        }

        // --- 3. WEATHER SECTION ---
        if (!prefs.showWeather) {
            views.setViewVisibility(R.id.widget_weather_container, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_weather_container, View.VISIBLE)

            // Weather Icon
            if (prefs.showIcon) {
                views.setViewVisibility(R.id.widget_weather_icon, View.VISIBLE)
                views.setImageViewResource(R.id.widget_weather_icon, getWeatherIconRes(weather.weatherCode))
                if (prefs.weatherIconColorHex.isNotBlank() && ColorPalettes.isValidHex(prefs.weatherIconColorHex)) {
                    val tint = parseColorSafe(prefs.weatherIconColorHex, Color.WHITE)
                    views.setInt(R.id.widget_weather_icon, "setColorFilter", tint)
                }
            } else {
                views.setViewVisibility(R.id.widget_weather_icon, View.GONE)
            }

            // Temperature
            if (prefs.showTemperature) {
                views.setViewVisibility(R.id.widget_weather_temp, View.VISIBLE)
                views.setTextViewText(R.id.widget_weather_temp, weather.getFormattedTemperature(prefs.isCelsius))
                views.setTextColor(R.id.widget_weather_temp, tempColor)
                views.setTextViewTextSize(
                    R.id.widget_weather_temp,
                    TypedValue.COMPLEX_UNIT_SP,
                    prefs.tempFontSizeSp.coerceIn(10, 28).toFloat()
                )
            } else {
                views.setViewVisibility(R.id.widget_weather_temp, View.GONE)
            }

            // Weather Condition
            if (prefs.showCondition) {
                views.setViewVisibility(R.id.widget_weather_condition, View.VISIBLE)
                views.setTextViewText(R.id.widget_weather_condition, weather.condition)
                views.setTextColor(R.id.widget_weather_condition, conditionColor)
                views.setTextViewTextSize(
                    R.id.widget_weather_condition,
                    TypedValue.COMPLEX_UNIT_SP,
                    prefs.conditionFontSizeSp.coerceIn(9, 24).toFloat()
                )
            } else {
                views.setViewVisibility(R.id.widget_weather_condition, View.GONE)
            }

            // Current Detected City Name
            if (prefs.showLocation && weather.locationName.isNotBlank()) {
                views.setViewVisibility(R.id.widget_weather_city, View.VISIBLE)
                views.setTextViewText(R.id.widget_weather_city, "• ${weather.locationName}")
                views.setTextColor(R.id.widget_weather_city, cityColor)
                views.setTextViewTextSize(
                    R.id.widget_weather_city,
                    TypedValue.COMPLEX_UNIT_SP,
                    prefs.cityFontSizeSp.coerceIn(9, 22).toFloat()
                )
            } else {
                views.setViewVisibility(R.id.widget_weather_city, View.GONE)
            }

            // Optional Extra Info (Humidity)
            if (prefs.showHumidity && weather.relativeHumidity > 0) {
                views.setViewVisibility(R.id.widget_weather_extra, View.VISIBLE)
                views.setTextViewText(R.id.widget_weather_extra, "(${weather.getFormattedHumidity()})")
                views.setTextColor(R.id.widget_weather_extra, conditionColor)
                views.setTextViewTextSize(
                    R.id.widget_weather_extra,
                    TypedValue.COMPLEX_UNIT_SP,
                    (prefs.conditionFontSizeSp - 2).coerceAtLeast(9).toFloat()
                )
            } else {
                views.setViewVisibility(R.id.widget_weather_extra, View.GONE)
            }

            // Weather Click Action (Opens Xiaomi Weather or ChronoWidget Weather Details)
            if (prefs.weatherTapAction != "NONE") {
                val weatherPendingIntent = WeatherLauncher.getPendingIntent(context, prefs.weatherTapAction)
                views.setOnClickPendingIntent(R.id.widget_weather_container, weatherPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_weather_icon, weatherPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_weather_temp, weatherPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_weather_condition, weatherPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_weather_city, weatherPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_weather_extra, weatherPendingIntent)
            }
        }

        // --- 4. ALIGNMENT & PADDING ---
        val hGravity = when (prefs.horizontalAlignment.uppercase()) {
            "LEFT" -> Gravity.START
            "RIGHT" -> Gravity.END
            else -> Gravity.CENTER_HORIZONTAL
        }
        val vGravity = when (prefs.verticalAlignment.uppercase()) {
            "TOP" -> Gravity.TOP
            "BOTTOM" -> Gravity.BOTTOM
            else -> Gravity.CENTER_VERTICAL
        }
        views.setInt(R.id.widget_root, "setGravity", hGravity or vGravity)

        val padPx = (prefs.paddingDp.coerceIn(0, 24) * context.resources.displayMetrics.density).toInt()
        views.setViewPadding(R.id.widget_root, padPx, padPx, padPx, padPx)

        return views
    }

    private fun parseColorSafe(hex: String, fallback: Int): Int {
        return ColorPalettes.parseColorSafe(hex, fallback)
    }
}
