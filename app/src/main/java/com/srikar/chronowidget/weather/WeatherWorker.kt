package com.srikar.chronowidget.weather

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class WeatherWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "WeatherWorker"
        private const val WORK_NAME = "chrono_periodic_weather_work"

        fun schedulePeriodicWork(context: Context, intervalMinutes: Int = 60) {
            val workManager = WorkManager.getInstance(context)

            if (intervalMinutes <= 0) {
                // Manual only mode: cancel periodic background work
                workManager.cancelUniqueWork(WORK_NAME)
                Log.d(TAG, "Periodic weather work cancelled (Manual mode)")
                return
            }

            // Clamped to at least 15 min per Android WorkManager limit
            val safeIntervalMinutes = intervalMinutes.coerceAtLeast(15)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true) // Battery protection constraint
                .build()

            val request = PeriodicWorkRequestBuilder<WeatherWorker>(
                safeIntervalMinutes.toLong(),
                TimeUnit.MINUTES,
                (safeIntervalMinutes / 4).coerceAtLeast(5).toLong(),
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Log.d(TAG, "Periodic weather work scheduled every $safeIntervalMinutes min (battery-optimized)")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Executing deferred background weather refresh...")
        return try {
            val repository = WeatherRepository.getInstance(applicationContext)
            repository.refreshWeather(triggerWidgetUpdate = true)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during background weather update: ${e.message}")
            Result.retry()
        }
    }
}
