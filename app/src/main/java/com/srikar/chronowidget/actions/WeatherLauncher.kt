package com.srikar.chronowidget.actions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.srikar.chronowidget.MainActivity

object WeatherLauncher {

    const val EXTRA_DESTINATION = "extra_destination"
    const val DESTINATION_WEATHER_DETAILS = "destination_weather_details"

    fun getLaunchIntent(context: Context, preference: String = "XIAOMI_OR_SYSTEM"): Intent {
        val packageManager = context.packageManager

        if (preference == "DETAILS") {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_DESTINATION, DESTINATION_WEATHER_DETAILS)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        if (preference == "DASHBOARD") {
            return Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        // 1. Xiaomi Weather app (MIUI / HyperOS)
        val xiaomiWeatherIntent = packageManager.getLaunchIntentForPackage("com.miui.weather2")
        if (xiaomiWeatherIntent != null) {
            xiaomiWeatherIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            return xiaomiWeatherIntent
        }

        // 2. Other common weather packages
        val alternateWeatherPackages = listOf(
            "com.google.android.apps.weather",
            "com.sec.android.daemonapp" // Samsung Weather
        )
        for (pkg in alternateWeatherPackages) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                return launchIntent
            }
        }

        // 3. Fallback to ChronoWidget Weather Details
        return Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_DESTINATION, DESTINATION_WEATHER_DETAILS)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    fun getPendingIntent(context: Context, preference: String = "XIAOMI_OR_SYSTEM", requestCode: Int = 103): PendingIntent {
        val intent = getLaunchIntent(context, preference)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }
}
