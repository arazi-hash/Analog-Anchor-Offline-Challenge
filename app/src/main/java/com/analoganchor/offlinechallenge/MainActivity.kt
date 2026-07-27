package com.analoganchor.offlinechallenge

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.analoganchor.offlinechallenge.data.ChallengePreferences
import com.analoganchor.offlinechallenge.service.MyVpnService
import com.analoganchor.offlinechallenge.ui.screens.ChallengeScreen
import com.analoganchor.offlinechallenge.ui.screens.CompletionScreen
import com.analoganchor.offlinechallenge.ui.screens.SetupScreen
import com.analoganchor.offlinechallenge.ui.screens.ShieldPermissionScreen
import com.analoganchor.offlinechallenge.ui.theme.OfflineChallengeTheme

class MainActivity : ComponentActivity() {

    private lateinit var challengePrefs: ChallengePreferences
    private var pendingDurationMs: Long = 0L
    private var pendingVpnCallback: (() -> Unit)? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
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
                // If notification permission is denied, notify user about home screen widget
                val isAr = challengePrefs.language == "ar"
                Toast.makeText(
                    this,
                    if (isAr) "يمكنك استخدام أداة الشاشة الرئيسية (Widget) لمتابعة التحدي مباشرة!" else "You can add the Home Screen Widget to track your challenge live!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun attachBaseContext(newBase: android.content.Context) {
        try {
            val prefs = com.analoganchor.offlinechallenge.data.ChallengePreferences(newBase)
            val locale = java.util.Locale(prefs.language)
            java.util.Locale.setDefault(locale)
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } catch (e: Exception) {
            super.attachBaseContext(newBase)
        }
    }

    private val showNotificationRationale = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        challengePrefs = ChallengePreferences(this)

        // Check Notification Permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                showNotificationRationale.value = true
            }
        }

        setContent {
            OfflineChallengeTheme(language = challengePrefs.language) {
                if (showNotificationRationale.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val isAr = challengePrefs.language == "ar"
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showNotificationRationale.value = false },
                        title = {
                            androidx.compose.material3.Text(
                                if (isAr) "🔔 تفعيل الإشعارات لشريط التقدم" else "🔔 Enable Live Progress Notifications",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                            )
                        },
                        text = {
                            androidx.compose.material3.Text(
                                if (isAr) 
                                    "يتطلب تطبيق الأوفلاين التنبيهات لعرض نسبة إنجاز التحدي والوقت المتبقي مباشرة في شريط الإشعارات وشاشة القفل.\n\n💡 ملاحظة: إذا رفضت التنبيهات، يمكنك دائماً إضافة أداة الشاشة الرئيسية (Widget) لمتابعة التحدي!" 
                                else 
                                    "Offline Challenge uses notifications to display your live progress percentage and remaining time directly on your lock screen and notification bar.\n\n💡 Tip: If you decline notifications, you can add our Home Screen Widget to track your progress!",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
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
                                androidx.compose.material3.OutlinedButton(
                                    onClick = {
                                        showNotificationRationale.value = false
                                        requestPinWidget(this@MainActivity)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    androidx.compose.material3.Text(
                                        text = if (isAr) "استخدام الويدجت" else "Use Widget Instead",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                androidx.compose.material3.Button(
                                    onClick = {
                                        showNotificationRationale.value = false
                                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    androidx.compose.material3.Text(
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
                AppNavHost()
            }
        }
    }

    @Composable
    fun AppNavHost() {
        val navController = rememberNavController()

        val startRoute = if (challengePrefs.isCompletedPendingShow) {
            "completion"
        } else if (challengePrefs.isActive) {
            "challenge"
        } else {
            "setup"
        }

        NavHost(navController = navController, startDestination = startRoute) {
            composable("setup") {
                SetupScreen(
                    onDurationSelected = { durationMs ->
                        pendingDurationMs = durationMs
                        navController.navigate("shield_permission")
                    }
                )
            }

            composable("shield_permission") {
                ShieldPermissionScreen(
                    onActivate = {
                        requestVpnPermission {
                            challengePrefs.startChallenge(pendingDurationMs)
                            navController.navigate("challenge") {
                                popUpTo("setup") { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable("challenge") {
                LaunchedEffect(Unit) {
                    if (!MyVpnService.isRunning && challengePrefs.isActive) {
                        requestVpnPermission { /* VPN restarted */ }
                    }
                }

                ChallengeScreen(
                    challengePrefs = challengePrefs,
                    onEmergencyUnlock = {
                        stopVpnService()
                        challengePrefs.endChallenge()
                        navController.navigate("setup") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onChallengeComplete = {
                        stopVpnService()
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
                    onHome = {
                        challengePrefs.isCompletedPendingShow = false
                        navController.navigate("setup") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
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
            startVpnService()
            onGranted()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_START
        }
        startForegroundService(intent)
        com.analoganchor.offlinechallenge.widget.ChallengeWidgetReceiver.updateWidget(this)
    }

    private fun stopVpnService() {
        val intent = Intent(this, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_STOP
        }
        startService(intent)
        com.analoganchor.offlinechallenge.widget.ChallengeWidgetReceiver.updateWidget(this)
    }

    private fun requestPinWidget(context: android.content.Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            val myProvider = android.content.ComponentName(context, com.analoganchor.offlinechallenge.widget.ChallengeWidgetReceiver::class.java)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val options = android.os.Bundle().apply {
                    putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 270)
                    putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 500)
                    putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 48)
                    putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 100)
                }
                appWidgetManager.requestPinAppWidget(myProvider, options, null)
            } else {
                val isAr = challengePrefs.language == "ar"
                Toast.makeText(
                    context,
                    if (isAr) "يمكنك إضافة الويدجت من شاشة هاتفك الرئيسية." else "Add the widget from your home screen.",
                    Toast.LENGTH_LONG
                ).show()
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
}
