package com.srikar.chronowidget

import com.srikar.chronowidget.data.models.WidgetPreferences
import com.srikar.chronowidget.datetime.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class DateTimeUtilsTest {

    @Test
    fun testTimeFormat12HourDefault() {
        val prefs = WidgetPreferences(is24Hour = false, showAmPm = true, showSeconds = false)
        val pattern = DateTimeUtils.getTimeFormatPattern(prefs)
        assertEquals("hh:mm a", pattern)
    }

    @Test
    fun testTimeFormat24Hour() {
        val prefs = WidgetPreferences(is24Hour = true, showAmPm = false, showSeconds = false)
        val pattern = DateTimeUtils.getTimeFormatPattern(prefs)
        assertEquals("HH:mm", pattern)
    }

    @Test
    fun testTimeFormatWithSeconds() {
        val prefs12 = WidgetPreferences(is24Hour = false, showAmPm = true, showSeconds = true)
        assertEquals("hh:mm:ss a", DateTimeUtils.getTimeFormatPattern(prefs12))

        val prefs24 = WidgetPreferences(is24Hour = true, showAmPm = false, showSeconds = true)
        assertEquals("HH:mm:ss", DateTimeUtils.getTimeFormatPattern(prefs24))
    }

    @Test
    fun testTimeFormatOverride() {
        val prefs = WidgetPreferences(timeFormatOverride = "h:mm")
        assertEquals("h:mm", DateTimeUtils.getTimeFormatPattern(prefs))
    }

    @Test
    fun testDateFormatDefault() {
        val date = LocalDate.of(2026, 11, 1)
        val prefs = WidgetPreferences(
            showWeekday = true,
            showDate = true,
            showMonth = true,
            showYear = true,
            dateFormatPreset = "EEEE, dd MMMM yyyy"
        )
        val formatted = DateTimeUtils.getFormattedDate(prefs, date, Locale.US)
        assertEquals("Sunday, 01 November 2026", formatted)
    }

    @Test
    fun testDateFormatDynamicSegments() {
        val date = LocalDate.of(2026, 11, 1)
        val prefsWithoutYear = WidgetPreferences(
            showWeekday = true,
            showDate = true,
            showMonth = true,
            showYear = false
        )
        val formatted = DateTimeUtils.getFormattedDate(prefsWithoutYear, date, Locale.US)
        assertEquals("Sunday, 01 November", formatted)
    }

    @Test
    fun testAmPmCapitalizationEvening() {
        val evening = java.time.LocalTime.of(18, 0)
        val prefs = WidgetPreferences(is24Hour = false, showAmPm = true, showSeconds = false)
        val formatted = DateTimeUtils.formatTime(evening, prefs, Locale.US)
        assertEquals("06:00 PM", formatted)
        assertTrue("Time string must end with uppercase PM", formatted.endsWith("PM"))
        assertTrue("Time string must never contain lowercase pm", !formatted.contains("pm"))
    }

    @Test
    fun testAmPmCapitalizationMorning() {
        val morning = java.time.LocalTime.of(6, 0)
        val prefs = WidgetPreferences(is24Hour = false, showAmPm = true, showSeconds = false)
        val formatted = DateTimeUtils.formatTime(morning, prefs, Locale.US)
        assertEquals("06:00 AM", formatted)
        assertTrue("Time string must end with uppercase AM", formatted.endsWith("AM"))
        assertTrue("Time string must never contain lowercase am", !formatted.contains("am"))
    }

    @Test
    fun testUppercaseAmPmTransformer() {
        assertEquals("06:00 PM", DateTimeUtils.uppercaseAmPm("06:00 pm"))
        assertEquals("06:00 AM", DateTimeUtils.uppercaseAmPm("06:00 am"))
        assertEquals("06:00 PM", DateTimeUtils.uppercaseAmPm("06:00 p.m."))
        assertEquals("06:00 AM", DateTimeUtils.uppercaseAmPm("06:00 a.m."))
        assertEquals("18:00", DateTimeUtils.uppercaseAmPm("18:00"))
    }

    @Test
    fun test24HourModePreservation() {
        val evening = java.time.LocalTime.of(18, 30)
        val prefs = WidgetPreferences(is24Hour = true, showAmPm = false, showSeconds = false)
        val formatted = DateTimeUtils.formatTime(evening, prefs, Locale.US)
        assertEquals("18:30", formatted)
    }
}
