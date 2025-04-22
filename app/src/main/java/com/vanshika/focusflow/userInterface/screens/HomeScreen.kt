package com.vanshika.focusflow.userInterface.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vanshika.focusflow.database.FocusViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(viewModel: FocusViewModel = viewModel()) {
    val context = LocalContext.current
    var startTime by remember { mutableStateOf<Long?>(null) }
    var sessionDuration by remember { mutableStateOf(0L) }

    // Step 1: Get today's date
    val today = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Step 2: Get all focus sessions
    val allSessions by viewModel.getAllSessions().collectAsState(initial = emptyList())

    // Step 3: Filter today’s sessions
    val todaySessions = allSessions.filter { it.date == today }

    // Step 4: Total duration for today
    val totalTodaySeconds = todaySessions.sumOf { it.durationInSeconds }
    val totalTodayFormatted = formatSecondsToHMS(totalTodaySeconds)

    LaunchedEffect(startTime) {
        if (startTime != null) {
            // Use a background coroutine to update the session duration
            viewModel.updateSessionDuration(startTime!!) { updatedDuration ->
                sessionDuration = updatedDuration
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Hey Vanshika 👋", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Ready to Focus Today?", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)

        Text("⏱ Session Duration: ${sessionDuration}s")

        Button(onClick = {
            if (startTime == null) {
                startTime = System.currentTimeMillis()
            } else {
                val endTime = System.currentTimeMillis()
                val duration = (endTime - startTime!!) / 1000
                viewModel.saveFocusSession(duration)
                startTime = null
                sessionDuration = 0
            }
        }) {
            Text(if (startTime == null) "Start Focus" else "End Focus")
        }

        // 👉 Step 5: Show today's focus summary
        Spacer(modifier = Modifier.height(32.dp))
        FocusSummaryCard(focusTime = totalTodayFormatted, distractions = 0) // Distractions for now = 0

        // Reset Stats button
        Button(onClick = { viewModel.clearSessions() }) {
            Text("Reset Stats")
        }
    }
}

@Composable
fun FocusSummaryCard(focusTime: String, distractions: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Today’s Summary", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("⏱ Focus Time: $focusTime")
            Text("⚠️ Distractions: $distractions")
        }
    }
}

fun formatSecondsToHMS(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

@Composable
fun ActionButtons() {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(onClick = { /* TODO: Trigger focus session */ }) {
            Text("Start Focus")
        }
        Button(onClick = { /* TODO: Show motivation */ }) {
            Text("Get Motivation")
        }
    }
}

@Composable
fun MotivationalQuote(quote: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(
            text = quote,
            modifier = Modifier.padding(16.dp),
            fontSize = 14.sp
        )
    }
}