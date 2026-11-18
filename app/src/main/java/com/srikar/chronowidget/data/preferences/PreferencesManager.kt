package com.srikar.chronowidget.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.srikar.chronowidget.data.models.WidgetPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chrono_widget_prefs")

class PreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val BACKGROUND_COLOR = stringPreferencesKey("background_color")
        val BACKGROUND_OPACITY = floatPreferencesKey("background_opacity")
        val CORNER_RADIUS = intPreferencesKey("corner_radius")
        val PRESET_NAME = stringPreferencesKey("preset_name")

        // Granular Element Colors
        val TIME_DIGITS_COLOR = stringPreferencesKey("time_digits_color")
        val AM_PM_COLOR = stringPreferencesKey("am_pm_color")
        val WEEKDAY_COLOR = stringPreferencesKey("weekday_color")
        val DATE_TEXT_COLOR = stringPreferencesKey("date_text_color")
        val MONTH_YEAR_COLOR = stringPreferencesKey("month_year_color")
        val WEATHER_ICON_COLOR = stringPreferencesKey("weather_icon_color")
        val TEMP_COLOR = stringPreferencesKey("temp_color")
        val CONDITION_COLOR = stringPreferencesKey("condition_color")
        val CITY_COLOR = stringPreferencesKey("city_color")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val TEXT_OPACITY = floatPreferencesKey("text_opacity")
        val AUTO_CONTRAST_MODE = booleanPreferencesKey("auto_contrast_mode")

        // Time Settings
        val SHOW_TIME = booleanPreferencesKey("show_time")
        val IS_24_HOUR = booleanPreferencesKey("is_24_hour")
        val SHOW_AM_PM = booleanPreferencesKey("show_am_pm")
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val TIME_FONT_SIZE = intPreferencesKey("time_font_size")
        val AM_PM_FONT_SIZE = intPreferencesKey("am_pm_font_size")
        val TIME_FORMAT_OVERRIDE = stringPreferencesKey("time_format_override")

        // Date Settings
        val SHOW_WEEKDAY = booleanPreferencesKey("show_weekday")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val SHOW_MONTH = booleanPreferencesKey("show_month")
        val SHOW_YEAR = booleanPreferencesKey("show_year")
        val DATE_FORMAT_PRESET = stringPreferencesKey("date_format_preset")
        val DATE_FONT_SIZE = intPreferencesKey("date_font_size")
        val CUSTOM_DATE_FORMAT = stringPreferencesKey("custom_date_format")

        // Weather Settings
        val SHOW_WEATHER = booleanPreferencesKey("show_weather")
        val SHOW_TEMPERATURE = booleanPreferencesKey("show_temperature")
        val SHOW_CONDITION = booleanPreferencesKey("show_condition")
        val SHOW_ICON = booleanPreferencesKey("show_icon")
        val SHOW_HUMIDITY = booleanPreferencesKey("show_humidity")
        val SHOW_LOCATION = booleanPreferencesKey("show_location")
        val IS_CELSIUS = booleanPreferencesKey("is_celsius")
        val TEMP_FONT_SIZE = intPreferencesKey("temp_font_size")
        val CONDITION_FONT_SIZE = intPreferencesKey("condition_font_size")
        val CITY_FONT_SIZE = intPreferencesKey("city_font_size")
        val WEATHER_ICON_SIZE = intPreferencesKey("weather_icon_size")
        val WEATHER_SOURCE_PREF = stringPreferencesKey("weather_source_preference")
        val WEATHER_REFRESH_INTERVAL = intPreferencesKey("weather_refresh_interval")

        // Typography
        val FONT_WEIGHT_NAME = stringPreferencesKey("font_weight_name")
        val IS_ITALIC = booleanPreferencesKey("is_italic")
        val LETTER_SPACING = floatPreferencesKey("letter_spacing")

        // Layout & Spacing
        val HORIZONTAL_ALIGNMENT = stringPreferencesKey("horizontal_alignment")
        val VERTICAL_ALIGNMENT = stringPreferencesKey("vertical_alignment")
        val TIME_TO_DATE_SPACING = intPreferencesKey("time_to_date_spacing")
        val DATE_TO_WEATHER_SPACING = intPreferencesKey("date_to_weather_spacing")
        val PADDING_DP = intPreferencesKey("padding_dp")

        // Text Shadow
        val ENABLE_SHADOW = booleanPreferencesKey("enable_shadow")
        val SHADOW_COLOR = stringPreferencesKey("shadow_color")
        val SHADOW_OPACITY = floatPreferencesKey("shadow_opacity")
        val SHADOW_RADIUS = floatPreferencesKey("shadow_radius")

        // Tap Actions
        val TIME_TAP_ACTION = stringPreferencesKey("time_tap_action")
        val DATE_TAP_ACTION = stringPreferencesKey("date_tap_action")
        val WEATHER_TAP_ACTION = stringPreferencesKey("weather_tap_action")

        // Location Preferences
        val USE_GPS_LOCATION = booleanPreferencesKey("use_gps_location")
        val MANUAL_LATITUDE = doublePreferencesKey("manual_latitude")
        val MANUAL_LONGITUDE = doublePreferencesKey("manual_longitude")
        val MANUAL_CITY_NAME = stringPreferencesKey("manual_city_name")

        // Recent Colors & Custom Presets
        val RECENT_COLORS = stringPreferencesKey("recent_colors")
        val CUSTOM_PRESETS = stringPreferencesKey("custom_presets")
    }

    val widgetPreferencesFlow: Flow<WidgetPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val legacyTime = prefs[stringPreferencesKey("time_text_color")] ?: "#FFFFFF"
            val legacyDate = prefs[stringPreferencesKey("date_text_color")] ?: "#E2E8F0"
            val legacyWeather = prefs[stringPreferencesKey("weather_text_color")] ?: "#E2E8F0"

            WidgetPreferences(
                backgroundColorHex = prefs[PreferencesKeys.BACKGROUND_COLOR] ?: "#000000",
                backgroundOpacity = prefs[PreferencesKeys.BACKGROUND_OPACITY] ?: 0.0f,
                cornerRadiusDp = prefs[PreferencesKeys.CORNER_RADIUS] ?: 0,
                presetName = prefs[PreferencesKeys.PRESET_NAME] ?: "Minimalist Transparent",

                timeDigitsColorHex = prefs[PreferencesKeys.TIME_DIGITS_COLOR] ?: legacyTime,
                amPmTextColorHex = prefs[PreferencesKeys.AM_PM_COLOR] ?: legacyWeather,
                weekdayTextColorHex = prefs[PreferencesKeys.WEEKDAY_COLOR] ?: "#FFFFFF",
                dateTextColorHex = prefs[PreferencesKeys.DATE_TEXT_COLOR] ?: legacyDate,
                monthYearTextColorHex = prefs[PreferencesKeys.MONTH_YEAR_COLOR] ?: legacyWeather,
                weatherIconColorHex = prefs[PreferencesKeys.WEATHER_ICON_COLOR] ?: "",
                tempTextColorHex = prefs[PreferencesKeys.TEMP_COLOR] ?: legacyWeather,
                conditionTextColorHex = prefs[PreferencesKeys.CONDITION_COLOR] ?: legacyWeather,
                cityTextColorHex = prefs[PreferencesKeys.CITY_COLOR] ?: legacyWeather,
                accentColorHex = prefs[PreferencesKeys.ACCENT_COLOR] ?: "#F59E0B",
                textOpacity = prefs[PreferencesKeys.TEXT_OPACITY] ?: 1.0f,
                autoContrastMode = prefs[PreferencesKeys.AUTO_CONTRAST_MODE] ?: false,

                showTime = prefs[PreferencesKeys.SHOW_TIME] ?: true,
                is24Hour = prefs[PreferencesKeys.IS_24_HOUR] ?: false,
                showAmPm = prefs[PreferencesKeys.SHOW_AM_PM] ?: true,
                showSeconds = prefs[PreferencesKeys.SHOW_SECONDS] ?: false,
                timeFontSizeSp = prefs[PreferencesKeys.TIME_FONT_SIZE] ?: 36,
                amPmFontSizeSp = prefs[PreferencesKeys.AM_PM_FONT_SIZE] ?: 16,
                timeFormatOverride = prefs[PreferencesKeys.TIME_FORMAT_OVERRIDE] ?: "",

                showWeekday = prefs[PreferencesKeys.SHOW_WEEKDAY] ?: true,
                showDate = prefs[PreferencesKeys.SHOW_DATE] ?: true,
                showMonth = prefs[PreferencesKeys.SHOW_MONTH] ?: true,
                showYear = prefs[PreferencesKeys.SHOW_YEAR] ?: true,
                dateFormatPreset = prefs[PreferencesKeys.DATE_FORMAT_PRESET] ?: "EEEE, dd MMMM yyyy",
                dateFontSizeSp = prefs[PreferencesKeys.DATE_FONT_SIZE] ?: 15,
                customDateFormat = prefs[PreferencesKeys.CUSTOM_DATE_FORMAT] ?: "",

                showWeather = prefs[PreferencesKeys.SHOW_WEATHER] ?: true,
                showTemperature = prefs[PreferencesKeys.SHOW_TEMPERATURE] ?: true,
                showCondition = prefs[PreferencesKeys.SHOW_CONDITION] ?: true,
                showIcon = prefs[PreferencesKeys.SHOW_ICON] ?: true,
                showHumidity = prefs[PreferencesKeys.SHOW_HUMIDITY] ?: false,
                showLocation = prefs[PreferencesKeys.SHOW_LOCATION] ?: true,
                isCelsius = prefs[PreferencesKeys.IS_CELSIUS] ?: true,
                tempFontSizeSp = prefs[PreferencesKeys.TEMP_FONT_SIZE] ?: 15,
                conditionFontSizeSp = prefs[PreferencesKeys.CONDITION_FONT_SIZE] ?: 14,
                cityFontSizeSp = prefs[PreferencesKeys.CITY_FONT_SIZE] ?: 13,
                weatherIconSizeDp = prefs[PreferencesKeys.WEATHER_ICON_SIZE] ?: 22,
                weatherSourcePreference = prefs[PreferencesKeys.WEATHER_SOURCE_PREF] ?: "AUTO",
                weatherRefreshIntervalMinutes = prefs[PreferencesKeys.WEATHER_REFRESH_INTERVAL] ?: 60,

                fontWeightName = prefs[PreferencesKeys.FONT_WEIGHT_NAME] ?: "BOLD",
                isItalic = prefs[PreferencesKeys.IS_ITALIC] ?: false,
                letterSpacingSp = prefs[PreferencesKeys.LETTER_SPACING] ?: 0.0f,

                horizontalAlignment = prefs[PreferencesKeys.HORIZONTAL_ALIGNMENT] ?: "CENTER",
                verticalAlignment = prefs[PreferencesKeys.VERTICAL_ALIGNMENT] ?: "CENTER",
                timeToDateSpacingDp = prefs[PreferencesKeys.TIME_TO_DATE_SPACING] ?: 4,
                dateToWeatherSpacingDp = prefs[PreferencesKeys.DATE_TO_WEATHER_SPACING] ?: 8,
                paddingDp = prefs[PreferencesKeys.PADDING_DP] ?: 8,

                enableShadow = prefs[PreferencesKeys.ENABLE_SHADOW] ?: false,
                shadowColorHex = prefs[PreferencesKeys.SHADOW_COLOR] ?: "#000000",
                shadowOpacity = prefs[PreferencesKeys.SHADOW_OPACITY] ?: 0.6f,
                shadowRadiusDp = prefs[PreferencesKeys.SHADOW_RADIUS] ?: 4.0f,

                timeTapAction = prefs[PreferencesKeys.TIME_TAP_ACTION] ?: "CLOCK",
                dateTapAction = prefs[PreferencesKeys.DATE_TAP_ACTION] ?: "CALENDAR",
                weatherTapAction = prefs[PreferencesKeys.WEATHER_TAP_ACTION] ?: "XIAOMI_OR_SYSTEM",

                useGpsLocation = prefs[PreferencesKeys.USE_GPS_LOCATION] ?: true,
                manualLatitude = prefs[PreferencesKeys.MANUAL_LATITUDE] ?: 12.9716,
                manualLongitude = prefs[PreferencesKeys.MANUAL_LONGITUDE] ?: 77.5946,
                manualCityName = prefs[PreferencesKeys.MANUAL_CITY_NAME] ?: "Bangalore",

                recentColorsCsv = prefs[PreferencesKeys.RECENT_COLORS] ?: "#FFFFFF,#000000,#E2E8F0,#424242,#38BDF8,#F59E0B",
                customPresetsJson = prefs[PreferencesKeys.CUSTOM_PRESETS] ?: "[]"
            )
        }

    suspend fun getPreferencesSnapshot(): WidgetPreferences {
        return widgetPreferencesFlow.first()
    }

    suspend fun updatePreferences(newPrefs: WidgetPreferences) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.BACKGROUND_COLOR] = newPrefs.backgroundColorHex
            prefs[PreferencesKeys.BACKGROUND_OPACITY] = newPrefs.backgroundOpacity
            prefs[PreferencesKeys.CORNER_RADIUS] = newPrefs.cornerRadiusDp
            prefs[PreferencesKeys.PRESET_NAME] = newPrefs.presetName

            prefs[PreferencesKeys.TIME_DIGITS_COLOR] = newPrefs.timeDigitsColorHex
            prefs[PreferencesKeys.AM_PM_COLOR] = newPrefs.amPmTextColorHex
            prefs[PreferencesKeys.WEEKDAY_COLOR] = newPrefs.weekdayTextColorHex
            prefs[PreferencesKeys.DATE_TEXT_COLOR] = newPrefs.dateTextColorHex
            prefs[PreferencesKeys.MONTH_YEAR_COLOR] = newPrefs.monthYearTextColorHex
            prefs[PreferencesKeys.WEATHER_ICON_COLOR] = newPrefs.weatherIconColorHex
            prefs[PreferencesKeys.TEMP_COLOR] = newPrefs.tempTextColorHex
            prefs[PreferencesKeys.CONDITION_COLOR] = newPrefs.conditionTextColorHex
            prefs[PreferencesKeys.CITY_COLOR] = newPrefs.cityTextColorHex
            prefs[PreferencesKeys.ACCENT_COLOR] = newPrefs.accentColorHex
            prefs[PreferencesKeys.TEXT_OPACITY] = newPrefs.textOpacity
            prefs[PreferencesKeys.AUTO_CONTRAST_MODE] = newPrefs.autoContrastMode

            prefs[PreferencesKeys.SHOW_TIME] = newPrefs.showTime
            prefs[PreferencesKeys.IS_24_HOUR] = newPrefs.is24Hour
            prefs[PreferencesKeys.SHOW_AM_PM] = newPrefs.showAmPm
            prefs[PreferencesKeys.SHOW_SECONDS] = newPrefs.showSeconds
            prefs[PreferencesKeys.TIME_FONT_SIZE] = newPrefs.timeFontSizeSp
            prefs[PreferencesKeys.AM_PM_FONT_SIZE] = newPrefs.amPmFontSizeSp
            prefs[PreferencesKeys.TIME_FORMAT_OVERRIDE] = newPrefs.timeFormatOverride

            prefs[PreferencesKeys.SHOW_WEEKDAY] = newPrefs.showWeekday
            prefs[PreferencesKeys.SHOW_DATE] = newPrefs.showDate
            prefs[PreferencesKeys.SHOW_MONTH] = newPrefs.showMonth
            prefs[PreferencesKeys.SHOW_YEAR] = newPrefs.showYear
            prefs[PreferencesKeys.DATE_FORMAT_PRESET] = newPrefs.dateFormatPreset
            prefs[PreferencesKeys.DATE_FONT_SIZE] = newPrefs.dateFontSizeSp
            prefs[PreferencesKeys.CUSTOM_DATE_FORMAT] = newPrefs.customDateFormat

            prefs[PreferencesKeys.SHOW_WEATHER] = newPrefs.showWeather
            prefs[PreferencesKeys.SHOW_TEMPERATURE] = newPrefs.showTemperature
            prefs[PreferencesKeys.SHOW_CONDITION] = newPrefs.showCondition
            prefs[PreferencesKeys.SHOW_ICON] = newPrefs.showIcon
            prefs[PreferencesKeys.SHOW_HUMIDITY] = newPrefs.showHumidity
            prefs[PreferencesKeys.SHOW_LOCATION] = newPrefs.showLocation
            prefs[PreferencesKeys.IS_CELSIUS] = newPrefs.isCelsius
            prefs[PreferencesKeys.TEMP_FONT_SIZE] = newPrefs.tempFontSizeSp
            prefs[PreferencesKeys.CONDITION_FONT_SIZE] = newPrefs.conditionFontSizeSp
            prefs[PreferencesKeys.CITY_FONT_SIZE] = newPrefs.cityFontSizeSp
            prefs[PreferencesKeys.WEATHER_ICON_SIZE] = newPrefs.weatherIconSizeDp
            prefs[PreferencesKeys.WEATHER_SOURCE_PREF] = newPrefs.weatherSourcePreference
            prefs[PreferencesKeys.WEATHER_REFRESH_INTERVAL] = newPrefs.weatherRefreshIntervalMinutes

            prefs[PreferencesKeys.FONT_WEIGHT_NAME] = newPrefs.fontWeightName
            prefs[PreferencesKeys.IS_ITALIC] = newPrefs.isItalic
            prefs[PreferencesKeys.LETTER_SPACING] = newPrefs.letterSpacingSp

            prefs[PreferencesKeys.HORIZONTAL_ALIGNMENT] = newPrefs.horizontalAlignment
            prefs[PreferencesKeys.VERTICAL_ALIGNMENT] = newPrefs.verticalAlignment
            prefs[PreferencesKeys.TIME_TO_DATE_SPACING] = newPrefs.timeToDateSpacingDp
            prefs[PreferencesKeys.DATE_TO_WEATHER_SPACING] = newPrefs.dateToWeatherSpacingDp
            prefs[PreferencesKeys.PADDING_DP] = newPrefs.paddingDp

            prefs[PreferencesKeys.ENABLE_SHADOW] = newPrefs.enableShadow
            prefs[PreferencesKeys.SHADOW_COLOR] = newPrefs.shadowColorHex
            prefs[PreferencesKeys.SHADOW_OPACITY] = newPrefs.shadowOpacity
            prefs[PreferencesKeys.SHADOW_RADIUS] = newPrefs.shadowRadiusDp

            prefs[PreferencesKeys.TIME_TAP_ACTION] = newPrefs.timeTapAction
            prefs[PreferencesKeys.DATE_TAP_ACTION] = newPrefs.dateTapAction
            prefs[PreferencesKeys.WEATHER_TAP_ACTION] = newPrefs.weatherTapAction

            prefs[PreferencesKeys.USE_GPS_LOCATION] = newPrefs.useGpsLocation
            prefs[PreferencesKeys.MANUAL_LATITUDE] = newPrefs.manualLatitude
            prefs[PreferencesKeys.MANUAL_LONGITUDE] = newPrefs.manualLongitude
            prefs[PreferencesKeys.MANUAL_CITY_NAME] = newPrefs.manualCityName

            prefs[PreferencesKeys.RECENT_COLORS] = newPrefs.recentColorsCsv
            prefs[PreferencesKeys.CUSTOM_PRESETS] = newPrefs.customPresetsJson
        }
    }
}
