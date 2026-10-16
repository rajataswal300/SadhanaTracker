# Daily Activity & Sadhana Tracker

A cross-device Android habit tracker featuring live start/stop timers for sleep and book reading sessions, daily Prabhupada lecture completion tracking, cloud synchronization with Google Sign-In, shareable daily report cards, and a dedicated consolidated Analytics tab calculating average sleep, reading totals, and consistency across custom time horizons (This Week, This Month, All-Time).

## User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions were confirmed during the planning and revision phases:

- **Confirmed Decision 1 (Auth & Multi-User Synchronization)**: Google Sign-In via Jetpack Credential Manager backed by Cloud Firestore. Each user has their own persistent, secure activity log in the cloud, allowing them and their friends to each track daily habits across any device without losing data.
- **Confirmed Decision 2 (Timer & Session Logging)**: One-tap live start and stop timer. Users simply tap "Start Sleep" or "Start Reading" when beginning an activity, and tap "Stop" when finished. The app records the exact session timestamps and automatically tallies total daily sleep hours (including night sleep and daytime naps) and total reading duration across multiple daily sessions.
- **Confirmed Decision 3 (Friend Sharing & Records)**: Personal view with shareable daily report cards. Users get a clean daily log dashboard with the ability to generate and export/share an aesthetic daily summary card (via system share sheet or clipboard) containing wake-up time, bedtime, total sleep hours, total reading time, and lecture completion status.
- **Confirmed Decision 4 (Consolidated Analytics & Averages)**: Dedicated "Analytics" tab in the bottom navigation featuring interactive time filters: **This Week**, **This Month**, and **All-Time** (up until now). Computes daily average sleep, total sleep duration, average wake-up time, reading totals/averages, and lecture completion consistency with visual progress bars and shareable consolidated summaries.

---

## 1. Overview & Core Concept

- **What It Does**: The app empowers individuals and their friend circles to track essential daily disciplines:
  1. **Sleep & Waking Tracking**: Automatically tracks bedtime, wake-up time, and daytime naps via a one-tap live timer, providing a complete breakdown and total daily sleep time.
  2. **Multi-Session Book Reading**: Live timer that can be started and stopped multiple times throughout the day (morning, afternoon, evening), accumulating total reading time.
  3. **Daily Lecture Check**: Simple one-tap toggle for tracking whether Srila Prabhupada's lecture was heard today.
  4. **Consolidated Reports & Averages**: Dedicated Analytics tab with filter chips (This Week, This Month, All-Time) calculating average sleep hours per day, total sleep duration, reading averages, and Prabhupada lecture consistency rates.
  5. **Daily & Consolidated Report Cards**: A beautifully rendered summary of daily and period achievements with one-tap "Share with Friends" (WhatsApp, Telegram, clipboard).
  6. **Cloud Multi-Device Sync**: Seamless cloud persistence powered by Google Sign-In and Firebase Firestore so users and friends can access their records from any Android device.
- **Target Audience / Persona**: Devotees, spiritual practitioners, and disciplined individuals striving to maintain consistent daily routines (sadhana) alongside friends who keep each other accountable.
- **Key Value**: Frictionless one-tap tracking that removes manual math and logs exact durations, paired with insightful consolidated averages and cross-device cloud persistence.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Authentication Flow**:
   - User opens the app; if not signed in, a calm, welcoming sign-in screen appears explaining the benefits (cloud sync across devices, secure private logs).
   - User taps "Sign in with Google" via the native Jetpack Credential Manager sheet.
   - Upon successful sign-in, the user enters the Daily Tracker Dashboard.

2. **Daily Tracking Tab ("Today")**:
   - **Date Selector Bar**: Displays today's date with previous/next day navigation arrows and a calendar picker to view past logs.
   - **Active Session Banner (if running)**: If a sleep or reading session is currently active, a pulsing high-visibility floating card shows the elapsed time (HH:MM:SS) and a prominent "End Session" button.
   - **Sleep Section**:
     - Status card showing Night Sleep start/end, daytime naps, and prominent **Total Sleep: Xh Ym**.
     - Action button: When awake -> "Start Nap / Sleep"; when sleeping -> "Wake Up (End Sleep)".
   - **Book Reading Section**:
     - Status card displaying list of recorded sessions for the selected day with individual start/end times and durations.
     - Prominent **Total Reading Time: Xh Ym**.
     - Action button: "Start Reading Session" / "End Reading Session".
   - **Prabhupada Lecture Section**:
     - Visual toggle card with an inspirational quote/badge: "Watched Prabhupada Lecture Today?" with a clear Yes/No switch and completion indicator.
   - **Quick Share Button**:
     - Floating or top-bar button to generate and share the Day's Report Card.

