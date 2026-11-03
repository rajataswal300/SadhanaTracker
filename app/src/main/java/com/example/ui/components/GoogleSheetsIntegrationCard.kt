package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivitySession
import com.example.data.model.ChecklistItem
import com.example.data.model.DailyChecklistCompletion
import com.example.data.model.DailySummary
import com.example.data.sheets.GoogleSheetsSyncService
import com.example.data.sheets.SheetsSyncResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoogleSheetsIntegrationCard(
    allSummaries: List<DailySummary>,
    allSessions: List<ActivitySession>,
    allChecklistItems: List<ChecklistItem>,
    allCompletions: List<DailyChecklistCompletion>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val syncService = remember { GoogleSheetsSyncService(context) }

    var webhookUrl by remember { mutableStateOf(syncService.getSavedWebhookUrl()) }
    var lastSyncTime by remember { mutableStateOf(syncService.getLastSyncTimestamp()) }

    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    var showSetupDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("google_sheets_card"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF0F9D58).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = Color(0xFF0F9D58),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Google Sheets & Visual Graphs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Works for all users & friends with their own sheet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { showSetupDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("sheets_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Setup My Google Sheet",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sync Status Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (webhookUrl.isNotBlank()) Icons.Default.CloudDone else Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = if (webhookUrl.isNotBlank()) Color(0xFF0F9D58) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (webhookUrl.isNotBlank()) "Personal Sheet Linked" else "No Sheet Linked Yet",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (lastSyncTime > 0) {
                        val dateStr = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(Date(lastSyncTime))
                        Text(
                            text = "Last sync: $dateStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Sync Message Alert
            if (syncMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isError) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = syncMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF1B5E20)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1: Live Cloud Sync
                Button(
                    onClick = {
                        if (webhookUrl.isBlank()) {
                            showSetupDialog = true
                        } else {
                            isSyncing = true
                            syncMessage = null
                            isError = false
                            scope.launch {
                                val records = syncService.buildDailyExportRecords(
                                    allSummaries, allSessions, allChecklistItems, allCompletions
                                )
                                val result = syncService.syncToWebhook(webhookUrl, records)
                                isSyncing = false
                                when (result) {
                                    is SheetsSyncResult.Success -> {
                                        syncMessage = result.message
                                        isError = false
                                        lastSyncTime = syncService.getLastSyncTimestamp()
                                    }
                                    is SheetsSyncResult.Error -> {
                                        syncMessage = result.errorMessage
                                        isError = true
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("sync_google_sheets_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F9D58)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync to Sheets", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Button 2: Export CSV & Open Directly in Google Sheets / Drive
                FilledTonalButton(
                    onClick = {
                        val records = syncService.buildDailyExportRecords(
                            allSummaries, allSessions, allChecklistItems, allCompletions
                        )
                        val uri = syncService.exportToCsvFile(records)
                        if (uri != null) {
                            val shareIntent = syncService.shareCsvIntent(uri)
                            context.startActivity(Intent.createChooser(shareIntent, "Open or Save to Google Sheets"))
                        } else {
                            Toast.makeText(context, "Could not generate CSV file", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("export_csv_sheets_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export CSV", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Setup Helper Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSetupDialog = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoGraph,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "How to connect my own Google Sheet with Graphs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Setup Instructions & Webhook URL Dialog
    if (showSetupDialog) {
        var inputUrl by remember { mutableStateOf(webhookUrl) }

        AlertDialog(
            onDismissRequest = { showSetupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF0F9D58))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Your Google Sheet", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Any user can connect their own private Google Sheet with automated graphs in 3 simple steps:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Step 1
                    Text(
                        text = "1. Create a new Sheet at sheets.google.com and click Extensions > Apps Script.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Step 2 & Copy Code Button
                    Text(
                        text = "2. Copy & paste the script below into Apps Script, then Deploy as Web App (Access: Anyone).",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Daily Sadhana Apps Script", GoogleSheetsSyncService.APPS_SCRIPT_TEMPLATE)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Apps Script code copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("copy_script_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Free Apps Script Code", fontSize = 12.sp)
                    }

                    // Step 3: Paste URL
                    Text(
                        text = "3. Paste your Web App URL here:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("Web App URL") },
                        placeholder = { Text("https://script.google.com/macros/s/...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("webhook_url_input"),
                        singleLine = true
                    )

                    Text(
                        text = "🔒 Strict Unique Dates: Each row in 'Daily Data' is guaranteed to be a unique date. Syncing updates existing rows in place without creating duplicate rows.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF1B5E20),
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "💡 Your sheet will automatically maintain 2 tabs: 'Daily Data' and 'Visual Dashboard' with combo line/bar charts for sleep vs. reading, consistency charts, and KPI formulas!",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        syncService.saveWebhookUrl(inputUrl)
                        webhookUrl = inputUrl.trim()
                        showSetupDialog = false
                        Toast.makeText(context, "Google Sheet URL saved!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("save_webhook_url_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58))
                ) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetupDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
