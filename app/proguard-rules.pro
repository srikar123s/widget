# Proguard rules for ChronoWidget
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class com.srikar.chronowidget.weather.** { *; }
-keep class com.srikar.chronowidget.data.models.** { *; }
