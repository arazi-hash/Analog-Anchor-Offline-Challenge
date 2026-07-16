package com.analoganchor.offlinechallenge.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.analoganchor.offlinechallenge.R
import com.analoganchor.offlinechallenge.ui.theme.*

@Composable
fun ShieldPermissionScreen(onActivate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.shield_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanGlow,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = stringResource(R.string.shield_body),
                    fontSize = 15.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val isAr = java.util.Locale.getDefault().language == "ar"
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Obsidian.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isAr) {
                            "🔋 نصيحة للبطارية:\nنقترح إيقاف الواي فاي وبيانات الهاتف يدوياً الآن لتوفير طاقة البطارية أثناء فترة التحدي."
                        } else {
                            "🔋 Battery Saving Tip:\nWe suggest manually turning off Wi-Fi and Mobile Data now to save battery during the challenge period."
                        },
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onActivate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow)
                ) {
                    Text(
                        text = stringResource(R.string.shield_activate),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Obsidian
                    )
                }
            }
        }
    }
}
