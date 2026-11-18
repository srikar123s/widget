package com.srikar.chronowidget.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.srikar.chronowidget.weather.WeatherRepository
import com.srikar.chronowidget.weather.WeatherWorker
import com.srikar.chronowidget.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles system-level broadcasts (boot, time, timezone, date changes).
 * Uses goAsync() with a managed SupervisorJob to ensure background work
 * completes reliably without leaking coroutines or risking process death.
 */
class SystemBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SystemBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "Received system broadcast action: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val pendingResult = goAsync()
                val job = SupervisorJob()
                val scope = CoroutineScope(Dispatchers.IO + job)
                scope.launch {
                    try {
                        WeatherWorker.schedulePeriodicWork(context)
                        WeatherRepository.getInstance(context).refreshWeather(triggerWidgetUpdate = true)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error refreshing weather on boot: ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                val pendingResult = goAsync()
                val job = SupervisorJob()
                val scope = CoroutineScope(Dispatchers.IO + job)
                scope.launch {
                    try {
                        WidgetUpdater.updateAllWidgets(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating widgets on time/date change: ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
