package com.srikar.chronowidget.ui.preview

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srikar.chronowidget.data.models.ColorPalettes
import com.srikar.chronowidget.data.models.WidgetPreferences
import com.srikar.chronowidget.datetime.DateTimeUtils
import com.srikar.chronowidget.weather.WeatherData
import com.srikar.chronowidget.widget.WidgetUpdater
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WidgetLivePreview(
    prefs: WidgetPreferences,
    weather: WeatherData,
    modifier: Modifier = Modifier,
    enableInteractions: Boolean = true,
    showWallpaperPicker: Boolean = true
) {
    val context = LocalContext.current

    // Wallpaper background test simulator (allows testing transparency on dark, light, colourful, and AMOLED wallpapers)
    var selectedWallpaper by remember { mutableStateOf("DARK") }

    val isLightWallpaper = selectedWallpaper == "LIGHT"

    // Live clock ticking state for preview
    var currentTimeDigits by remember { mutableStateOf("") }
    var currentAmPm by remember { mutableStateOf("") }

    LaunchedEffect(prefs.is24Hour, prefs.showAmPm, prefs.showSeconds, prefs.timeFormatOverride) {
        while (true) {
            val now = LocalTime.now()
            if (prefs.timeFormatOverride.isNotBlank()) {
                val formatted = try {
                    now.format(DateTimeFormatter.ofPattern(prefs.timeFormatOverride, Locale.getDefault()))
                } catch (e: Exception) {
                    now.format(DateTimeFormatter.ofPattern(if (prefs.is24Hour) "HH:mm" else "hh:mm a", Locale.getDefault()))
                }
                currentTimeDigits = DateTimeUtils.uppercaseAmPm(formatted)
                currentAmPm = ""
            } else if (prefs.is24Hour) {
                val pattern = if (prefs.showSeconds) "HH:mm:ss" else "HH:mm"
                currentTimeDigits = now.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
                currentAmPm = ""
            } else {
                val pattern = if (prefs.showSeconds) "hh:mm:ss" else "hh:mm"
                currentTimeDigits = now.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
                currentAmPm = if (prefs.showAmPm) now.format(DateTimeFormatter.ofPattern("a", Locale.US)).uppercase() else ""
            }
            delay(if (prefs.showSeconds) 500L else 2000L)
        }
    }

    // Auto-contrast or explicit element colors
    fun resolveColor(hex: String, fallback: Color, darkFallback: Color = Color(0xFF1E293B)): Color {
        return if (prefs.autoContrastMode && isLightWallpaper) {
            darkFallback
        } else {
            parseColor(hex, fallback).copy(alpha = prefs.textOpacity.coerceIn(0.1f, 1.0f))
        }
    }

    val timeColor = resolveColor(prefs.timeDigitsColorHex, Color.White, Color(0xFF0F172A))
    val amPmColor = resolveColor(prefs.amPmTextColorHex, Color(0xFFE2E8F0), Color(0xFF334155))
    val weekdayColor = resolveColor(prefs.weekdayTextColorHex, Color.White, Color(0xFF0F172A))
    val dateDayColor = resolveColor(prefs.dateTextColorHex, Color.White, Color(0xFF0F172A))
    val monthYearColor = resolveColor(prefs.monthYearTextColorHex, Color(0xFFE2E8F0), Color(0xFF475569))
    val tempColor = resolveColor(prefs.tempTextColorHex, Color.White, Color(0xFF0F172A))
    val conditionColor = resolveColor(prefs.conditionTextColorHex, Color(0xFFE2E8F0), Color(0xFF334155))
    val cityColor = resolveColor(prefs.cityTextColorHex, Color(0xFFE2E8F0), Color(0xFF334155))

    val isTransparent = prefs.backgroundOpacity <= 0.01f
    val baseBgColor = parseColor(prefs.backgroundColorHex, Color.Black)
    val effectiveBgColor = if (isTransparent) Color.Transparent else baseBgColor.copy(alpha = prefs.backgroundOpacity)

    val horizontalAlign = when (prefs.horizontalAlignment.uppercase()) {
        "LEFT" -> Alignment.Start
        "RIGHT" -> Alignment.End
        else -> Alignment.CenterHorizontally
    }

    val textAlign = when (prefs.horizontalAlignment.uppercase()) {
        "LEFT" -> TextAlign.Start
        "RIGHT" -> TextAlign.End
        else -> TextAlign.Center
    }

    val verticalArrangement = when (prefs.verticalAlignment.uppercase()) {
        "TOP" -> Arrangement.Top
        "BOTTOM" -> Arrangement.Bottom
        else -> Arrangement.Center
    }

    val fontWeight = when (prefs.fontWeightName.uppercase()) {
        "LIGHT" -> FontWeight.Light
        "REGULAR" -> FontWeight.Normal
        "MEDIUM" -> FontWeight.Medium
        "SEMIBOLD" -> FontWeight.SemiBold
        "BOLD" -> FontWeight.Bold
        else -> FontWeight.Bold
    }

    val fontStyle = if (prefs.isItalic) FontStyle.Italic else FontStyle.Normal
    val letterSpacing = prefs.letterSpacingSp.sp

    val textShadow = if (prefs.enableShadow) {
        Shadow(
            color = parseColor(prefs.shadowColorHex, Color.Black).copy(alpha = prefs.shadowOpacity.coerceIn(0f, 1f)),
            offset = Offset(2f, 2f),
            blurRadius = (prefs.shadowRadiusDp * 2).coerceAtLeast(1f)
        )
    } else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0A0D14))
            .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live Widget Preview (4×2)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            if (isTransparent) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "100% Transparent",
                        color = Color(0xFF34D399),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF38BDF8).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${(prefs.backgroundOpacity * 100).toInt()}% Opacity",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Wallpaper Simulator Box
        val wallpaperModifier = when (selectedWallpaper) {
            "COLOURFUL" -> Modifier.background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF6366F1),
                        Color(0xFFA855F7),
                        Color(0xFFEC4899),
                        Color(0xFFF97316)
                    )
                )
            )
            "LIGHT" -> Modifier.background(Color(0xFFF1F5F9))
            "AMOLED" -> Modifier.background(Color(0xFF000000))
            "NAVY" -> Modifier.background(Color(0xFF0B192C))
            else -> Modifier.background(Color(0xFF161B22))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp)
                .clip(RoundedCornerShape(16.dp))
                .then(wallpaperModifier)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(vertical = 12.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Widget Frame (no container/border when transparent)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .then(
                        if (!isTransparent) {
                            Modifier
                                .clip(RoundedCornerShape(prefs.cornerRadiusDp.dp))
                                .background(effectiveBgColor)
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(prefs.cornerRadiusDp.dp))
                        } else {
                            Modifier.background(Color.Transparent)
                        }
                    )
                    .padding(prefs.paddingDp.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = horizontalAlign,
                    verticalArrangement = verticalArrangement
                ) {
                    // 1. TIME SECTION (Time Digits + AM/PM)
                    if (prefs.showTime) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = enableInteractions) {
                                    Toast.makeText(context, "Time tapped: opens Clock (${prefs.timeTapAction})", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = when (prefs.horizontalAlignment.uppercase()) {
                                "LEFT" -> Arrangement.Start
                                "RIGHT" -> Arrangement.End
                                else -> Arrangement.Center
                            }
                        ) {
                            Text(
                                text = if (currentTimeDigits.isNotBlank()) currentTimeDigits else "04:26",
                                style = TextStyle(
                                    color = timeColor,
                                    fontSize = prefs.timeFontSizeSp.coerceIn(24, 64).sp,
                                    fontWeight = fontWeight,
                                    fontStyle = fontStyle,
                                    letterSpacing = letterSpacing,
                                    textAlign = textAlign,
                                    shadow = textShadow
                                ),
                                lineHeight = (prefs.timeFontSizeSp * 1.05).sp
                            )

                            if (currentAmPm.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentAmPm,
                                    style = TextStyle(
                                        color = amPmColor,
                                        fontSize = prefs.amPmFontSizeSp.coerceIn(10, 30).sp,
                                        fontWeight = fontWeight,
                                        fontStyle = fontStyle,
                                        letterSpacing = letterSpacing,
                                        shadow = textShadow
                                    ),
                                    modifier = Modifier.padding(bottom = (prefs.timeFontSizeSp * 0.1).coerceIn(2.0, 8.0).dp)
                                )
                            }
                        }
                    }

                    // Spacing between Time and Date
                    if (prefs.showTime && (prefs.showWeekday || prefs.showDate || prefs.showMonth || prefs.showYear)) {
                        Spacer(modifier = Modifier.height(prefs.timeToDateSpacingDp.dp))
                    }

                    // 2. DATE SECTION (Granular Weekday, Day, Month & Year)
                    val today = LocalDate.now()
                    val hasDateElements = prefs.showWeekday || prefs.showDate || prefs.showMonth || prefs.showYear

                    if (hasDateElements) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = enableInteractions) {
                                    Toast.makeText(context, "Date tapped: opens Calendar (${prefs.dateTapAction})", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = when (prefs.horizontalAlignment.uppercase()) {
                                "LEFT" -> Arrangement.Start
                                "RIGHT" -> Arrangement.End
                                else -> Arrangement.Center
                            }
                        ) {
                            if (prefs.showWeekday) {
                                Text(
                                    text = DateTimeUtils.getWeekday(today),
                                    style = TextStyle(
                                        color = weekdayColor,
                                        fontSize = prefs.dateFontSizeSp.coerceIn(10, 28).sp,
                                        fontWeight = fontWeight,
                                        fontStyle = fontStyle,
                                        letterSpacing = letterSpacing,
                                        textAlign = textAlign,
                                        shadow = textShadow
                                    )
                                )
                            }

                            if (prefs.showDate) {
                                Text(
                                    text = DateTimeUtils.getDay(today),
                                    style = TextStyle(
                                        color = dateDayColor,
                                        fontSize = prefs.dateFontSizeSp.coerceIn(10, 28).sp,
                                        fontWeight = fontWeight,
                                        fontStyle = fontStyle,
                                        letterSpacing = letterSpacing,
                                        textAlign = textAlign,
                                        shadow = textShadow
                                    )
                                )
                            }

                            if (prefs.showMonth || prefs.showYear) {
                                Text(
                                    text = DateTimeUtils.getMonthAndYear(prefs.showMonth, prefs.showYear, today),
                                    style = TextStyle(
                                        color = monthYearColor,
                                        fontSize = prefs.dateFontSizeSp.coerceIn(10, 28).sp,
                                        fontWeight = fontWeight,
                                        fontStyle = fontStyle,
                                        letterSpacing = letterSpacing,
                                        textAlign = textAlign,
                                        shadow = textShadow
                                    )
                                )
                            }
                        }
                    }

                    // Spacing between Date and Weather
                    if (hasDateElements && prefs.showWeather) {
                        Spacer(modifier = Modifier.height(prefs.dateToWeatherSpacingDp.dp))
                    }

                    // 3. WEATHER SECTION (Icon, Temp, Condition, City)
                    if (prefs.showWeather) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = enableInteractions) {
                                    Toast.makeText(context, "Weather tapped: opens Weather (${prefs.weatherTapAction})", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = when (prefs.horizontalAlignment.uppercase()) {
                                "LEFT" -> Arrangement.Start
                                "RIGHT" -> Arrangement.End
                                else -> Arrangement.Center
                            }
                        ) {
                            if (prefs.showIcon) {
                                val iconRes = WidgetUpdater.getWeatherIconRes(weather.weatherCode)
                                val iconTint = if (prefs.weatherIconColorHex.isNotBlank()) {
                                    resolveColor(prefs.weatherIconColorHex, Color.Unspecified)
                                } else {
                                    Color.Unspecified
                                }

                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = "Weather Icon",
                                    tint = iconTint,
                                    modifier = Modifier.size(prefs.weatherIconSizeDp.coerceIn(16, 36).dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            if (prefs.showTemperature) {
                                Text(
                                    text = weather.getFormattedTemperature(prefs.isCelsius),
                                    style = TextStyle(
                                        color = tempColor,
                                        fontSize = prefs.tempFontSizeSp.coerceIn(10, 28).sp,
                                        fontWeight = fontWeight,
                                        fontStyle = fontStyle,
                                        letterSpacing = letterSpacing,
                                        shadow = textShadow
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            if (prefs.showCondition && weather.condition.isNotBlank()) {
                                Text(
                                    text = weather.condition,
                                    style = TextStyle(
                                        color = conditionColor,
                                        fontSize = prefs.conditionFontSizeSp.coerceIn(9, 24).sp,
                                        fontWeight = fontWeight,
                                        fontStyle = fontStyle,
                                        letterSpacing = letterSpacing,
                                        shadow = textShadow
                                    )
                                )
                            }

                            if (prefs.showLocation && weather.locationName.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${weather.locationName}",
                                    style = TextStyle(
                                        color = cityColor,
                                        fontSize = prefs.cityFontSizeSp.coerceIn(9, 22).sp,
                                        fontWeight = fontWeight,
                                        fontStyle = fontStyle,
                                        letterSpacing = letterSpacing,
                                        shadow = textShadow
                                    )
                                )
                            }

                            if (prefs.showHumidity && weather.relativeHumidity > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${weather.getFormattedHumidity()})",
                                    style = TextStyle(
                                        color = conditionColor.copy(alpha = 0.7f),
                                        fontSize = (prefs.conditionFontSizeSp - 2).coerceAtLeast(9).sp,
                                        fontWeight = fontWeight,
                                        fontStyle = fontStyle,
                                        shadow = textShadow
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Test Transparency Wallpaper Picker
        if (showWallpaperPicker) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Preview On:",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                listOf(
                    "DARK" to "Dark",
                    "LIGHT" to "Light",
                    "COLOURFUL" to "Colourful",
                    "AMOLED" to "AMOLED",
                    "NAVY" to "Navy"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedWallpaper == key,
                        onClick = { selectedWallpaper = key },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

private fun parseColor(hex: String, fallback: Color): Color {
    return try {
        val clean = ColorPalettes.sanitizeHex(hex)
        val colorInt = android.graphics.Color.parseColor(clean)
        Color(colorInt)
    } catch (e: Exception) {
        fallback
    }
}
