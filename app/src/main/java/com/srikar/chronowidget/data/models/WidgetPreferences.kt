package com.srikar.chronowidget.data.models

/**
 * Immutable configuration model storing all customization settings for ChronoWidget.
 * Default style is Minimalist Transparent (alpha 0, floating text).
 */
data class WidgetPreferences(
    // 1. Transparent Floating Appearance (Default: alpha 0, no card/borders)
    val backgroundColorHex: String = "#000000",
    val backgroundOpacity: Float = 0.0f, // 0.0f = 100% Transparent
    val cornerRadiusDp: Int = 0,
    val presetName: String = "Minimalist Transparent",

    // 2. Granular Independent Element Colours
    val timeDigitsColorHex: String = "#FFFFFF",
    val amPmTextColorHex: String = "#E2E8F0",
    val weekdayTextColorHex: String = "#FFFFFF",
    val dateTextColorHex: String = "#FFFFFF",
    val monthYearTextColorHex: String = "#E2E8F0",
    val weatherIconColorHex: String = "", // empty = default multi-color vector
    val tempTextColorHex: String = "#FFFFFF",
    val conditionTextColorHex: String = "#E2E8F0",
    val cityTextColorHex: String = "#E2E8F0",
    val accentColorHex: String = "#F59E0B",
    val textOpacity: Float = 1.0f,
    val autoContrastMode: Boolean = false,

    // Backward-compatibility aliases
    val timeTextColorHex: String = timeDigitsColorHex,
    val weatherTextColorHex: String = tempTextColorHex,
    val primaryTextColorHex: String = timeDigitsColorHex,
    val secondaryTextColorHex: String = dateTextColorHex,

    // 3. Time Settings & Granular Font Sizes
    val showTime: Boolean = true,
    val is24Hour: Boolean = false,
    val showAmPm: Boolean = true,
    val showSeconds: Boolean = false,
    val timeFontSizeSp: Int = 36, // 24 to 64sp
    val amPmFontSizeSp: Int = 16,  // 10 to 30sp
    val timeFormatOverride: String = "",

    // 4. Date Settings
    val showWeekday: Boolean = true,
    val showDate: Boolean = true,
    val showMonth: Boolean = true,
    val showYear: Boolean = true,
    val dateFormatPreset: String = "EEEE, dd MMMM yyyy",
    val dateFontSizeSp: Int = 15, // 10 to 28sp
    val customDateFormat: String = "",

    // 5. Weather Settings & Granular Sizes
    val showWeather: Boolean = true,
    val showTemperature: Boolean = true,
    val showCondition: Boolean = true,
    val showIcon: Boolean = true,
    val showHumidity: Boolean = false,
    val showLocation: Boolean = true,
    val isCelsius: Boolean = true,
    val tempFontSizeSp: Int = 15,      // 10 to 28sp
    val conditionFontSizeSp: Int = 14, // 9 to 24sp
    val cityFontSizeSp: Int = 13,      // 9 to 22sp
    val weatherFontSizeSp: Int = tempFontSizeSp,
    val weatherIconSizeDp: Int = 22,   // 16 to 36dp
    val weatherSourcePreference: String = "AUTO", // AUTO, XIAOMI, OPEN_METEO
    val weatherRefreshIntervalMinutes: Int = 60, // 60 minutes battery-saving default

    // 6. Typography Controls
    val fontWeightName: String = "BOLD", // LIGHT, REGULAR, MEDIUM, SEMIBOLD, BOLD
    val isItalic: Boolean = false,
    val letterSpacingSp: Float = 0.0f, // 0.0 to 4.0sp

    // 7. Layout & Spacing Controls
    val horizontalAlignment: String = "CENTER", // CENTER, LEFT, RIGHT
    val verticalAlignment: String = "CENTER",   // CENTER, TOP, BOTTOM
    val alignment: String = horizontalAlignment,
    val spacingLevel: String = "MEDIUM",
    val timeToDateSpacingDp: Int = 4,     // 4 to 24dp
    val dateToWeatherSpacingDp: Int = 8,  // 4 to 24dp
    val paddingDp: Int = 8,               // 0 to 24dp

    // 8. Text Shadow
    val enableShadow: Boolean = false,
    val shadowColorHex: String = "#000000",
    val shadowOpacity: Float = 0.6f, // 0.0 to 1.0
    val shadowRadiusDp: Float = 4.0f, // 0 to 12dp

    // 9. Tap Actions
    val timeTapAction: String = "CLOCK", // CLOCK, DASHBOARD, NONE
    val dateTapAction: String = "CALENDAR", // CALENDAR, DASHBOARD, NONE
    val weatherTapAction: String = "XIAOMI_OR_SYSTEM", // XIAOMI_OR_SYSTEM, DETAILS, DASHBOARD, NONE

    // 10. Location Preferences
    val useGpsLocation: Boolean = true,
    val manualLatitude: Double = 12.9716,
    val manualLongitude: Double = 77.5946,
    val manualCityName: String = "Bangalore",

    // 11. Custom Themes & Recent Colors Persistence
    val recentColorsCsv: String = "#FFFFFF,#000000,#E2E8F0,#424242,#38BDF8,#F59E0B",
    val customPresetsJson: String = "[]"
)

enum class BackgroundPreset(
    val title: String,
    val bgHex: String,
    val opacity: Float,
    val radius: Int,
    val timeText: String,
    val dateText: String,
    val weatherText: String
) {
    MINIMALIST_TRANSPARENT("Minimalist Transparent", "#000000", 0.0f, 0, "#FFFFFF", "#E0E0E0", "#E0E0E0"),
    TRANSPARENT_BLACK("Transparent Dark", "#000000", 0.0f, 0, "#000000", "#424242", "#424242"),
    TRANSPARENT_WHITE("Transparent Light", "#000000", 0.0f, 0, "#FFFFFF", "#E0E0E0", "#E0E0E0"),
    HYPEROS_DARK("HyperOS Dark (Card)", "#11141C", 0.95f, 24, "#FFFFFF", "#E0E0E0", "#D1D5DB"),
    DEEP_AMOLED("Deep AMOLED (Card)", "#000000", 1.0f, 20, "#FFFFFF", "#E0E0E0", "#9CA3AF"),
    TRANSLUCENT_GLASS("Translucent Glass", "#0F172A", 0.40f, 24, "#FFFFFF", "#E0E0E0", "#E2E8F0")
}

object TextColorPresets {
    const val BLACK = "#000000"
    const val DARK_GREY = "#424242"
    const val WHITE = "#FFFFFF"
    const val PALE_GREY = "#E0E0E0"

    val standardColors = listOf(
        Pair("White", WHITE),
        Pair("Pale Grey", PALE_GREY),
        Pair("Dark Grey", DARK_GREY),
        Pair("Black", BLACK)
    )
}
