package com.srikar.screenlock

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

enum class ServiceStatus {
    READY,
    HALTED_MALFUNCTIONING,
    ACTIVATION_REQUIRED
}

class MainActivity : ComponentActivity() {

    private val serviceStatusState = mutableStateOf(ServiceStatus.ACTIVATION_REQUIRED)
    private val diagnosticsState = mutableStateOf<List<String>>(emptyList())
    private var uiRendered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashDiagnostics.init(applicationContext)
        CrashDiagnostics.logEvent(applicationContext, "MAIN_ACTIVITY_ON_CREATE (action=${intent?.action})")

        refreshStatus()

        val isExplicitSettings = intent?.action == ACTION_SETTINGS ||
                intent?.getBooleanExtra(EXTRA_OPEN_SETTINGS, false) == true

        if (!isExplicitSettings && ScreenLockAccessibilityService.isServiceEnabledInSettings(this)) {
            // Priority 1: Service already bound and running
            if (ScreenLockAccessibilityService.isServiceRunning()) {
                val locked = ScreenLockAccessibilityService.lockScreen(this)
                if (locked) {
                    finishAndRemoveTask()
                    return
                }
            }

            // Priority 2: Process cold-started. Await async binding handshake
            lifecycleScope.launch {
                CrashDiagnostics.logEvent(applicationContext, "COLD_START_AWAITING_SERVICE_BINDING...")
                val connected = ScreenLockAccessibilityService.awaitConnection(timeoutMs = 400L)
                if (connected) {
                    val locked = ScreenLockAccessibilityService.lockScreen(this@MainActivity)
                    if (locked) {
                        finishAndRemoveTask()
                        return@launch
                    }
                }
                CrashDiagnostics.logEvent(applicationContext, "COLD_START_TIMEOUT: service halted or uninitialized")
                renderSetupUi()
            }
            return
        }

        renderSetupUi()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        CrashDiagnostics.logEvent(applicationContext, "MAIN_ACTIVITY_ON_NEW_INTENT (action=${intent.action})")
        refreshStatus()

        val isExplicitSettings = intent.action == ACTION_SETTINGS ||
                intent.getBooleanExtra(EXTRA_OPEN_SETTINGS, false)

