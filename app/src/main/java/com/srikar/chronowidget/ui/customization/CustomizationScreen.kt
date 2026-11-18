package com.srikar.chronowidget.ui.customization

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srikar.chronowidget.data.models.ColorOption
import com.srikar.chronowidget.data.models.ColorPalettes
import com.srikar.chronowidget.data.models.UserPreset
import com.srikar.chronowidget.data.models.WidgetPreferences
import com.srikar.chronowidget.data.preferences.PreferencesManager
import com.srikar.chronowidget.location.LocationHelper
import com.srikar.chronowidget.ui.preview.WidgetLivePreview
import com.srikar.chronowidget.weather.WeatherData
import com.srikar.chronowidget.weather.WeatherWorker
import com.srikar.chronowidget.widget.WidgetUpdater
import kotlinx.coroutines.launch
import java.util.UUID

enum class ElementTarget(val displayName: String) {
    TIME_DIGITS("Time Digits"),
    AM_PM("AM/PM"),
    WEEKDAY("Weekday"),
    DATE_DAY("Date Number"),
    MONTH_YEAR("Month & Year"),
    WEATHER_ICON("Weather Icon"),
    TEMPERATURE("Temperature"),
    CONDITION("Condition"),
    CITY("Current City")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    initialPrefs: WidgetPreferences,
    weather: WeatherData,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }

    var draftPrefs by remember { mutableStateOf(initialPrefs) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Colours", "Presets", "Typography", "Sizes", "Layout", "Time & Date", "Weather")

    // State for user presets
    var userPresets by remember {
        mutableStateOf(ColorPalettes.deserializePresets(draftPrefs.customPresetsJson))
    }

    // Dialog state for Save/Rename preset
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    var editingPresetId by remember { mutableStateOf<String?>(null) }

    fun addRecentColor(newHex: String) {
        val sanitized = ColorPalettes.sanitizeHex(newHex)
        val currentList = draftPrefs.recentColorsCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableList()
        currentList.remove(sanitized)
        currentList.add(0, sanitized)
        val updatedCsv = currentList.take(8).joinToString(",")
        draftPrefs = draftPrefs.copy(recentColorsCsv = updatedCsv)
    }

    fun applyPreset(preset: UserPreset) {
        draftPrefs = draftPrefs.copy(
            presetName = preset.name,
            timeDigitsColorHex = preset.timeColor,
            amPmTextColorHex = preset.amPmColor,
            weekdayTextColorHex = preset.weekdayColor,
            dateTextColorHex = preset.dateColor,
            monthYearTextColorHex = preset.monthYearColor,
            tempTextColorHex = preset.tempColor,
            conditionTextColorHex = preset.conditionColor,
            cityTextColorHex = preset.cityColor,
            timeFontSizeSp = preset.timeSize,
            amPmFontSizeSp = preset.amPmSize,
            dateFontSizeSp = preset.dateSize,
            tempFontSizeSp = preset.tempSize,
            conditionFontSizeSp = preset.conditionSize,
            cityFontSizeSp = preset.citySize,
            fontWeightName = preset.fontWeight,
            isItalic = preset.isItalic,
            backgroundOpacity = preset.bgOpacity
        )
        Toast.makeText(context, "Applied '${preset.name}' theme", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Widget Appearance Editor",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            draftPrefs = WidgetPreferences()
                            Toast.makeText(context, "Reset all settings to defaults", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Defaults",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0E1117)
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color(0xFF11141C),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // Sync user presets into preferences
                                val updatedPrefs = draftPrefs.copy(
                                    customPresetsJson = ColorPalettes.serializePresets(userPresets)
                                )
                                preferencesManager.updatePreferences(updatedPrefs)
                                WeatherWorker.schedulePeriodicWork(context, updatedPrefs.weatherRefreshIntervalMinutes)
                                WidgetUpdater.updateAllWidgets(context)
                                Toast.makeText(context, "Settings saved and widget updated!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save & Apply to Widget",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF0E1117)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // STICKY TOP PREVIEW (Updates dynamically in real-time)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E1117))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                WidgetLivePreview(
                    prefs = draftPrefs,
                    weather = weather,
                    enableInteractions = false,
                    showWallpaperPicker = true
                )
            }

            // SECTION TABS
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF11141C),
                contentColor = Color(0xFF38BDF8),
                edgePadding = 14.dp,
                divider = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // TAB CONTENT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                when (selectedTab) {
                    0 -> ColoursTabContent(
                        prefs = draftPrefs,
                        onUpdate = { draftPrefs = it },
                        onColorSelected = { hex -> addRecentColor(hex) }
                    )
                    1 -> PresetsTabContent(
                        prefs = draftPrefs,
                        userPresets = userPresets,
                        onApplyPreset = { applyPreset(it) },
                        onSaveNewPreset = {
                            presetNameInput = "My Custom Theme"
                            editingPresetId = null
                            showSavePresetDialog = true
                        },
                        onRenamePreset = { preset ->
                            presetNameInput = preset.name
                            editingPresetId = preset.id
                            showSavePresetDialog = true
                        },
                        onDeletePreset = { presetId ->
                            userPresets = userPresets.filter { it.id != presetId }
                            draftPrefs = draftPrefs.copy(
                                customPresetsJson = ColorPalettes.serializePresets(userPresets)
                            )
                            Toast.makeText(context, "Preset deleted", Toast.LENGTH_SHORT).show()
                        }
                    )
                    2 -> TypographyTabContent(
                        prefs = draftPrefs,
                        onUpdate = { draftPrefs = it },
                        onColorSelected = { hex -> addRecentColor(hex) }
                    )
                    3 -> SizesTabContent(
                        prefs = draftPrefs,
                        onUpdate = { draftPrefs = it }
                    )
                    4 -> LayoutTabContent(
                        prefs = draftPrefs,
                        onUpdate = { draftPrefs = it }
                    )
                    5 -> TimeAndDateTabContent(
                        prefs = draftPrefs,
                        onUpdate = { draftPrefs = it }
                    )
                    6 -> WeatherAndActionsTabContent(
                        prefs = draftPrefs,
                        onUpdate = { draftPrefs = it }
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Save/Rename Preset Dialog
    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = {
                Text(
                    text = if (editingPresetId == null) "Save Custom Theme Preset" else "Rename Preset",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a name for this colour and typography combination:",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        label = { Text("Preset Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = presetNameInput.trim().ifEmpty { "My Preset" }
                        if (editingPresetId == null) {
                            val newPreset = UserPreset(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                timeColor = draftPrefs.timeDigitsColorHex,
                                amPmColor = draftPrefs.amPmTextColorHex,
                                weekdayColor = draftPrefs.weekdayTextColorHex,
                                dateColor = draftPrefs.dateTextColorHex,
                                monthYearColor = draftPrefs.monthYearTextColorHex,
                                tempColor = draftPrefs.tempTextColorHex,
                                conditionColor = draftPrefs.conditionTextColorHex,
                                cityColor = draftPrefs.cityTextColorHex,
                                timeSize = draftPrefs.timeFontSizeSp,
                                amPmSize = draftPrefs.amPmFontSizeSp,
                                dateSize = draftPrefs.dateFontSizeSp,
                                tempSize = draftPrefs.tempFontSizeSp,
                                conditionSize = draftPrefs.conditionFontSizeSp,
                                citySize = draftPrefs.cityFontSizeSp,
                                fontWeight = draftPrefs.fontWeightName,
                                isItalic = draftPrefs.isItalic,
                                bgOpacity = draftPrefs.backgroundOpacity,
                                isBuiltIn = false
                            )
                            userPresets = userPresets + newPreset
                            draftPrefs = draftPrefs.copy(
                                presetName = name,
                                customPresetsJson = ColorPalettes.serializePresets(userPresets)
                            )
                            Toast.makeText(context, "Preset '$name' saved!", Toast.LENGTH_SHORT).show()
                        } else {
                            userPresets = userPresets.map {
                                if (it.id == editingPresetId) it.copy(name = name) else it
                            }
                            draftPrefs = draftPrefs.copy(
                                presetName = name,
                                customPresetsJson = ColorPalettes.serializePresets(userPresets)
                            )
                            Toast.makeText(context, "Preset renamed to '$name'", Toast.LENGTH_SHORT).show()
                        }
                        showSavePresetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF161B22)
        )
    }
}

