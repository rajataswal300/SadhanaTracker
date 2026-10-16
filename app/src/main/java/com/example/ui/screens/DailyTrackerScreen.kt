package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.data.model.ActivitySession
import com.example.data.model.DailySummary
import com.example.ui.components.ActiveSessionBanner
import com.example.ui.components.LectureSection
import com.example.ui.components.ReadingSection
import com.example.ui.components.ShareReportDialog
import com.example.ui.components.SleepSection
import com.example.ui.viewmodel.DailyTrackerViewModel

@Composable
fun DailyTrackerScreen(
    viewModel: DailyTrackerViewModel,
    selectedDateKey: String,
    currentTickerTime: Long,
    sessions: List<ActivitySession>,
    activeSessions: List<ActivitySession>,
    dailySummary: DailySummary?
) {
    var showShareDialog by remember { mutableStateOf(false) }

    val activeSleepSession = activeSessions.find { it.activityType == "SLEEP" }
    val activeReadingSession = activeSessions.find { it.activityType == "READING" }
    val isToday = viewModel.isToday(selectedDateKey)
    val displayDate = viewModel.formatDisplayDate(selectedDateKey)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("daily_tracker_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Selector & Share Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("date_selector_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { viewModel.navigateDate(-1) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("prev_date_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { viewModel.jumpToToday() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isToday) "Today ($displayDate)" else displayDate,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateDate(1) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("next_date_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                    }
                }
            }
        }

        // Active Session Pulsing Live Banner
        if (activeSessions.isNotEmpty()) {
            item {
                ActiveSessionBanner(
                    activeSessions = activeSessions,
                    currentTickerTime = currentTickerTime,
                    onStopSession = { sessionId -> viewModel.stopSession(sessionId) }
                )
            }
        }

        // Sleep & Rest Section
        item {
            SleepSection(
                sessions = sessions,
                dailySummary = dailySummary,
                activeSleepSession = activeSleepSession,
                onStartSleep = { isNight -> viewModel.startSleepSession(isNight) },
                onWakeUp = { id -> viewModel.stopSession(id) },
                onUpdateTimes = { bed, wake -> viewModel.updateBedAndWakeTimes(bed, wake) },
                onDeleteSession = { id -> viewModel.deleteSession(id) }
            )
        }

        // Book Reading Section
        item {
            ReadingSection(
                sessions = sessions,
                activeReadingSession = activeReadingSession,
                onStartReading = { viewModel.startReadingSession() },
                onStopReading = { id -> viewModel.stopSession(id) },
                onDeleteSession = { id -> viewModel.deleteSession(id) }
            )
        }

        // Prabhupada Lecture Section
        item {
            LectureSection(
                lectureWatched = dailySummary?.prabhupadaLectureWatched ?: false,
                onToggleLecture = { watched -> viewModel.toggleLectureWatched(watched) }
            )
        }

        // Share Daily Report Action Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showShareDialog = true }
                    .testTag("share_daily_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
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
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Share Today's Sadhana Card",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Send summary to friends via WhatsApp or copy",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Button(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.testTag("open_share_dialog_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Share", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        val report = viewModel.generateDailyReportText(selectedDateKey)
        ShareReportDialog(
            title = "Daily Activity Card",
            reportText = report,
            onDismiss = { showShareDialog = false }
        )
    }
}
