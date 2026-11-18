package com.srikar.chronowidget.data.models

import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

data class ColorOption(val name: String, val hex: String)

data class ColorPalette(val category: String, val colors: List<ColorOption>)

data class UserPreset(
    val id: String,
    val name: String,
    val timeColor: String,
    val amPmColor: String,
    val weekdayColor: String,
    val dateColor: String,
    val monthYearColor: String,
    val tempColor: String,
    val conditionColor: String,
    val cityColor: String,
    val timeSize: Int,
    val amPmSize: Int,
    val dateSize: Int,
    val tempSize: Int,
    val conditionSize: Int,
    val citySize: Int,
    val fontWeight: String,
    val isItalic: Boolean,
    val bgOpacity: Float,
    val isBuiltIn: Boolean = false
)

object ColorPalettes {

    // 1. Light Palette
    val LIGHT = ColorPalette(
        category = "Light",
        colors = listOf(
            ColorOption("White", "#FFFFFF"),
            ColorOption("Off-white", "#F8FAFC"),
            ColorOption("Pale Grey", "#E2E8F0"),
            ColorOption("Silver", "#CBD5E1"),
            ColorOption("Light Blue", "#BAE6FD"),
            ColorOption("Ice Blue", "#E0F2FE")
        )
    )

    // 2. Dark Palette
    val DARK = ColorPalette(
        category = "Dark",
        colors = listOf(
            ColorOption("Black", "#000000"),
            ColorOption("Charcoal", "#1E1E1E"),
            ColorOption("Dark Grey", "#424242"),
            ColorOption("Slate", "#334155"),
            ColorOption("Graphite", "#27272A"),
            ColorOption("Dark Blue-Grey", "#1E293B")
        )
    )

    // 3. Warm Palette
    val WARM = ColorPalette(
        category = "Warm",
        colors = listOf(
            ColorOption("Cream", "#FFFBEB"),
            ColorOption("Beige", "#F5F5DC"),
            ColorOption("Champagne", "#F7E7CE"),
            ColorOption("Peach", "#FFDAB9"),
            ColorOption("Sand", "#E6C280"),
            ColorOption("Warm Grey", "#78716C")
        )
    )

    // 4. Cool Palette
    val COOL = ColorPalette(
        category = "Cool",
        colors = listOf(
            ColorOption("Sky Blue", "#38BDF8"),
            ColorOption("Powder Blue", "#B0E0E6"),
            ColorOption("Mint", "#A7F3D0"),
            ColorOption("Teal", "#2DD4BF"),
            ColorOption("Lavender", "#E9D5FF"),
            ColorOption("Periwinkle", "#C7D2FE")
        )
    )

    // 5. Accent Palette
    val ACCENT = ColorPalette(
        category = "Accent",
        colors = listOf(
            ColorOption("Gold", "#F59E0B"),
            ColorOption("Amber", "#D97706"),
            ColorOption("Coral", "#FB7185"),
            ColorOption("Rose", "#F43F5E"),
            ColorOption("Purple", "#A855F7"),
            ColorOption("Turquoise", "#06B6D4")
        )
    )

    val ALL_PALETTES = listOf(LIGHT, DARK, WARM, COOL, ACCENT)