// =======================================================================
// 1. COLOURS TAB CONTENT (Independent element colours + 5 palettes + Hex)
// =======================================================================
@Composable
private fun ColoursTabContent(
    prefs: WidgetPreferences,
    onUpdate: (WidgetPreferences) -> Unit,
    onColorSelected: (String) -> Unit
) {
    var selectedElement by remember { mutableStateOf(ElementTarget.TIME_DIGITS) }

    // Resolve current element color
    val currentHex = when (selectedElement) {
        ElementTarget.TIME_DIGITS -> prefs.timeDigitsColorHex
        ElementTarget.AM_PM -> prefs.amPmTextColorHex
        ElementTarget.WEEKDAY -> prefs.weekdayTextColorHex
        ElementTarget.DATE_DAY -> prefs.dateTextColorHex
        ElementTarget.MONTH_YEAR -> prefs.monthYearTextColorHex
        ElementTarget.WEATHER_ICON -> prefs.weatherIconColorHex.ifEmpty { "#FFFFFF" }
        ElementTarget.TEMPERATURE -> prefs.tempTextColorHex
        ElementTarget.CONDITION -> prefs.conditionTextColorHex
        ElementTarget.CITY -> prefs.cityTextColorHex
    }

    fun applyColorToCurrentElement(newHex: String) {
        val sanitized = ColorPalettes.sanitizeHex(newHex)
        onColorSelected(sanitized)
        val updated = when (selectedElement) {
            ElementTarget.TIME_DIGITS -> prefs.copy(timeDigitsColorHex = sanitized)
            ElementTarget.AM_PM -> prefs.copy(amPmTextColorHex = sanitized)
            ElementTarget.WEEKDAY -> prefs.copy(weekdayTextColorHex = sanitized)
            ElementTarget.DATE_DAY -> prefs.copy(dateTextColorHex = sanitized)
            ElementTarget.MONTH_YEAR -> prefs.copy(monthYearTextColorHex = sanitized)
            ElementTarget.WEATHER_ICON -> prefs.copy(weatherIconColorHex = sanitized)
            ElementTarget.TEMPERATURE -> prefs.copy(tempTextColorHex = sanitized)
            ElementTarget.CONDITION -> prefs.copy(conditionTextColorHex = sanitized)
            ElementTarget.CITY -> prefs.copy(cityTextColorHex = sanitized)
        }
        onUpdate(updated)
    }

    fun applyColorToAllElements(newHex: String) {
        val sanitized = ColorPalettes.sanitizeHex(newHex)
        onColorSelected(sanitized)
        onUpdate(
            prefs.copy(
                timeDigitsColorHex = sanitized,
                amPmTextColorHex = sanitized,
                weekdayTextColorHex = sanitized,
                dateTextColorHex = sanitized,
                monthYearTextColorHex = sanitized,
                weatherIconColorHex = sanitized,
                tempTextColorHex = sanitized,
                conditionTextColorHex = sanitized,
                cityTextColorHex = sanitized
            )
        )
    }

    SectionHeader("1. Select Element to Colour")
    Text(
        text = "Each element can have its own independent colour.",
        color = Color(0xFF94A3B8),
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    // Element Selector chips
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(ElementTarget.values()) { target ->
            val targetHex = when (target) {
                ElementTarget.TIME_DIGITS -> prefs.timeDigitsColorHex
                ElementTarget.AM_PM -> prefs.amPmTextColorHex
                ElementTarget.WEEKDAY -> prefs.weekdayTextColorHex
                ElementTarget.DATE_DAY -> prefs.dateTextColorHex
                ElementTarget.MONTH_YEAR -> prefs.monthYearTextColorHex
                ElementTarget.WEATHER_ICON -> prefs.weatherIconColorHex.ifEmpty { "#FFFFFF" }
                ElementTarget.TEMPERATURE -> prefs.tempTextColorHex
                ElementTarget.CONDITION -> prefs.conditionTextColorHex
                ElementTarget.CITY -> prefs.cityTextColorHex
            }
            val isSelected = selectedElement == target
            FilterChip(
                selected = isSelected,
                onClick = { selectedElement = target },
                label = { Text(target.displayName, fontSize = 12.sp) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(ColorPalettes.parseColorSafe(targetHex)))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0284C7),
                    selectedLabelColor = Color.White
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Interactive Colour Preview Box
    SectionHeader("2. Colour Preview (${selectedElement.displayName})")
    ColorPreviewBox(
        hex = currentHex,
        label = selectedElement.displayName
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Recently Used Colours
    val recentList = prefs.recentColorsCsv
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() && ColorPalettes.isValidHex(it) }

    if (recentList.isNotEmpty()) {
        SectionHeader("3. Recently Used Colours")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            recentList.take(8).forEach { hex ->
                val isCurrent = currentHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(ColorPalettes.parseColorSafe(hex)))
                        .border(
                            width = if (isCurrent) 2.5.dp else 1.dp,
                            color = if (isCurrent) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .clickable { applyColorToCurrentElement(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrent) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = if (isColorDark(hex)) Color.White else Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }

    // 5 Selectable Colour Palettes
    SectionHeader("4. Selectable Colour Palettes")
    Text(
        text = "Tap any swatch to apply to ${selectedElement.displayName}.",
        color = Color(0xFF94A3B8),
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )

    ColorPalettes.ALL_PALETTES.forEach { palette ->
        PaletteGroup(
            paletteName = palette.category,
            colors = palette.colors,
            selectedHex = currentHex,
            onSelect = { applyColorToCurrentElement(it) }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Custom HEX Input with validation
    SectionHeader("5. Full Custom HEX Input")
    var hexInput by remember(currentHex) { mutableStateOf(currentHex) }
    var hexError by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = hexInput,
            onValueChange = { input ->
                hexInput = input
                if (ColorPalettes.isValidHex(input)) {
                    hexError = null
                    applyColorToCurrentElement(input)
                } else {
                    hexError = "Invalid HEX code (use 6 or 8 chars, e.g. #FFFFFF)"
                }
            },
            label = { Text("HEX code (e.g. #38BDF8)") },
            isError = hexError != null,
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                errorTextColor = Color(0xFFF87171)
            )
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Preset swatch preview for input
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(ColorPalettes.parseColorSafe(hexInput)))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
        )
    }

    if (hexError != null) {
        Text(
            text = hexError!!,
            color = Color(0xFFF87171),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Quick Actions: Apply to All & Reset to Default
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { applyColorToAllElements(currentHex) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Apply Color to All", fontSize = 12.sp, color = Color(0xFF38BDF8))
        }

        OutlinedButton(
            onClick = {
                onUpdate(
                    prefs.copy(
                        timeDigitsColorHex = "#FFFFFF",
                        amPmTextColorHex = "#E2E8F0",
                        weekdayTextColorHex = "#FFFFFF",
                        dateTextColorHex = "#FFFFFF",
                        monthYearTextColorHex = "#E2E8F0",
                        weatherIconColorHex = "",
                        tempTextColorHex = "#FFFFFF",
                        conditionTextColorHex = "#E2E8F0",
                        cityTextColorHex = "#E2E8F0"
                    )
                )
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Reset Colours", fontSize = 12.sp, color = Color(0xFFF87171))
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Auto-Contrast Mode
    SectionHeader("6. Automatic Contrast Mode")
    SwitchSetting(
        label = "Auto-Contrast Text",
        subtitle = "Adapt text shade dynamically to wallpaper luminance where supported",
        checked = prefs.autoContrastMode,
        onCheckedChange = { onUpdate(prefs.copy(autoContrastMode = it)) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Background & Transparency
    SectionHeader("7. Background Transparency")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                onUpdate(
                    prefs.copy(
                        backgroundOpacity = 0.0f,
                        cornerRadiusDp = 0,
                        presetName = "Minimalist Transparent"
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (prefs.backgroundOpacity <= 0.01f) Color(0xFF0284C7) else Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Set 100% Transparent", fontSize = 13.sp)
        }
    }

    Spacer(modifier = Modifier.height(6.dp))
    SliderSetting(
        label = "Background Opacity (${(prefs.backgroundOpacity * 100).toInt()}%)",
        value = prefs.backgroundOpacity,
        range = 0.0f..1.0f,
        onValueChange = { onUpdate(prefs.copy(backgroundOpacity = it)) }
    )
}

// =======================================================================
// 2. PRESETS TAB CONTENT (8 Built-in Themes + User Saved Presets)
// =======================================================================
@Composable
private fun PresetsTabContent(
    prefs: WidgetPreferences,
    userPresets: List<UserPreset>,
    onApplyPreset: (UserPreset) -> Unit,
    onSaveNewPreset: () -> Unit,
    onRenamePreset: (UserPreset) -> Unit,
    onDeletePreset: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeader("8 Ready-Made Built-in Themes")
    }

    ColorPalettes.BUILT_IN_THEMES.forEach { theme ->
        PresetCard(
            preset = theme,
            isCurrent = prefs.presetName == theme.name,
            onApply = { onApplyPreset(theme) },
            onRename = null,
            onDelete = null
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    Spacer(modifier = Modifier.height(18.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeader("Custom Saved Themes (${userPresets.size})")

        Button(
            onClick = onSaveNewPreset,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save Current", fontSize = 12.sp)
        }
    }

    if (userPresets.isEmpty()) {
        Surface(
            color = Color(0xFF161B22),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "No custom themes saved yet. Customize colours & typography above and tap 'Save Current' to save your combination.",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    } else {
        userPresets.forEach { preset ->
            PresetCard(
                preset = preset,
                isCurrent = prefs.presetName == preset.name,
                onApply = { onApplyPreset(preset) },
                onRename = { onRenamePreset(preset) },
                onDelete = { onDeletePreset(preset.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PresetCard(
    preset: UserPreset,
    isCurrent: Boolean,
    onApply: () -> Unit,
    onRename: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Surface(
        color = if (isCurrent) Color(0xFF1E293B) else Color(0xFF161B22),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCurrent) 1.5.dp else 1.dp,
            color = if (isCurrent) Color(0xFF0284C7) else Color(0xFF1F2937)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onApply() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preset.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF0284C7),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Active",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Color swatches indicator
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        preset.timeColor,
                        preset.amPmColor,
                        preset.weekdayColor,
                        preset.tempColor,
                        preset.conditionColor
                    ).forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(ColorPalettes.parseColorSafe(hex)))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                    Text(
                        text = "• ${preset.timeSize}sp • ${preset.fontWeight}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onRename != null) {
                    IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// =======================================================================
// 3. TYPOGRAPHY TAB CONTENT
// =======================================================================
@Composable
private fun TypographyTabContent(
    prefs: WidgetPreferences,
    onUpdate: (WidgetPreferences) -> Unit,
    onColorSelected: (String) -> Unit
) {
    SectionHeader("1. Font Weight")
    val weights = listOf("LIGHT", "REGULAR", "MEDIUM", "SEMIBOLD", "BOLD")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weights.forEach { weight ->
            FilterChip(
                selected = prefs.fontWeightName.equals(weight, ignoreCase = true),
                onClick = { onUpdate(prefs.copy(fontWeightName = weight)) },
                label = { Text(weight.capitalize(), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0284C7),
                    selectedLabelColor = Color.White
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionHeader("2. Font Style")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = !prefs.isItalic,
            onClick = { onUpdate(prefs.copy(isItalic = false)) },
            label = { Text("Normal Style") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF0284C7),
                selectedLabelColor = Color.White
            )
        )
        FilterChip(
            selected = prefs.isItalic,
            onClick = { onUpdate(prefs.copy(isItalic = true)) },
            label = { Text("Italic Style") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF0284C7),
                selectedLabelColor = Color.White
            )
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionHeader("3. Letter Spacing (${String.format("%.1f", prefs.letterSpacingSp)}sp)")
    Slider(
        value = prefs.letterSpacingSp,
        onValueChange = { onUpdate(prefs.copy(letterSpacingSp = it)) },
        valueRange = 0.0f..4.0f,
        steps = 8,
        colors = SliderDefaults.colors(
            thumbColor = Color(0xFF38BDF8),
            activeTrackColor = Color(0xFF0284C7)
        )
    )

    Spacer(modifier = Modifier.height(14.dp))

    SectionHeader("4. Optional Text Shadow")
    SwitchSetting(
        label = "Enable Text Shadow",
        subtitle = "Improves legibility on busy or bright wallpapers",
        checked = prefs.enableShadow,
        onCheckedChange = { onUpdate(prefs.copy(enableShadow = it)) }
    )

    if (prefs.enableShadow) {
        Spacer(modifier = Modifier.height(8.dp))
        SliderSetting(
            label = "Shadow Opacity (${(prefs.shadowOpacity * 100).toInt()}%)",
            value = prefs.shadowOpacity,
            range = 0.0f..1.0f,
            onValueChange = { onUpdate(prefs.copy(shadowOpacity = it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))
        SliderSetting(
            label = "Shadow Blur (${prefs.shadowRadiusDp.toInt()}dp)",
            value = prefs.shadowRadiusDp,
            range = 0.0f..12.0f,
            onValueChange = { onUpdate(prefs.copy(shadowRadiusDp = it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("Shadow Colour", color = Color.White, fontSize = 13.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("#000000", "#1E1E1E", "#334155", "#424242").forEach { hex ->
                val isSelected = prefs.shadowColorHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(hex)))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF38BDF8) else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { onUpdate(prefs.copy(shadowColorHex = hex)) }
                )
            }
        }
    }
}

// =======================================================================
// 4. SIZES TAB CONTENT (Sliders + Manual Numeric Entry with range validation)
// =======================================================================
@Composable
private fun SizesTabContent(
    prefs: WidgetPreferences,
    onUpdate: (WidgetPreferences) -> Unit
) {
    SectionHeader("Granular Font Sizes & Icon Controls")
    Text(
        text = "Adjust using either the slider or by typing an exact numeric sp value.",
        color = Color(0xFF94A3B8),
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    // 1. Time: 24–64sp
    SizeSettingRow(
        label = "Time Digits Size",
        currentValue = prefs.timeFontSizeSp,
        range = 24..64,
        unit = "sp",
        onValueChange = { onUpdate(prefs.copy(timeFontSizeSp = it)) }
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 2. AM/PM: 10–30sp
    SizeSettingRow(
        label = "AM/PM Indicator Size",
        currentValue = prefs.amPmFontSizeSp,
        range = 10..30,
        unit = "sp",
        onValueChange = { onUpdate(prefs.copy(amPmFontSizeSp = it)) }
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Date: 10–28sp
    SizeSettingRow(
        label = "Date Row Size",
        currentValue = prefs.dateFontSizeSp,
        range = 10..28,
        unit = "sp",
        onValueChange = { onUpdate(prefs.copy(dateFontSizeSp = it)) }
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 4. Temperature: 10–28sp
    SizeSettingRow(
        label = "Temperature Size",
        currentValue = prefs.tempFontSizeSp,
        range = 10..28,
        unit = "sp",
        onValueChange = { onUpdate(prefs.copy(tempFontSizeSp = it)) }
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 5. Weather Condition: 9–24sp
    SizeSettingRow(
        label = "Weather Condition Size",
        currentValue = prefs.conditionFontSizeSp,
        range = 9..24,
        unit = "sp",
        onValueChange = { onUpdate(prefs.copy(conditionFontSizeSp = it)) }
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 6. City: 9–22sp
    SizeSettingRow(
        label = "Current City Size",
        currentValue = prefs.cityFontSizeSp,
        range = 9..22,
        unit = "sp",
        onValueChange = { onUpdate(prefs.copy(cityFontSizeSp = it)) }
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 7. Weather Icon: 16–36dp
    SizeSettingRow(
        label = "Weather Icon Size",
        currentValue = prefs.weatherIconSizeDp,
        range = 16..36,
        unit = "dp",
        onValueChange = { onUpdate(prefs.copy(weatherIconSizeDp = it)) }
    )
}

@Composable
private fun SizeSettingRow(
    label: String,
    currentValue: Int,
    range: IntRange,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    var textValue by remember(currentValue) { mutableStateOf(currentValue.toString()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Range: ${range.first} to ${range.last}$unit",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            // Numeric Text Input
            OutlinedTextField(
                value = textValue,
                onValueChange = { input ->
                    textValue = input
                    val parsed = input.toIntOrNull()
                    if (parsed != null && parsed in range) {
                        onValueChange(parsed)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(76.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        Slider(
            value = currentValue.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF38BDF8),
                activeTrackColor = Color(0xFF0284C7)
            )
        )
    }
}

// =======================================================================
// 5. LAYOUT TAB CONTENT
// =======================================================================
@Composable
private fun LayoutTabContent(
    prefs: WidgetPreferences,
    onUpdate: (WidgetPreferences) -> Unit
) {
    SectionHeader("1. Horizontal Alignment")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("LEFT" to "Left", "CENTER" to "Centre", "RIGHT" to "Right").forEach { (key, label) ->
            FilterChip(
                selected = prefs.horizontalAlignment.equals(key, ignoreCase = true),
                onClick = { onUpdate(prefs.copy(horizontalAlignment = key)) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0284C7),
                    selectedLabelColor = Color.White
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionHeader("2. Vertical Alignment")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("TOP" to "Top", "CENTER" to "Centre", "BOTTOM" to "Bottom").forEach { (key, label) ->
            FilterChip(
                selected = prefs.verticalAlignment.equals(key, ignoreCase = true),
                onClick = { onUpdate(prefs.copy(verticalAlignment = key)) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0284C7),
                    selectedLabelColor = Color.White
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionHeader("3. Spacing & Padding Controls")
    SizeSettingRow(
        label = "Time-to-Date Spacing",
        currentValue = prefs.timeToDateSpacingDp,
        range = 4..24,
        unit = "dp",
        onValueChange = { onUpdate(prefs.copy(timeToDateSpacingDp = it)) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    SizeSettingRow(
        label = "Date-to-Weather Spacing",
        currentValue = prefs.dateToWeatherSpacingDp,
        range = 4..24,
        unit = "dp",
        onValueChange = { onUpdate(prefs.copy(dateToWeatherSpacingDp = it)) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    SizeSettingRow(
        label = "Widget Inner Padding",
        currentValue = prefs.paddingDp,
        range = 0..24,
        unit = "dp",
        onValueChange = { onUpdate(prefs.copy(paddingDp = it)) }
    )

    Spacer(modifier = Modifier.height(14.dp))

    SectionHeader("4. Individual Element Visibility")
    SwitchSetting(
        label = "Show Time",
        subtitle = "Display digital clock digits",
        checked = prefs.showTime,
        onCheckedChange = { onUpdate(prefs.copy(showTime = it)) }
    )
    SwitchSetting(
        label = "Show AM/PM",
        subtitle = "Display morning/evening indicator",
        checked = prefs.showAmPm,
        onCheckedChange = { onUpdate(prefs.copy(showAmPm = it)) }
    )
    SwitchSetting(
        label = "Show Weekday",
        subtitle = "e.g. Sunday",
        checked = prefs.showWeekday,
        onCheckedChange = { onUpdate(prefs.copy(showWeekday = it)) }
    )
    SwitchSetting(
        label = "Show Date (Day of Month)",
        subtitle = "e.g. 01",
        checked = prefs.showDate,
        onCheckedChange = { onUpdate(prefs.copy(showDate = it)) }
    )
    SwitchSetting(
        label = "Show Month",
        subtitle = "e.g. November",
        checked = prefs.showMonth,
        onCheckedChange = { onUpdate(prefs.copy(showMonth = it)) }
    )
    SwitchSetting(
        label = "Show Year",
        subtitle = "e.g. 2026",
        checked = prefs.showYear,
        onCheckedChange = { onUpdate(prefs.copy(showYear = it)) }
    )
    SwitchSetting(
        label = "Show Weather Section",
        subtitle = "Master switch for weather details",
        checked = prefs.showWeather,
        onCheckedChange = { onUpdate(prefs.copy(showWeather = it)) }
    )
    if (prefs.showWeather) {
        SwitchSetting(
            label = "Show Temperature",
            subtitle = "e.g. 28°C",
            checked = prefs.showTemperature,
            onCheckedChange = { onUpdate(prefs.copy(showTemperature = it)) }
        )
        SwitchSetting(
            label = "Show Condition Text",
            subtitle = "e.g. Partly Cloudy",
            checked = prefs.showCondition,
            onCheckedChange = { onUpdate(prefs.copy(showCondition = it)) }
        )
        SwitchSetting(
            label = "Show Weather Icon",
            subtitle = "Sun/clouds/rain vector icon",
            checked = prefs.showIcon,
            onCheckedChange = { onUpdate(prefs.copy(showIcon = it)) }
        )
        SwitchSetting(
            label = "Show Current City",
            subtitle = "Location name",
            checked = prefs.showLocation,
            onCheckedChange = { onUpdate(prefs.copy(showLocation = it)) }
        )
        SwitchSetting(
            label = "Show Humidity",
            subtitle = "e.g. 50%",
            checked = prefs.showHumidity,
            onCheckedChange = { onUpdate(prefs.copy(showHumidity = it)) }
        )
    }
}

// =======================================================================
// 6. TIME & DATE TAB CONTENT
// =======================================================================
@Composable
private fun TimeAndDateTabContent(
    prefs: WidgetPreferences,
    onUpdate: (WidgetPreferences) -> Unit
) {
    SectionHeader("1. Time Format")
    SwitchSetting(
        label = "24-Hour Format",
        subtitle = "Switch between 12-hour (04:26 PM) and 24-hour (16:26)",
        checked = prefs.is24Hour,
        onCheckedChange = { onUpdate(prefs.copy(is24Hour = it)) }
    )
    SwitchSetting(
        label = "Show Seconds",
        subtitle = "Ticking seconds where supported by launcher",
        checked = prefs.showSeconds,
        onCheckedChange = { onUpdate(prefs.copy(showSeconds = it)) }
    )

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
        value = prefs.timeFormatOverride,
        onValueChange = { onUpdate(prefs.copy(timeFormatOverride = it)) },
        label = { Text("Custom Time Format (e.g. hh:mm a)") },
        placeholder = { Text("Leave blank to use default") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    SectionHeader("2. Date Format Presets")
    val presets = listOf(
        "EEEE, dd MMMM yyyy",
        "EEE, dd MMM yyyy",
        "dd/MM/yyyy",
        "dd MMMM yyyy",
        "EEEE | MMMM dd"
    )
    presets.forEach { preset ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUpdate(prefs.copy(dateFormatPreset = preset, customDateFormat = "")) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = prefs.dateFormatPreset == preset && prefs.customDateFormat.isEmpty(),
                onClick = { onUpdate(prefs.copy(dateFormatPreset = preset, customDateFormat = "")) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(preset, color = Color.White, fontSize = 14.sp)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = prefs.customDateFormat,
        onValueChange = { onUpdate(prefs.copy(customDateFormat = it)) },
        label = { Text("Custom Date Pattern (e.g. dd MMM)") },
        placeholder = { Text("Leave blank to use preset above") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

// =======================================================================
// 7. WEATHER & ACTIONS TAB CONTENT
// =======================================================================
@Composable
private fun WeatherAndActionsTabContent(
    prefs: WidgetPreferences,
    onUpdate: (WidgetPreferences) -> Unit
) {
    SectionHeader("1. Temperature Unit")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilterChip(
            selected = prefs.isCelsius,
            onClick = { onUpdate(prefs.copy(isCelsius = true)) },
            label = { Text("Celsius (°C)") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF0284C7),
                selectedLabelColor = Color.White
            )
        )
        FilterChip(
            selected = !prefs.isCelsius,
            onClick = { onUpdate(prefs.copy(isCelsius = false)) },
            label = { Text("Fahrenheit (°F)") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF0284C7),
                selectedLabelColor = Color.White
            )
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionHeader("2. Battery-Efficient Refresh Frequency")
    val intervalOptions = listOf(
        30 to "Every 30 minutes",
        60 to "Every 60 minutes (Recommended)",
        120 to "Every 2 hours",
        240 to "Every 4 hours",
        0 to "Manual Only (0% wakeups)"
    )
    intervalOptions.forEach { (minutes, label) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUpdate(prefs.copy(weatherRefreshIntervalMinutes = minutes)) }
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = prefs.weatherRefreshIntervalMinutes == minutes,
                onClick = { onUpdate(prefs.copy(weatherRefreshIntervalMinutes = minutes)) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.White, fontSize = 13.sp)
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionHeader("3. City & Location Source")
    SwitchSetting(
        label = "Auto Detect Location",
        subtitle = "Uses passive cached location with 0% extra battery drain",
        checked = prefs.useGpsLocation,
        onCheckedChange = { onUpdate(prefs.copy(useGpsLocation = it)) }
    )

    if (!prefs.useGpsLocation) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Manual City Fallback: ${prefs.manualCityName}",
            color = Color(0xFF38BDF8),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LocationHelper.POPULAR_CITIES.take(3).forEach { city ->
                FilterChip(
                    selected = prefs.manualCityName.equals(city.cityName, ignoreCase = true),
                    onClick = {
                        onUpdate(
                            prefs.copy(
                                manualCityName = city.cityName,
                                manualLatitude = city.latitude,
                                manualLongitude = city.longitude
                            )
                        )
                    },
                    label = { Text(city.cityName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0284C7),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    SectionHeader("4. Tap Actions")
    ActionSelector(
        title = "Time Tap Action",
        selected = prefs.timeTapAction,
        options = listOf(
            "CLOCK" to "Open Clock App",
            "DASHBOARD" to "Open ChronoWidget Dashboard",
            "NONE" to "No Action"
        ),
        onSelect = { onUpdate(prefs.copy(timeTapAction = it)) }
    )

    Spacer(modifier = Modifier.height(10.dp))

    ActionSelector(
        title = "Date Tap Action",
        selected = prefs.dateTapAction,
        options = listOf(
            "CALENDAR" to "Open Calendar App",
            "DASHBOARD" to "Open ChronoWidget Dashboard",
            "NONE" to "No Action"
        ),
        onSelect = { onUpdate(prefs.copy(dateTapAction = it)) }
    )

    Spacer(modifier = Modifier.height(10.dp))

    ActionSelector(
        title = "Weather Tap Action",
        selected = prefs.weatherTapAction,
        options = listOf(
            "XIAOMI_OR_SYSTEM" to "Open Xiaomi / System Weather App",
            "DETAILS" to "Open ChronoWidget Weather Details",
            "DASHBOARD" to "Open ChronoWidget Dashboard",
            "NONE" to "No Action"
        ),
        onSelect = { onUpdate(prefs.copy(weatherTapAction = it)) }
    )
}

// =======================================================================
// REUSABLE HELPER COMPONENTS
// =======================================================================
@Composable
private fun ColorPreviewBox(
    hex: String,
    label: String
) {
    val parsedColor = Color(ColorPalettes.parseColorSafe(hex))

    Surface(
        color = Color(0xFF161B22),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$label: ${ColorPalettes.sanitizeHex(hex)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Split tile showing color on Dark and Light backgrounds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                // Dark background half
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sample Text",
                        color = parsedColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Light background half
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sample Text",
                        color = parsedColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteGroup(
    paletteName: String,
    colors: List<ColorOption>,
    selectedHex: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$paletteName Palette",
            color = Color(0xFFE2E8F0),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.forEach { option ->
                val isSelected = selectedHex.equals(option.hex, ignoreCase = true)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(option.hex) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(ColorPalettes.parseColorSafe(option.hex)))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (isColorDark(option.hex)) Color.White else Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = option.name,
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionSelector(
    title: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    Column {
        Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        options.forEach { (key, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(key) }
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == key,
                    onClick = { onSelect(key) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF38BDF8),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SwitchSetting(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF38BDF8),
                activeTrackColor = Color(0xFF0284C7)
            )
        )
    }
}

private fun isColorDark(hex: String): Boolean {
    return try {
        val colorInt = ColorPalettes.parseColorSafe(hex)
        val r = android.graphics.Color.red(colorInt)
        val g = android.graphics.Color.green(colorInt)
        val b = android.graphics.Color.blue(colorInt)
        // Luminance formula
        (0.299 * r + 0.587 * g + 0.114 * b) < 140
    } catch (_: Exception) {
        false
    }
}

private fun String.capitalize(): String =
    this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
