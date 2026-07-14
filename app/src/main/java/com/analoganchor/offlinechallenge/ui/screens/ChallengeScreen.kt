package com.analoganchor.offlinechallenge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
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