3. **Consolidated Analytics Tab ("Reports")**:
   - **Time Filter Segmented Buttons / Chips**:
     - `This Week (Last 7 Days)`
     - `This Month (Current Month)`
     - `All-Time (Up Until Now)`
   - **Consolidated Metric Cards**:
     - **Average Sleep per Day**: e.g., `7h 25m / day` with comparison indicator.
     - **Total Sleep Logged**: e.g., `51h 55m` over 7 logged days.
     - **Average Wake-Up Time**: e.g., `05:15 AM`.
     - **Average Reading per Day**: e.g., `45m / day` (`5h 15m` total).
     - **Lecture Consistency**: e.g., `6 / 7 days (85%)` with an animated circular progress ring.
   - **Daily Distribution Breakdown**:
     - Visual day-by-day bar chart showing sleep hours and reading hours side-by-side for each day in the selected period.
   - **Share Consolidated Report**:
     - Generates a formatted summary text (e.g., "Weekly Sadhana Summary: Avg Sleep 7h 20m | Reading 5h 30m | 6/7 Lectures Watched") ready to copy or send to friends.

4. **Sharing & Report Card Modal**:
   - A modal dialog displays a formatted "Daily Sadhana & Activity Card" preview:
     ```text
     🌅 Daily Activity Log - Oct 15, 2026
     👤 Rajat
     ━━━━━━━━━━━━━━━━━━━━━
     🛌 Total Sleep: 7h 30m (Bed: 10:30 PM | Wake: 5:00 AM | Nap: 1h)
     📖 Book Reading: 1h 45m (2 sessions)
     🎧 Prabhupada Lecture: ✅ Completed
     ━━━━━━━━━━━━━━━━━━━━━
     Shared via Daily Tracker
     ```
   - Actions: "Copy to Clipboard" and "Share via Apps" (WhatsApp, Telegram, Message, etc.) invoking Android's native share sheet.

### Visual Identity & Theme

- **Aesthetic Direction**: Warm, serene, mindful aesthetic with refined typography and soft spiritual undertones (amber/saffron highlights paired with slate and deep warm charcoal).
- **Color Palette**:
  - Primary: Deep Warm Saffron / Amber (`#E65100` / `#FF9800`)
  - Secondary: Teakwood / Muted Terracotta (`#8D6E63`)
  - Background Light: Warm cream / Linen (`#FAF8F5`)
  - Surface Light: Pristine card background (`#FFFFFF`)
  - Background Dark: Deep contemplative slate (`#121417`)
  - Surface Dark: Elevated container (`#1E2228`)
  - Accent Success: Forest Sage Green (`#2E7D32`) for completed habits
- **Typography & Hierarchy**:
  - Headings: Clean, modern sans-serif with strong typographic hierarchy.
  - Timers & Stats: Monospace tabular numbers (`FontFamily.Monospace`) for flicker-free live timer ticking and aligned statistics.
- **Component Styling & Layout**:
  - Bottom Navigation Bar with two clear destinations: **Today (Tracker)** and **Analytics (Consolidated Reports)**.
  - Rounded cards (`shape = RoundedCornerShape(16.dp)`), subtle elevation, and tactile interactive ripples.
  - Safe-area insets padding (`WindowInsets.safeDrawing`, `navigationBarsPadding`).
  - Minimum touch targets of 48.dp for all buttons.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Live Start/Stop Timer Architecture**:
  - *Chosen Approach*: When user taps "Start", the app immediately persists an open session document with `startTime = ServerTimestamp` and `endTime = null`. When they tap "End", `endTime` is populated.
  - *Why*: If the user closes the app, reboots their phone, or switches devices while sleeping or reading, the session remains active in the cloud and on the local device. When reopened, the timer resumes seamlessly from the recorded start timestamp.
- **Decision 2: Cloud Firestore Database Structure**:
  - *Chosen Approach*: Multi-user partitioned schema under `/users/{userId}/daily_logs/{dateKey}` with subcollections for `/sessions`.
  - *Why*: Adheres strictly to zero-trust security rules (`request.auth.uid == userId`), fast date-keyed lookups, efficient date range queries for weekly/monthly analytics, and offline caching support built into the Firestore SDK.
- **Decision 3: Consolidated Report Calculation Strategy**:
  - *Chosen Approach*: Dynamic aggregation in ViewModel using Flow streams over the selected date range (`This Week`, `This Month`, `All-Time`).
  - *Why*: Provides instant updates whenever today's timer ends, zero extra backend fees or Cloud Functions required, and handles missing days gracefully (computing averages over active recorded days or the full period).
- **Decision 4: Daily Report Card Sharing Mechanism**:
  - *Chosen Approach*: Formatted clipboard copy and native `Intent(Intent.ACTION_SEND)` share sheet.
  - *Why*: Allows instantaneous sharing to any chat group (WhatsApp, Telegram, Signal) where friends coordinate, without needing friends to install custom proprietary invitation protocols unless they also install the app.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             Jetpack Compose UI                              │
