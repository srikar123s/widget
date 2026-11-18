package com.srikar.chronowidget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import com.srikar.chronowidget.data.preferences.PreferencesManager
import com.srikar.chronowidget.weather.WeatherCache

class ChronoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefsManager = PreferencesManager(context)
        val prefs = prefsManager.getPreferencesSnapshot()
        val weatherCache = WeatherCache(context)
        val weatherData = weatherCache.getCachedWeather() ?: weatherCache.getDefaultFallbackWeather()

        val remoteViews = WidgetUpdater.buildRemoteViews(context, prefs, weatherData)

        provideContent {
            // AndroidRemoteViews inside Glance ensures live ticking TextClock and zero battery overhead
            AndroidRemoteViews(remoteViews = remoteViews)
        }
    }
}

class ChronoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChronoWidget()
}
