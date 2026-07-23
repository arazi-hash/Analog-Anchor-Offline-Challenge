# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in getDefaultProguardFile("proguard-android-optimize.txt").

# Keep all project classes & members
-keep class com.analoganchor.offlinechallenge.** { *; }
-keepclassmembers class com.analoganchor.offlinechallenge.** { *; }

# Keep Glance AppWidget classes & receivers
-keep class androidx.glance.** { *; }
-keepclassmembers class androidx.glance.** { *; }
-keep class * extends android.appwidget.AppWidgetProvider { *; }

# Keep Navigation and AndroidX Core lifecycle components
-keep class androidx.navigation.** { *; }
-keep class androidx.core.** { *; }
-keep class androidx.compose.** { *; }
