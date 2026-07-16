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
                        text = if (isAr) "🎁 خطوات الحصول على الخصم" else "🎁 How to Claim Your Discount",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // --- STEP 1 ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Circular Number Badge
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(CyanGlow.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                .border(BorderStroke(1.dp, CyanGlow), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isAr) "١" else "1",
                                color = CyanGlow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAr) "إرسال إثبات السكرين تايم" else "Send Screen Time Proof",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = if (isAr) {
                                    "للحفاظ على مصداقية التحدي ومكافأة الملتزمين فعلياً، يرجى إرسال لقطة شاشة لإحصاءات وقت الشاشة عبر واتساب للتحقق."
                                } else {
                                    "To keep the challenge fair for everyone, please send us a screenshot of your screen time stats via WhatsApp to verify your success."
                                },
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
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
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepSurface),
                                border = BorderStroke(1.dp, Color(0xFF25D366))
                            ) {
                                Text(
                                    text = if (isAr) "💬 إرسال وقت الشاشة عبر واتساب" else "💬 Share Screen Time via WhatsApp",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF25D366)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // --- STEP 2 ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Circular Number Badge
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(CyanGlow.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                .border(BorderStroke(1.dp, CyanGlow), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isAr) "٢" else "2",
                                color = CyanGlow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAr) "تفعيل رمز الخصم" else "Apply Your Discount Code",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = if (isAr) {
                                    "تفضل بزيارة موقعنا لشراء مرساة الأنالوج وتطبيق رمز الخصم الحصري الخاص بك."
                                } else {
                                    "Visit our store to purchase your Analog Anchor and apply your exclusive coupon code."
                                },
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Code Display Box & Copy Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .background(Obsidian, RoundedCornerShape(10.dp))
                                        .border(BorderStroke(1.dp, TrackColor), RoundedCornerShape(10.dp))
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = discountCode ?: "",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanGlow,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 1.sp
                                    )
                                }
                                
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
                                        .height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TrackColor)
                                ) {
                                    Text(
                                        text = if (isAr) "نسخ الرمز" else "Copy Code",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Go to Website Button
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://get-analog-anchor.com"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow)
                            ) {
                                Text(
                                    text = if (isAr) "🌐 زيارة الموقع الإلكتروني" else "🌐 Visit Website",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Obsidian
                                )
                            }
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
