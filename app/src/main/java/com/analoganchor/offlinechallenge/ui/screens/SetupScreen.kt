package com.analoganchor.offlinechallenge.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.analoganchor.offlinechallenge.R
import com.analoganchor.offlinechallenge.data.ChallengePreferences
import com.analoganchor.offlinechallenge.ui.theme.*

@Composable
fun SetupScreen(onDurationSelected: (Long) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { ChallengePreferences(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
    ) {
        // Language Toggle
        OutlinedButton(
            onClick = {
                prefs.language = if (prefs.language == "ar") "en" else "ar"
                (context as? Activity)?.recreate()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
                .height(44.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanGlow),
            border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.5f))
        ) {
            Text(
                text = if (prefs.language == "ar") "English" else "العربية",
                color = CyanGlow,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 32.sp,
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
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

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
                Button(
                    onClick = { onDurationSelected(ms) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepSurface)
                ) {
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow
                    )
                }
            }
        }
    }
}
