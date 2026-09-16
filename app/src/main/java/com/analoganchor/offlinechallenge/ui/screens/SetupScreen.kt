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
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.analoganchor.offlinechallenge.R
import com.analoganchor.offlinechallenge.data.ChallengePreferences
import com.analoganchor.offlinechallenge.ui.theme.*

@Composable
fun SetupScreen(
    onDurationSelected: (Long) -> Unit,
    onOpenEmergencyModal: () -> Unit = {}
) {
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

            // --- 1. App Branding Hero (At Very Top) ---
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = CyanGlow,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.setup_subtitle),
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --- 2. Attention & Pre-Commitment Philosophy Card (Decide Once for Your Attention) ---
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface.copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val websiteUrl = if (isAr) "https://get-analog-anchor.com/" else "https://get-analog-anchor.com/?lang=en"
                        openUrl(context, websiteUrl)
                    }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.think_about_it_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarning,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val inlineContent = mapOf(
                        "open_icon" to InlineTextContent(
                            Placeholder(
                                width = 11.sp,
                                height = 11.sp,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_open_in_new),
                                contentDescription = null,
                                tint = CyanGlow,
                                modifier = Modifier
                                    .size(10.dp)
                                    .padding(bottom = 1.dp)
                            )
                        }
                    )

                    val thinkAboutItAnnotated = remember(isAr) {
                        buildAnnotatedString {
                            append(if (isAr) "هذا التحدي الأوفلاين هو خطوتك الأولى لاستعادة انتباهك، وفهم ما يقتطعه الاتصال الدائم من حضورك الذهني. بعد انتهاء التحدي، يساعدك " else "This offline challenge is your first step to understand what constant connectivity costs your daily presence. After completing this challenge, ")
                            withStyle(
                                style = SpanStyle(
                                    color = CyanGlow,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(if (isAr) "تطبيق Analog Anchor الرئيسي" else "the main Analog Anchor app")
                                append(" ")
                                appendInlineContent("open_icon", "[↗]")
                            }
                            append(if (isAr) " على بناء تحكّم واعٍ ومباشر في اتصالك — لتبقى في حالة حضور وتفاعل مقصود قبل أن يستحوذ الهاتف على يومك." else " helps you build conscious control over your connection—keeping you intentional before the screen takes over.")
                        }
                    }

                    Text(
                        text = thinkAboutItAnnotated,
                        inlineContent = inlineContent,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Option B: Partner & Group Circle Pill Selector ---
            var isGroupSelected by remember { mutableStateOf(prefs.isGroupSession) }
            var showMainAppRequiredDialog by remember { mutableStateOf(false) }

            fun handleCompanionPillClick(isGroup: Boolean) {
                if (isGroup) {
                    isGroupSelected = true
                    prefs.isGroupSession = true
                    prefs.sessionPoints = 70
                } else {
                    isGroupSelected = false
                    prefs.isGroupSession = false
                    prefs.sessionPoints = 40
                }

                val isInstalled = try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.getPackageInfo(
                            "com.analoganchor.app",
                            android.content.pm.PackageManager.PackageInfoFlags.of(0)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo("com.analoganchor.app", 0)
                    }
                    true
                } catch (e: Exception) {
                    false
                }

                if (isInstalled) {
                    // No need to confirm: immediately open main Analog Anchor app in outdoor tab
                    val launchIntent = context.packageManager.getLaunchIntentForPackage("com.analoganchor.app")
                    if (launchIntent != null) {
                        launchIntent.apply {
                            putExtra("target_tab", "OUTDOOR")
                            putExtra("action", if (isGroup) "join_group" else "join_partner")
                            putExtra("is_group", isGroup)
                            putExtra("points", if (isGroup) 70 else 40)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        context.startActivity(launchIntent)
                    }
                } else {
                    // Main app not installed: show explanatory dialog linking to Google Play
                    showMainAppRequiredDialog = true
                }
            }

            if (showMainAppRequiredDialog) {
                AlertDialog(
                    onDismissRequest = { showMainAppRequiredDialog = false },
                    title = {
                        Text(
                            text = stringResource(R.string.main_app_required_title),
                            color = CyanGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.main_app_required_body),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showMainAppRequiredDialog = false
                                val playUrl = "https://play.google.com/store/apps/details?id=com.analoganchor.app"
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playUrl)).apply {
                                        setPackage("com.android.vending")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.btn_install_main_app),
                                color = Obsidian,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showMainAppRequiredDialog = false }
                        ) {
                            Text(
                                text = if (isAr) "إلغاء" else "Cancel",
                                color = TextSecondary
                            )
                        }
                    },
                    containerColor = DeepSurface,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { handleCompanionPillClick(isGroup = false) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (!isGroupSelected) CyanGlow.copy(alpha = 0.12f) else DeepSurface,
                    border = BorderStroke(1.5.dp, if (!isGroupSelected) CyanGlow else TrackColor)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.mode_duo),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isGroupSelected) CyanGlow else TextSecondary
                        )
                        Text(
                            text = stringResource(R.string.points_duo_badge),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (!isGroupSelected) CyanGlow else TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }

                Surface(
                    onClick = { handleCompanionPillClick(isGroup = true) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isGroupSelected) CyanGlow.copy(alpha = 0.12f) else DeepSurface,
                    border = BorderStroke(1.5.dp, if (isGroupSelected) CyanGlow else TrackColor)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.mode_group),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGroupSelected) CyanGlow else TextSecondary
                        )
                        Text(
                            text = stringResource(R.string.points_group_badge),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGroupSelected) AmberWarning else TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- 3. Section Header: Select Challenge Duration ---
            Text(
                text = stringResource(R.string.select_duration),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // --- 4. Duration Buttons (2m, 12h, 18h, 36h, 72h) ---
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
                        .height(48.dp)
                        .padding(vertical = 3.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTestDuration) DeepSurface.copy(alpha = 0.9f) else DeepSurface
                    ),
                    border = if (isTestDuration) BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f)) else BorderStroke(1.dp, CyanGlow.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTestDuration) AmberWarning else CyanGlow
                    )
                }
            }

            // Universal Emergency Code Access
            TextButton(
                onClick = onOpenEmergencyModal,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_universal_emergency),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberWarning
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 5. Support & Info Card (Moved to Bottom Area) ---
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Website Info
                    val websiteText = remember(isAr) {
                        buildAnnotatedString {
                            append("🌐  ")
                            append(if (isAr) "لمزيد من المعلومات، تفضل بزيارة " else "For more information, visit ")
                            withStyle(
                                style = SpanStyle(
                                    color = CyanGlow,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append("get-analog-anchor.com")
                            }
                        }
                    }

                    Text(
                        text = websiteText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val websiteUrl = if (isAr) "https://get-analog-anchor.com/" else "https://get-analog-anchor.com/?lang=en"
                                openUrl(context, websiteUrl)
                            }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // WhatsApp & SMS Number
                    val whatsappText = remember(isAr) {
                        buildAnnotatedString {
                            append("💬  ")
                            withStyle(
                                style = SpanStyle(
                                    color = Color(0xFF25D366),
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(if (isAr) "واتساب وSMS: " else "WhatsApp & SMS: ")
                                append("\u200E+973 33371163")
                            }
                        }
                    }

                    Text(
                        text = whatsappText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                openUrl(context, "https://wa.me/97333371163")
                            }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Support Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Obsidian.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = stringResource(R.string.support_note),
                            fontSize = 10.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(70.dp)) // Padding for bottom language toggle
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
