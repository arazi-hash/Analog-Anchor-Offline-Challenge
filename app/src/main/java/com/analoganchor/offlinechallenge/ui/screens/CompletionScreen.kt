package com.analoganchor.offlinechallenge.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.analoganchor.offlinechallenge.R
import com.analoganchor.offlinechallenge.data.ChallengePreferences
import com.analoganchor.offlinechallenge.ui.theme.*

@Composable
fun CompletionScreen(
    challengePrefs: ChallengePreferences,
    onHome: () -> Unit
) {
    val context = LocalContext.current
    val isAr = challengePrefs.language == "ar"
    
    val discountCode = challengePrefs.discountCode
    val discountAmount = challengePrefs.discountAmount
    val hasDiscount = !discountCode.isNullOrEmpty() && discountAmount > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.challenge_complete).split("!").first() + "!",
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            color = CyanGlow,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.challenge_complete).split("!").getOrElse(1) { "" }.trim(),
            fontSize = 16.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )
        
        if (hasDiscount) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isAr) "🎁 مكافأة الالتزام!" else "🎁 Commitment Reward!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = if (isAr) {
                            "لقد حصلت على خصم بقيمة $discountAmount% لالتزامك بالتحدي! انسخ الرمز أدناه لاستخدامه في موقعنا، أو شاركه معنا مباشرة عبر الواتساب لتفعيل خصمك."
                        } else {
                            "You earned a $discountAmount% discount for completing the challenge! Copy the code below to use on our website, or share it with us directly on WhatsApp to claim your discount."
                        },
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Code Display Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Obsidian, RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, TrackColor), RoundedCornerShape(10.dp))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = discountCode ?: "",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Go to Website Button (Primary action - Full Width)
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://get-analog-anchor.com"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow)
                    ) {
                        Text(
                            text = if (isAr) "🌐 انتقال للموقع الإلكتروني" else "🌐 Go to Website",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Obsidian
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Row with Copy and WhatsApp buttons (Backup / Secondary actions)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Copy Button
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Discount Code", discountCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(
                                    context,
                                    if (isAr) "تم نسخ الرمز!" else "Code copied!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TrackColor)
                        ) {
                            Text(
                                text = if (isAr) "نسخ الرمز" else "Copy Code",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        
                        // WhatsApp Share Button
                        Button(
                            onClick = {
                                val whatsappMsg = if (isAr) {
                                    "أهلاً، لقد أكملت تحدي الأوفلاين بنجاح وحصلت على رمز الخصم: $discountCode"
                                } else {
                                    "Hello, I successfully completed the offline challenge and earned my discount code: $discountCode"
                                }
                                val whatsappUrl = "https://wa.me/97333371163?text=" + Uri.encode(whatsappMsg)
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        if (isAr) "لم نتمكن من فتح واتساب" else "Could not open WhatsApp",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepSurface),
                            border = BorderStroke(1.dp, Color(0xFF25D366))
                        ) {
                            Text(
                                text = if (isAr) "💬 الواتساب" else "💬 WhatsApp",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF25D366)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(if (hasDiscount) 8.dp else 40.dp))
        
        Button(
            onClick = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepSurface)
        ) {
            Text(
                text = if (isAr) "العودة للرئيسية" else "Back to Setup",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CyanGlow
            )
        }
    }
}
