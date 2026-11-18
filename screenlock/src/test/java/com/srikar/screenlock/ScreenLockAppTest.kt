package com.srikar.screenlock

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ScreenLockAppTest {

    @Test
    fun testSettingsConstants() {
        assertEquals("com.srikar.screenlock.ACTION_SETTINGS", MainActivity.ACTION_SETTINGS)
        assertEquals("com.srikar.screenlock.EXTRA_OPEN_SETTINGS", MainActivity.EXTRA_OPEN_SETTINGS)
    }

    @Test
    fun testFunctionalAccessibilityConfigXmlExistsAndIsPrivacySafe() {
        val a11yConfigFile = File("src/functional/res/xml/accessibility_service_config.xml")
        val altFile = File("screenlock/src/functional/res/xml/accessibility_service_config.xml")
        val targetFile = if (a11yConfigFile.exists()) a11yConfigFile else altFile

        assertTrue("accessibility_service_config.xml must exist in functional source set", targetFile.exists())
        val content = targetFile.readText()
        assertTrue("Accessibility config must have accessibility-service root tag", content.contains("<accessibility-service"))
        assertTrue("Accessibility config must not retrieve window content for privacy", content.contains("android:canRetrieveWindowContent=\"false\""))
        assertTrue("Accessibility config must not perform gestures", content.contains("android:canPerformGestures=\"false\""))
        assertTrue("Accessibility config must declare valid event types", content.contains("android:accessibilityEventTypes=\"typeWindowStateChanged\""))
        assertTrue("Accessibility config must declare notificationTimeout", content.contains("android:notificationTimeout=\"1000\""))
    }

    @Test
    fun testShortcutsXmlConfiguredCorrectly() {
        val shortcutsFile = File("src/main/res/xml/shortcuts.xml")
        val altFile = File("screenlock/src/main/res/xml/shortcuts.xml")
        val targetFile = if (shortcutsFile.exists()) shortcutsFile else altFile

        assertTrue("shortcuts.xml must exist for POCO/HyperOS quick settings", targetFile.exists())
        val content = targetFile.readText()
        assertTrue("shortcuts.xml must target MainActivity", content.contains("com.srikar.screenlock.MainActivity"))
        assertTrue("shortcuts.xml must declare ACTION_SETTINGS", content.contains("com.srikar.screenlock.ACTION_SETTINGS"))
        // Verify widget shortcut exists
        assertTrue("shortcuts.xml must declare widget shortcut", content.contains("screen_lock_widget"))
        assertTrue("shortcuts.xml must reference ScreenLockWidgetProvider", content.contains(".widget.ScreenLockWidgetProvider"))
    }

    @Test
    fun testMainAndroidManifestDeclarations() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        val altFile = File("screenlock/src/main/AndroidManifest.xml")
        val targetFile = if (manifestFile.exists()) manifestFile else altFile

        assertTrue("AndroidManifest.xml must exist", targetFile.exists())
        val content = targetFile.readText()

        // Primary launcher
        assertTrue("Manifest must declare MAIN and LAUNCHER for MainActivity",
            content.contains("android.intent.action.MAIN") && content.contains("android.intent.category.LAUNCHER"))

        // Security guarantees: No Device Admin & No Internet
        assertFalse("Manifest must NOT declare DeviceAdminReceiver", content.contains("ScreenLockAdminReceiver"))
        assertFalse("Manifest must NOT request BIND_DEVICE_ADMIN", content.contains("android.permission.BIND_DEVICE_ADMIN"))
        assertFalse("Manifest must NOT request INTERNET permission", content.contains("android.permission.INTERNET"))
    }

    @Test
    fun testFunctionalManifestDeclaresAccessibilityService() {
        val manifestFile = File("src/functional/AndroidManifest.xml")
        val altFile = File("screenlock/src/functional/AndroidManifest.xml")
        val targetFile = if (manifestFile.exists()) manifestFile else altFile

        assertTrue("functional/AndroidManifest.xml must exist", targetFile.exists())
        val content = targetFile.readText()

        assertTrue("Functional manifest must declare ScreenLockAccessibilityService",
            content.contains(".ScreenLockAccessibilityService"))
        assertTrue("Service must require BIND_ACCESSIBILITY_SERVICE permission",
            content.contains("android.permission.BIND_ACCESSIBILITY_SERVICE"))
    }

    @Test
    fun testFunctionalManifestDeclaresWidgetProvider() {
        val manifestFile = File("src/functional/AndroidManifest.xml")
        val altFile = File("screenlock/src/functional/AndroidManifest.xml")
        val targetFile = if (manifestFile.exists()) manifestFile else altFile

        assertTrue("functional/AndroidManifest.xml must exist", targetFile.exists())
        val content = targetFile.readText()

        assertTrue("Functional manifest must declare ScreenLockWidgetProvider",
            content.contains(".widget.ScreenLockWidgetProvider"))
        assertTrue("Widget provider must handle APPWIDGET_UPDATE",
            content.contains("android.appwidget.action.APPWIDGET_UPDATE"))
        assertTrue("Widget provider must handle ACTION_LOCK_SCREEN",
            content.contains("com.srikar.screenlock.ACTION_LOCK_SCREEN"))
    }

    @Test
    fun testWidgetInfoXmlContent() {
        val infoFile = File("src/functional/res/xml/screen_lock_widget_info.xml")
        val altFile = File("screenlock/src/functional/res/xml/screen_lock_widget_info.xml")
        val targetFile = if (infoFile.exists()) infoFile else altFile

        assertTrue("screen_lock_widget_info.xml must exist in functional source set", targetFile.exists())
        val content = targetFile.readText()
        assertTrue("Target width must be 1 cell", content.contains("targetCellWidth=\"1\""))
        assertTrue("Target height must be 1 cell", content.contains("targetCellHeight=\"1\""))
        assertTrue("Update period must be 0 for zero battery consumption", content.contains("updatePeriodMillis=\"0\""))
        assertTrue("Must point to screen lock layout", content.contains("@layout/widget_screen_lock"))
    }

    @Test
    fun testWidgetLayoutXmlContent() {
        val layoutFile = File("src/functional/res/layout/widget_screen_lock.xml")
        val altFile = File("screenlock/src/functional/res/layout/widget_screen_lock.xml")
        val targetFile = if (layoutFile.exists()) layoutFile else altFile

        assertTrue("widget_screen_lock.xml must exist in functional source set", targetFile.exists())
        val content = targetFile.readText()
        assertTrue("Background must be fully transparent", content.contains("@android:color/transparent"))
        assertTrue("Must include screen lock icon view", content.contains("widget_screen_lock_icon"))
        assertTrue("Must reference ic_lock_grey drawable", content.contains("@drawable/ic_lock_grey"))
    }

    @Test
    fun testWidgetProviderCompiles() {
        // Verify the widget provider class exists in functional source set
        val providerFile = File("src/functional/java/com/srikar/screenlock/widget/ScreenLockWidgetProvider.kt")
        val altFile = File("screenlock/src/functional/java/com/srikar/screenlock/widget/ScreenLockWidgetProvider.kt")
        val targetFile = if (providerFile.exists()) providerFile else altFile

        assertTrue("ScreenLockWidgetProvider.kt must exist in functional source set", targetFile.exists())
        val content = targetFile.readText()
        assertTrue("Must handle APPWIDGET_UPDATE", content.contains("APPWIDGET_UPDATE"))
        assertTrue("Must declare and use ACTION_LOCK_SCREEN", content.contains("ACTION_LOCK_SCREEN"))
        assertTrue("Must delegate to accessibility service lock", content.contains("ScreenLockAccessibilityService"))
    }
}
