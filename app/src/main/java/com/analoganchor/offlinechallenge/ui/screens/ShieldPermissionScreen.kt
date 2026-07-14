package com.analoganchor.offlinechallenge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    text = "جارٍ تفعيل درع الأوفلاين...",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanGlow,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "يستخدم هذا التطبيق طبقة حماية محلية لتنفيذ التزامك بالتحدي. بياناتك لا تغادر جهازك أبداً. يرجى الموافقة على الرسالة التالية من النظام لتفعيل الدرع.",
                    fontSize = 15.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onActivate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow)
                ) {
                    Text(
                        text = "تفعيل الدرع",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Obsidian
                    )
                }
            }
        }
    }
}
