package com.vanshika.focusflow.userInterface.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    focusApps: List<String>,
    distractionApps: List<String>,
    onUpdateFocusApps: (List<String>) -> Unit,
    onUpdateDistractionApps: (List<String>) -> Unit
) {
    val installedApps = listOf(
        "Instagram",
        "YouTube",
        "Gmail",
        "Google Calendar",
        "Spotify",
        "WhatsApp",
        "Zoom",
        "Slack",
        "Chrome",
        "Notion"
    )
    var notificationsEnabled by remember { mutableStateOf(true) }

    var showFocusDialog by remember { mutableStateOf(false) }
    var showDistractionDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("⚙️ App Settings", fontSize = 22.sp)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable Motivation Alerts", modifier = Modifier.weight(1f))
            Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
        }

        OutlinedButton(onClick = { showFocusDialog = true }) {
            Text("🧘 Manage Focus Apps")
        }

        OutlinedButton(onClick = { showDistractionDialog = true }) {
            Text("📱 Manage Distraction Apps")
        }

        OutlinedButton(onClick = { /* TODO: Clear focus history */ }) {
            Text("🧹 Clear Focus History")
        }
    }

    if (showFocusDialog) {
        AppSelectionDialog(
            title = "Select Focus Apps",
            allApps = installedApps,
            selectedApps = focusApps,
            onDismiss = { showFocusDialog = false },
            onConfirm = {
                onUpdateFocusApps(it)
                showFocusDialog = false
            }
        )
    }

    if (showDistractionDialog) {
        AppSelectionDialog(
            title = "Select Distraction Apps",
            allApps = installedApps,
            selectedApps = distractionApps,
            onDismiss = { showDistractionDialog = false },
            onConfirm = {
                onUpdateDistractionApps(it)
                showDistractionDialog = false
            }
        )
    }
}

@Composable
fun AppSelectionDialog(
    title: String,
    allApps: List<String>,
    selectedApps: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val selectedSet = remember { mutableStateOf(selectedApps.toMutableSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedSet.value.toList()) }) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text(title) },
        text = {
            Column {
                allApps.forEach { app ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectedSet.value.contains(app),
                            onCheckedChange = {
                                if (it) selectedSet.value.add(app)
                                else selectedSet.value.remove(app)
                            }
                        )
                        Text(text = app)
                    }
                }
            }
        }
    )
}