package com.example.data.sheets

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.ActivitySession
import com.example.data.model.ChecklistEntry
import com.example.data.model.ChecklistItem
import com.example.data.model.DailyChecklistCompletion
import com.example.data.model.DailySummary
import com.example.ui.components.TimerHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DailyExportRecord(
    val dateKey: String,
    val bedtime: String,
    val wakeTime: String,
    val sleepHoursDecimal: Double,
    val sleepFormatted: String,
    val readingMinutes: Long,
    val readingFormatted: String,
    val lectureWatched: Boolean,
    val lectureNumeric: Int,
    val strongMayaAttack: Boolean,
    val mayaAttackNumeric: Int,
    val checklistRatio: String,
    val checklistPercent: Int,
    val completedHabits: String
)

sealed class SheetsSyncResult {
    data class Success(val message: String, val rowsSynced: Int) : SheetsSyncResult()
    data class Error(val errorMessage: String) : SheetsSyncResult()
}

class GoogleSheetsSyncService(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val prefs = context.getSharedPreferences("google_sheets_sync_prefs", Context.MODE_PRIVATE)

    fun getSavedWebhookUrl(): String {
        return prefs.getString("webhook_url", "") ?: ""
    }

    fun saveWebhookUrl(url: String) {
        prefs.edit().putString("webhook_url", url.trim()).apply()
    }

    fun getLastSyncTimestamp(): Long {
        return prefs.getLong("last_sync_timestamp", 0L)
    }

    fun setLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong("last_sync_timestamp", timestamp).apply()
    }

    fun buildDailyExportRecords(
        allSummaries: List<DailySummary>,
        allSessions: List<ActivitySession>,
        allChecklistItems: List<ChecklistItem>,
        allCompletions: List<DailyChecklistCompletion>
    ): List<DailyExportRecord> {
        val allDates = (allSummaries.map { it.dateKey } +
                allSessions.map { it.dateKey } +
                allCompletions.map { it.dateKey }).distinct().sorted()

        val summariesByDate = allSummaries.associateBy { it.dateKey }
        val sessionsByDate = allSessions.groupBy { it.dateKey }
        val completionsByDate = allCompletions.groupBy { it.dateKey }
        val checklistMap = allChecklistItems.associateBy { it.id }

        return allDates.map { date ->
            val summary = summariesByDate[date]
            val daySessions = sessionsByDate[date] ?: emptyList()
            val dayCompletions = completionsByDate[date] ?: emptyList()

            val sleepSessions = daySessions.filter { it.activityType == "SLEEP" }
            val readSessions = daySessions.filter { it.activityType == "READING" }

            val totalSleepMinutes = sleepSessions.sumOf { it.durationMinutes }
            val totalReadMinutes = readSessions.sumOf { it.durationMinutes }

            val sleepHoursDecimal = Math.round((totalSleepMinutes / 60.0) * 100.0) / 100.0

            val bedtime = summary?.bedTimePreviousNight ?: "--"
            val wakeTime = summary?.wakeUpTime ?: "--"

            val lecture = summary?.prabhupadaLectureWatched ?: false
            val maya = summary?.strongMayaAttack ?: false

            val completedItems = dayCompletions.filter { it.isCompleted }
                .mapNotNull { checklistMap[it.itemId]?.title }

            val totalChecklist = allChecklistItems.size
            val completedCount = completedItems.size
            val ratio = if (totalChecklist > 0) "$completedCount/$totalChecklist" else "0/0"
            val percent = if (totalChecklist > 0) (completedCount * 100) / totalChecklist else 0

            DailyExportRecord(
                dateKey = date,
                bedtime = bedtime,
                wakeTime = wakeTime,
                sleepHoursDecimal = sleepHoursDecimal,
                sleepFormatted = TimerHelpers.formatMinutes(totalSleepMinutes),
                readingMinutes = totalReadMinutes,
                readingFormatted = TimerHelpers.formatMinutes(totalReadMinutes),
                lectureWatched = lecture,
                lectureNumeric = if (lecture) 1 else 0,
                strongMayaAttack = maya,
                mayaAttackNumeric = if (maya) 1 else 0,
                checklistRatio = ratio,
                checklistPercent = percent,
                completedHabits = completedItems.joinToString(", ")
            )
        }
    }

    fun exportToCsvFile(records: List<DailyExportRecord>): Uri? {
        val csvHeader = "Date,Bedtime,Wake Time,Sleep Hours (Decimal),Sleep Formatted,Reading Minutes,Reading Formatted,Prabhupada Lecture,Strong Maya Attack,Checklist Progress,Checklist %,Completed Habits\n"
        val csvContent = StringBuilder(csvHeader)

        records.forEach { r ->
            csvContent.append(
                "\"${r.dateKey}\"," +
                "\"${r.bedtime}\"," +
                "\"${r.wakeTime}\"," +
                "${r.sleepHoursDecimal}," +
                "\"${r.sleepFormatted}\"," +
                "${r.readingMinutes}," +
                "\"${r.readingFormatted}\"," +
                "\"${if (r.lectureWatched) "Yes" else "No"}\"," +
                "\"${if (r.strongMayaAttack) "Yes" else "No"}\"," +
                "\"${r.checklistRatio}\"," +
                "${r.checklistPercent}," +
                "\"${r.completedHabits.replace("\"", "\"\"")}\"\n"
            )
        }

        return try {
            val file = File(context.cacheDir, "Daily_Sadhana_Export.csv")
            FileOutputStream(file).use { out ->
                out.write(csvContent.toString().toByteArray())
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareCsvIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Daily Sadhana Data for Google Sheets")
            putExtra(Intent.EXTRA_TEXT, "Here is the Daily Sadhana tracker data export. You can open and import this directly into Google Sheets!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun syncToWebhook(webhookUrl: String, records: List<DailyExportRecord>): SheetsSyncResult =
        withContext(Dispatchers.IO) {
            if (webhookUrl.isBlank() || !webhookUrl.startsWith("http")) {
                return@withContext SheetsSyncResult.Error("Please enter a valid Google Apps Script Web App URL")
            }

            try {
                val jsonArray = JSONArray()
                records.forEach { r ->
                    val obj = JSONObject().apply {
                        put("dateKey", r.dateKey)
                        put("bedtime", r.bedtime)
                        put("wakeTime", r.wakeTime)
                        put("sleepHoursDecimal", r.sleepHoursDecimal)
                        put("sleepFormatted", r.sleepFormatted)
                        put("readingMinutes", r.readingMinutes)
                        put("readingFormatted", r.readingFormatted)
                        put("lectureWatched", if (r.lectureWatched) "Yes" else "No")
                        put("lectureNumeric", r.lectureNumeric)
                        put("strongMayaAttack", if (r.strongMayaAttack) "Yes" else "No")
                        put("mayaAttackNumeric", r.mayaAttackNumeric)
                        put("checklistRatio", r.checklistRatio)
                        put("checklistPercent", r.checklistPercent)
                        put("completedHabits", r.completedHabits)
                    }
                    jsonArray.put(obj)
                }

                val payload = JSONObject().apply {
                    put("action", "sync_sadhana")
                    put("records", jsonArray)
                    put("syncDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                }

                val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        setLastSyncTimestamp(System.currentTimeMillis())
                        SheetsSyncResult.Success(
                            message = "Successfully synced ${records.size} days to your Google Sheet with charts!",
                            rowsSynced = records.size
                        )
                    } else {
                        SheetsSyncResult.Error("Sync failed: HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                SheetsSyncResult.Error(e.message ?: "Network error connecting to Google Sheets Web App")
            }
        }

    companion object {
        const val APPS_SCRIPT_TEMPLATE = """
// ─── DAILY SADHANA GOOGLE APPS SCRIPT TEMPLATE ───
// 1. Create a new Google Sheet at sheets.google.com
// 2. Click Extensions > Apps Script
// 3. Paste this code completely, overwriting any template code.
// 4. Click Deploy > New deployment > Select 'Web app'
// 5. Execute as: 'Me' | Who has access: 'Anyone'
// 6. Click Deploy and copy the Web App URL into the Sadhana Android App!

function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    
    // 1. Setup or get 'Daily Data' sheet
    var dataSheet = ss.getSheetByName("Daily Data");
    if (!dataSheet) {
      dataSheet = ss.insertSheet("Daily Data");
      dataSheet.appendRow([
        "Date", "Bedtime", "Wake Time", "Sleep Hours", "Sleep (Formatted)",
        "Reading (Minutes)", "Reading (Formatted)", "Prabhupada Lecture",
        "Lecture (1/0)", "Maya Attack", "Maya Attack (1/0)", "Checklist", "Checklist %", "Completed Habits"
      ]);
      dataSheet.getRange(1, 1, 1, 14).setBackground("#D84315").setFontColor("#FFFFFF").setFontWeight("bold");
      dataSheet.setFrozenRows(1);
    }
    
    // Read existing dates to update or append
    var existingRows = dataSheet.getDataRange().getValues();
    var dateRowMap = {};
    for (var i = 1; i < existingRows.length; i++) {
      dateRowMap[existingRows[i][0]] = i + 1; // row number
    }
    
    var records = data.records || [];
    for (var j = 0; j < records.length; j++) {
      var r = records[j];
      var rowData = [
        r.dateKey, r.bedtime, r.wakeTime, r.sleepHoursDecimal, r.sleepFormatted,
        r.readingMinutes, r.readingFormatted, r.lectureWatched, r.lectureNumeric,
        r.strongMayaAttack, r.mayaAttackNumeric, r.checklistRatio, r.checklistPercent, r.completedHabits
      ];
      
      if (dateRowMap[r.dateKey]) {
        dataSheet.getRange(dateRowMap[r.dateKey], 1, 1, 14).setValues([rowData]);
      } else {
        dataSheet.appendRow(rowData);
      }
    }
    dataSheet.autoResizeColumns(1, 14);
    
    // 2. Setup or get 'Visual Dashboard' sheet with automated graphs
    setupDashboard(ss, dataSheet);
    
    return ContentService.createTextOutput(JSON.stringify({status: "success", count: records.length}))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({status: "error", message: err.toString()}))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function setupDashboard(ss, dataSheet) {
  var dash = ss.getSheetByName("Visual Dashboard");
  if (!dash) {
    dash = ss.insertSheet("Visual Dashboard");
  }
  
  // Clean prior charts to re-render fresh
  var existingCharts = dash.getCharts();
  for (var k = 0; k < existingCharts.length; k++) {
    dash.removeChart(existingCharts[k]);
  }
  
  // KPI Header block
  dash.getRange("A1:H1").merge().setValue("🧘 DAILY SADHANA VISUAL DASHBOARD & ANALYTICS")
    .setBackground("#5D4037").setFontColor("#FFFFFF").setFontWeight("bold").setFontSize(14).setHorizontalAlignment("center");
    
  dash.getRange("A3").setValue("7-Day Avg Sleep (Hours):").setFontWeight("bold");
  dash.getRange("B3").setFormula('=AVERAGE(\'Daily Data\'!D2:D8)');
  
  dash.getRange("D3").setValue("Weekly Total Reading (Mins):").setFontWeight("bold");
  dash.getRange("E3").setFormula('=SUM(\'Daily Data\'!F2:F8)');
  
  dash.getRange("G3").setValue("Lecture Consistency:").setFontWeight("bold");
  dash.getRange("H3").setFormula('=TEXT(AVERAGE(\'Daily Data\'!I2:I100),"0%")');
  
  var lastRow = Math.max(dataSheet.getLastRow(), 2);
  
  // Chart 1: Sleep Hours & Reading Minutes Trend Combo Chart
  var chart1 = dash.newChart()
    .setChartType(Charts.ChartType.COMBO)
    .addRange(dataSheet.getRange("A1:A" + lastRow))
    .addRange(dataSheet.getRange("D1:D" + lastRow))
    .addRange(dataSheet.getRange("F1:F" + lastRow))
    .setPosition(5, 1, 0, 0)
    .setOption("title", "Weekly & Monthly Sleep (Hours) vs Reading (Minutes)")
    .setOption("width", 680)
    .setOption("height", 340)
    .setOption("series", {
      0: {type: "line", targetAxisIndex: 0, color: "#3F51B5"},
      1: {type: "bars", targetAxisIndex: 1, color: "#D84315"}
    })
    .setOption("vAxes", {
      0: {title: "Sleep (Hours)"},
      1: {title: "Reading (Minutes)"}
    })
    .build();
  dash.insertChart(chart1);
  
  // Chart 2: Prabhupada Lecture & Maya Attack Frequency
  var chart2 = dash.newChart()
    .setChartType(Charts.ChartType.COLUMN)
    .addRange(dataSheet.getRange("A1:A" + lastRow))
    .addRange(dataSheet.getRange("I1:I" + lastRow))
    .addRange(dataSheet.getRange("K1:K" + lastRow))
    .setPosition(24, 1, 0, 0)
    .setOption("title", "Lecture Completed (1) & Strong Maya Attack (1)")
    .setOption("width", 680)
    .setOption("height", 300)
    .setOption("colors", ["#2E7D32", "#D32F2F"])
    .build();
  dash.insertChart(chart2);
}
"""
    }
}
