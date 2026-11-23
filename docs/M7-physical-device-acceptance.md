# MyDay M7 — Physical-device acceptance plan

**Target device:** Sharp AQUOS zero6 A102SH, Android 13  
**Release branch:** development only  
**Current baseline:** M6 version 0.3.5 (versionCode 17)  
**M7 candidate:** version 0.3.6 (versionCode 18), subject to CI and device acceptance  
**Installation gate:** Do not install the candidate until all pre-install release checks pass and the user explicitly authorizes device testing.

## Test controls and safety

- Use a disposable test dataset or export a verified backup before destructive restore tests.
- Do not clear app storage, uninstall the app, or reset the phone as part of ordinary update testing.
- Keep the device timezone set to its normal local timezone for baseline tests; record it before testing.
- Record app version, build SHA, test date, Android version, launcher, battery optimization state, and notification permission.
- For each test record: PASS, FAIL, BLOCKED, or NOT RUN; attach a short note and screenshot where useful.
- A test is not PASS merely because the screen opens. Verify persisted results after leaving and reopening the relevant screen.
- If any data-loss, signing, application-ID, crash, or migration failure occurs, stop release acceptance.

## A. Pre-install release gates

| ID | Check | Pass condition |
|---|---|---|
| A1 | Branch isolation | Candidate commit is on development; main SHA is unchanged |
| A2 | Source audit | M1–M6 features, schema migrations, manifest receivers, backup paths, and tests reviewed |
| A3 | Automated tests | Full unit/Robolectric suite passes with no skipped or hidden failures |
| A4 | Build | Android debug APK assembles successfully using Java 17 / configured Android SDK |
| A5 | Signing identity | APK signer SHA-256 equals permanent #101 fingerprint exactly: 19e1a377b55b7fc12f0ee50d042d24ceea511a9c169ff87bb443b697456ac8d6 |
| A6 | Package/version | Application ID is com.haskatech.myday; versionCode is greater than 17 |
| A7 | Artifact integrity | Downloaded APK artifact digest matches GitHub Actions artifact digest; APK certificate rechecked independently |
| A8 | Release configuration | No fallback to debug key in the release build path; no secrets exposed in logs |

## B. Device baseline and installation/update

| ID | Test | Procedure and expected result |
|---|---|---|
| B1 | Device identity | Confirm Sharp AQUOS zero6 A102SH, Android 13, available storage, launcher, and timezone |
| B2 | Existing data baseline | Export backup; note representative tasks, recurring series, reminders, tracking entry, and active session |
| B3 | Update compatibility | Install only the approved M7 APK as an update over the compatible #101-signed baseline; installation must retain application data |
| B4 | First launch | App opens without crash, ANR, migration error, or unexpected onboarding/reset |
| B5 | Version display | Settings → About matches the installed build version |

## C. M1 — Calendar, timetable, task editing and recurrence

| ID | Test | Expected result |
|---|---|---|
| C1 | Full-day timetable | Weekly timetable contains every hour from 00:00 through 23:00; scroll reaches both ends |
| C2 | Seven-day layout | All seven days can be reached horizontally; day headings match dates |
| C3 | Device timezone | Scheduled task appears at the correct local hour; changing device timezone and reopening recalculates local display without corrupting the instant |
| C4 | Day/week/month views | Navigation, Today action, month grid and selected-day list show correct dates and tasks |
| C5 | Unscheduled task | Task without date remains in task lists and is not placed in calendar |
| C6 | Duration/conflict | Test supported durations and overlapping blocks; warnings match actual intervals |
| C7 | Recurrence | Daily, weekly and monthly series create expected occurrences; monthly dates clamp safely (e.g. 31st to month end) |
| C8 | DST/timezone rules | If a DST-observing timezone is available, verify recurring local wall-clock time across transition; Uganda itself has no DST |
| C9 | Series editing | Edit one occurrence and entire series; completed past occurrences remain unchanged where intended |
| C10 | Completion and deletion | Complete, reopen and delete tasks; lists and calendar update consistently |

## D. M2 — Home-screen widgets

| ID | Test | Expected result |
|---|---|---|
| D1 | Add widget | MyDay widget appears in the device launcher |
| D2 | Resize | Small, medium and large layouts adapt without clipped controls or text |
| D3 | Daily progress | Completed/total count matches today's scheduled tasks |
| D4 | Quick add | + Add opens the task creation dialog and saved task appears in app/widget |
| D5 | Complete from widget | Tap task row; task becomes completed in Room and app; repeated tap does not duplicate completion |
| D6 | Refresh | Refresh, task changes, date change and timezone change update widget content |
| D7 | Reboot/launcher recovery | Widget returns with correct data after device restart; if launcher behavior differs, record launcher-specific limitation |

## E. M3 — Offline reliability, database integrity and backup