    // 8 Ready-made Built-in Themes
    val BUILT_IN_THEMES = listOf(
        UserPreset(
            id = "minimal_white",
            name = "Minimal White",
            timeColor = "#FFFFFF",
            amPmColor = "#E2E8F0",
            weekdayColor = "#FFFFFF",
            dateColor = "#FFFFFF",
            monthYearColor = "#E2E8F0",
            tempColor = "#FFFFFF",
            conditionColor = "#E2E8F0",
            cityColor = "#E2E8F0",
            timeSize = 36,
            amPmSize = 16,
            dateSize = 15,
            tempSize = 15,
            conditionSize = 14,
            citySize = 13,
            fontWeight = "BOLD",
            isItalic = false,
            bgOpacity = 0.0f,
            isBuiltIn = true
        ),
        UserPreset(
            id = "midnight",
            name = "Midnight",
            timeColor = "#E0F2FE",
            amPmColor = "#38BDF8",
            weekdayColor = "#BAE6FD",
            dateColor = "#38BDF8",
            monthYearColor = "#94A3B8",
            tempColor = "#38BDF8",
            conditionColor = "#BAE6FD",
            cityColor = "#38BDF8",
            timeSize = 38,
            amPmSize = 16,
            dateSize = 15,
            tempSize = 16,
            conditionSize = 14,
            citySize = 13,
            fontWeight = "BOLD",
            isItalic = false,
            bgOpacity = 0.0f,
            isBuiltIn = true
        ),
        UserPreset(
            id = "soft_grey",
            name = "Soft Grey",
            timeColor = "#E2E8F0",
            amPmColor = "#CBD5E1",
            weekdayColor = "#CBD5E1",
            dateColor = "#E2E8F0",
            monthYearColor = "#94A3B8",
            tempColor = "#E2E8F0",
            conditionColor = "#CBD5E1",
            cityColor = "#94A3B8",
            timeSize = 36,
            amPmSize = 15,
            dateSize = 14,
            tempSize = 14,
            conditionSize = 13,
            citySize = 12,
            fontWeight = "MEDIUM",
            isItalic = false,
            bgOpacity = 0.0f,
            isBuiltIn = true
        ),
        UserPreset(
            id = "frosted_light",
            name = "Frosted Light",
            timeColor = "#F8FAFC",
            amPmColor = "#BAE6FD",
            weekdayColor = "#F8FAFC",
            dateColor = "#BAE6FD",
            monthYearColor = "#CBD5E1",
            tempColor = "#BAE6FD",
            conditionColor = "#E2E8F0",
            cityColor = "#BAE6FD",
            timeSize = 36,
            amPmSize = 16,
            dateSize = 15,
            tempSize = 15,
            conditionSize = 14,
            citySize = 13,
            fontWeight = "SEMIBOLD",
            isItalic = false,
            bgOpacity = 0.0f,
            isBuiltIn = true
        ),
        UserPreset(
            id = "warm_sand",
            name = "Warm Sand",
            timeColor = "#FFFBEB",
            amPmColor = "#E6C280",
            weekdayColor = "#F7E7CE",
            dateColor = "#E6C280",
            monthYearColor = "#F5F5DC",
            tempColor = "#E6C280",
            conditionColor = "#F7E7CE",
            cityColor = "#E6C280",
            timeSize = 36,
            amPmSize = 16,
            dateSize = 15,
            tempSize = 15,
            conditionSize = 14,
            citySize = 13,
            fontWeight = "BOLD",
            isItalic = false,
            bgOpacity = 0.0f,
            isBuiltIn = true
        ),
        UserPreset(
            id = "ocean_blue",
            name = "Ocean Blue",
            timeColor = "#38BDF8",
            amPmColor = "#A7F3D0",
            weekdayColor = "#2DD4BF",
            dateColor = "#38BDF8",
            monthYearColor = "#A7F3D0",
            tempColor = "#38BDF8",
            conditionColor = "#2DD4BF",
            cityColor = "#A7F3D0",
            timeSize = 38,
            amPmSize = 16,
            dateSize = 15,
            tempSize = 15,
            conditionSize = 14,
            citySize = 13,
            fontWeight = "BOLD",
            isItalic = false,
            bgOpacity = 0.0f,
            isBuiltIn = true
        ),
        UserPreset(
            id = "amoled_minimal",
            name = "AMOLED Minimal",
            timeColor = "#FFFFFF",
            amPmColor = "#CBD5E1",
            weekdayColor = "#FFFFFF",
            dateColor = "#FFFFFF",
            monthYearColor = "#94A3B8",
            tempColor = "#FFFFFF",
            conditionColor = "#CBD5E1",
            cityColor = "#94A3B8",
            timeSize = 42,
            amPmSize = 18,
            dateSize = 15,
            tempSize = 15,
            conditionSize = 14,
            citySize = 13,
            fontWeight = "BOLD",
            isItalic = false,
            bgOpacity = 0.0f,
            isBuiltIn = true
        ),
        UserPreset(
            id = "auto_contrast",
            name = "Auto Contrast",
            timeColor = "#FFFFFF",
            amPmColor = "#E0E0E0",
            weekdayColor = "#FFFFFF",
            dateColor = "#E0E0E0",
            monthYearColor = "#E0E0E0",
            tempColor = "#FFFFFF",
            conditionColor = "#E0E0E0",
            cityColor = "#E0E0E0",
            timeSize = 36,
            amPmSize = 16,
            dateSize = 15,
            tempSize = 15,
            conditionSize = 14,
            citySize = 13,
            fontWeight = "BOLD",
            isItalic = false,
            bgOpacity = 0.0f,
            isBuiltIn = true
        )
    )

