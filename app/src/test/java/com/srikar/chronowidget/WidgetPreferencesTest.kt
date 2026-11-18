package com.srikar.chronowidget

import com.srikar.chronowidget.data.models.BackgroundPreset
import com.srikar.chronowidget.data.models.TextColorPresets
import com.srikar.chronowidget.data.models.WidgetPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPreferencesTest {

    @Test
    fun testDefaultTransparentAppearance() {
        val defaultPrefs = WidgetPreferences()
        // Must be fully transparent by default
        assertEquals(0.0f, defaultPrefs.backgroundOpacity, 0.001f)
        assertEquals(0, defaultPrefs.cornerRadiusDp)
        assertEquals("Minimalist Transparent", defaultPrefs.presetName)
    }

    @Test
    fun testRowIndependentColors() {
        val customPrefs = WidgetPreferences(
            timeTextColorHex = TextColorPresets.WHITE,
            dateTextColorHex = TextColorPresets.PALE_GREY,
            weatherTextColorHex = TextColorPresets.DARK_GREY
        )
        assertEquals("#FFFFFF", customPrefs.timeTextColorHex)
        assertEquals("#E0E0E0", customPrefs.dateTextColorHex)
        assertEquals("#424242", customPrefs.weatherTextColorHex)
    }

    @Test
    fun testBatterySavingRefreshDefault() {
        val prefs = WidgetPreferences()
        // Default refresh frequency must be 60 minutes for battery optimization
        assertEquals(60, prefs.weatherRefreshIntervalMinutes)
        // Automatic location detection enabled with zero-battery passive prioritization
        assertEquals(true, prefs.useGpsLocation)
        // City name display enabled by default
        assertEquals(true, prefs.showLocation)
    }

    @Test
    fun testTransparentPresets() {
        val transparentPreset = BackgroundPreset.MINIMALIST_TRANSPARENT
        assertEquals(0.0f, transparentPreset.opacity, 0.001f)
        assertEquals(0, transparentPreset.radius)
        assertEquals("#FFFFFF", transparentPreset.timeText)
        assertEquals("#E0E0E0", transparentPreset.dateText)
    }
}
