package com.analoganchor.offlinechallenge

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.analoganchor.offlinechallenge.data.ChallengePreferences
import com.analoganchor.offlinechallenge.service.MyVpnService
import com.analoganchor.offlinechallenge.util.PinVault
import com.analoganchor.offlinechallenge.util.TokenDecoder
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var challengePrefs: ChallengePreferences
    private var pendingDurationMs: Long = 0L

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        challengePrefs = ChallengePreferences(this)

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        background = Color(0xFF0B1012),
                        surface = Color(0xFF172126),
                        onBackground = Color(0xFFF3F8FA),
                        onSurface = Color(0xFFF3F8FA),
                        primary = Color(0xFF7DEEFF)
                    )
                ) {
                    DebugScreen()
                }
            }
        }
    }

    @Composable
    fun DebugScreen() {
        val context = LocalContext.current
        var vpnRunning by remember { mutableStateOf(MyVpnService.isRunning) }
        var challengeActive by remember { mutableStateOf(challengePrefs.isActive) }
        var tokenInput by remember { mutableStateOf("") }
        var tokenResult by remember { mutableStateOf("") }
        var progress by remember { mutableFloatStateOf(challengePrefs.getProgress()) }
        var remainingText by remember { mutableStateOf("") }
        var requestStep by remember { mutableIntStateOf(challengePrefs.currentRequestStep) }

        // Periodic refresh
        LaunchedEffect(challengeActive) {
            while (challengeActive) {
                delay(1000)
                progress = challengePrefs.getProgress()
                vpnRunning = MyVpnService.isRunning
                requestStep = challengePrefs.currentRequestStep
                val remaining = challengePrefs.getRemainingMillis()
                val totalSec = remaining / 1000
                val h = totalSec / 3600
                val m = (totalSec % 3600) / 60
                val s = totalSec % 60
                remainingText = "${h}h ${m}m ${s}s"

                if (challengePrefs.isExpired()) {
                    challengePrefs.endChallenge()
                    challengeActive = false
                    stopVpnService()
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1012))
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // Title
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF7DEEFF),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Phase 1 — Debug UI",
                fontSize = 14.sp,
                color = Color(0xFFA2AFB6),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF172126))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = if (vpnRunning) stringResource(R.string.vpn_active)
                               else stringResource(R.string.vpn_inactive),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (vpnRunning) Color(0xFF7DEEFF) else Color(0xFFF3B65C)
                    )

                    if (challengeActive) {
                        Spacer(Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Color(0xFF7DEEFF),
                            trackColor = Color(0xFF2B3A40)
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "${(progress * 100).toInt()}% — $remainingText",
                            fontSize = 14.sp,
                            color = Color(0xFFA2AFB6),
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = stringResource(R.string.request_step, requestStep),
                            fontSize = 12.sp,
                            color = Color(0xFFA2AFB6),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Duration Buttons (only show when no active challenge)
            if (!challengeActive) {
                Text(
                    text = stringResource(R.string.select_duration),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF3F8FA),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val durations = listOf(
                    2 * 60 * 1000L to stringResource(R.string.duration_test),
                    12 * 60 * 60 * 1000L to stringResource(R.string.duration_12h),
                    18 * 60 * 60 * 1000L to stringResource(R.string.duration_18h),
                    24 * 60 * 60 * 1000L to stringResource(R.string.duration_24h),
                    36 * 60 * 60 * 1000L to stringResource(R.string.duration_36h)
                )

                durations.forEach { (durationMs, label) ->
                    Button(
                        onClick = {
                            pendingDurationMs = durationMs
                            requestVpnPermission()
                            challengePrefs.startChallenge(durationMs)
                            challengeActive = true
                            requestStep = 1
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4DC8DD)
                        )
                    ) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B1012)
                        )
                    }
                }
            }

            // Emergency Token Section (always visible)
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF172126))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.emergency_unlock),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF3B65C),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = {
                            tokenInput = it
                            tokenResult = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                stringResource(R.string.token_hint),
                                color = Color(0xFF4E5C62)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7DEEFF),
                            unfocusedBorderColor = Color(0xFF2B3A40),
                            focusedTextColor = Color(0xFFF3F8FA),
                            unfocusedTextColor = Color(0xFFF3F8FA),
                            cursorColor = Color(0xFF7DEEFF)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Start,
                            // Force LTR for token input since tokens are Latin characters
                            // This is done via layoutDirection override below
                        ),
                        maxLines = 3
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val decoded = TokenDecoder.decode(tokenInput.trim())
                            if (decoded == null) {
                                tokenResult = context.getString(R.string.token_rejected)
                            } else {
                                val expectedRequest = challengePrefs.currentRequestStep
                                if (decoded.requestNumber != expectedRequest) {
                                    tokenResult = "Wrong request number. Expected: $expectedRequest, got: ${decoded.requestNumber}"
                                } else if (!PinVault.verify(decoded.decodedPin, decoded.requestNumber)) {
                                    tokenResult = context.getString(R.string.token_rejected)
                                } else {
                                    // SUCCESS — consume this request and stop VPN
                                    challengePrefs.consumeRequest(decoded.requestNumber)
                                    if (decoded.requestNumber < 3) {
                                        challengePrefs.currentRequestStep = decoded.requestNumber + 1
                                    }
                                    challengePrefs.endChallenge()
                                    challengeActive = false
                                    stopVpnService()
                                    tokenResult = context.getString(R.string.token_accepted)
                                    tokenInput = ""
                                    requestStep = challengePrefs.currentRequestStep
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF3B65C)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.verify_token),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B1012)
                        )
                    }

                    if (tokenResult.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = tokenResult,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tokenResult.contains("accepted") || tokenResult.contains("قبول"))
                                Color(0xFF7DEEFF) else Color(0xFFFF5252)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    private fun requestVpnPermission() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            // Permission already granted
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_START
        }
        startForegroundService(intent)
    }

    private fun stopVpnService() {
        val intent = Intent(this, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_STOP
        }
        startService(intent)
    }
}
