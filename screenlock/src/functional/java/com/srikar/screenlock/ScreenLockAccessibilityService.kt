package com.srikar.screenlock

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.delay

/**
 * Hardened, lifecycle-resilient AccessibilityService for GLOBAL_ACTION_LOCK_SCREEN.
 *
 * Designed to survive process recycling, deep doze, and HyperOS background sweeps:
 * - Proper onUnbind delegation ensuring standard AOSP reconnect lifecycle (onBind -> init -> onServiceConnected).
 * - Safe volatile instance tracking with immediate cleanup on unbind/destroy prevents memory leaks and stale binder calls.
 * - Non-blocking connection waiter resolves cold-start race conditions.
 * - Privacy-first: strictly zero event processing, zero content retrieval, zero gesture injection.
 */
class ScreenLockAccessibilityService : AccessibilityService() {

    override fun onCreate() {
        super.onCreate()
        CrashDiagnostics.init(applicationContext)
        CrashDiagnostics.logEvent(applicationContext, "SERVICE_CREATED")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = this
        CrashDiagnostics.logEvent(applicationContext, "SERVICE_CONNECTED and ready")
        Log.i(TAG, "ScreenLockAccessibilityService connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: privacy-first. We do not observe or process any user interactions or screen content.
    }

    override fun onInterrupt() {
        CrashDiagnostics.logEvent(applicationContext, "SERVICE_INTERRUPTED")
        Log.w(TAG, "ScreenLockAccessibilityService interrupted.")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (activeInstance === this) {
            activeInstance = null
        }
        CrashDiagnostics.logEvent(applicationContext, "SERVICE_UNBOUND")
        Log.i(TAG, "ScreenLockAccessibilityService unbound.")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeInstance === this) {
            activeInstance = null
        }
        CrashDiagnostics.logEvent(applicationContext, "SERVICE_DESTROYED")
        Log.i(TAG, "ScreenLockAccessibilityService destroyed.")
    }

    companion object {
        private const val TAG = "ScreenLockA11y"

        @Volatile
        private var activeInstance: ScreenLockAccessibilityService? = null

        /**
         * Checks whether the service is currently bound and active in memory.
         */
        fun isServiceRunning(): Boolean = activeInstance != null

        /**
         * Awaits service connection for up to [timeoutMs] milliseconds during process cold starts.
         * Returns true as soon as the service connects, or false if the timeout expires.
         */
        suspend fun awaitConnection(timeoutMs: Long = 400L): Boolean {
            if (isServiceRunning()) return true
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (isServiceRunning()) return true
                delay(20)
            }
            return isServiceRunning()
        }

        /**
         * Checks whether the user has enabled this Accessibility Service in system settings.
         */
        fun isServiceEnabledInSettings(context: Context): Boolean {
            val expectedComponent = ComponentName(context, ScreenLockAccessibilityService::class.java).flattenToString()
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val component = colonSplitter.next()
                if (component.equals(expectedComponent, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        /**
         * Locks the screen using GLOBAL_ACTION_LOCK_SCREEN.
         * Returns true if the action was successfully performed, false otherwise.
         */
        fun lockScreen(context: Context? = null): Boolean {
            val service = activeInstance ?: run {
                CrashDiagnostics.logEvent(context, "LOCK_ATTEMPT_FAILED: activeInstance is null")
                return false
            }
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val success = service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    CrashDiagnostics.logEvent(service.applicationContext, "GLOBAL_ACTION_LOCK_SCREEN executed (success=$success)")
                    success
                } else {
                    CrashDiagnostics.logEvent(service.applicationContext, "LOCK_ATTEMPT_FAILED: SDK < API 28")
                    false
                }
            } catch (e: Exception) {
                CrashDiagnostics.logEvent(service.applicationContext, "LOCK_ACTION_EXCEPTION: ${e.javaClass.simpleName} - ${e.message}")
                Log.e(TAG, "Error executing GLOBAL_ACTION_LOCK_SCREEN", e)
                false
            }
        }

        /**
         * Opens Android's Accessibility Settings page.
         */
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        /**
         * Opens the App Info settings page for HyperOS permissions, Autostart, and Battery Saver.
         */
        fun openAppInfoSettings(context: Context) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
