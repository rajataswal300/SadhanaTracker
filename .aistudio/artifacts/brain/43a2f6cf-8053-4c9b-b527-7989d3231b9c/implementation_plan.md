# Draggable Reorderable Sections Implementation Plan

Enable drag-and-drop / long-press reordering for primary tracker sections (*Daily Sadhana Toggles*, *Sleep & Rest*, *Book Reading*, *Prabhupada Lecture*, *Strong Maya Attack*, *Google Sheets*, and *Share Card*) with permanent local persistence.

---

## 1. Section Enum & Persistent Ordering Layer
- **Section ID Enum**:
  - `TOGGLES` (Daily Sadhana Toggles)
  - `SLEEP` (Sleep & Rest)
  - `READING` (Book Reading)
  - `LECTURE` (Prabhupada Lecture)
  - `MAYA_ATTACK` (Strong Maya Attack)
  - `SHEETS` (Google Sheets & Sync)
  - `SHARE` (Share Card)
- **`LayoutPreferences`**:
  - Stored permanently in Android `SharedPreferences` (`sadhana_layout_prefs`) as an ordered list of keys.
  - Automatically loads on app launch and saves immediately whenever items are reordered.
  - Includes a "Reset to Default Layout" option.

---

## 2. Drag & Reorder Gesture System in Compose
- **Visual Drag Handle & Long-Press**:
  - Add a refined Material 3 drag handle icon (`Icons.Default.DragIndicator` / `DragHandle`) in the header of each card.
  - Long-pressing the drag handle (or holding the card) activates drag mode with haptic feedback.
  - Supports dragging up or down with live item swapping and smooth Compose animated placement (`Modifier.animateItem()`).
- **Accessibility & Alternate Reorder Sheet**:
  - Provide accessible "Move Up" / "Move Down" and quick reset options via a "Reorder Layout" action in the top app bar / sheet for users who prefer tap controls over fine-motor dragging.

---

## 3. Screen Integration (`DailyTrackerScreen.kt` & `DailyTrackerViewModel.kt`)
- `DailyTrackerViewModel` exposes `sectionOrder: StateFlow<List<SectionId>>`.
- Dynamically renders each section in the user's customized sequence inside `LazyColumn`.
- Saves reordered list to `LayoutPreferences` seamlessly.

---

## 4. Verification & Testing
- Unit tests verifying layout preference persistence and reordering logic.
- Verify smooth compilation and build.
