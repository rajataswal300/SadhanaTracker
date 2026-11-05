package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.auth.AuthManager
import com.example.data.db.AppDatabase
import com.example.data.firestore.FirestoreSyncRepository
import com.example.data.repository.TrackerRepository
import com.example.ui.components.AccountSyncSheet
import com.example.ui.components.ShareReportDialog
import com.example.ui.screens.DailyTrackerScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DailyTrackerViewModel

enum class MainDestination(val label: String) {
    TODAY("Today"),
    ANALYTICS("Analytics")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (_: Exception) {}
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val firestoreSync = remember { FirestoreSyncRepository(context, database.activityDao(), database.bookDao()) }
    val repository = remember { TrackerRepository(database.activityDao(), database.bookDao(), firestoreSync) }
    val layoutPreferences = remember { com.example.data.prefs.LayoutPreferences(context) }
    val authManager = remember { AuthManager(context) }
    val viewModel: DailyTrackerViewModel = viewModel(
        factory = DailyTrackerViewModel.Factory(repository, layoutPreferences, authManager)
    )

    var currentScreen by remember { mutableStateOf(MainDestination.TODAY) }
    var showTopShareDialog by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val lastSyncTimeMillis by viewModel.lastSyncTimeMillis.collectAsStateWithLifecycle()

    val selectedDateKey by viewModel.selectedDateKey.collectAsStateWithLifecycle()
    val currentTickerTime by viewModel.currentTickerTimeMillis.collectAsStateWithLifecycle()
    val activeSessions by viewModel.activeSessions.collectAsStateWithLifecycle()
    val daySessions by viewModel.currentDaySessions.collectAsStateWithLifecycle()
    val daySummary by viewModel.currentDaySummary.collectAsStateWithLifecycle()
    val checklistEntries by viewModel.currentDayChecklistEntries.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val consolidatedReport by viewModel.consolidatedReport.collectAsStateWithLifecycle()
    val allSummaries by viewModel.allSummaries.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val allChecklistItems by viewModel.allChecklistItems.collectAsStateWithLifecycle()
    val allCompletions by viewModel.allChecklistCompletions.collectAsStateWithLifecycle()
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()

    BackHandler(enabled = currentScreen != MainDestination.TODAY) {
        currentScreen = MainDestination.TODAY
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentScreen == MainDestination.TODAY) "Daily Sadhana" else "Consolidated Analytics",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showAccountSheet = true },
                        modifier = Modifier.testTag("top_app_bar_account_button")
                    ) {
                        if (currentUser != null) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = (currentUser?.displayName ?: currentUser?.email ?: "U")
                                    .firstOrNull()?.uppercase() ?: "U"
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Cloud Sync & Google Login",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = { showTopShareDialog = true },
                        modifier = Modifier.testTag("top_app_bar_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = currentScreen == MainDestination.TODAY,
                    onClick = { currentScreen = MainDestination.TODAY },
                    icon = { Icon(Icons.Default.Today, contentDescription = "Today") },
                    label = { Text("Today") },
                    modifier = Modifier.testTag("nav_tab_today"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == MainDestination.ANALYTICS,
                    onClick = { currentScreen = MainDestination.ANALYTICS },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    modifier = Modifier.testTag("nav_tab_analytics"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                MainDestination.TODAY -> {
                    DailyTrackerScreen(
                        viewModel = viewModel,
                        selectedDateKey = selectedDateKey,
                        currentTickerTime = currentTickerTime,
                        sessions = daySessions,
                        activeSessions = activeSessions,
                        dailySummary = daySummary,
                        checklistEntries = checklistEntries
                    )
                }
                MainDestination.ANALYTICS -> {
                    ReportsScreen(
                        viewModel = viewModel,
                        consolidatedReport = consolidatedReport,
                        selectedPeriod = selectedPeriod,
                        allSummaries = allSummaries,
                        allSessions = allSessions,
                        allChecklistItems = allChecklistItems,
                        allCompletions = allCompletions,
                        allBooks = allBooks
                    )
                }
            }
        }
    }

    if (showTopShareDialog) {
        val shareText = if (currentScreen == MainDestination.TODAY) {
            viewModel.generateDailyReportText(selectedDateKey)
        } else {
            viewModel.generateConsolidatedReportText(selectedPeriod)
        }
        val title = if (currentScreen == MainDestination.TODAY) "Daily Activity Card" else "${selectedPeriod.label} Summary"
        ShareReportDialog(
            title = title,
            reportText = shareText,
            onDismiss = { showTopShareDialog = false }
        )
    }

    if (showAccountSheet) {
        AccountSyncSheet(
            viewModel = viewModel,
            currentUser = currentUser,
            syncStatus = syncStatus,
            lastSyncTimeMillis = lastSyncTimeMillis,
            onDismiss = { showAccountSheet = false }
        )
    }
}
