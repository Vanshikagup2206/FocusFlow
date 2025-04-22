package com.vanshika.focusflow.utils

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppUsageManager {
    private const val TAG = "AppUsageManager"

    suspend fun getForegroundApp(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            if (!hasUsageAccess(context)) {
                Log.w(TAG, "Usage access not granted")
                return@withContext null
            }

            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 10000 // Check last 10 seconds

            val usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST, // More real-time than INTERVAL_DAILY
                beginTime,
                endTime
            )

            if (usageStatsList.isNullOrEmpty()) {
                Log.d(TAG, "No usage stats found")
                return@withContext null
            }

            val recentStat = usageStatsList
                .filter { it.lastTimeUsed > 0 }
                .maxByOrNull { it.lastTimeUsed }

            val packageName = recentStat?.packageName
            Log.d(TAG, "Foreground app: $packageName")
            return@withContext packageName
        } catch (e: Exception) {
            Log.e(TAG, "Error getting foreground app: ${e.message}")
            return@withContext null
        }
    }

    fun hasUsageAccess(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage access: ${e.message}")
            false
        }
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun isDistractionApp(packageName: String): Boolean {
        val distractions = listOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.google.android.youtube",
            "com.zhiliaoapp.musically", // TikTok
            "com.twitter.android"
        )
        return distractions.contains(packageName)
    }

    fun isFocusApp(packageName: String): Boolean {
        val focusApps = listOf(
            "com.whatsapp",
            "org.mozilla.firefox",
            "com.microsoft.office.word",
            "com.google.android.keep",
            "com.android.chrome"
        )
        return focusApps.contains(packageName)
    }
}