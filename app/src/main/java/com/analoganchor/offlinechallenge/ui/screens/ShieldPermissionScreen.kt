package com.analoganchor.offlinechallenge.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.analoganchor.offlinechallenge.R
import com.analoganchor.offlinechallenge.ui.theme.*

@Composable
fun ShieldPermissionScreen(onActivate: (pin: String) -> Unit) {
    val context = LocalContext.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val view = androidx.compose.ui.platform.LocalView.current
    var pinText by remember { mutableStateOf("") }
    val isPinValid = pinText.length in 3..4

    var isDefenseReady by remember { mutableStateOf(false) }
    val canLockAndStart = isPinValid && isDefenseReady
    var showDefenseWarning by remember { mutableStateOf(false) }

    LaunchedEffect(showDefenseWarning) {
        if (showDefenseWarning) {
            kotlinx.coroutines.delay(2500)
            showDefenseWarning = false
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.disclosure_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.disclosure_body),
                        fontSize = 13.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- 🎲 Forget-Me Commitment PIN Section ---
                    // --- 🎲 Forget-Me Commitment PIN Section ---
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Obsidian.copy(alpha = 0.8f)),
                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val shareMsg = stringResource(R.string.share_commitment_message)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.pin_setup_title),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarning
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Surface(
                                    onClick = {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            putExtra(Intent.EXTRA_TEXT, shareMsg)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF0C2B1A),
                                    border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.8f)),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_share_nodes),
                                            contentDescription = "Share Status",
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.share_pill_label),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF25D366)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            painter = painterResource(R.drawable.ic_open_in_new),
                                            contentDescription = null,
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = stringResource(R.string.pin_setup_hint),
                                fontSize = 10.5.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            LaunchedEffect(pinText.length) {
                                if (pinText.length >= 4) {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    val imm = view.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                                    imm?.hideSoftInputFromWindow(view.windowToken, 0)
                                }
                            }

                            OutlinedTextField(
                                value = pinText,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }
                                    if (filtered.length <= 4) {
                                        pinText = filtered
                                    }
                                },
                                readOnly = pinText.length >= 4,
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(40.dp),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = TextPrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AmberWarning,
                                    unfocusedBorderColor = AmberWarning.copy(alpha = 0.5f),
                                    cursorColor = AmberWarning
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // --- Merged Recommendations: Pre-Challenge Downloads & NFC Payments ---
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Obsidian.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = stringResource(R.string.tip_no_excuse_title),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanGlow,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )

                            val mapsUrl = "https://youtube.com/shorts/Oook3GAojyA?si=v_fdVp-eJcXzhkI8"
                            val translateUrl = "https://youtube.com/shorts/DdAK5ydqa_w?si=rhV2oyuuDzvLIbjl"
                            val aiGalleryUrl = "https://play.google.com/store/apps/details?id=com.google.ai.edge.gallery&pcampaignid=web_share"

                            val linkStyle = TextLinkStyles(
                                style = SpanStyle(
                                    color = CyanGlow,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )

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

                            val annotatedEcosystem = buildAnnotatedString {
                                append(stringResource(R.string.eco_prefix))
                                withLink(LinkAnnotation.Url(mapsUrl, linkStyle)) {
                                    append(stringResource(R.string.eco_maps))
                                    append(" ")
                                    appendInlineContent("open_icon", "[↗]")
                                }
                                append(stringResource(R.string.eco_mid1))
                                withLink(LinkAnnotation.Url(translateUrl, linkStyle)) {
                                    append(stringResource(R.string.eco_translate))
                                    append(" ")
                                    appendInlineContent("open_icon", "[↗]")
                                }
                                append(stringResource(R.string.eco_mid2))
                                withLink(LinkAnnotation.Url(aiGalleryUrl, linkStyle)) {
                                    append(stringResource(R.string.eco_ai))
                                    append(" ")
                                    appendInlineContent("open_icon", "[↗]")
                                }
                                append(stringResource(R.string.eco_suffix))
                            }

                            Text(
                                text = annotatedEcosystem,
                                inlineContent = inlineContent,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = stringResource(R.string.tip_nfc_assurance),
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- Background Shield Defense ---
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Obsidian.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, if (isDefenseReady) CyanGlow.copy(alpha = 0.4f) else AmberWarning.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = stringResource(R.string.tip_autostart_title),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDefenseReady) CyanGlow else AmberWarning,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            )

                            Text(
                                text = stringResource(R.string.tip_autostart_body),
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )

                            if (!isDefenseReady) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        isDefenseReady = true
                                        showDefenseWarning = false
                                        com.analoganchor.offlinechallenge.util.AutostartHelper.openAutostartSettings(context)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                                ) {
                                    Text(
                                        text = stringResource(R.string.tip_autostart_button),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Obsidian
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (canLockAndStart) {
                                onActivate(pinText)
                            } else if (!isDefenseReady) {
                                showDefenseWarning = true
                            } else if (!isPinValid) {
                                val isAr = com.analoganchor.offlinechallenge.data.ChallengePreferences(context).language == "ar"
                                android.widget.Toast.makeText(
                                    context,
                                    if (isAr) "يرجى تحديد رمز الالتزام أولاً (3 أو 4 أرقام)" else "Please set your commitment PIN first (3 or 4 digits)",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canLockAndStart) {
                                CyanGlow
                            } else if (showDefenseWarning) {
                                AmberWarning.copy(alpha = 0.2f)
                            } else {
                                CyanGlow.copy(alpha = 0.2f)
                            }
                        ),
                        border = if (!canLockAndStart && showDefenseWarning) {
                            BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f))
                        } else null
                    ) {
                        Text(
                            text = if (showDefenseWarning) {
                                stringResource(R.string.tip_autostart_required_btn)
                            } else {
                                stringResource(R.string.pin_lock_start)
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canLockAndStart) {
                                Obsidian
                            } else if (showDefenseWarning) {
                                AmberWarning
                            } else {
                                TextSecondary.copy(alpha = 0.6f)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}



