package com.example.data.sheets

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.ActivitySession
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

data class CustomToggleExport(
    val title: String,
    val isCompleted: Boolean
)

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
    val customToggles: List<CustomToggleExport>
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
        allToggleItems: List<ChecklistItem>,
        allCompletions: List<DailyChecklistCompletion>
    ): List<DailyExportRecord> {
        val allDates = (allSummaries.map { it.dateKey } +
                allSessions.map { it.dateKey } +
                allCompletions.map { it.dateKey }).distinct().sorted()

        val summariesByDate = allSummaries.associateBy { it.dateKey }
        val sessionsByDate = allSessions.groupBy { it.dateKey }
        val completionsByDate = allCompletions.groupBy { it.dateKey }

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

            val customToggles = allToggleItems.map { item ->
                val isDone = dayCompletions.any { it.itemId == item.id && it.isCompleted }
                CustomToggleExport(title = item.title, isCompleted = isDone)
            }

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
                customToggles = customToggles
            )
        }
    }

    fun exportToCsvFile(records: List<DailyExportRecord>): Uri? {
        val allCustomTitles = records.flatMap { it.customToggles.map { t -> t.title } }.distinct()

        val headerBuilder = StringBuilder("Date,Bedtime,Wake Time,Sleep Duration,Reading Time,Prabhupada Lecture,Strong Maya Attack")
        allCustomTitles.forEach { title ->
            headerBuilder.append(",\"${title.replace("\"", "\"\"")}\"")
        }
        headerBuilder.append("\n")

        val csvContent = StringBuilder(headerBuilder.toString())

        records.forEach { r ->
            csvContent.append(
                "\"${r.dateKey}\"," +
                "\"${r.bedtime}\"," +
                "\"${r.wakeTime}\"," +
                "\"${r.sleepFormatted}\"," +
                "\"${r.readingFormatted}\"," +
                "\"${if (r.lectureWatched) "Yes" else "No"}\"," +
                "\"${if (r.strongMayaAttack) "Yes" else "No"}\""
            )

            allCustomTitles.forEach { title ->
                val toggle = r.customToggles.find { it.title == title }
                val isChecked = toggle?.isCompleted == true
                csvContent.append(",\"${if (isChecked) "Yes" else "No"}\"")
            }
            csvContent.append("\n")
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
            putExtra(Intent.EXTRA_TEXT, "Here is your Daily Sadhana export with clean single-column metrics. Each row is unique by date!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun syncToWebhook(webhookUrl: String, records: List<DailyExportRecord>): SheetsSyncResult =
        withContext(Dispatchers.IO) {
            if (webhookUrl.isBlank() || !webhookUrl.startsWith("http")) {
                return@withContext SheetsSyncResult.Error("Please enter a valid Google Apps Script Web App URL")
            }

            try {
                val allCustomTitles = records.flatMap { it.customToggles.map { t -> t.title } }.distinct()
                val customHeadersJson = JSONArray()
                allCustomTitles.forEach { customHeadersJson.put(it) }

                val jsonArray = JSONArray()
                records.forEach { r ->
                    val obj = JSONObject().apply {
                        put("dateKey", r.dateKey)
                        put("bedtime", r.bedtime)
                        put("wakeTime", r.wakeTime)
                        put("sleepDuration", r.sleepFormatted)
                        put("readingTime", r.readingFormatted)
                        put("lectureWatched", if (r.lectureWatched) "Yes" else "No")
                        put("strongMayaAttack", if (r.strongMayaAttack) "Yes" else "No")

                        val togglesObj = JSONObject()
                        r.customToggles.forEach { toggle ->
                            togglesObj.put(toggle.title, if (toggle.isCompleted) "Yes" else "No")
                        }
                        put("customToggles", togglesObj)
                    }
                    jsonArray.put(obj)
                }

                val payload = JSONObject().apply {
                    put("action", "sync_sadhana")
                    put("customHeaders", customHeadersJson)
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
                            message = "Synced ${records.size} unique days! Clean single-column layout updated.",
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
// ─── DAILY SADHANA GOOGLE APPS SCRIPT TEMPLATE (CLEAN SINGLE COLUMNS & UNIQUE DATES) ───
// 1. Open your Google Sheet at sheets.google.com
// 2. Click Extensions > Apps Script
// 3. Paste this code completely, overwriting all existing code.
// 4. Click Deploy > New deployment > Select type 'Web app'
// 5. Execute as: 'Me' | Who has access: 'Anyone'
// 6. Click Deploy and copy the Web App URL into the Android App!

function doPost(e) {
  var lock = LockService.getScriptLock();
  lock.tryLock(30000); // Thread-safe lock to prevent concurrent execution conflicts
  
  try {
    var data = JSON.parse(e.postData.contents);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    
    // 1. Setup or get 'Daily Data' sheet
    var dataSheet = ss.getSheetByName("Daily Data");
    if (!dataSheet) {
      dataSheet = ss.insertSheet("Daily Data");
    }
    
    // Clean core headers — ONE column per metric (NO duplicate raw or 1/0 columns)
    var coreHeaders = [
      "Date", "Bedtime", "Wake Time", "Sleep Duration",
      "Reading Time", "Prabhupada Lecture", "Strong Maya Attack"
    ];
    
    // Append custom dynamic toggle headers (1 column per switch)
    var customHeaders = data.customHeaders || [];
    var fullHeaders = coreHeaders.concat(customHeaders);
    
    // Read ALL existing rows to preserve any older dates not in this batch
    // Using a Map keyed by normalized 'YYYY-MM-DD' GUARANTEES 100% UNIQUE DATE ROWS!
    var uniqueDateMap = {};
    var lastRow = dataSheet.getLastRow();
    var lastCol = dataSheet.getLastColumn();
    
    if (lastRow > 1 && lastCol > 0) {
      var oldValues = dataSheet.getRange(2, 1, lastRow - 1, lastCol).getValues();
      var oldDisplayValues = dataSheet.getRange(2, 1, lastRow - 1, 1).getDisplayValues();
      
      for (var rIdx = 0; rIdx < oldValues.length; rIdx++) {
        var rawDate = oldDisplayValues[rIdx][0] || oldValues[rIdx][0];
        var normDate = normalizeDate(rawDate);
        if (normDate) {
          uniqueDateMap[normDate] = oldValues[rIdx];
        }
      }
    }
    
    // Process incoming records:
    // If date already exists -> UPDATES EXISTING ROW IN PLACE
    // If date is new -> INSERTS AS NEW UNIQUE ROW
    var records = data.records || [];
    var updatedCount = 0;
    var insertedCount = 0;
    
    for (var j = 0; j < records.length; j++) {
      var r = records[j];
      var dateKey = normalizeDate(r.dateKey);
      if (!dateKey) continue;
      
      var rowData = [
        dateKey,
        r.bedtime || "--",
        r.wakeTime || "--",
        r.sleepDuration || "0m",
        r.readingTime || "0m",
        r.lectureWatched || "No",
        r.strongMayaAttack || "No"
      ];
      
      // Dynamic custom toggle values (1 column per toggle)
      var toggles = r.customToggles || {};
      for (var c = 0; c < customHeaders.length; c++) {
        var cTitle = customHeaders[c];
        var itemVal = toggles[cTitle];
        rowData.push(itemVal || "No");
      }
      
      if (uniqueDateMap[dateKey]) {
        updatedCount++;
      } else {
        insertedCount++;
      }
      
      // Overwrite / upsert in unique date map
      uniqueDateMap[dateKey] = rowData;
    }
    
    // Sort all unique dates chronologically ascending (e.g. 2026-10-28, 2026-10-29, 2026-10-30)
    var sortedDates = Object.keys(uniqueDateMap).sort();
    
    // Build clean 2D dataset
    var cleanRows = [];
    for (var s = 0; s < sortedDates.length; s++) {
      var dStr = sortedDates[s];
      var row = uniqueDateMap[dStr];
      while (row.length < fullHeaders.length) {
        row.push("");
      }
      if (row.length > fullHeaders.length) {
        row = row.slice(0, fullHeaders.length);
      }
      row[0] = dStr; // Ensure date column is formatted as clean YYYY-MM-DD
      cleanRows.push(row);
    }
    
    // Clear sheet contents to completely purge any old duplicate or cluttered columns
    dataSheet.clear();
    
    // Set Header row (Row 1)
    dataSheet.getRange(1, 1, 1, fullHeaders.length).setValues([fullHeaders]);
    dataSheet.getRange(1, 1, 1, fullHeaders.length)
      .setBackground("#D84315")
      .setFontColor("#FFFFFF")
      .setFontWeight("bold");
    dataSheet.setFrozenRows(1);
    
    // Set Column A as Plain Text so Google Sheets never shifts dates by timezones
    dataSheet.getRange("A:A").setNumberFormat("@");
    
    // Center align status columns (Lecture, Maya Attack, Custom Switches)
    if (fullHeaders.length >= 6) {
      dataSheet.getRange(1, 6, Math.max(cleanRows.length + 1, 2), fullHeaders.length - 5)
        .setHorizontalAlignment("center");
    }
    
    // Bulk write all clean, deduplicated, unique date rows
    if (cleanRows.length > 0) {
      dataSheet.getRange(2, 1, cleanRows.length, fullHeaders.length).setValues(cleanRows);
    }
    
    dataSheet.autoResizeColumns(1, fullHeaders.length);
    
    // 2. Setup or refresh 'Visual Dashboard' sheet with automated analytics
    setupDashboard(ss, dataSheet, customHeaders);
    
    return ContentService.createTextOutput(JSON.stringify({
      status: "success",
      totalUniqueDates: cleanRows.length,
      columnsCount: fullHeaders.length,
      updatedRows: updatedCount,
      insertedRows: insertedCount
    })).setMimeType(ContentService.MimeType.JSON);
    
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({status: "error", message: err.toString()}))
      .setMimeType(ContentService.MimeType.JSON);
  } finally {
    lock.releaseLock();
  }
}

function normalizeDate(val) {
  if (!val) return "";
  if (val instanceof Date) {
    var y = val.getFullYear();
    var m = ("0" + (val.getMonth() + 1)).slice(-2);
    var d = ("0" + val.getDate()).slice(-2);
    return y + "-" + m + "-" + d;
  }
  var s = String(val).trim();
  var match = s.match(/(\d{4})-(\d{1,2})-(\d{1,2})/);
  if (match) {
    var y = match[1];
    var m = ("0" + match[2]).slice(-2);
    var d = ("0" + match[3]).slice(-2);
    return y + "-" + m + "-" + d;
  }
  return s;
}

function setupDashboard(ss, dataSheet, customHeaders) {
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
  dash.getRange("A1:H1").merge().setValue("🧘 DAILY SADHANA ANALYTICS DASHBOARD")
    .setBackground("#5D4037").setFontColor("#FFFFFF").setFontWeight("bold").setFontSize(14).setHorizontalAlignment("center");
    
  var totalRowsFormula = "COUNTA('Daily Data'!A2:A100)";
  
  dash.getRange("A3").setValue("Total Logged Days:").setFontWeight("bold");
  dash.getRange("B3").setFormula('=' + totalRowsFormula);
  
  dash.getRange("D3").setValue("Lecture Completed Days:").setFontWeight("bold");
  dash.getRange("E3").setFormula('=COUNTIF(\'Daily Data\'!F2:F100, "Yes")');
  
  dash.getRange("G3").setValue("Strong Maya Attack Faced:").setFontWeight("bold");
  dash.getRange("H3").setFormula('=COUNTIF(\'Daily Data\'!G2:G100, "Yes")');
  
  dash.getRange("A5").setValue("Lecture Consistency:").setFontWeight("bold");
  dash.getRange("B5").setFormula('=IF(' + totalRowsFormula + '>0, TEXT(COUNTIF(\'Daily Data\'!F2:F100, "Yes")/' + totalRowsFormula + ',"0%"), "0%")');
}
"""
    }
}
