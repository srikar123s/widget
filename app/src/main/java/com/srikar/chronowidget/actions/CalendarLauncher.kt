package com.srikar.chronowidget.actions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import com.srikar.chronowidget.MainActivity

object CalendarLauncher {

    fun getLaunchIntent(context: Context): Intent {
        val packageManager = context.packageManager

        // 1. Standard Android Calendar Category
        val categoryIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (categoryIntent.resolveActivity(packageManager) != null) {
            return categoryIntent
        }

        // 2. Standard Android Calendar Content URI
        val timeUri = Uri.parse("content://com.android.calendar/time/${System.currentTimeMillis()}")
        val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
            data = timeUri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (calendarIntent.resolveActivity(packageManager) != null) {
            return calendarIntent
        }

        // 2. Intent using CalendarContract
        val contractIntent = Intent(Intent.ACTION_VIEW).apply {
            data = CalendarContract.CONTENT_URI
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (contractIntent.resolveActivity(packageManager) != null) {
            return contractIntent
        }

        // 3. Known manufacturer calendar packages
        val calendarPackages = listOf(
            "com.xiaomi.calendar",              // Xiaomi HyperOS / MIUI Calendar
            "com.google.android.calendar",      // Google Calendar
            "com.android.calendar",             // AOSP Calendar
            "com.samsung.android.calendar"      // Samsung Calendar
        )

        for (pkg in calendarPackages) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                return launchIntent
            }
        }

        // 4. Fallback to ChronoWidget main activity
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    fun getPendingIntent(context: Context, requestCode: Int = 102): PendingIntent {
        val intent = getLaunchIntent(context)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }
}
