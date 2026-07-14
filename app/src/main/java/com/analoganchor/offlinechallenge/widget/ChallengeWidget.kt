package com.analoganchor.offlinechallenge.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.analoganchor.offlinechallenge.MainActivity
import com.analoganchor.offlinechallenge.data.ChallengePreferences
import androidx.glance.unit.ColorProvider

import android.content.Intent

class ChallengeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = ChallengePreferences(context)
            val isActive = prefs.isActive
            val remainingMillis = prefs.getRemainingMillis()
            val progress = prefs.getProgress()

            val bgDark = Color(0xFF0B1012)
            val textCyan = Color(0xFF7DEEFF)
            val textAmber = Color(0xFFF3B65C)

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgDark)
                    .padding(16.dp)
                    .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isActive) {
                    val totalSeconds = remainingMillis / 1000
                    val hours = totalSeconds / 3600
                    val minutes = (totalSeconds % 3600) / 60
                    val remainingText = "${hours}h ${minutes}m"
                    val percent = (progress * 100).toInt()

                    Text(
                        text = "التحدي نشط",
                        style = TextStyle(
                            color = ColorProvider(textAmber),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    
                    Text(
                        text = remainingText,
                        style = TextStyle(
                            color = ColorProvider(textCyan),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    Text(
                        text = "$percent%",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Text(
                        text = "لا يوجد تحدي نشط",
                        style = TextStyle(
                            color = ColorProvider(Color.Gray),
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = "انقر للبدء",
                        style = TextStyle(
                            color = ColorProvider(textCyan),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
