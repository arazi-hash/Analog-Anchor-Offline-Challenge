package com.analoganchor.offlinechallenge.ui.screens

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
import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
    onChallengeComplete: () -> Unit,
    onOpenVpnSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isAr = challengePrefs.language == "ar"
    var progress by remember { mutableFloatStateOf(challengePrefs.getProgress()) }
    var remainingText by remember { mutableStateOf("") }
    var tokenInput by remember { mutableStateOf("") }
    var tokenResult by remember { mutableStateOf("") }
    var requestStep by remember { mutableIntStateOf(challengePrefs.currentRequestStep) }
    var isWifiOrDataOn by remember { mutableStateOf(false) }
    var hasPassedHalfMinute by remember { mutableStateOf(false) }

    var showPartnerPromptDialog by remember { mutableStateOf(false) }
    var hasPromptedPartnerHour by remember { mutableStateOf(false) }
    var partnerElapsedMinutes by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            val active = challengePrefs.isActive
            val pendingShow = challengePrefs.isCompletedPendingShow

            if (pendingShow) {
                onChallengeComplete()
                break
            }
            if (!active) {
                onEmergencyUnlock()
                break
            }

            progress = challengePrefs.getProgress()
            requestStep = challengePrefs.currentRequestStep
            val remaining = challengePrefs.getRemainingMillis()
            val totalSec = remaining / 1000
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            remainingText = "${h}h ${m}m ${s}s"

            val elapsed = System.currentTimeMillis() - challengePrefs.startTimeMillis
            val passedHalfMin = elapsed >= 30 * 1000L
            hasPassedHalfMinute = passedHalfMin
            if (passedHalfMin) {
                isWifiOrDataOn = com.analoganchor.offlinechallenge.util.NetworkHelper.isWifiOrDataActive(context)
            }

            if (challengePrefs.isPartnerSession) {
                val elapsedMins = (elapsed / 60000L).toInt()
                partnerElapsedMinutes = elapsedMins
                if (!hasPromptedPartnerHour && (elapsedMins >= 60 || challengePrefs.isExpired() || challengePrefs.isHoldingOffline)) {
                    hasPromptedPartnerHour = true
                    showPartnerPromptDialog = true
                }
            } else {
                if (challengePrefs.isExpired()) {
                    onChallengeComplete()
                    break
                }
            }
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = stringResource(R.string.shield_active),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = CyanGlow
        )

        // Partner / Group Sanctuary Card
        if (challengePrefs.isPartnerSession) {
            val canConclude = partnerElapsedMinutes >= 60 || challengePrefs.isExpired() || challengePrefs.isHoldingOffline
            val minutesLeft = (60 - partnerElapsedMinutes).coerceAtLeast(0)
            val isGroup = challengePrefs.isGroupSession
            val targetPoints = challengePrefs.sessionPoints

            Spacer(modifier = Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.5.dp, CyanGlow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = if (isGroup) "👨‍👩‍👧‍👦" else "🤝", fontSize = 20.sp)
                        Text(
                            text = if (isGroup) stringResource(R.string.group_circle_header) else stringResource(R.string.partner_outing_header),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isAr) "مع: ${challengePrefs.partnerName}" else "With: ${challengePrefs.partnerName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isGroup) stringResource(R.string.group_bonus_desc) else stringResource(R.string.duo_desc),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    if (!canConclude) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AmberWarning.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.time_left_to_claim, minutesLeft, targetPoints),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarning,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isAr) "درع الحظر نشط لمنع استخدام الهاتف أثناء اللقاء"
                                    else "Shield is actively maintaining digital presence",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { showPartnerPromptDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanGlow)
                        ) {
                            Text(
                                text = if (isAr) "🎯 خيارات إنهاء أو تمديد الجلسة (+$targetPoints نقطة)" else "🎯 Conclude or Extend (+$targetPoints PTS)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Obsidian
                            )
                        }
                    }
                }
            }
        }

        // 1-Hour Partner Milestone Prompt Modal (Shield Holds Offline until explicit choice)
        if ((showPartnerPromptDialog || challengePrefs.isHoldingOffline) && challengePrefs.isPartnerSession) {
            val isGroup = challengePrefs.isGroupSession
            val targetPoints = challengePrefs.sessionPoints
            AlertDialog(
                onDismissRequest = { /* Non-dismissable: user must decide to extend or conclude */ },
                title = {
                    Text(
                        text = stringResource(R.string.milestone_completed_title),
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow
                    )
                },
                text = {
                    Text(
                        text = if (isGroup) stringResource(R.string.milestone_completed_body_group) else stringResource(R.string.milestone_completed_body_duo),
                        color = TextPrimary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPartnerPromptDialog = false
                            challengePrefs.isHoldingOffline = false
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.session_concluded_toast),
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            onChallengeComplete()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_conclude_online),
                            color = Obsidian,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                challengePrefs.extendChallenge(30 * 60 * 1000L)
                                showPartnerPromptDialog = false
                                hasPromptedPartnerHour = false
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.session_extended_toast),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            border = BorderStroke(1.dp, CyanGlow),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(stringResource(R.string.btn_extend_30m), color = CyanGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                challengePrefs.extendChallenge(1 * 3600 * 1000L)
                                showPartnerPromptDialog = false
                                hasPromptedPartnerHour = false
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.session_extended_toast),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            border = BorderStroke(1.dp, CyanGlow),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(stringResource(R.string.btn_extend_1h), color = CyanGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                challengePrefs.extendChallenge(2 * 3600 * 1000L)
                                showPartnerPromptDialog = false
                                hasPromptedPartnerHour = false
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.session_extended_toast),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            border = BorderStroke(1.dp, CyanGlow),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(stringResource(R.string.btn_extend_2h), color = CyanGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                },
                containerColor = DeepSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Always-On VPN reminder banner
        if (!challengePrefs.isAlwaysOnVpnActivated) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Obsidian.copy(alpha = 0.9f)),
                border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenVpnSettings() }
            ) {
                Text(
                    text = stringResource(R.string.always_on_banner),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberWarning,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (isAr) "${(progress * 100).toInt()}٪" else "${(progress * 100).toInt()}%",
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "${stringResource(R.string.time_remaining)} $remainingText",
            fontSize = 16.sp,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            color = CyanGlow,
            trackColor = TrackColor
        )
        
        Spacer(modifier = Modifier.height(20.dp))

        // Reassurance & Battery Saving Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurface.copy(alpha = 0.8f)),
            border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                val reassuranceText = remember(isAr) {
                    buildAnnotatedString {
                        append(if (isAr) "المكالمات الهاتفية والرسائل النصية القصيرة SMS تعمل بشكل طبيعي.. " else "Phone calls and SMS messages work normally. ")
                        withStyle(
                            style = SpanStyle(
                                color = CyanGlow,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(if (isAr) "من يتصل بك الآن هو شخص يحتاجك فعلاً وليس لمجرد تضييع الوقت." else "Anyone reaching out right now is someone who genuinely needs you.")
                        }
                        append(if (isAr) " استرخِ واستمتع بصفاء ذهنك!" else " Relax and embrace your uninterrupted focus!")
                        append("\n\n")
                        append(if (isAr) "لتوفير البطارية: نقترح إيقاف الواي فاي وبيانات الهاتف يدوياً؛ فالدرع يحجب الإنترنت بالكامل وإيقافهما يمنع استنزاف البطارية في البحث المستمر عن شبكات." else "Battery Saving Tip: We suggest manually turning off Wi-Fi and Mobile Data to conserve battery power, as the shield blocks internet access anyway.")
                    }
                }

                Text(
                    text = reassuranceText,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                // Half-Minute Wi-Fi / Mobile Data Reminder (Only if 30 seconds elapsed)
                if (hasPassedHalfMinute) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isWifiOrDataOn) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AmberWarning.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.battery_reminder_inapp_warning),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CyanGlow.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.battery_reminder_inapp_ok),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = CyanGlow,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Emergency Token Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.emergency_unlock),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberWarning,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it; tokenResult = "" },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.token_hint), color = TextSecondary) },
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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        autoCorrectEnabled = false
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    )
                )
                
                Text(
                    text = stringResource(R.string.request_step, requestStep),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                )

                Button(
                    onClick = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        try {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            (context as? Activity)?.currentFocus?.let { v ->
                                imm?.hideSoftInputFromWindow(v.windowToken, 0)
                            }
                            (context as? Activity)?.window?.decorView?.let { dv ->
                                imm?.hideSoftInputFromWindow(dv.windowToken, 0)
                            }
                        } catch (_: Exception) {}

                        val rawInput = tokenInput.trim()
                        tokenInput = "" // Clear input field immediately so code does not remain visible

                        val decoded = TokenDecoder.decode(rawInput)
                        if (decoded == null) {
                            tokenResult = context.getString(R.string.token_rejected)
                        } else {
                            if (decoded.requestNumber != challengePrefs.currentRequestStep) {
                                tokenResult = String.format(context.getString(R.string.token_rejected))
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
                    Text(stringResource(R.string.verify_token), fontWeight = FontWeight.Bold, color = Obsidian)
                }

                if (tokenResult.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = tokenResult,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tokenResult.contains("قبول") || tokenResult == context.getString(R.string.token_accepted)) CyanGlow else Color(0xFFFF5252)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

