package com.srikar.chronowidget.actions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.util.Log
import com.srikar.chronowidget.MainActivity

object ClockLauncher {

    private const val TAG = "ClockLauncher"

    // Comprehensive priority list of manufacturer and system clock packages
    val CLOCK_PACKAGES = listOf(
        "com.miui.deskclock",               // Xiaomi HyperOS / MIUI DeskClock
        "com.google.android.deskclock",     // Google Pixel / Nexus Clock
        "com.android.deskclock",            // Standard Android AOSP DeskClock
        "com.sec.android.app.clockpackage", // Samsung One UI Clock
        "com.oneplus.deskclock",            // OnePlus OxygenOS Clock
        "com.coloros.alarmclock",           // OPPO ColorOS Clock
        "com.realme.alarmclock",            // Realme UI Clock
        "com.oppo.alarmclock",              // OPPO Clock
        "com.transsion.deskclock",          // Tecno / Infinix / Itel Clock
        "com.motorola.blur.alarmclock",     // Motorola Blur Clock
        "com.huawei.deskclock",             // Huawei EMUI Clock
        "com.honor.deskclock",              // Honor MagicOS Clock
        "com.asus.alarmclock",              // ASUS ZenUI Clock
        "com.zte.clock",                    // ZTE / Nubia Clock
        "com.sonyericsson.organizer"        // Sony Xperia Clock
    )

    fun getLaunchIntent(context: Context): Intent {
        val packageManager = context.packageManager

        // 1. Direct launch intent for known OEM clock applications (Highest reliability on Xiaomi HyperOS, Samsung, Pixel, etc.)
        for (pkg in CLOCK_PACKAGES) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                Log.d(TAG, "Resolved clock app to package: $pkg")
                return launchIntent
            }
        }

        // 2. Query for explicit activity matching APP_CLOCKS category
        val clockCategoryIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory("android.intent.category.APP_CLOCKS")
        }
        val categoryMatches = packageManager.queryIntentActivities(clockCategoryIntent, 0)
        if (categoryMatches.isNotEmpty()) {
            val activityInfo = categoryMatches[0].activityInfo
            Log.d(TAG, "Resolved clock app via APP_CLOCKS: ${activityInfo.packageName}/${activityInfo.name}")
            return Intent(Intent.ACTION_MAIN).apply {
                setClassName(activityInfo.packageName, activityInfo.name)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
        }

        // 3. Query for explicit activity matching AlarmClock.ACTION_SHOW_ALARMS
        val showAlarmsIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        val alarmMatches = packageManager.queryIntentActivities(showAlarmsIntent, 0)
        if (alarmMatches.isNotEmpty()) {
            val activityInfo = alarmMatches[0].activityInfo
            Log.d(TAG, "Resolved clock app via SHOW_ALARMS: ${activityInfo.packageName}/${activityInfo.name}")
            return Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                setClassName(activityInfo.packageName, activityInfo.name)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
        }

        // 4. Query for explicit activity matching AlarmClock.ACTION_SET_ALARM
        val setAlarmIntent = Intent(AlarmClock.ACTION_SET_ALARM)
        val setAlarmMatches = packageManager.queryIntentActivities(setAlarmIntent, 0)
        if (setAlarmMatches.isNotEmpty()) {
            val activityInfo = setAlarmMatches[0].activityInfo
            Log.d(TAG, "Resolved clock app via SET_ALARM: ${activityInfo.packageName}/${activityInfo.name}")
            return Intent(AlarmClock.ACTION_SET_ALARM).apply {
                setClassName(activityInfo.packageName, activityInfo.name)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
        }

        // 5. Fallback to ChronoWidget main dashboard activity
        Log.w(TAG, "No system clock app could be resolved; falling back to ChronoWidget dashboard")
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    fun getPendingIntent(context: Context, requestCode: Int = 101): PendingIntent {
        val intent = getLaunchIntent(context)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }
}
