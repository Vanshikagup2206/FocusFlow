package com.vanshika.focusflow.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.vanshika.focusflow.R
import com.vanshika.focusflow.database.FocusViewModel
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class TrackingService : Service(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TrackingService"
        private const val NOTIFICATION_CHANNEL_ID = "focus_alert_channel"
        private const val NOTIFICATION_ID = 1
        private const val QUOTE_NOTIFICATION_ID = 1001
        private const val CHECK_INTERVAL_MS = 6000L
        private const val GROQ_API_KEY = "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"
        private const val TTS_UTTERANCE_ID = "FocusFlowTTS"
    }

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var serviceJob: Job? = null
    private val client = OkHttpClient()
    private val ttsQueue = mutableListOf<String>()
    private var distractionCount = 0
    private var sessionStartTime = 0L
    private lateinit var viewModel: FocusViewModel

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        viewModel = FocusViewModel(application)
        sessionStartTime = System.currentTimeMillis() // Track session start time
        initializeTTS()
        createNotificationChannel()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this, this).apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS started speaking")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS finished speaking")
                    if (ttsQueue.isNotEmpty()) {
                        speak(ttsQueue.removeAt(0))
                    }
                }

                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS error occurred")
                }
            })
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS Language not supported")
            } else {
                isTtsReady = true
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(1.1f)
                Log.d(TAG, "TTS initialized successfully")
                // Process any queued messages
                if (ttsQueue.isNotEmpty()) {
                    speak(ttsQueue.removeAt(0))
                }
            }
        } else {
            Log.e(TAG, "TTS initialization failed")
        }
    }

    private fun speak(message: String) {
        if (isTtsReady) {
            tts?.speak(message, TextToSpeech.QUEUE_ADD, null, TTS_UTTERANCE_ID)
        } else {
            ttsQueue.add(message)
            Log.d(TAG, "TTS not ready, added to queue")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service starting")

        // Initialize ViewModel and session tracking
        viewModel = FocusViewModel(application)
        sessionStartTime = System.currentTimeMillis()

        // Create notification channel (important for Android 8.0+)
        createNotificationChannel()

        // Start as foreground service with notification
        startForeground(NOTIFICATION_ID, buildServiceNotification())

        serviceJob = CoroutineScope(Dispatchers.Default).launch {
            if (!checkPermissions()) return@launch

            while (isActive) {
                try {
                    val foregroundApp = withContext(Dispatchers.IO) {
                        AppUsageManager.getForegroundApp(applicationContext)
                    }

                    foregroundApp?.let { app ->
                        when {
                            AppUsageManager.isDistractionApp(app) -> handleDistraction(app)
                            AppUsageManager.isFocusApp(app) -> handleFocus(app)
                        }
                    }
                    delay(CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Tracking error", e)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroying")
        serviceJob?.cancel()
        tts?.stop()
        tts?.shutdown()
        ttsQueue.clear()
        // Save session when service stops
        saveCurrentSession()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Focus Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when distraction or focus apps are opened"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildServiceNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("FocusFlow is Active")
            .setContentText("Tracking your app usage...")
            .setSmallIcon(R.drawable.baseline_show_chart_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun showMotivation(message: String, isFocus: Boolean) {
        val emoji = if (isFocus) "💡" else "😈"
        val title = "FocusFlow Alert"

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_show_chart_24)
            .setContentTitle(title)
            .setContentText("$emoji $message")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$emoji $message"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(QUOTE_NOTIFICATION_ID, notification)

        speak(message)
        Log.d(TAG, "Notification shown: $message")
    }

    private suspend fun getGroqMessage(isFocusApp: Boolean): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = if (isFocusApp) {
                    listOf(
                        "Give a short motivational quote to help someone stay focused. Under 20 words.",
                        "Share a powerful productivity quote in less than 20 words.",
                        "Say something encouraging to help resist distractions. Max 20 words.",
                        "Give a motivational one-liner to inspire focus. Short and punchy."
                    ).random()
                } else {
                    listOf(
                        "Give a short, sarcastic roast for someone who can't stop scrolling. Keep it under 20 words.",
                        "Roast someone distracted by YouTube in a funny way. Max 20 words.",
                        "Write a witty insult for a procrastinator. Short and funny.",
                        "Mock someone for constantly opening Instagram. Keep it light and under 20 words."
                    ).random()
                }

                val requestBody = JSONObject().apply {
                    put("model", "mixtral-8x7b-32768")
                    put("temperature", 0.9)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", "You're a helpful assistant who gives ${if (isFocusApp) "uplifting motivational quotes" else "funny sarcastic roasts"}.")
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                }.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $GROQ_API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext defaultFallback(isFocusApp)
                    }

                    val body = response.body?.string()
                    if (body.isNullOrEmpty()) {
                        return@withContext defaultFallback(isFocusApp)
                    }

                    try {
                        JSONObject(body)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing response", e)
                        defaultFallback(isFocusApp)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error", e)
                defaultFallback(isFocusApp)
            }
        }
    }

    private fun defaultFallback(isFocus: Boolean) = if (isFocus)
        "You're capable of amazing things. Stay focused! 💪"
    else
        "Back to scrolling instead of growing? Classic. 😏"

    private fun handleDistraction() {
        distractionCount++
        // Show notification/message
        showMotivation("Avoid distractions! You've been distracted $distractionCount times today", false)
    }

    // When session ends (e.g., when service stops or app closes)
    private fun saveCurrentSession() {
        val duration = (System.currentTimeMillis() - sessionStartTime) / 1000
        viewModel.saveSessionWithDistractions(duration, distractionCount)
        Log.d(TAG, "Session saved - Duration: ${duration}s, Distractions: $distractionCount")
        distractionCount = 0
    }

    // [NEW] Permission check helper
    private suspend fun checkPermissions(): Boolean {
        if (!AppUsageManager.hasUsageAccess(this@TrackingService)) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@TrackingService,
                    "Please enable Usage Access permission",
                    Toast.LENGTH_LONG
                ).show()
                AppUsageManager.openUsageAccessSettings(this@TrackingService)
            }
            stopSelf()
            return false
        }
        return true
    }

    // [NEW] Distraction handler
    private fun handleDistraction(app: String) {
        distractionCount++
        Log.d(TAG, "Distraction detected on $app (Total: $distractionCount)")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val message = getGroqMessage(false)
                showNotification("⚠️ Distraction #$distractionCount", message, false)
            } catch (e: Exception) {
                showNotification("⚠️ Distraction", "You got distracted! ($distractionCount today)", false)
            }
        }
    }

    // [NEW] Focus handler
    private fun handleFocus(app: String) {
        Log.d(TAG, "Focus app detected: $app")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val message = getGroqMessage(true)
                showNotification("🎯 Good focus!", message, true)
            } catch (e: Exception) {
                Log.e(TAG, "Focus message error", e)
            }
        }
    }

    // [MODIFIED] Notification with fixes
    private fun showNotification(title: String, message: String, isFocus: Boolean) {
        try {
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_show_chart_24)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(System.currentTimeMillis().toInt(), notification)

            // TTS in background
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, TTS_UTTERANCE_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Notification failed", e)
        }
    }
}