├──────────────────────────────────────┬──────────────────────────────────────┤
│         AuthScreen (Google)          │         Navigation Scaffold          │
│   - CredentialManager button         │   - BottomNavigationBar:             │
│   - Auto-sign-in on startup          │     [1] Today (DailyTrackerScreen)   │
│                                      │     [2] Analytics (ReportsScreen)    │
├──────────────────────────────────────┴──────────────────────────────────────┤
│ [1] DailyTrackerScreen:                                                     │
│   - Active Timer Bar (Pulsing live elapsed time)                            │
│   - Sleep & Nap Card (Start/Wake buttons, bedtime & wake time)              │
│   - Multi-session Reading Card (Start/Stop, session list, total)            │
│   - Prabhupada Lecture Toggle (Yes / No)                                    │
│   - Date Selector / Previous / Next navigation                              │
│                                                                             │
│ [2] ReportsScreen (Consolidated):                                           │
│   - Time Horizon Chips (This Week | This Month | All-Time)                  │
│   - Stat Cards: Average Sleep, Total Sleep, Avg Wake Time, Avg Reading      │
│   - Lecture Consistency Gauge (% days completed)                            │
│   - Day-by-Day Activity Chart / Breakdown                                   │
│   - Share Period Summary Button                                             │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            DailyTrackerViewModel                            │
│  - Realtime observation of selected date's DailyLog & Sessions              │
│  - Realtime observation of date range logs for Analytics                    │
│  - Live elapsed time ticker (ticking every 1s when session active)          │
│  - Math engines: calculateAverageSleep(), calculateConsistencyRate()        │
│  - Text report generator (daily card & consolidated period card)            │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                             TrackerRepository                               │
│  - Auth state via FirebaseAuth                                              │
│  - Firestore instance scoped with context.getString(firestore_database_id)  │
│  - observeDailyLog(dateKey), observeDateRange(startDate, endDate)           │
│  - startSession(), endSession(), toggleLectureWatched()                     │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     Cloud Firestore Security & Storage                      │
│  /users/{userId}/daily_logs/{dateStr}                                       │
│    - date: "2026-10-15"                                                     │
│    - prabhupadaLectureWatched: Boolean                                      │
│    - totalSleepMinutes: Long                                                │
│    - totalReadingMinutes: Long                                              │
│    - sessions: [ { type: "SLEEP"|"READING", start, end } ]                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & Firestore Schema

1. **DailyLog Document** (`users/{userId}/daily_logs/{dateKey}`):
   - `id: String` (date in `YYYY-MM-DD` format, e.g., `2026-10-15`)
   - `userId: String`
   - `date: String`
   - `prabhupadaLectureWatched: Boolean`
   - `wakeUpTime: String?` (formatted HH:mm of morning wake-up)
   - `bedTimePreviousNight: String?` (formatted HH:mm of bedtime)
   - `totalSleepMinutes: Long`
   - `totalReadingMinutes: Long`
   - `createdAt: Timestamp?`
   - `updatedAt: Timestamp?`

2. **ActivitySession Document** (`users/{userId}/sessions/{sessionId}`):
   - `id: String` (auto-generated ID)
   - `userId: String`
   - `dateKey: String` (`YYYY-MM-DD`)
   - `activityType: String` (`"SLEEP"` or `"READING"`)
   - `startTime: Timestamp`
   - `endTime: Timestamp?` (null while session is running)
   - `durationMinutes: Long` (computed upon end)
   - `notes: String`

---

## 5. Execution Steps (Post-Approval)

1. **Provision Backend**: Perform Keystore extraction and execute the `show_aistudio_ui` / `ProvisionFirebase` setup for Cloud Firestore and Google Sign-In.
2. **Schema & Security Rules**: Write `firebase-blueprint.json` and strict `firestore.rules` validating data types and user boundaries. Deploy rules via RPC action.
3. **Dependencies & Gradle Setup**: Activate Firebase Auth, Firestore, Credential Manager, and GoogleId dependencies in `app/build.gradle.kts`.
4. **Data Layer Implementation**: Build type-safe models, `handleFirestoreError` error handler, and `TrackerRepository`.
5. **UI & ViewModels**:
   - `DailyTrackerViewModel` with live timer calculations, range aggregations, and StateFlow.
   - `AuthScreen` with Google Sign-In.
   - `DailyTrackerScreen` ("Today") with live timers for sleep and reading, Prabhupada lecture toggle, and daily metrics.
   - `ReportsScreen` ("Analytics") with Week, Month, and All-Time filter chips, average sleep duration, total sleep, average reading, and lecture consistency.
   - `ReportCardDialog` with Android system share sheet integration for both single-day and period summaries.
6. **Verification**: Run `compile_applet` to verify compilation and app functionality.
