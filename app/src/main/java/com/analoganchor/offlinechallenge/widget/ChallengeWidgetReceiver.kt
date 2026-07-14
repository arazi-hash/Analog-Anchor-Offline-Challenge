package com.analoganchor.offlinechallenge.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChallengeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChallengeWidget()

    companion object {
        fun updateWidget(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                ChallengeWidget().updateAll(context)
            }
        }
    }
}
