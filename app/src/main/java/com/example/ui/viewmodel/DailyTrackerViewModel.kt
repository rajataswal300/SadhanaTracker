package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActivitySession
import com.example.data.model.DailySummary
import com.example.data.repository.TrackerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PeriodFilter(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All-Time")
}

data class DayReportItem(
    val dateKey: String,
    val displayLabel: String,
    val sleepMinutes: Long,
    val readingMinutes: Long,
    val lectureWatched: Boolean
)

data class ConsolidatedReport(
    val period: PeriodFilter,
    val averageSleepMinutes: Double,
    val totalSleepMinutes: Long,
    val averageReadingMinutes: Double,
    val totalReadingMinutes: Long,
    val lectureWatchedDays: Int,
    val totalDays: Int,
    val consistencyPercentage: Int,
    val dailyItems: List<DayReportItem>
)

class DailyTrackerViewModel(private val repository: TrackerRepository) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())

    private val _selectedDateKey = MutableStateFlow(dateFormat.format(Date()))
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(PeriodFilter.THIS_WEEK)
    val selectedPeriod: StateFlow<PeriodFilter> = _selectedPeriod.asStateFlow()

    private val _currentTickerTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val currentTickerTimeMillis: StateFlow<Long> = _currentTickerTimeMillis.asStateFlow()

    init {
        // Ticker coroutine updating current time every second for live timer display
        viewModelScope.launch {
            while (isActive) {
                _currentTickerTimeMillis.value = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    val activeSessions: StateFlow<List<ActivitySession>> = repository.observeActiveSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentDaySessions: StateFlow<List<ActivitySession>> = _selectedDateKey
        .flatMapLatest { dateKey -> repository.observeSessionsForDate(dateKey) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentDaySummary: StateFlow<DailySummary?> = _selectedDateKey
        .flatMapLatest { dateKey -> repository.observeDailySummary(dateKey) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSessions: StateFlow<List<ActivitySession>> = repository.observeAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSummaries: StateFlow<List<DailySummary>> = repository.observeAllSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Consolidated report calculation derived from all records & selected period
    val consolidatedReport: StateFlow<ConsolidatedReport> = combine(
        _selectedPeriod,
        allSessions,
        allSummaries
    ) { period, sessions, summaries ->
        computeConsolidatedReport(period, sessions, summaries)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ConsolidatedReport(PeriodFilter.THIS_WEEK, 0.0, 0, 0.0, 0, 0, 0, 0, emptyList())
    )

    fun setSelectedDate(dateKey: String) {
        _selectedDateKey.value = dateKey
    }

    fun setPeriod(period: PeriodFilter) {
        _selectedPeriod.value = period
    }

    fun navigateDate(daysDelta: Int) {
        try {
            val date = dateFormat.parse(_selectedDateKey.value) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_YEAR, daysDelta)
            }
            _selectedDateKey.value = dateFormat.format(cal.time)
        } catch (_: Exception) {
            _selectedDateKey.value = dateFormat.format(Date())
        }
    }

    fun jumpToToday() {
        _selectedDateKey.value = dateFormat.format(Date())
    }

    fun isToday(dateKey: String = _selectedDateKey.value): Boolean {
        return dateKey == dateFormat.format(Date())
    }

    fun formatDisplayDate(dateKey: String = _selectedDateKey.value): String {
        return try {
            val date = dateFormat.parse(dateKey) ?: return dateKey
            displayDateFormat.format(date)
        } catch (_: Exception) {
            dateKey
        }
    }

    fun startSleepSession(isNightSleep: Boolean) {
        viewModelScope.launch {
            val subType = if (isNightSleep) "NIGHT_SLEEP" else "NAP"
            repository.startSession("SLEEP", subType, _selectedDateKey.value)
        }
    }

    fun startReadingSession() {
        viewModelScope.launch {
            repository.startSession("READING", "READING", _selectedDateKey.value)
        }
    }

    fun stopSession(sessionId: Long) {
        viewModelScope.launch {
            repository.stopSession(sessionId)
        }
    }

    fun toggleLectureWatched(watched: Boolean) {
        viewModelScope.launch {
            repository.setLectureWatched(_selectedDateKey.value, watched)
        }
    }

    fun updateBedAndWakeTimes(bedTime: String?, wakeTime: String?) {
        viewModelScope.launch {
            repository.updateTimes(_selectedDateKey.value, bedTime, wakeTime)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    private fun computeConsolidatedReport(
        period: PeriodFilter,
        sessions: List<ActivitySession>,
        summaries: List<DailySummary>
    ): ConsolidatedReport {
        val today = Calendar.getInstance()
        val startDateCal = Calendar.getInstance()

        when (period) {
            PeriodFilter.THIS_WEEK -> {
                startDateCal.add(Calendar.DAY_OF_YEAR, -6) // Last 7 days including today
            }
            PeriodFilter.THIS_MONTH -> {
                startDateCal.set(Calendar.DAY_OF_MONTH, 1) // First day of current month
            }
            PeriodFilter.ALL_TIME -> {
                // Find earliest session or summary or 30 days ago
                val allDates = (sessions.map { it.dateKey } + summaries.map { it.dateKey }).sorted()
                if (allDates.isNotEmpty()) {
                    try {
                        val earliest = dateFormat.parse(allDates.first())
                        if (earliest != null) startDateCal.time = earliest
                    } catch (_: Exception) {}
                } else {
                    startDateCal.add(Calendar.DAY_OF_YEAR, -29)
                }
            }
        }

        val startDateStr = dateFormat.format(startDateCal.time)
        val todayStr = dateFormat.format(today.time)

        // Generate all dates in this range
        val dateList = mutableListOf<String>()
        val iterateCal = startDateCal.clone() as Calendar
        while (!iterateCal.after(today)) {
            dateList.add(dateFormat.format(iterateCal.time))
            iterateCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val summariesByDate = summaries.associateBy { it.dateKey }
        val sessionsByDate = sessions.groupBy { it.dateKey }

        val dailyItems = dateList.map { dateStr ->
            val daySessions = sessionsByDate[dateStr] ?: emptyList()
            val sleepMins = daySessions.filter { it.activityType == "SLEEP" }
                .sumOf { it.durationMinutes }
            val readMins = daySessions.filter { it.activityType == "READING" }
                .sumOf { it.durationMinutes }
            val lecture = summariesByDate[dateStr]?.prabhupadaLectureWatched ?: false

            val shortLabel = try {
                val d = dateFormat.parse(dateStr)
                if (d != null) dayOfWeekFormat.format(d) else dateStr
            } catch (_: Exception) {
                dateStr
            }

            DayReportItem(
                dateKey = dateStr,
                displayLabel = shortLabel,
                sleepMinutes = sleepMins,
                readingMinutes = readMins,
                lectureWatched = lecture
            )
        }

        val totalSleepMins = dailyItems.sumOf { it.sleepMinutes }
        val totalReadMins = dailyItems.sumOf { it.readingMinutes }
        val daysCount = dailyItems.size.coerceAtLeast(1)
        val lectureDays = dailyItems.count { it.lectureWatched }

        val avgSleep = totalSleepMins.toDouble() / daysCount
        val avgRead = totalReadMins.toDouble() / daysCount
        val consistency = ((lectureDays.toDouble() / daysCount) * 100).toInt()

        return ConsolidatedReport(
            period = period,
            averageSleepMinutes = avgSleep,
            totalSleepMinutes = totalSleepMins,
            averageReadingMinutes = avgRead,
            totalReadingMinutes = totalReadMins,
            lectureWatchedDays = lectureDays,
            totalDays = daysCount,
            consistencyPercentage = consistency,
            dailyItems = dailyItems
        )
    }

    fun generateDailyReportText(
        dateKey: String = _selectedDateKey.value,
        userName: String = "Friend"
    ): String {
        val sessions = currentDaySessions.value
        val summary = currentDaySummary.value

        val sleepSessions = sessions.filter { it.activityType == "SLEEP" }
        val readingSessions = sessions.filter { it.activityType == "READING" }

        val totalSleepMins = sleepSessions.sumOf { it.durationMinutes }
        val totalReadMins = readingSessions.sumOf { it.durationMinutes }

        val sleepHours = totalSleepMins / 60
        val sleepRemainMins = totalSleepMins % 60

        val readHours = totalReadMins / 60
        val readRemainMins = totalReadMins % 60

        val lectureStatus = if (summary?.prabhupadaLectureWatched == true) "✅ Completed" else "❌ Not yet"
        val wake = summary?.wakeUpTime ?: "Logged via sessions"
        val bed = summary?.bedTimePreviousNight ?: "Logged via sessions"

        val formattedDate = formatDisplayDate(dateKey)

        return buildString {
            appendLine("🌅 Daily Sadhana & Activity Log")
            appendLine("📅 $formattedDate")
            appendLine("👤 $userName")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🛌 Total Sleep: ${sleepHours}h ${sleepRemainMins}m")
            if (summary?.wakeUpTime != null || summary?.bedTimePreviousNight != null) {
                appendLine("   • Bedtime: $bed | Wake: $wake")
            }
            if (sleepSessions.size > 1) {
                appendLine("   • Sessions: ${sleepSessions.size} (incl. naps)")
            }
            appendLine("📖 Book Reading: ${readHours}h ${readRemainMins}m (${readingSessions.size} sessions)")
            appendLine("🎧 Prabhupada Lecture: $lectureStatus")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("✨ Logged with Daily Sadhana Tracker")
        }
    }

    fun generateConsolidatedReportText(
        period: PeriodFilter = _selectedPeriod.value,
        userName: String = "Friend"
    ): String {
        val report = consolidatedReport.value
        val avgSleepHours = (report.averageSleepMinutes / 60).toInt()
        val avgSleepRemainMins = (report.averageSleepMinutes % 60).toInt()

        val totalSleepHours = report.totalSleepMinutes / 60
        val totalSleepRemainMins = report.totalSleepMinutes % 60

        val avgReadHours = (report.averageReadingMinutes / 60).toInt()
        val avgReadRemainMins = (report.averageReadingMinutes % 60).toInt()

        val totalReadHours = report.totalReadingMinutes / 60
        val totalReadRemainMins = report.totalReadingMinutes % 60

        return buildString {
            appendLine("📊 Consolidated Sadhana Report (${period.label})")
            appendLine("👤 $userName | 📅 Across ${report.totalDays} Days")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🛌 Sleep Average: ${avgSleepHours}h ${avgSleepRemainMins}m / day")
            appendLine("   • Total Sleep: ${totalSleepHours}h ${totalSleepRemainMins}m")
            appendLine("📖 Reading Average: ${avgReadHours}h ${avgReadRemainMins}m / day")
            appendLine("   • Total Reading: ${totalReadHours}h ${totalReadRemainMins}m")
            appendLine("🎧 Lecture Consistency: ${report.lectureWatchedDays}/${report.totalDays} days (${report.consistencyPercentage}%)")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("✨ Keep up the steady sadhana! Daily Sadhana Tracker")
        }
    }

    class Factory(private val repository: TrackerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DailyTrackerViewModel(repository) as T
        }
    }
}
