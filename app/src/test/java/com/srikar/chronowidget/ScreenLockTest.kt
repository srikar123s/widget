package com.srikar.chronowidget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verifies that the ChronoWidget main app module is strictly decoupled from screen-lock functionality,
 * which is exclusively maintained in the standalone :screenlock application.
 */
class ScreenLockTest {

    @Test
    fun testScreenLockComponentsCompletelyRemovedFromChronoWidget() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        val altFile = File("app/src/main/AndroidManifest.xml")
        val targetFile = if (manifestFile.exists()) manifestFile else altFile

        assertTrue("AndroidManifest.xml must exist", targetFile.exists())
        val content = targetFile.readText()

        // Verify zero screen-lock declarations in ChronoWidget
        assertFalse("ChronoWidget must NOT declare ScreenLockWidgetProvider", content.contains("ScreenLockWidgetProvider"))
        assertFalse("ChronoWidget must NOT declare ScreenLockAdminReceiver", content.contains("ScreenLockAdminReceiver"))
        assertFalse("ChronoWidget must NOT declare ScreenLockActivity", content.contains("ScreenLockActivity"))
        assertFalse("ChronoWidget must NOT request BIND_DEVICE_ADMIN", content.contains("android.permission.BIND_DEVICE_ADMIN"))
        assertFalse("ChronoWidget must NOT reference device_admin_policies", content.contains("device_admin_policies"))
        assertFalse("ChronoWidget must NOT reference screen_lock_widget_info", content.contains("screen_lock_widget_info"))
    }
}
