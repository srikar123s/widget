package com.srikar.chronowidget

import android.app.Application
import android.util.Log
import com.srikar.chronowidget.weather.WeatherRepository
import com.srikar.chronowidget.weather.WeatherWorker

class ChronoApplication : Application() {

    companion object {
        private const val TAG = "ChronoApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Initializing ChronoWidget Application...")

        // Schedule periodic background weather updates via WorkManager
        try {
            WeatherWorker.schedulePeriodicWork(this)
        } catch (e: Exception) {
            Log.e(TAG, "Could not initialize WorkManager: ${e.message}")
        }

        // Pre-warm weather repository
        WeatherRepository.getInstance(this)
    }
}
