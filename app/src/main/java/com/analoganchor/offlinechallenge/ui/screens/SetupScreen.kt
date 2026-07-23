package com.analoganchor.offlinechallenge.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.analoganchor.offlinechallenge.R
import com.analoganchor.offlinechallenge.data.ChallengePreferences
import com.analoganchor.offlinechallenge.ui.theme.*

@Composable
fun SetupScreen(onDurationSelected: (Long) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { ChallengePreferences(context) }
    val isAr = prefs.language == "ar"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- Upper Section: Professional Support & Info Header Card ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Line 1: Website Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.clickable {
                            val websiteUrl = if (isAr) "https://get-analog-anchor.com/" else "https://get-analog-anchor.com/?lang=en"
                            openUrl(context, websiteUrl)
                        }
                    ) {
                        Text(
                            text = "🌐  ",
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isAr) "لمزيد من المعلومات، تفضل بزيارة " else "For more information, visit ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "get-analog-anchor.com",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow,
                            textAlign = TextAlign.Center,
                            textDecoration = TextDecoration.Underline
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Line 2: WhatsApp Number with forced LTR country code formatting
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.clickable {
                            openUrl(context, "https://wa.me/97333371163")
                        }
                    ) {
                        Text(
                            text = "💬  ",
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isAr) "واتساب فقط: " else "WhatsApp only: ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF25D366),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "\u200E+973 33371163",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF25D366),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Line 3: WhatsApp Support Notice (Messages Only)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Obsidian.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = stringResource(R.string.support_note),
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- App Branding Hero ---
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = CyanGlow,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.setup_subtitle),
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // --- Duration Selector Section ---
            Text(
                text = stringResource(R.string.select_duration),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val durations = listOf(
                2 * 60 * 1000L to stringResource(R.string.duration_test),
                12 * 60 * 60 * 1000L to stringResource(R.string.duration_12h),
                18 * 60 * 60 * 1000L to stringResource(R.string.duration_18h),
                36 * 60 * 60 * 1000L to stringResource(R.string.duration_36h),
                72 * 60 * 60 * 1000L to stringResource(R.string.duration_72h)
            )

            durations.forEach { (ms, label) ->
                val isTestDuration = ms == 2 * 60 * 1000L
                Button(
                    onClick = { onDurationSelected(ms) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 5.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTestDuration) DeepSurface.copy(alpha = 0.9f) else DeepSurface
                    ),
                    border = if (isTestDuration) BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f)) else BorderStroke(1.dp, CyanGlow.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTestDuration) AmberWarning else CyanGlow
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Padding before bottom language toggle
        }

        // --- Bottom Section: Language Switcher Toggle ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            OutlinedButton(
                onClick = {
                    prefs.language = if (isAr) "en" else "ar"
                    (context as? Activity)?.recreate()
                },
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Obsidian.copy(alpha = 0.9f),
                    contentColor = CyanGlow
                ),
                border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.6f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🌐",
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isAr) "English" else "العربية",
                        color = CyanGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Safely handle if no browser app is installed
    }
}
