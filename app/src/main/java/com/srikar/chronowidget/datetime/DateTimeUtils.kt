package com.srikar.chronowidget.datetime

import com.srikar.chronowidget.data.models.WidgetPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {

    /**
     * Resolves the TextClock format string for 12-hour or 24-hour mode.
     */
    fun getTimeFormatPattern(prefs: WidgetPreferences): String {
        if (prefs.timeFormatOverride.isNotBlank()) {
            return prefs.timeFormatOverride
        }
        return if (prefs.is24Hour) {
            if (prefs.showSeconds) "HH:mm:ss" else "HH:mm"
        } else {
            val base = if (prefs.showSeconds) "hh:mm:ss" else "hh:mm"
            if (prefs.showAmPm) "$base a" else base
        }
    }

    /**
     * Formats a LocalTime object, ensuring AM/PM is always strictly uppercase (e.g. "06:00 PM", never "06:00 pm").
     */
    fun formatTime(time: java.time.LocalTime, prefs: WidgetPreferences, locale: Locale = Locale.US): String {
        val pattern = getTimeFormatPattern(prefs)
        val formatted = time.format(DateTimeFormatter.ofPattern(pattern, locale))
        return uppercaseAmPm(formatted)
    }

    /**
     * Ensures any AM/PM indicator in a formatted time string is uppercase.
     */
    fun uppercaseAmPm(timeString: String): String {
        return timeString
            .replace(Regex("(?i)\\b(am)\\b"), "AM")
            .replace(Regex("(?i)\\b(pm)\\b"), "PM")
            .replace("a.m.", "AM", ignoreCase = true)
            .replace("p.m.", "PM", ignoreCase = true)
    }

    fun getWeekday(date: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): String {
        return date.format(DateTimeFormatter.ofPattern("EEEE, ", locale))
    }

    fun getDay(date: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): String {
        return date.format(DateTimeFormatter.ofPattern("dd ", locale))
    }

    fun getMonthAndYear(showMonth: Boolean = true, showYear: Boolean = true, date: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): String {
        return when {
            showMonth && showYear -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            showMonth -> date.format(DateTimeFormatter.ofPattern("MMMM", locale))
            showYear -> date.format(DateTimeFormatter.ofPattern("yyyy", locale))
            else -> ""
        }
    }

    /**
     * Resolves the formatted date string based on preferences and system date.
     */
    fun getFormattedDate(prefs: WidgetPreferences, date: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): String {
        if (prefs.customDateFormat.isNotBlank()) {
            return try {
                val formatter = DateTimeFormatter.ofPattern(prefs.customDateFormat, locale)
                date.format(formatter)
            } catch (e: Exception) {
                fallbackFormattedDate(date, locale)
            }
        }

        // If user disabled some date segments, construct pattern dynamically
        if (!prefs.showWeekday || !prefs.showDate || !prefs.showMonth || !prefs.showYear) {
            return buildDynamicDate(prefs, date, locale)
        }

        return try {
            val formatter = DateTimeFormatter.ofPattern(prefs.dateFormatPreset, locale)
            date.format(formatter)
        } catch (e: Exception) {
            fallbackFormattedDate(date, locale)
        }
    }

    private fun buildDynamicDate(prefs: WidgetPreferences, date: LocalDate, locale: Locale): String {
        val parts = mutableListOf<String>()
        if (prefs.showWeekday) {
            parts.add(date.format(DateTimeFormatter.ofPattern("EEEE", locale)))
        }
        val dateMonthParts = mutableListOf<String>()
        if (prefs.showDate) {
            dateMonthParts.add(date.format(DateTimeFormatter.ofPattern("dd", locale)))
        }
        if (prefs.showMonth) {
            dateMonthParts.add(date.format(DateTimeFormatter.ofPattern("MMMM", locale)))
        }
        if (prefs.showYear) {
            dateMonthParts.add(date.format(DateTimeFormatter.ofPattern("yyyy", locale)))
        }

        val dateMonthString = dateMonthParts.joinToString(" ")
        return if (parts.isNotEmpty()) {
            if (dateMonthString.isNotEmpty()) {
                "${parts[0]}, $dateMonthString"
            } else {
                parts[0]
            }
        } else {
            dateMonthString
        }
    }

    private fun fallbackFormattedDate(date: LocalDate, locale: Locale): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", locale)
        return date.format(formatter)
    }
}
