package com.srikar.chronowidget

import com.srikar.chronowidget.data.models.ColorPalettes
import com.srikar.chronowidget.data.models.UserPreset
import com.srikar.chronowidget.data.models.WidgetPreferences
import org.junit.Assert.*
import org.junit.Test

class AdvancedCustomizationTest {

    @Test
    fun testEveryColorPaletteHasSixColors() {
        // Must provide 5 palettes: Light, Dark, Warm, Cool, Accent
        assertEquals(5, ColorPalettes.ALL_PALETTES.size)

        for (palette in ColorPalettes.ALL_PALETTES) {
            assertEquals("Palette ${palette.category} must have exactly 6 colors", 6, palette.colors.size)
            for (color in palette.colors) {
                assertTrue("Color name should not be blank", color.name.isNotBlank())
                assertTrue("Color hex '${color.hex}' must be valid hex", ColorPalettes.isValidHex(color.hex))
            }
        }
    }

    @Test
    fun testSpecificPaletteNamesAndColors() {
        // 1. Light: White, Off-white, Pale Grey, Silver, Light Blue, Ice Blue
        val lightNames = ColorPalettes.LIGHT.colors.map { it.name }
        assertTrue(lightNames.contains("White"))
        assertTrue(lightNames.contains("Off-white"))
        assertTrue(lightNames.contains("Pale Grey"))
        assertTrue(lightNames.contains("Silver"))
        assertTrue(lightNames.contains("Light Blue"))
        assertTrue(lightNames.contains("Ice Blue"))

        // 2. Dark: Black, Charcoal, Dark Grey, Slate, Graphite, Dark Blue-Grey
        val darkNames = ColorPalettes.DARK.colors.map { it.name }
        assertTrue(darkNames.contains("Black"))
        assertTrue(darkNames.contains("Charcoal"))
        assertTrue(darkNames.contains("Dark Grey"))
        assertTrue(darkNames.contains("Slate"))
        assertTrue(darkNames.contains("Graphite"))
        assertTrue(darkNames.contains("Dark Blue-Grey"))

        // 3. Warm: Cream, Beige, Champagne, Peach, Sand, Warm Grey
        val warmNames = ColorPalettes.WARM.colors.map { it.name }
        assertTrue(warmNames.contains("Cream"))
        assertTrue(warmNames.contains("Beige"))
        assertTrue(warmNames.contains("Champagne"))
        assertTrue(warmNames.contains("Peach"))
        assertTrue(warmNames.contains("Sand"))
        assertTrue(warmNames.contains("Warm Grey"))

        // 4. Cool: Sky Blue, Powder Blue, Mint, Teal, Lavender, Periwinkle
        val coolNames = ColorPalettes.COOL.colors.map { it.name }
        assertTrue(coolNames.contains("Sky Blue"))
        assertTrue(coolNames.contains("Powder Blue"))
        assertTrue(coolNames.contains("Mint"))
        assertTrue(coolNames.contains("Teal"))
        assertTrue(coolNames.contains("Lavender"))
        assertTrue(coolNames.contains("Periwinkle"))

        // 5. Accent: Gold, Amber, Coral, Rose, Purple, Turquoise
        val accentNames = ColorPalettes.ACCENT.colors.map { it.name }
        assertTrue(accentNames.contains("Gold"))
        assertTrue(accentNames.contains("Amber"))
        assertTrue(accentNames.contains("Coral"))
        assertTrue(accentNames.contains("Rose"))
        assertTrue(accentNames.contains("Purple"))
        assertTrue(accentNames.contains("Turquoise"))
    }

    @Test
    fun testCustomHexValidation() {
        // Valid hex strings
        assertTrue(ColorPalettes.isValidHex("#FFFFFF"))
        assertTrue(ColorPalettes.isValidHex("#ffffff"))
        assertTrue(ColorPalettes.isValidHex("FFFFFF"))
        assertTrue(ColorPalettes.isValidHex("#000000"))
        assertTrue(ColorPalettes.isValidHex("#38BDF8"))
        assertTrue(ColorPalettes.isValidHex("#FFFFFFFF")) // 8-char ARGB
        assertTrue(ColorPalettes.isValidHex("#FFF"))      // 3-char shorthand

        // Invalid hex strings
        assertFalse(ColorPalettes.isValidHex(""))
        assertFalse(ColorPalettes.isValidHex("   "))
        assertFalse(ColorPalettes.isValidHex("#GGGGGG"))
        assertFalse(ColorPalettes.isValidHex("not-a-color"))
        assertFalse(ColorPalettes.isValidHex("#12345"))   // 5 chars
        assertFalse(ColorPalettes.isValidHex("#1234567")) // 7 chars
    }

    @Test
    fun testHexSanitization() {
        assertEquals("#FFFFFF", ColorPalettes.sanitizeHex("#ffffff"))
        assertEquals("#38BDF8", ColorPalettes.sanitizeHex("38bdf8"))
        assertEquals("#FFFFFF", ColorPalettes.sanitizeHex("#FFF"))
        assertEquals("#000000", ColorPalettes.sanitizeHex("#000"))
        assertEquals("#FFFFFF", ColorPalettes.sanitizeHex("invalid-hex", fallback = "#FFFFFF"))
    }