    /**
     * Validates a HEX color string (3, 6, or 8 hex digits, optional leading #).
     */
    fun isValidHex(hex: String): Boolean {
        if (hex.isBlank()) return false
        val clean = hex.removePrefix("#").trim()
        if (clean.length != 6 && clean.length != 8 && clean.length != 3) return false
        return clean.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
    }

    /**
     * Normalizes a hex string to uppercase `#RRGGBB` or `#AARRGGBB`.
     */
    fun sanitizeHex(hex: String, fallback: String = "#FFFFFF"): String {
        val clean = hex.removePrefix("#").trim()
        if (!isValidHex(clean)) return fallback
        return when (clean.length) {
            3 -> "#${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}".uppercase()
            else -> "#${clean.uppercase()}"
        }
    }

    fun parseColorSafe(hex: String, fallback: Int = Color.WHITE): Int {
        return try {
            Color.parseColor(sanitizeHex(hex))
        } catch (_: Exception) {
            fallback
        }
    }

    /**
     * Serializes a list of UserPresets to JSON for DataStore persistence.
     */
    fun serializePresets(presets: List<UserPreset>): String {
        val array = JSONArray()
        for (p in presets) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("timeColor", p.timeColor)
                put("amPmColor", p.amPmColor)
                put("weekdayColor", p.weekdayColor)
                put("dateColor", p.dateColor)
                put("monthYearColor", p.monthYearColor)
                put("tempColor", p.tempColor)
                put("conditionColor", p.conditionColor)
                put("cityColor", p.cityColor)
                put("timeSize", p.timeSize)
                put("amPmSize", p.amPmSize)
                put("dateSize", p.dateSize)
                put("tempSize", p.tempSize)
                put("conditionSize", p.conditionSize)
                put("citySize", p.citySize)
                put("fontWeight", p.fontWeight)
                put("isItalic", p.isItalic)
                put("bgOpacity", p.bgOpacity.toDouble())
                put("isBuiltIn", p.isBuiltIn)
            }
            array.put(obj)
        }
        return array.toString()
    }

    /**
     * Deserializes a JSON string into a list of UserPresets.
     */
    fun deserializePresets(json: String): List<UserPreset> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<UserPreset>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    UserPreset(
                        id = obj.optString("id", UUID()),
                        name = obj.optString("name", "Custom Preset"),
                        timeColor = obj.optString("timeColor", "#FFFFFF"),
                        amPmColor = obj.optString("amPmColor", "#E2E8F0"),
                        weekdayColor = obj.optString("weekdayColor", "#FFFFFF"),
                        dateColor = obj.optString("dateColor", "#FFFFFF"),
                        monthYearColor = obj.optString("monthYearColor", "#E2E8F0"),
                        tempColor = obj.optString("tempColor", "#FFFFFF"),
                        conditionColor = obj.optString("conditionColor", "#E2E8F0"),
                        cityColor = obj.optString("cityColor", "#E2E8F0"),
                        timeSize = obj.optInt("timeSize", 36),
                        amPmSize = obj.optInt("amPmSize", 16),
                        dateSize = obj.optInt("dateSize", 15),
                        tempSize = obj.optInt("tempSize", 15),
                        conditionSize = obj.optInt("conditionSize", 14),
                        citySize = obj.optInt("citySize", 13),
                        fontWeight = obj.optString("fontWeight", "BOLD"),
                        isItalic = obj.optBoolean("isItalic", false),
                        bgOpacity = obj.optDouble("bgOpacity", 0.0).toFloat(),
                        isBuiltIn = obj.optBoolean("isBuiltIn", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun UUID(): String = java.util.UUID.randomUUID().toString()
}