        if (!isExplicitSettings && ScreenLockAccessibilityService.isServiceEnabledInSettings(this)) {
            if (ScreenLockAccessibilityService.isServiceRunning()) {
                val locked = ScreenLockAccessibilityService.lockScreen(this)
                if (locked) {
                    finishAndRemoveTask()
                    return
                }
            }

            lifecycleScope.launch {
                val connected = ScreenLockAccessibilityService.awaitConnection(timeoutMs = 400L)
                if (connected) {
                    val locked = ScreenLockAccessibilityService.lockScreen(this@MainActivity)
                    if (locked) {
                        finishAndRemoveTask()
                    }
                } else {
                    refreshStatus()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val isRunning = ScreenLockAccessibilityService.isServiceRunning()
        val isEnabledInSettings = ScreenLockAccessibilityService.isServiceEnabledInSettings(this)

        serviceStatusState.value = when {
            isRunning -> ServiceStatus.READY
            isEnabledInSettings -> ServiceStatus.HALTED_MALFUNCTIONING
            else -> ServiceStatus.ACTIVATION_REQUIRED
        }

        diagnosticsState.value = CrashDiagnostics.getRecentEvents()
    }

    private fun renderSetupUi() {
        refreshStatus()
        if (uiRendered) {
            return
        }
        uiRendered = true

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF90CAF9),
                    onPrimary = Color(0xFF003258),
                    surface = Color(0xFF1E1E24),
                    background = Color(0xFF121216),
                    error = Color(0xFFCF6679)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScreenLockSetupScreen(
                        status = serviceStatusState.value,
                        recentEvents = diagnosticsState.value,
                        onEnableA11yClick = {
                            ScreenLockAccessibilityService.openAccessibilitySettings(this)
                        },
                        onRestartA11yClick = {
                            ScreenLockAccessibilityService.openAccessibilitySettings(this)
                        },
                        onOpenAppInfoClick = {
                            ScreenLockAccessibilityService.openAppInfoSettings(this)
                        },
                        onTestLockClick = {
                            val success = ScreenLockAccessibilityService.lockScreen(this)
                            refreshStatus()
                            if (!success) {
                                Toast.makeText(
                                    this,
                                    "Accessibility service not active. Toggle it OFF and ON in Accessibility settings.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onRefreshDiagnostics = {
                            refreshStatus()
                        },
                        onClearDiagnostics = {
                            CrashDiagnostics.clearLogs(this)
                            refreshStatus()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val ACTION_SETTINGS = "com.srikar.screenlock.ACTION_SETTINGS"
        const val EXTRA_OPEN_SETTINGS = "com.srikar.screenlock.EXTRA_OPEN_SETTINGS"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenLockSetupScreen(
    status: ServiceStatus,
    recentEvents: List<String>,
    onEnableA11yClick: () -> Unit,
    onRestartA11yClick: () -> Unit,
    onOpenAppInfoClick: () -> Unit,
    onTestLockClick: () -> Unit,
    onRefreshDiagnostics: () -> Unit,
    onClearDiagnostics: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screen Lock") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header icon
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(Color(0xFF242630), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF808080),
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Screen Lock",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Fingerprint-Compatible 1-Tap Lock for POCO / HyperOS",
                fontSize = 12.sp,
                color = Color(0xFFAAAAAA),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Dynamic Status Card based on current ServiceStatus
            when (status) {
                ServiceStatus.READY -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF143324)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Status: Ready & Active",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Service is active in memory. Tapping the dock icon locks the phone instantly with fingerprint unlock preserved.",
                                    color = Color(0xFFCCCCCC),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onTestLockClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Screen Lock Now", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                ServiceStatus.HALTED_MALFUNCTIONING -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF381A1A)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Service Halted by HyperOS",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "HyperOS put the background service to sleep during idle. Android reports this in Settings as:\n" +
                                        "\"This service is malfunctioning\".\n\n" +
                                        "To restore it immediately: tap below, toggle Screen Lock OFF and then back ON.",
                                color = Color(0xFFFFCDD2),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onRestartA11yClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restart Service in Settings", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // How to reset instructions
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF231E20)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Quick 3-Step Reset Guide:",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB74D),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. Tap 'Restart Service in Settings' above.\n" +
                                        "2. In Downloaded Apps, tap Screen Lock and turn the switch OFF.\n" +
                                        "3. Turn the switch back ON (wait for HyperOS 10s countdown).\n" +
                                        "4. Return to Screen Lock — status will turn green!",
                                fontSize = 12.sp,
                                color = Color(0xFFE0E0E0),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                ServiceStatus.ACTIVATION_REQUIRED -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF332214)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Status: Activation Required",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Accessibility permission is required so the app can trigger the system power button lock action.",
                                    color = Color(0xFFCCCCCC),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onEnableA11yClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable Accessibility Service", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onOpenAppInfoClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF90CAF9))
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("If greyed out: Open App Info", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // HyperOS Long-Term Stability Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222D)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "POCO HyperOS Long-Term Stability",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To prevent HyperOS from terminating the service during overnight sleep or long idle periods:\n\n" +
                                "1. Battery Saver: Open App Info > Battery Saver > select 'No restrictions'.\n" +
                                "2. Autostart: Open App Info > toggle 'Autostart' to ON.\n" +
                                "3. Lock in Recents: Open Recent Apps overview, press and hold Screen Lock, and tap the Padlock icon so it is never cleared from RAM.",
                        fontSize = 12.sp,
                        color = Color(0xFFCFD8DC),
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenAppInfoClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Open App Info to Apply Settings", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Diagnostic Logs Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181A20)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Service Lifecycle Diagnostics",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = onRefreshDiagnostics, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (recentEvents.isEmpty()) {
                        Text(
                            text = "No diagnostic events recorded yet.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    } else {
                        recentEvents.takeLast(6).forEach { event ->
                            Text(
                                text = event,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFB0BEC5),
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onClearDiagnostics) {
                            Text("Clear Logs", fontSize = 11.sp, color = Color(0xFF90CAF9))
                        }
                    }
                }
            }
        }
    }
}