| ID | Test | Expected result |
|---|---|---|
| E1 | Offline core | Enable airplane mode; create, edit, complete, browse and delete tasks successfully |
| E2 | Force-stop persistence | Force-stop app from Android settings, relaunch; tasks, recurrence, time entries and session persist |
| E3 | Reboot persistence | Restart phone; reopen app; all Room data persists |
| E4 | Migration | Update from the prior compatible signed baseline; schema migrates to version 11 without data loss |
| E5 | Integrity check | Settings integrity check reports success |
| E6 | Export | Export JSON to user-selected local storage; file exists and is non-empty |
| E7 | Valid restore | Restore test backup after explicit confirmation; tasks, time entries and session match backup |
| E8 | Invalid restore | Attempt malformed/unsupported backup; import fails safely and existing database remains unchanged |
| E9 | Reminder recovery | After force-stop/reboot or app update, future reminders are rescheduled when the app/receiver runs |
| E10 | Storage failure | Denied/unavailable file URI produces clear error; no partial database replacement |

## F. M4 — Smart scheduling

| ID | Test | Expected result |
|---|---|---|
| F1 | Recommendations | Plan screen shows up to three explainable recommendations |
| F2 | Warnings | Overlap, insufficient deadline time, outside work hours and overload warnings are understandable |
| F3 | No auto-apply | Opening or refreshing suggestions does not silently change task schedule |
| F4 | Approve | Explicit approval writes the proposed schedule and duration |
| F5 | Edit | Edit opens the task editor with correct task data |
| F6 | Rejection guards | Completed task, conflicting slot, outside work hours or deadline violation cannot be approved |
| F7 | Voice/manual fallback | Voice command rule handling is tested where available; manual confirmation/edit path remains usable |

## G. M5 — UI, navigation and accessibility

| ID | Test | Expected result |
|---|---|---|
| G1 | Navigation | All six tabs open and retain coherent state |
| G2 | Visual consistency | Navy/lime/white/paper theme is consistent across main, Plan and Focus screens |
| G3 | Small-screen layout | No horizontal clipping, overlapping controls, or unreachable primary actions |
| G4 | Font scaling | Increase Android font/display size; important controls and content remain usable |
| G5 | Touch targets | Buttons, checkboxes, chips, widget controls are reliably tappable |
| G6 | Dark mode/system settings | No unreadable system bars, dialogs, or notification text |

## H. M6 — Productivity analytics and focus tracking

| ID | Test | Expected result |
|---|---|---|
| H1 | Date filters | 7-day, 30-day, 90-day and all-time ranges show correct local date boundaries |
| H2 | Task cohort | Tasks-created and current completion cohort match source task records |
| H3 | Historical caveat | UI clearly explains completion timestamps are not stored and does not claim historical completion dates |
| H4 | Focus trend | Tracked time is split at local midnight and daily totals match records |
| H5 | Active session | Running session contributes to current tracked-time total without double counting |
| H6 | Overdue count | Only incomplete tasks with past deadlines count as overdue |
| H7 | Pomodoro | Start, pause, resume, reset, phase transitions and notification behavior work |
| H8 | Tracking | Start, pause and stop task tracking; entries and totals persist after relaunch |

## I. Android 13 notifications, alarms and lifecycle

| ID | Test | Expected result |
|---|---|---|
| I1 | Notification permission | Grant/deny POST_NOTIFICATIONS; app remains usable in both states |
| I2 | Task reminder | A future reminder fires within Android's allowed alarm timing tolerance when permissions/settings allow |
| I3 | Notification controls | Notification channel, sound/vibration settings and tap behavior work as expected |
| I4 | Time/timezone change | Reminder schedules recover after TIME_SET/TIMEZONE_CHANGED broadcasts |
| I5 | App update | MY_PACKAGE_REPLACED receiver restores reminders and Pomodoro alarm where applicable |
| I6 | Battery restrictions | Record behavior under normal and battery-restricted modes; do not promise exact alarms where Android/OEM restrictions prevent them |

## J. Performance and release sign-off

- Cold launch and tab navigation remain responsive; no crash or ANR.
- Scroll timetable, task list, calendar, analytics and widgets repeatedly.
- Test with a realistic dataset (at least 100 tasks, multiple recurring series and several weeks of time entries).
- Confirm no unexpected network dependency for core task operations.
- Verify no task, reminder, recurrence, or time-tracking record disappears during normal update.
- Review all FAIL/BLOCKED/NOT RUN cases; document OEM/launcher restrictions separately from app defects.
- Release acceptance requires all pre-install gates A1–A8 and all critical data-integrity, signing, migration and core workflow checks to PASS.
- Physical-device checks must be completed on the named device before final release sign-off. Any blocked device-only test means acceptance remains pending.

## Execution record

| Field | Value |
|---|---|
| Device | Sharp AQUOS zero6 A102SH |
| Android | 13 |
| Build SHA | To be recorded |
| APK version/versionCode | To be recorded |
| Signing fingerprint | To be independently verified |
| Test date | To be recorded |
| Tester | User + guided test support |
| Result | NOT RUN — awaiting pre-install release gates and explicit installation authorization |
