package com.analoganchor.offlinechallenge.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.analoganchor.offlinechallenge.MainActivity
import com.analoganchor.offlinechallenge.data.ChallengePreferences

class ChallengeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = ChallengePreferences(context)
            val isActive = prefs.isActive
            val remainingMillis = prefs.getRemainingMillis()
            val progress = prefs.getProgress()

            // Semi-transparent dark background for clean wallpaper blending
            val bgTransparentDark = Color(0x660B1012)
            val textCyan = Color(0xFF7DEEFF)
            val textAmber = Color(0xFFF3B65C)

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgTransparentDark)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isActive) {
                    val percent = (progress * 100).toInt()
                    val totalSeconds = remainingMillis / 1000
                    val hours = totalSeconds / 3600
                    val minutes = (totalSeconds % 3600) / 60
                    val seconds = totalSeconds % 60
                    val isAr = prefs.language == "ar"

                    val timeStr = if (hours > 0) {
                        "${hours}h ${minutes}m ${seconds}s"
                    } else if (minutes > 0) {
                        "${minutes}m ${seconds}s"
                    } else {
                        "${seconds}s"
                    }

                    val titleText = if (isAr) "تحدي الأوفلاين – %$percent" else "Offline Challenge – $percent%"
                    val timeLabel = if (isAr) "الوقت المتبقي: $timeStr" else "Remaining: $timeStr"

                    Text(
                        text = titleText,
                        style = TextStyle(
                            color = ColorProvider(textCyan),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                        color = ColorProvider(textAmber),
                        backgroundColor = ColorProvider(Color(0x44FFFFFF))
                    )

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    Text(
                        text = timeLabel,
                        style = TextStyle(
                            color = ColorProvider(Color(0xEEFFFFFF)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                } else {
                    val isAr = prefs.language == "ar"
                    Text(
                        text = if (isAr) "⚓ تحدي الأوفلاين" else "⚓ Offline Challenge",
                        style = TextStyle(
                            color = ColorProvider(textCyan),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
