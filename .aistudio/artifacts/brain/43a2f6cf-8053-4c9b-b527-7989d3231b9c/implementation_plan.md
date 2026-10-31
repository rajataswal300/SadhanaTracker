# Disable Future Date Navigation Implementation Plan

Prevent users from navigating into future dates by disabling and dimming the forward arrow button when on today's date, and enforcing a guard in the ViewModel.

---

## 1. UI Navigation Updates (`DailyTrackerScreen.kt`)
- **Forward Arrow Button (`next_date_button`)**:
  - Bound to `enabled = !isToday`.
  - When on today (`isToday == true`):
    - Button click is disabled.
    - Icon color is dimmed (`MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)`).
  - When browsing past dates (`isToday == false`):
    - Forward button is fully enabled with normal tint, allowing users to step forward up until today.

---

## 2. ViewModel Date Boundary Guard (`DailyTrackerViewModel.kt`)
- In `navigateDate(daysDelta: Int)`:
  - Add an upper boundary check against `today`:
    - If `daysDelta > 0` and the selected date is already today or would exceed today, cap the date at today (`cal.time = minOf(targetDate, today)`).
  - Prevents programmatic navigation into future dates.

---

## 3. Verification & Testing
- Unit test in `ExampleRobolectricTest.kt` verifying:
  - `navigateDate(1)` from today does not advance past today.
  - `navigateDate(-1)` from today advances to yesterday.
  - `navigateDate(1)` from yesterday successfully advances back to today.
- Gradle compilation and unit test execution.
