package com.srikar.screenlock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.srikar.screenlock.R
import com.srikar.screenlock.ScreenLockAccessibilityService

/**
 * Dedicated 1×1 Screen Lock Widget Provider for the standalone Screen Lock app.
 * Displays a centered grey lock symbol on a transparent background.
 * Tapping directly locks the device via ScreenLockAccessibilityService (GLOBAL_ACTION_LOCK_SCREEN).
 */
class ScreenLockWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "ScreenLockWidget"
        private var lastLockTimestamp = 0L
        private const val LOCK_DEBOUNCE_MS = 800L

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = android.content.ComponentName(context, ScreenLockWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) return

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_screen_lock)

            // Intent to perform the lock action via broadcast
            val lockIntent = Intent(context, ScreenLockWidgetProvider::class.java).apply {
                action = ACTION_LOCK_SCREEN
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                201,
                lockIntent,
                flags
            )

            views.setOnClickPendingIntent(R.id.widget_screen_lock_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_screen_lock_icon, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private const val ACTION_LOCK_SCREEN = "com.srikar.screenlock.ACTION_LOCK_SCREEN"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_LOCK_SCREEN) {
            val now = System.currentTimeMillis()
            if (now - lastLockTimestamp < LOCK_DEBOUNCE_MS) {
                Log.d(TAG, "Ignoring rapid repeat lock tap (debounced)")
                return
            }
            lastLockTimestamp = now

            // Delegate to the accessibility service
            val locked = ScreenLockAccessibilityService.lockScreen(context)
            if (!locked) {
                // Only show toast if service is enabled but not running (edge case)
                if (ScreenLockAccessibilityService.isServiceEnabledInSettings(context)) {
                    android.widget.Toast.makeText(
                        context,
                        "Service starting... tap again in a moment",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}