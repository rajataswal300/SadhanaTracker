package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.example.ui.util.rememberSadhanaHaptics
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivitySession
import com.example.data.model.ChecklistEntry
import com.example.data.model.DailySummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PastDaySummaryView(
    dateKey: String,
    displayDate: String,
    sessions: List<ActivitySession>,
    dailySummary: DailySummary?,
    checklistEntries: List<ChecklistEntry>,
    onJumpToToday: () -> Unit,
    onShareDay: () -> Unit,
    onToggleLecture: (Boolean) -> Unit,
    onToggleMayaAttack: (Boolean) -> Unit,
    onUpdateTimes: (bedTime: String?, wakeTime: String?) -> Unit,
    onToggleChecklistItem: (itemId: Long, isCompleted: Boolean) -> Unit
) {
    val sleepSessions = sessions.filter { it.activityType == "SLEEP" }
    val readingSessions = sessions.filter { it.activityType == "READING" }

    val totalSleepMinutes = sleepSessions.sumOf { it.durationMinutes }
    val totalReadingMinutes = readingSessions.sumOf { it.durationMinutes }

    val lectureWatched = dailySummary?.prabhupadaLectureWatched ?: false
    val mayaAttack = dailySummary?.strongMayaAttack ?: false

    val completedHabits = checklistEntries.filter { it.isCompleted }
    val totalHabits = checklistEntries.size

    var showEditDialog by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    val haptics = rememberSadhanaHaptics()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("past_day_summary_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Overview Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Daily Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Jump to Today Button
                    FilledTonalButton(
                        onClick = onJumpToToday,
                        modifier = Modifier.testTag("jump_to_today_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Today", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Stats Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryMetricPill(
                        label = "Sleep",
                        value = TimerHelpers.formatMinutes(totalSleepMinutes),
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFF3F51B5)
                    )
                    SummaryMetricPill(
                        label = "Reading",
                        value = TimerHelpers.formatMinutes(totalReadingMinutes),
                        modifier = Modifier.weight(1f),
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    SummaryMetricPill(
                        label = "Checklist",
                        value = "$totalHabits / ${completedHabits.size}",
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFF2E7D32)
                    )
                }
            }
        }

        // Action Toolbar (Edit This Day & Share)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { showEditDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("edit_this_day_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit This Day", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            Button(
                onClick = onShareDay,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("share_this_day_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Card", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        // Sleep & Rest Summary Card
        PastDayMetricCard(
            title = "Sleep & Rest",
            mainText = TimerHelpers.formatMinutes(totalSleepMinutes),
            subText = buildString {
                val bed = dailySummary?.bedTimePreviousNight ?: "--"
                val wake = dailySummary?.wakeUpTime ?: "--"
                append("Bedtime: $bed • Wake: $wake")
                if (sleepSessions.size > 1) {
                    append(" • ${sleepSessions.size} sessions")
                }
            },
            icon = Icons.Default.Bedtime,
            iconTint = Color(0xFF3F51B5),
            badgeText = if (totalSleepMinutes > 0) "${(totalSleepMinutes / 60.0).let { Math.round(it * 10) / 10.0 }}h" else "0h"
        )

        // Book Reading Summary Card
        PastDayMetricCard(
            title = "Book Reading",
            mainText = TimerHelpers.formatMinutes(totalReadingMinutes),
            subText = if (readingSessions.isNotEmpty()) "${readingSessions.size} reading session(s) logged" else "No reading logged for this day",
            icon = Icons.Default.MenuBook,
            iconTint = MaterialTheme.colorScheme.primary,
            badgeText = "${totalReadingMinutes}m"
        )

        // Prabhupada Lecture Status Card
        PastDayMetricCard(
            title = "Prabhupada Lecture",
            mainText = if (lectureWatched) "Completed" else "Not Watched",
            subText = if (lectureWatched) "Spiritual nourishment received" else "No lecture recorded for this day",
            icon = Icons.Default.Headphones,
            iconTint = if (lectureWatched) Color(0xFF2E7D32) else Color(0xFF757575),
            statusIcon = if (lectureWatched) Icons.Default.CheckCircle else Icons.Default.Close,
            statusColor = if (lectureWatched) Color(0xFF2E7D32) else Color(0xFF757575)
        )

        // Strong Maya Attack Status Card
        PastDayMetricCard(
            title = "Strong Maya Attack",
            mainText = if (mayaAttack) "Faced Strong Attack" else "Protected & Steady",
            subText = if (mayaAttack) "Chanted and took shelter of Krishna" else "Mind remained steady in sadhana",
            icon = if (mayaAttack) Icons.Default.WarningAmber else Icons.Default.Security,
            iconTint = if (mayaAttack) Color(0xFFD32F2F) else Color(0xFF2E7D32),
            statusIcon = if (mayaAttack) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
            statusColor = if (mayaAttack) Color(0xFFD32F2F) else Color(0xFF2E7D32)
        )

        // Daily Sadhana Toggles Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("past_day_checklist_card"),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF00796B).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00796B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Daily Sadhana Toggles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${completedHabits.size} of $totalHabits toggles completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (completedHabits.size == totalHabits && totalHabits > 0) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (totalHabits > 0) "${(completedHabits.size * 100) / totalHabits}%" else "0%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (completedHabits.size == totalHabits && totalHabits > 0) Color(0xFF2E7D32)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (checklistEntries.isEmpty()) {
                    Text(
                        text = "No custom toggles were tracked on this date.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        checklistEntries.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (entry.isCompleted) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = if (entry.isCompleted) 1.5.dp else 0.5.dp,
                                        color = if (entry.isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (entry.isCompleted) Icons.Default.CheckCircle else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (entry.isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = entry.item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (entry.isCompleted) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expandable Detailed Sessions Breakdown
        if (sessions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDetails = !showDetails }
                    .testTag("session_details_card"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Individual Sessions Breakdown (${sessions.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = showDetails) {
                        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sessions.forEach { s ->
                                val startStr = timeFormat.format(Date(s.startTimeMillis))
                                val endStr = if (s.endTimeMillis != null) timeFormat.format(Date(s.endTimeMillis)) else "In progress"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = if (s.activityType == "SLEEP") "Sleep: ${s.subType}" else "Reading",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "$startStr – $endStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = TimerHelpers.formatMinutes(s.durationMinutes),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (s.activityType == "SLEEP") Color(0xFF3F51B5) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // "Edit This Day" Retroactive Edit Dialog
    if (showEditDialog) {
        var editBedTime by remember { mutableStateOf(dailySummary?.bedTimePreviousNight ?: "") }
        var editWakeTime by remember { mutableStateOf(dailySummary?.wakeUpTime ?: "") }
        var editLecture by remember { mutableStateOf(lectureWatched) }
        var editMaya by remember { mutableStateOf(mayaAttack) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Record ($dateKey)", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Adjust any missed logs for this day:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Bed & Wake Times
                    OutlinedTextField(
                        value = editBedTime,
                        onValueChange = { editBedTime = it },
                        label = { Text("Bedtime (Previous Night)") },
                        placeholder = { Text("e.g. 10:30 PM") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_bedtime_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editWakeTime,
                        onValueChange = { editWakeTime = it },
                        label = { Text("Wake-up Time") },
                        placeholder = { Text("e.g. 04:30 AM") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_waketime_input"),
                        singleLine = true
                    )

                    HorizontalDivider()

                    // Prabhupada Lecture Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Prabhupada Lecture", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(if (editLecture) "Watched" else "Not watched", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = editLecture,
                            onCheckedChange = {
                                haptics.toggleClick()
                                editLecture = it
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF2E7D32))
                        )
                    }

                    // Strong Maya Attack Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Strong Maya Attack", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(if (editMaya) "Faced Attack" else "Protected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = editMaya,
                            onCheckedChange = {
                                haptics.toggleClick()
                                editMaya = it
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFD32F2F))
                        )
                    }

                    if (checklistEntries.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Custom Toggle Switches for this Day:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        checklistEntries.forEach { entry ->
                            var checkedState by remember(entry.item.id) { mutableStateOf(entry.isCompleted) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = entry.item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (checkedState) "Yes" else "No",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (checkedState) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Switch(
                                        checked = checkedState,
                                        onCheckedChange = {
                                            haptics.toggleClick()
                                            checkedState = it
                                            onToggleChecklistItem(entry.item.id, it)
                                        },
                                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF2E7D32))
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateTimes(
                            editBedTime.trim().ifBlank { null },
                            editWakeTime.trim().ifBlank { null }
                        )
                        onToggleLecture(editLecture)
                        onToggleMayaAttack(editMaya)
                        showEditDialog = false
                    },
                    modifier = Modifier.testTag("save_past_day_edit_button")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SummaryMetricPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PastDayMetricCard(
    title: String,
    mainText: String,
    subText: String,
    icon: ImageVector,
    iconTint: Color,
    badgeText: String? = null,
    statusIcon: ImageVector? = null,
    statusColor: Color? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(iconTint.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mainText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = iconTint
                    )
                }
            } else if (statusIcon != null && statusColor != null) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
