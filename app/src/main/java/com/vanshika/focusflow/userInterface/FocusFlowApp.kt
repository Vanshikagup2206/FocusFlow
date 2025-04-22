package com.vanshika.focusflow.userInterface

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.*
import com.vanshika.focusflow.navigation.BottomNavItem
import com.vanshika.focusflow.navigation.BottomNavigationBar
import com.vanshika.focusflow.userInterface.screens.HomeScreen
import com.vanshika.focusflow.userInterface.screens.ProgressScreen
import com.vanshika.focusflow.userInterface.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusFlowApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var focusApps by remember { mutableStateOf(listOf("Google Calendar", "Notion")) }
    var distractionApps by remember { mutableStateOf(listOf("Instagram", "YouTube")) }

    LaunchedEffect(Unit) {
        if (!hasUsageAccess(context)) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    val items = listOf(
        BottomNavItem("Home", "home", Icons.Default.Home),
        BottomNavItem("Progress", "progress", Icons.Default.DateRange),
        BottomNavItem("Settings", "settings", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController, items)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            val installedApps = listOf(
                "Instagram", "YouTube", "Gmail", "Google Calendar", "Spotify",
                "WhatsApp", "Zoom", "Slack", "Chrome", "Notion"
            )
            composable("home") { HomeScreen() }
            composable("progress") { ProgressScreen() }
            composable("settings") { SettingsScreen(
                focusApps = focusApps,
                distractionApps = distractionApps,
                onUpdateFocusApps = { updatedList -> focusApps = updatedList },
                onUpdateDistractionApps = { updatedList -> distractionApps = updatedList }
            ) }
        }
    }
}

fun hasUsageAccess(context: Context): Boolean {
    return try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        Log.e("FocusFlow", "Usage access check failed", e)
        false
    }
}