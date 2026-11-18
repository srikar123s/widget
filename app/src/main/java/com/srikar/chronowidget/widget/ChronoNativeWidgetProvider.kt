package com.srikar.chronowidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Primary 4×2 Native RemoteViews Widget Provider.
 *
 * Employs goAsync() with a managed SupervisorJob to safely handle async widget updates
 * within the BroadcastReceiver lifecycle, preventing coroutine leaks and premature process termination.
 */
class ChronoNativeWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "ChronoNativeWidget"
        const val ACTION_UPDATE_WIDGET = "com.srikar.chronowidget.ACTION_UPDATE_WIDGET"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
        val pendingResult = goAsync()
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)
        scope.launch {
            try {
                WidgetUpdater.updateAllWidgets(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error in onUpdate: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive intent: ${intent.action}")
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val pendingResult = goAsync()
            val job = SupervisorJob()
            val scope = CoroutineScope(Dispatchers.IO + job)
            scope.launch {
                try {
                    WidgetUpdater.updateAllWidgets(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onReceive: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Log.d(TAG, "onAppWidgetOptionsChanged for widgetId $appWidgetId")
        val pendingResult = goAsync()
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)
        scope.launch {
            try {
                WidgetUpdater.updateAllWidgets(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error in onAppWidgetOptionsChanged: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
