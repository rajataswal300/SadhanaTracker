package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailySummary
import com.example.data.sheets.GoogleSheetsSyncService
import com.example.data.sheets.SheetsSyncResult
import com.example.ui.util.rememberSadhanaHaptics
import kotlinx.coroutines.launch

@Composable
fun NotesAndLearningsCard(
    currentNotes: String,
    allSummariesWithNotes: List<DailySummary>,
    previousNotesCandidate: Pair<String, String>?,
    onSaveNotes: (String) -> Unit,
    onOpenHistory: () -> Unit,
    isCurrentDay: Boolean = true,
    onOpenReorder: (() -> Unit)? = null
) {
    val haptics = rememberSadhanaHaptics()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val syncService = remember { GoogleSheetsSyncService(context) }

    var textState by remember(currentNotes) { mutableStateOf(currentNotes) }
    var isSaving by remember { mutableStateOf(false) }
    var saveConfirmationVisible by remember { mutableStateOf(false) }

    // Sync state
    var isSyncing by remember { mutableStateOf(false) }
    var syncFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var syncIsError by remember { mutableStateOf(false) }

    val hasChanges = textState != currentNotes

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notes_and_learnings_card"),
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF2E7D32).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Notes & Learnings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCurrentDay) "Carries forward daily • Syncs to Sheet" else "Logged realizations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // History button
                    IconButton(
                        onClick = {
                            haptics.toggleClick()
                            onOpenHistory()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("open_notes_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View Learnings History",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Drag handle
                    if (onOpenReorder != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onOpenReorder,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("reorder_handle_notes")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = "Rearrange Sections",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Carry-Forward Context Chip if candidate is available and notes are identical or can be re-applied
            if (previousNotesCandidate != null && previousNotesCandidate.second.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2E7D32).copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Carry-forwarded from ${previousNotesCandidate.first}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1B5E20),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (textState != previousNotesCandidate.second) {
                        TextButton(
                            onClick = {
                                haptics.toggleClick()
                                textState = previousNotesCandidate.second
                                onSaveNotes(previousNotesCandidate.second)
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Re-apply", fontSize = 11.sp, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-line Text Area
            OutlinedTextField(
                value = textState,
                onValueChange = { newText ->
                    textState = newText
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_notes_input"),
                placeholder = {
                    Text(
                        text = "Write your realizations, sloka reflections, or key learnings today. Previous day notes carry forward automatically...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                minLines = 4,
                maxLines = 10,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2E7D32),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action row: Save Button + Sync to Sheet Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Save Note Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            haptics.toggleClick()
                            onSaveNotes(textState)
                            saveConfirmationVisible = true
                        },
                        modifier = Modifier.testTag("save_notes_button"),
                        enabled = hasChanges,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    AnimatedVisibility(
                        visible = saveConfirmationVisible && !hasChanges,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Saved!",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Sync Notes to Google Sheet Button
                FilledTonalButton(
                    onClick = {
                        val webhook = syncService.getSavedWebhookUrl()
                        if (webhook.isBlank()) {
                            syncIsError = true
                            syncFeedbackMessage = "Please set Webhook URL in Google Sheets section first"
                            return@FilledTonalButton
                        }

                        haptics.toggleClick()
                        isSyncing = true
                        syncFeedbackMessage = null
                        syncIsError = false

                        coroutineScope.launch {
                            // Ensure today's note is committed first
                            onSaveNotes(textState)
                            val notesRecords = syncService.buildNotesExportRecords(allSummariesWithNotes)
                            val result = syncService.syncNotesToWebhook(webhook, notesRecords)
                            isSyncing = false
                            when (result) {
                                is SheetsSyncResult.Success -> {
                                    syncIsError = false
                                    syncFeedbackMessage = result.message
                                }
                                is SheetsSyncResult.Error -> {
                                    syncIsError = true
                                    syncFeedbackMessage = result.errorMessage
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("sync_notes_to_sheet_button"),
                    enabled = !isSyncing,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Syncing...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync to Sheet", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Sync feedback message banner
            AnimatedVisibility(visible = syncFeedbackMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (syncIsError) MaterialTheme.colorScheme.errorContainer else Color(0xFF2E7D32).copy(alpha = 0.12f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = syncFeedbackMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (syncIsError) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF1B5E20),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
