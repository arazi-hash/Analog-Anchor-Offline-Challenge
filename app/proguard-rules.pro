# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in getDefaultProguardFile("proguard-android-optimize.txt").

# Keep project classes & members
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

# Keep WorkManager & Room Database generated implementations (prevents WorkDatabase reflection crash)
-keep class androidx.work.** { *; }
-keepclassmembers class androidx.work.** { *; }
-keep class androidx.work.impl.** { *; }
-keepclassmembers class androidx.work.impl.** { *; }
-keep class androidx.room.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.work.**
-dontwarn androidx.room.**
