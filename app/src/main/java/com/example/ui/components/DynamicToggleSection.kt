package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChecklistEntry
import com.example.ui.util.rememberSadhanaHaptics

@Composable
fun DynamicToggleSection(
    toggleEntries: List<ChecklistEntry>,
    onToggleItem: (itemId: Long, isCompleted: Boolean) -> Unit,
    onAddItem: (title: String) -> Unit,
    onEditTitle: (itemId: Long, newTitle: String) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    isCurrentDay: Boolean = true,
    onOpenReorder: (() -> Unit)? = null
) {
    val haptics = rememberSadhanaHaptics()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<ChecklistEntry?>(null) }
    var itemToDelete by remember { mutableStateOf<ChecklistEntry?>(null) }

    val completedCount = toggleEntries.count { it.isCompleted }
    val totalCount = toggleEntries.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dynamic_toggles_card"),
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF00796B).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF00796B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Daily Sadhana Toggles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCurrentDay) "Resets OFF every new day" else "Logged for this date (Editable on Today)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_toggle_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Switch", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (onOpenReorder != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onOpenReorder,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("reorder_handle_toggles")
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

            Spacer(modifier = Modifier.height(14.dp))

            if (toggleEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No custom toggle switches yet. Tap '+ Add Switch' above to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    toggleEntries.forEach { entry ->
                        val item = entry.item
                        val isChecked = entry.isCompleted

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("toggle_card_${item.id}"),
                            shape = RoundedCornerShape(14.dp),
                            border = if (isChecked) BorderStroke(1.5.dp, Color(0xFF2E7D32))
                            else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isChecked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { editingEntry = entry },
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .testTag("edit_heading_button_${item.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Heading",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isChecked) "Completed today" else "Not completed",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = if (isChecked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (isChecked) "Yes" else "No",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChecked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Switch(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            haptics.toggleClick()
                                            onToggleItem(item.id, it)
                                        },
                                        enabled = isCurrentDay,
                                        modifier = Modifier.testTag("dynamic_switch_${item.id}"),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF2E7D32),
                                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Switch Dialog
    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Toggle Switch", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter switch name (e.g. Mangala Aarti, Deity Worship, Japa Round 16):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Switch Title") },
                        placeholder = { Text("e.g. Mangala Aarti") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_toggle_title_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onAddItem(newTitle.trim())
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_toggle_button")
                ) {
                    Text("Add Switch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Heading / Delete Dialog
    editingEntry?.let { entry ->
        var updatedTitle by remember { mutableStateOf(entry.item.title) }

        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { Text("Edit Toggle Switch", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Update the heading of this switch:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = updatedTitle,
                        onValueChange = { updatedTitle = it },
                        label = { Text("Heading") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_toggle_title_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            itemToDelete = entry
                            editingEntry = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                        modifier = Modifier.fillMaxWidth().testTag("delete_toggle_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Switch")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (updatedTitle.isNotBlank()) {
                            onEditTitle(entry.item.id, updatedTitle.trim())
                            editingEntry = null
                        }
                    },
                    modifier = Modifier.testTag("save_toggle_title_button")
                ) {
                    Text("Save Heading")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete '${entry.item.title}'?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this toggle switch?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteItem(entry.item.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.testTag("confirm_delete_toggle_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
