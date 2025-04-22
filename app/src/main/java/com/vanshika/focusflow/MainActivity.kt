package com.vanshika.focusflow

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.vanshika.focusflow.userInterface.FocusFlowTheme
import com.vanshika.focusflow.userInterface.FocusFlowApp
import com.vanshika.focusflow.utils.AppUsageManager
import com.vanshika.focusflow.utils.TrackingService

class MainActivity : ComponentActivity() {
    // For handling permission request results
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Handle the case where user denies notification permission
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request necessary permissions
        checkPermissions()

        // Start the tracking service
        startService(Intent(this, TrackingService::class.java))

        setContent {
            FocusFlowTheme {
                FocusFlowApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Re-check permissions when activity comes to foreground
        checkPermissions()
    }

    private fun checkPermissions() {
        // Check and request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        // Check and request usage access permission
        if (!AppUsageManager.hasUsageAccess(this)) {
            AppUsageManager.openUsageAccessSettings(this)
        }

        // Check battery optimization (Android 6+)
        checkBatteryOptimization()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(PowerManager::class.java)
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

}