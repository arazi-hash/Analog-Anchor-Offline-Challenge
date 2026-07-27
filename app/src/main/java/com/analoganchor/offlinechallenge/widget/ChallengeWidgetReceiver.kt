package com.analoganchor.offlinechallenge.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.analoganchor.offlinechallenge.MainActivity
import com.analoganchor.offlinechallenge.R
import com.analoganchor.offlinechallenge.data.ChallengePreferences

class ChallengeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChallengeWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidget(context)
    }

    companion object {
        fun updateWidget(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, ChallengeWidgetReceiver::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

                if (appWidgetIds == null || appWidgetIds.isEmpty()) return

                val prefs = ChallengePreferences(context)
                val isActive = prefs.isActive
                val remainingMillis = prefs.getRemainingMillis()
                val progress = prefs.getProgress()
                val isAr = prefs.language == "ar"

                val views = RemoteViews(context.packageName, R.layout.widget_challenge_layout)

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                if (isActive) {
                    val percent = (progress * 100).toInt()
                    val totalSeconds = remainingMillis / 1000
                    val hours = totalSeconds / 3600
                    val minutes = (totalSeconds % 3600) / 60
                    val seconds = totalSeconds % 60

                    val timeStr = if (hours > 0) {
                        "${hours}h ${minutes}m ${seconds}s"
                    } else if (minutes > 0) {
                        "${minutes}m ${seconds}s"
                    } else {
                        "${seconds}s"
                    }

                    val titleText = if (isAr) "تحدي الأوفلاين – %$percent" else "Offline Challenge – $percent%"
                    val timeLabel = if (isAr) "الوقت المتبقي: $timeStr" else "Remaining: $timeStr"

                    views.setTextViewText(R.id.widget_title, titleText)
                    views.setProgressBar(R.id.widget_progress, 1000, (progress * 1000).toInt(), false)
                    views.setTextViewText(R.id.widget_remaining, timeLabel)
                    views.setViewVisibility(R.id.widget_progress, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_remaining, View.VISIBLE)
                } else {
                    views.setTextViewText(R.id.widget_title, if (isAr) "⚓ تحدي الأوفلاين" else "⚓ Offline Challenge")
                    views.setViewVisibility(R.id.widget_progress, View.GONE)
                    views.setViewVisibility(R.id.widget_remaining, View.GONE)
                }

                appWidgetManager.updateAppWidget(appWidgetIds, views)
            } catch (e: Exception) {
                // Protection against widget update exceptions
            }
        }
    }
}
