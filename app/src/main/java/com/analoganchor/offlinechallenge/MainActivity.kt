package com.analoganchor.offlinechallenge

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.analoganchor.offlinechallenge.data.ChallengePreferences
import com.analoganchor.offlinechallenge.service.DeviceAdminReceiver
import com.analoganchor.offlinechallenge.service.MyVpnService
import com.analoganchor.offlinechallenge.service.NetworkGuard
import com.analoganchor.offlinechallenge.service.VpnGuardWorker
import com.analoganchor.offlinechallenge.ui.screens.ChallengeScreen
import com.analoganchor.offlinechallenge.ui.screens.CompletionScreen
import com.analoganchor.offlinechallenge.ui.screens.SetupScreen
import com.analoganchor.offlinechallenge.ui.screens.ShieldPermissionScreen
import com.analoganchor.offlinechallenge.ui.theme.AmberWarning
import com.analoganchor.offlinechallenge.ui.theme.CyanGlow
import com.analoganchor.offlinechallenge.ui.theme.DeepSurface
import com.analoganchor.offlinechallenge.ui.theme.Obsidian
import com.analoganchor.offlinechallenge.ui.theme.OfflineChallengeTheme
import com.analoganchor.offlinechallenge.ui.theme.TextPrimary
import com.analoganchor.offlinechallenge.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private lateinit var challengePrefs: ChallengePreferences
    private var pendingDurationMs: Long = 0L
    private var pendingStartTimeMs: Long = 0L
    private var pendingPin: String = ""
    private var pendingPartnerName: String = "Anchor Partner"
    private var pendingSanctuaryName: String = ""
    private var pendingSessionId: String = ""
    private var pendingIsGroup: Boolean = false
    private var pendingPoints: Int = 40
    private var isIncomingPartnerChallenge = false
    private val pendingNavigationRoute = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private var pendingVpnCallback: (() -> Unit)? = null
    private val showUniversalEmergencyModalState = mutableStateOf(false)

    private val adminPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // After Device Admin prompt completes (whether activated or cancelled), proceed to VPN request
        requestVpnPermission {
            showAlwaysOnModalState.value = true
        }
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingVpnCallback?.invoke()
            pendingVpnCallback = null
        } else {
            pendingVpnCallback = null
            val isAr = challengePrefs.language == "ar"
            Toast.makeText(
                this,
                if (isAr) "يتطلب تفعيل الدرع الموافقة على إذن VPN" else "VPN permission is required to activate the shield.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                val isAr = challengePrefs.language == "ar"
                Toast.makeText(
                    this,
                    if (isAr) "يمكنك استخدام أداة الشاشة الرئيسية (Widget) لمتابعة التحدي مباشرة!" else "You can add the Home Screen Widget to track your challenge live!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val showNotificationRationale = mutableStateOf(false)
    private val showAlwaysOnModalState = mutableStateOf(false)
    private var onChallengeReadyNavigate: (() -> Unit)? = null

    override fun attachBaseContext(newBase: Context) {
        val contextToAttach = try {
            val prefs = ChallengePreferences(newBase)
            val locale = java.util.Locale(prefs.language)
            java.util.Locale.setDefault(locale)
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            newBase.createConfigurationContext(config)
        } catch (e: Throwable) {
            newBase
        }
        super.attachBaseContext(contextToAttach)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            installSplashScreen()
        } catch (e: Throwable) {
            // Ignore splashscreen initialization errors on custom ROMs
        }
        super.onCreate(savedInstanceState)
        
        challengePrefs = ChallengePreferences(this)
        handleIncomingIntent(intent)

        // Check Notification Permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                showNotificationRationale.value = true
            }
        }

        // If no challenge is active and not pending completion, ensure any stray VPN or notifications are stopped
        if (!challengePrefs.isActive && !challengePrefs.isCompletedPendingShow) {
            stopVpnService()
        }

        setContent {
            OfflineChallengeTheme(language = challengePrefs.language) {
                if (showNotificationRationale.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val isAr = challengePrefs.language == "ar"
                    AlertDialog(
                        onDismissRequest = { showNotificationRationale.value = false },
                        title = {
                            Text(
                                if (isAr) "🔔 تفعيل الإشعارات لشريط التقدم" else "🔔 Enable Live Progress Notifications",
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        text = {
                            Text(
                                if (isAr) 
                                    "يتطلب تطبيق الأوفلاين التنبيهات لعرض نسبة إنجاز التحدي والوقت المتبقي مباشرة في شريط الإشعارات وشاشة القفل.\n\n💡 ملاحظة: إذا رفضت التنبيهات، يمكنك دائماً إضافة أداة الشاشة الرئيسية (Widget) لمتابعة التحدي!" 
                                else 
                                    "Offline Challenge uses notifications to display your live progress percentage and remaining time directly on your lock screen and notification bar.\n\n💡 Tip: If you decline notifications, you can add our Home Screen Widget to track your progress!",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showNotificationRationale.value = false
                                        requestPinWidget(this@MainActivity)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isAr) "استخدام الويدجت" else "Use Widget Instead",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                Button(
                                    onClick = {
                                        showNotificationRationale.value = false
                                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isAr) "سماح بالتنبيهات" else "Allow Notifications",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        },
                        dismissButton = null
                    )
                }

                // 🔐 Always-On VPN PIN Confirmation Modal
                if (showAlwaysOnModalState.value) {
                    var confirmPinText by remember { mutableStateOf("") }
                    val modalKeyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
                    val modalFocusManager = androidx.compose.ui.platform.LocalFocusManager.current
                    val modalView = androidx.compose.ui.platform.LocalView.current

                    AlertDialog(
                        onDismissRequest = { /* Non-dismissable to reinforce commitment ceremony */ },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = DeepSurface,
                        title = {
                            Text(
                                text = stringResource(R.string.pin_activate_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.pin_activate_body),
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )

                                LaunchedEffect(confirmPinText.length) {
                                    if (confirmPinText.length >= 3) {
                                        modalFocusManager.clearFocus(force = true)
                                        modalKeyboardController?.hide()
                                        val imm = modalView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                                        imm?.hideSoftInputFromWindow(modalView.windowToken, 0)
                                    }
                                }

                                OutlinedTextField(
                                    value = confirmPinText,
                                    onValueChange = { input ->
                                        val filtered = input.filter { it.isDigit() }
                                        if (filtered.length <= 3) {
                                            confirmPinText = filtered
                                        }
                                    },
                                    readOnly = confirmPinText.length >= 3,
                                    modifier = Modifier
                                        .width(160.dp)
                                        .height(48.dp),
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = TextPrimary
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberWarning,
                                        unfocusedBorderColor = AmberWarning.copy(alpha = 0.5f),
                                        cursorColor = AmberWarning
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (confirmPinText.length in 3..4) {
                                        val guidanceMsg = if (confirmPinText != pendingPin) {
                                            getString(R.string.pin_forgotten_success)
                                        } else {
                                            getString(R.string.pin_remembered_success)
                                        }
                                        showGuidanceToast(guidanceMsg)

                                        showAlwaysOnModalState.value = false
                                        var effectiveDurationMs = pendingDurationMs
                                        if (isIncomingPartnerChallenge && pendingStartTimeMs > 0L) {
                                            val elapsed = System.currentTimeMillis() - pendingStartTimeMs
                                            if (elapsed in 10_000L..(5 * 60 * 1000L)) {
                                                effectiveDurationMs = (pendingDurationMs - elapsed).coerceAtLeast(60_000L)
                                            }
                                        }

                                        if (isIncomingPartnerChallenge) {
                                            challengePrefs.startPartnerChallenge(
                                                effectiveDurationMs,
                                                pendingPartnerName,
                                                pendingSessionId,
                                                pendingIsGroup,
                                                pendingPoints
                                            )
                                            challengePrefs.setCommitmentPin(pendingPin)
                                        } else {
                                            challengePrefs.startChallenge(pendingDurationMs, pendingPin)
                                        }
                                        challengePrefs.isAlwaysOnVpnActivated = true
                                        startVpnService()

                                        if (isIncomingPartnerChallenge || challengePrefs.isPartnerSession) {
                                            val pName = if (pendingPartnerName.isNotBlank()) pendingPartnerName else challengePrefs.partnerName
                                            val sId = if (pendingSessionId.isNotBlank()) pendingSessionId else challengePrefs.partnerSessionId
                                            val durationMins = if (effectiveDurationMs > 0) (effectiveDurationMs / (60 * 1000L)).toInt().coerceAtLeast(1) else 60
                                            try {
                                                val startIntent = Intent("com.analoganchor.app.ACTION_PARTNER_CHALLENGE_STARTED").apply {
                                                    setPackage("com.analoganchor.app")
                                                    putExtra("EXTRA_SESSION_ID", sId)
                                                    putExtra("EXTRA_DURATION_MINUTES", durationMins)
                                                    putExtra("EXTRA_PARTNER_NAME", pName)
                                                    putExtra("EXTRA_IS_GROUP", pendingIsGroup)
                                                    putExtra("EXTRA_POINTS", pendingPoints)
                                                    putExtra("EXTRA_START_TIME_MS", System.currentTimeMillis())
                                                }
                                                sendBroadcast(startIntent)
                                            } catch (_: Exception) {}
                                        }

                                        openVpnSettings(this@MainActivity)
                                        onChallengeReadyNavigate?.invoke()
                                    }
                                },
                                enabled = confirmPinText.length in 3..4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberWarning,
                                    disabledContainerColor = AmberWarning.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.pin_activate_button),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Obsidian
                                )
                            }
                        },
                        dismissButton = null
                    )
                }

                // 🚨 Universal Emergency Unlock Modal
                if (showUniversalEmergencyModalState.value) {
                    var universalTokenInput by remember { mutableStateOf("") }
                    var universalTokenResult by remember { mutableStateOf("") }
                    val emergencyKeyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
                    val emergencyFocusManager = androidx.compose.ui.platform.LocalFocusManager.current
                    val currentView = androidx.compose.ui.platform.LocalView.current

                    AlertDialog(
                        onDismissRequest = {
                            showUniversalEmergencyModalState.value = false
                            universalTokenInput = ""
                            universalTokenResult = ""
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.emergency_dialog_title),
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning
                            )
                        },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = stringResource(R.string.emergency_dialog_body),
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = universalTokenInput,
                                    onValueChange = {
                                        universalTokenInput = it
                                        universalTokenResult = ""
                                    },
                                    placeholder = { Text(stringResource(R.string.token_hint), color = TextSecondary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberWarning,
                                        unfocusedBorderColor = AmberWarning.copy(alpha = 0.5f),
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 13.sp
                                    )
                                )
                                if (universalTokenResult.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = universalTokenResult,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (universalTokenResult.contains("قبول") || universalTokenResult == getString(R.string.token_accepted)) CyanGlow else Color(0xFFFF5252)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    emergencyFocusManager.clearFocus(force = true)
                                    emergencyKeyboardController?.hide()
                                    try {
                                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                                        imm?.hideSoftInputFromWindow(currentView.windowToken, 0)
                                    } catch (_: Exception) {}

                                    val decoded = com.analoganchor.offlinechallenge.util.TokenDecoder.decode(universalTokenInput.trim())
                                    universalTokenInput = ""
                                    if (decoded == null) {
                                        universalTokenResult = getString(R.string.token_rejected)
                                    } else {
                                        if (decoded.requestNumber != challengePrefs.currentRequestStep) {
                                            universalTokenResult = getString(R.string.token_rejected)
                                        } else if (!com.analoganchor.offlinechallenge.util.PinVault.verify(decoded.decodedPin, decoded.requestNumber)) {
                                            universalTokenResult = getString(R.string.token_rejected)
                                        } else {
                                            challengePrefs.consumeRequest(decoded.requestNumber)
                                            if (decoded.requestNumber < 3) {
                                                challengePrefs.currentRequestStep = decoded.requestNumber + 1
                                            }
                                            challengePrefs.broadcastPartnerCompletionToAnalogAnchor(this@MainActivity, isSuccess = false)
                                            stopVpnService()
                                            removeDeviceAdmin()
                                            challengePrefs.endChallenge()
                                            showUniversalEmergencyModalState.value = false
                                            Toast.makeText(this@MainActivity, getString(R.string.token_accepted), Toast.LENGTH_LONG).show()
                                            pendingNavigationRoute.value = "setup"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                            ) {
                                Text(
                                    text = stringResource(R.string.verify_token),
                                    fontWeight = FontWeight.Bold,
                                    color = Obsidian
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showUniversalEmergencyModalState.value = false
                                universalTokenInput = ""
                                universalTokenResult = ""
                            }) {
                                Text(if (challengePrefs.language == "ar") "إلغاء" else "Cancel", color = TextSecondary)
                            }
                        },
                        containerColor = DeepSurface,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                AppNavHost()
            }
        }
    }

    @Composable
    fun AppNavHost() {
        val navController = rememberNavController()
        val pendingRoute by pendingNavigationRoute.collectAsState()

        LaunchedEffect(pendingRoute) {
            pendingRoute?.let { route ->
                navController.navigate(route) {
                    popUpTo(0) { inclusive = true }
                }
                pendingNavigationRoute.value = null
            }
        }

        val startRoute = if (challengePrefs.isCompletedPendingShow) {
            stopVpnService()
            "completion"
        } else if (challengePrefs.isActive) {
            "challenge"
        } else if (isIncomingPartnerChallenge || pendingDurationMs > 0) {
            "shield_permission"
        } else {
            stopVpnService()
            "setup"
        }

        NavHost(navController = navController, startDestination = startRoute) {
            composable("setup") {
                SetupScreen(
                    onDurationSelected = { durationMs ->
                        pendingDurationMs = durationMs
                        navController.navigate("shield_permission")
                    },
                    onOpenEmergencyModal = {
                        showUniversalEmergencyModalState.value = true
                    }
                )
            }

            composable("shield_permission") {
                ShieldPermissionScreen(
                    onActivate = { pin ->
                        pendingPin = pin
                        challengePrefs.setCommitmentPin(pin)
                        onChallengeReadyNavigate = {
                            navController.navigate("challenge") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        requestAdminAndVpn()
                    }
                )
            }

            composable("challenge") {
                val showRebootCalibrationDialog = remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    if (!MyVpnService.isRunning && challengePrefs.isActive) {
                        requestVpnPermission { /* VPN restarted */ }
                        if (!MyVpnService.isRunning) {
                            showRebootCalibrationDialog.value = true
                        }
                    }
                }

                if (showRebootCalibrationDialog.value) {
                    val isAr = challengePrefs.language == "ar"
                    AlertDialog(
                        onDismissRequest = { /* Non-dismissable */ },
                        title = {
                            Text(
                                if (isAr) "🛡️ مطلوب معايرة حماية النظام" else "🛡️ System Protection Calibration Required",
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        text = {
                            Text(
                                if (isAr)
                                    "تم اكتشاف إعادة تشغيل للهاتف أو انقطاع في النظام أثناء التحدي النشط.\n\nللحفاظ على استمرار الحماية التلقائية بعد إعادة تشغيل الهاتف، يُرجى تفعيل التغطية الدائمة في إعدادات النظام."
                                else
                                    "A device restart or system interruption was detected during your active challenge.\n\nTo maintain automatic shield protection across device reboots, please activate System Always-On Protection.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showRebootCalibrationDialog.value = false
                                    requestVpnPermission { /* Restart VPN */ }
                                    openVpnSettings(this@MainActivity)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (isAr) "تفعيل التغطية الدائمة" else "Activate Always-On Protection",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissButton = null
                    )
                }

                ChallengeScreen(
                    challengePrefs = challengePrefs,
                    onOpenVpnSettings = { openVpnSettings(this@MainActivity) },
                    onEmergencyUnlock = {
                        challengePrefs.broadcastPartnerCompletionToAnalogAnchor(this@MainActivity, isSuccess = false)
                        stopVpnService()
                        removeDeviceAdmin()
                        challengePrefs.endChallenge()
                        navController.navigate("setup") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onChallengeComplete = {
                        challengePrefs.broadcastPartnerCompletionToAnalogAnchor(this@MainActivity, isSuccess = true)
                        stopVpnService()
                        removeDeviceAdmin()
                        challengePrefs.endChallenge()
                        challengePrefs.isCompletedPendingShow = true
                        navController.navigate("completion") {
                            popUpTo("challenge") { inclusive = true }
                        }
                    }
                )
            }

            composable("completion") {
                CompletionScreen(
                    challengePrefs = challengePrefs,
                    onOpenVpnSettings = { openVpnSettings(this@MainActivity) },
                    onOpenEmergencyModal = {
                        showUniversalEmergencyModalState.value = true
                    },
                    onHome = {
                        challengePrefs.isCompletedPendingShow = false
                        stopVpnService()
                        navController.navigate("setup") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }

    private fun requestAdminAndVpn() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComp = DeviceAdminReceiver.getComponentName(this)
        if (dpm?.isAdminActive(adminComp) != true) {
            val adminIntent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComp)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.device_admin_description))
            }
            adminPermissionLauncher.launch(adminIntent)
        } else {
            requestVpnPermission {
                showAlwaysOnModalState.value = true
            }
        }
    }

    private fun requestVpnPermission(onGranted: () -> Unit) {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            pendingVpnCallback = onGranted
            try {
                vpnPermissionLauncher.launch(vpnIntent)
            } catch (e: Exception) {
                pendingVpnCallback = null
            }
        } else {
            onGranted()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_START
        }
        startForegroundService(intent)
        // Arm Layer 2 & 3 guards for reboot protection
        VpnGuardWorker.schedule(this)
        NetworkGuard.register(this)
        com.analoganchor.offlinechallenge.widget.ChallengeWidgetReceiver.updateWidget(this)
    }

    private fun stopVpnService() {
        val intent = Intent(this, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_STOP
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to startService ACTION_STOP: ${e.message}")
        }
        // Disarm Layer 2 & 3 guards — challenge is ending
        VpnGuardWorker.cancel(this)
        NetworkGuard.unregister(this)
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(1)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to cancel notification 1: ${e.message}")
        }
        com.analoganchor.offlinechallenge.widget.ChallengeWidgetReceiver.updateWidget(this)
    }

    private fun removeDeviceAdmin() {
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val adminComp = DeviceAdminReceiver.getComponentName(this)
            if (dpm?.isAdminActive(adminComp) == true) {
                dpm.removeActiveAdmin(adminComp)
                Log.i("MainActivity", "Device Admin removed successfully")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to remove admin: ${e.message}")
        }
    }

    private fun requestPinWidget(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            val myProvider = android.content.ComponentName(context, com.analoganchor.offlinechallenge.widget.ChallengeWidgetReceiver::class.java)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val options = Bundle().apply {
                    putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 270)
                    putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 500)
                    putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 48)
                    putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 100)
                }
            }
        } else {
            val isAr = challengePrefs.language == "ar"
            Toast.makeText(
                context,
                if (isAr) "يمكنك إضافة الويدجت من شاشة هاتفك الرئيسية." else "Add the widget from your home screen.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openVpnSettings(context: Context) {
        try {
            val intent = Intent("android.net.vpn.SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e1: Exception) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_VPN_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e3: Exception) {
                    // Ignore if settings cannot be opened
                }
            }
        }
    }

    private fun showGuidanceToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        window.decorView.postDelayed({
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }, 2200)
        window.decorView.postDelayed({
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }, 4400)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val challengeType = intent.getStringExtra("EXTRA_CHALLENGE_TYPE")
        val targetRoute = intent.getStringExtra("EXTRA_TARGET_ROUTE")
        if (targetRoute == "challenge") {
            pendingNavigationRoute.value = "challenge"
        }
        if (challengeType == "PARTNER_OUTDOOR" || intent.action == "com.analoganchor.action.START_PARTNER_CHALLENGE" || targetRoute == "shield_permission") {
            val partnerName = intent.getStringExtra("EXTRA_PARTNER_NAME") ?: "Anchor Partner"
            val sanctuaryName = intent.getStringExtra("EXTRA_SANCTUARY_NAME") ?: ""
            val sessionId = intent.getStringExtra("EXTRA_SESSION_ID") ?: System.currentTimeMillis().toString()
            val durationMinutes = intent.getIntExtra("EXTRA_DURATION_MINUTES", 60)
            val startTimeMs = intent.getLongExtra("EXTRA_START_TIME", System.currentTimeMillis())
            val isGroup = intent.getBooleanExtra("EXTRA_IS_GROUP", false)
            val points = intent.getIntExtra("EXTRA_POINTS", if (isGroup) 70 else 40)
            val durationMs = durationMinutes * 60 * 1000L

            pendingDurationMs = durationMs
            pendingStartTimeMs = startTimeMs
            pendingPartnerName = partnerName
            pendingSanctuaryName = sanctuaryName
            pendingSessionId = sessionId
            pendingIsGroup = isGroup
            pendingPoints = points
            isIncomingPartnerChallenge = true

            if (!challengePrefs.isActive) {
                // Route directly to ShieldPermissionScreen (Screenshot 2: "حماية الدرع المتقدمة")
                pendingNavigationRoute.value = "shield_permission"
            } else {
                if (!challengePrefs.isPartnerSession) {
                    challengePrefs.isPartnerSession = true
                    challengePrefs.partnerName = partnerName
                    challengePrefs.partnerSessionId = sessionId
                    challengePrefs.isGroupSession = isGroup
                    challengePrefs.sessionPoints = points
                }
            }
        }
    }
}

