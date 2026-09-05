# Project specific ProGuard / R8 rules.
# Appended to getDefaultProguardFile("proguard-android-optimize.txt").

# Keep project classes & members to guarantee 100% startup stability across all Android versions
-keep class com.analoganchor.offlinechallenge.** { *; }
-keepclassmembers class com.analoganchor.offlinechallenge.** { *; }

# Keep Glance AppWidget classes & receivers (Glance 1.0 relies on internal reflection)
-keep class androidx.glance.** { *; }
-keepclassmembers class androidx.glance.** { *; }
-keep class * extends android.appwidget.AppWidgetProvider { *; }

# WorkManager worker reflection support
-keep class androidx.work.** { *; }
-keepclassmembers class androidx.work.** { *; }

# Keep R fields for RemoteViews widget layout reflection
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep SplashScreen library classes
-keep class androidx.core.splashscreen.** { *; }
-dontwarn androidx.core.splashscreen.**



