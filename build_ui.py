import os

base_dir = "c:/Analog Anchor Offline Challenge/app/src/main/java/com/analoganchor/offlinechallenge/ui"
dirs = [
    f"{base_dir}/theme",
    f"{base_dir}/screens"
]

for d in dirs:
    os.makedirs(d, exist_ok=True)

color_kt = """package com.analoganchor.offlinechallenge.ui.theme

import androidx.compose.ui.graphics.Color

val Obsidian = Color(0xFF0B1012)
val DeepSurface = Color(0xFF172126)
val CyanGlow = Color(0xFF7DEEFF)
val CyanGlowVariant = Color(0xFF4DC8DD)
val AmberWarning = Color(0xFFF3B65C)
val TextPrimary = Color(0xFFF3F8FA)
val TextSecondary = Color(0xFFA2AFB6)
val TrackColor = Color(0xFF2B3A40)
"""

theme_kt = """package com.analoganchor.offlinechallenge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    background = Obsidian,
    surface = DeepSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    primary = CyanGlow,
    secondary = CyanGlowVariant,
    error = AmberWarning,
    onPrimary = Obsidian
)

@Composable
fun OfflineChallengeTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
"""

type_kt = """package com.analoganchor.offlinechallenge.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )
)
"""

setup_screen_kt = """package com.analoganchor.offlinechallenge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.analoganchor.offlinechallenge.R
import com.analoganchor.offlinechallenge.ui.theme.*

@Composable
fun SetupScreen(onDurationSelected: (Long) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
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
            text = "يساعدك هذا التحدي على فصل هاتفك والتركيز. اختر مدة الانقطاع التام.",
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
"""

shield_screen_kt = """package com.analoganchor.offlinechallenge.ui.screens

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
"""

challenge_screen_kt = """package com.analoganchor.offlinechallenge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.analoganchor.offlinechallenge.util.PinVault
import com.analoganchor.offlinechallenge.util.TokenDecoder
import kotlinx.coroutines.delay

@Composable
fun ChallengeScreen(
    challengePrefs: ChallengePreferences,
    onEmergencyUnlock: () -> Unit,
    onChallengeComplete: () -> Unit
) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(challengePrefs.getProgress()) }
    var remainingText by remember { mutableStateOf("") }
    var tokenInput by remember { mutableStateOf("") }
    var tokenResult by remember { mutableStateOf("") }
    var requestStep by remember { mutableIntStateOf(challengePrefs.currentRequestStep) }

    LaunchedEffect(Unit) {
        while (challengePrefs.isActive) {
            delay(1000)
            progress = challengePrefs.getProgress()
            requestStep = challengePrefs.currentRequestStep
            val remaining = challengePrefs.getRemainingMillis()
            val totalSec = remaining / 1000
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            remainingText = "${h}h ${m}m ${s}s"

            if (challengePrefs.isExpired()) {
                onChallengeComplete()
                break
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "درع الأوفلاين نشط",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = CyanGlow
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "${(progress * 100).toInt()}٪",
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "الوقت المتبقي: $remainingText",
            fontSize = 18.sp,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            color = CyanGlow,
            trackColor = TrackColor
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Emergency Token Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "فتح طوارئ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberWarning,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it; tokenResult = "" },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("الصق رمز الطوارئ هنا", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanGlow,
                        unfocusedBorderColor = TrackColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Start
                    ),
                    maxLines = 3
                )
                
                Text(
                    text = stringResource(R.string.request_step, requestStep),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                )

                Button(
                    onClick = {
                        val decoded = TokenDecoder.decode(tokenInput.trim())
                        if (decoded == null) {
                            tokenResult = context.getString(R.string.token_rejected)
                        } else {
                            if (decoded.requestNumber != challengePrefs.currentRequestStep) {
                                tokenResult = "الرمز للطلب رقم ${decoded.requestNumber} ولكن المتوقع ${challengePrefs.currentRequestStep}"
                            } else if (!PinVault.verify(decoded.decodedPin, decoded.requestNumber)) {
                                tokenResult = context.getString(R.string.token_rejected)
                            } else {
                                challengePrefs.consumeRequest(decoded.requestNumber)
                                if (decoded.requestNumber < 3) {
                                    challengePrefs.currentRequestStep = decoded.requestNumber + 1
                                }
                                tokenResult = context.getString(R.string.token_accepted)
                                onEmergencyUnlock()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                ) {
                    Text("تحقق من الرمز", fontWeight = FontWeight.Bold, color = Obsidian)
                }

                if (tokenResult.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = tokenResult,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tokenResult.contains("قبول")) CyanGlow else Color(0xFFFF5252)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
"""

completion_screen_kt = """package com.analoganchor.offlinechallenge.ui.screens

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
fun CompletionScreen(onHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "أحسنت!",
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = CyanGlow,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "لقد أكملت التحدي بنجاح واستعدت السيطرة على وقتك. يمكنك الآن استخدام هاتفك بشكل طبيعي.",
            fontSize = 18.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepSurface)
        ) {
            Text(
                text = "العودة للرئيسية",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CyanGlow
            )
        }
    }
}
"""

files = {
    f"{base_dir}/theme/Color.kt": color_kt,
    f"{base_dir}/theme/Theme.kt": theme_kt,
    f"{base_dir}/theme/Type.kt": type_kt,
    f"{base_dir}/screens/SetupScreen.kt": setup_screen_kt,
    f"{base_dir}/screens/ShieldPermissionScreen.kt": shield_screen_kt,
    f"{base_dir}/screens/ChallengeScreen.kt": challenge_screen_kt,
    f"{base_dir}/screens/CompletionScreen.kt": completion_screen_kt
}

for path, content in files.items():
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)

print("Generated UI Compose files successfully.")
