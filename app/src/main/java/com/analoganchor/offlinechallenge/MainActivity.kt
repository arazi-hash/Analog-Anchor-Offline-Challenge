package com.analoganchor.offlinechallenge

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "تم رفض الإذن. لا يمكن تشغيل الدرع.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val locale = java.util.Locale("ar")
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        challengePrefs = ChallengePreferences(this)

        setContent {
            OfflineChallengeTheme {
                AppNavHost()
            }
        }
    }

    @Composable
    fun AppNavHost() {
        val navController = rememberNavController()

        val startRoute = if (challengePrefs.isActive) {
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
                        requestVpnPermission()
                        challengePrefs.startChallenge(pendingDurationMs)
                        navController.navigate("challenge") {
                            popUpTo("setup") { inclusive = true }
                        }
                    }
                )
            }

            composable("challenge") {
                LaunchedEffect(Unit) {
                    if (!MyVpnService.isRunning && challengePrefs.isActive) {
                        requestVpnPermission()
                    }
                }

                ChallengeScreen(
                    challengePrefs = challengePrefs,
                    onEmergencyUnlock = {
                        stopVpnService()
                        navController.navigate("setup") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onChallengeComplete = {
                        stopVpnService()
                        navController.navigate("completion") {
                            popUpTo("challenge") { inclusive = true }
                        }
                    }
                )
            }

            composable("completion") {
                CompletionScreen(
                    onHome = {
                        navController.navigate("setup") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }

    private fun requestVpnPermission() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
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