    @Test
    fun testIndependentElementColors() {
        val prefs = WidgetPreferences(
            timeDigitsColorHex = "#FFFFFF",
            amPmTextColorHex = "#BAE6FD",
            weekdayTextColorHex = "#F59E0B",
            dateTextColorHex = "#38BDF8",
            monthYearTextColorHex = "#E2E8F0",
            weatherIconColorHex = "#F59E0B",
            tempTextColorHex = "#A7F3D0",
            conditionTextColorHex = "#2DD4BF",
            cityTextColorHex = "#C7D2FE"
        )

        // Ensure every single element maintains its own independent color
        assertEquals("#FFFFFF", prefs.timeDigitsColorHex)
        assertEquals("#BAE6FD", prefs.amPmTextColorHex)
        assertEquals("#F59E0B", prefs.weekdayTextColorHex)
        assertEquals("#38BDF8", prefs.dateTextColorHex)
        assertEquals("#E2E8F0", prefs.monthYearTextColorHex)
        assertEquals("#F59E0B", prefs.weatherIconColorHex)
        assertEquals("#A7F3D0", prefs.tempTextColorHex)
        assertEquals("#2DD4BF", prefs.conditionTextColorHex)
        assertEquals("#C7D2FE", prefs.cityTextColorHex)

        // Assert they are not all forced to be equal
        assertNotEquals(prefs.timeDigitsColorHex, prefs.weekdayTextColorHex)
        assertNotEquals(prefs.tempTextColorHex, prefs.cityTextColorHex)
    }

    @Test
    fun testFontSizeLimitsAndClamping() {
        val testPrefs = WidgetPreferences(
            timeFontSizeSp = 100, // Should clamp to 64
            amPmFontSizeSp = 5,   // Should clamp to 10
            dateFontSizeSp = 40,  // Should clamp to 28
            tempFontSizeSp = 5,   // Should clamp to 10
            conditionFontSizeSp = 50, // Should clamp to 24
            cityFontSizeSp = 2    // Should clamp to 9
        )

        val clampedTime = testPrefs.timeFontSizeSp.coerceIn(24, 64)
        val clampedAmPm = testPrefs.amPmFontSizeSp.coerceIn(10, 30)
        val clampedDate = testPrefs.dateFontSizeSp.coerceIn(10, 28)
        val clampedTemp = testPrefs.tempFontSizeSp.coerceIn(10, 28)
        val clampedCond = testPrefs.conditionFontSizeSp.coerceIn(9, 24)
        val clampedCity = testPrefs.cityFontSizeSp.coerceIn(9, 22)

        assertEquals(64, clampedTime)
        assertEquals(10, clampedAmPm)
        assertEquals(28, clampedDate)
        assertEquals(10, clampedTemp)
        assertEquals(24, clampedCond)
        assertEquals(9, clampedCity)
    }

    @Test
    fun testBuiltInThemesExist() {
        val themeNames = ColorPalettes.BUILT_IN_THEMES.map { it.name }
        assertEquals(8, themeNames.size)

        assertTrue(themeNames.contains("Minimal White"))
        assertTrue(themeNames.contains("Midnight"))
        assertTrue(themeNames.contains("Soft Grey"))
        assertTrue(themeNames.contains("Frosted Light"))
        assertTrue(themeNames.contains("Warm Sand"))
        assertTrue(themeNames.contains("Ocean Blue"))
        assertTrue(themeNames.contains("AMOLED Minimal"))
        assertTrue(themeNames.contains("Auto Contrast"))
    }

    @Test
    fun testUserPresetSerializationAndDeserialization() {
        val originalPresets = listOf(
            UserPreset(
                id = "test_preset_1",
                name = "Sunset Glow",
                timeColor = "#FB7185",
                amPmColor = "#F59E0B",
                weekdayColor = "#F43F5E",
                dateColor = "#FB7185",
                monthYearColor = "#E6C280",
                tempColor = "#FB7185",
                conditionColor = "#F59E0B",
                cityColor = "#F59E0B",
                timeSize = 40,
                amPmSize = 18,
                dateSize = 16,
                tempSize = 16,
                conditionSize = 15,
                citySize = 14,
                fontWeight = "BOLD",
                isItalic = true,
                bgOpacity = 0.0f,
                isBuiltIn = false
            )
        )

        val json = ColorPalettes.serializePresets(originalPresets)
        assertTrue(json.contains("Sunset Glow"))
        assertTrue(json.contains("#FB7185"))

        val restored = ColorPalettes.deserializePresets(json)
        assertEquals(1, restored.size)
        assertEquals("Sunset Glow", restored[0].name)
        assertEquals("#FB7185", restored[0].timeColor)
        assertEquals(40, restored[0].timeSize)
        assertTrue(restored[0].isItalic)
        assertEquals("BOLD", restored[0].fontWeight)
    }

    @Test
    fun testTypographyAndShadowPreferences() {
        val prefs = WidgetPreferences(
            fontWeightName = "SEMIBOLD",
            isItalic = true,
            letterSpacingSp = 1.5f,
            horizontalAlignment = "LEFT",
            verticalAlignment = "TOP",
            enableShadow = true,
            shadowColorHex = "#000000",
            shadowOpacity = 0.8f,
            shadowRadiusDp = 6.0f,
            timeToDateSpacingDp = 10,
            dateToWeatherSpacingDp = 12,
            paddingDp = 16
        )

        assertEquals("SEMIBOLD", prefs.fontWeightName)
        assertTrue(prefs.isItalic)
        assertEquals(1.5f, prefs.letterSpacingSp, 0.01f)
        assertEquals("LEFT", prefs.horizontalAlignment)
        assertEquals("TOP", prefs.verticalAlignment)
        assertTrue(prefs.enableShadow)
        assertEquals("#000000", prefs.shadowColorHex)
        assertEquals(0.8f, prefs.shadowOpacity, 0.01f)
        assertEquals(6.0f, prefs.shadowRadiusDp, 0.01f)
        assertEquals(10, prefs.timeToDateSpacingDp)
        assertEquals(12, prefs.dateToWeatherSpacingDp)
        assertEquals(16, prefs.paddingDp)
    }
}
