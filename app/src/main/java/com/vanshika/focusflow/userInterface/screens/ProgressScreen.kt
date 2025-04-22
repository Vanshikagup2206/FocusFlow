package com.vanshika.focusflow.userInterface.screens

import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.component.text.textComponent
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.vanshika.focusflow.database.FocusViewModel
import com.vanshika.focusflow.database.FocusSession
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProgressScreen(viewModel: FocusViewModel = viewModel()) {
    val sessionsFlow = viewModel.getAllSessions().collectAsState(initial = emptyList())
    val sessions = sessionsFlow.value

    // Calculate metrics from actual data
    val streak = remember(sessions) { calculateStreak(sessions) }
    val todayDuration = remember(sessions) {
        sessions.lastOrNull { it.date == getCurrentDate() }?.durationInSeconds ?: 0
    }
    val todayDistractions = remember(sessions) {
        sessions.lastOrNull { it.date == getCurrentDate() }?.distractions ?: 0
    }
    val weeklyFocus = remember(sessions) {
        sessions.filter { isDateInCurrentWeek(it.date) }.sumOf { it.durationInSeconds }
    }
    val weeklyDistractions = remember(sessions) {
        sessions.filter { isDateInCurrentWeek(it.date) }.sumOf { it.distractions }
    }

    var showQuote by remember { mutableStateOf(false) }
    if (todayDuration >= 1800 && !showQuote) {
        showQuote = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📈 Progress Overview", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // Streak Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🔥 Current Streak: $streak days", fontWeight = FontWeight.SemiBold)
            }
        }

        // Chart Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📊 Weekly Focus Chart", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                FocusTimeChart(sessions = sessions.filter { isDateInCurrentWeek(it.date) })
            }
        }

        // Today's Stats Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Today", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Focus Time: ${secondsToTimeString(todayDuration)}")
                Text("Distractions: $todayDistractions")
            }
        }

        // Weekly Stats Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("This Week", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total Focus Time: ${secondsToTimeString(weeklyFocus)}")
                Text("Total Distractions: $weeklyDistractions")
            }
        }

        Button(onClick = { viewModel.clearSessions() }) {
            Text("Reset Stats")
        }

        if (showQuote) {
            AlertDialog(
                onDismissRequest = { showQuote = false },
                confirmButton = {
                    TextButton(onClick = { showQuote = false }) {
                        Text("Thanks!")
                    }
                },
                title = { Text("🎉 Milestone Reached!") },
                text = { Text("You've focused for 30+ minutes today! Keep it up! 🚀") }
            )
        }
    }
}

@Composable
private fun FocusTimeChart(sessions: List<FocusSession>) {
    // Sort sessions by date for proper chart ordering
    val sortedSessions = remember(sessions) {
        sessions.sortedBy { session ->
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(session.date)?.time ?: 0L
        }
    }

    val chartEntryModel = remember(sortedSessions) {
        entryModelOf(*sortedSessions.mapIndexed { index, session ->
            index.toFloat() to (session.durationInSeconds / 60f)
        }.toTypedArray())
    }

    ProvideChartStyle {
        Chart(
            chart = lineChart(
                lines = listOf(
                    lineSpec(
                        lineColor = MaterialTheme.colorScheme.primary,
                        lineThickness = 4.dp
                    )
                )
            ),
            model = chartEntryModel,
            startAxis = startAxis(
                title = "Minutes",
                titleComponent = textComponent {
                    color = Color.BLUE
                },
                guideline = null
            ),
            bottomAxis = bottomAxis(
                title = "Days",
                titleComponent = textComponent {
                    color = Color.BLUE
                },
                guideline = null
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}

private fun calculateStreak(sessions: List<FocusSession>): Int {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()
    var streak = 0

    // Group sessions by date and sum durations
    val dailySessions = sessions.groupBy { it.date }
        .mapValues { (_, sessions) -> sessions.sumOf { it.durationInSeconds } }

    while (true) {
        val dateString = dateFormat.format(calendar.time)
        val totalDuration = dailySessions[dateString] ?: 0L

        if (totalDuration >= 600) { // 10 minutes minimum for streak
            streak++
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            break
        }
    }
    return streak
}

private fun getCurrentDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}

private fun isDateInCurrentWeek(dateString: String): Boolean {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = dateFormat.parse(dateString) ?: return false

    val currentCalendar = Calendar.getInstance()
    val targetCalendar = Calendar.getInstance().apply { time = date }

    return currentCalendar.get(Calendar.WEEK_OF_YEAR) == targetCalendar.get(Calendar.WEEK_OF_YEAR) &&
            currentCalendar.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR)
}

private fun secondsToTimeString(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "$hours hrs $minutes min"
        else -> "$minutes min"
    }
}