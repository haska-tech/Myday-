# Task time tracking

MyDay stores each completed tracking segment in the local Room database. A segment is written atomically with the task's cumulative time total when a timer is paused or stopped.

## Reporting rules

- Session timestamps are stored as epoch milliseconds.
- Daily and weekly reports use the Africa/Kampala time zone.
- A session that crosses midnight is split across the two local dates for reporting.
- The weekly report starts on Monday and ends on Sunday.
- A currently running session is included in reports before it is paused or stopped.
- Deleting a task does not delete its historical time entries, so historical reports remain intact.
- Existing task lifetime totals are retained through the database migration; historical daily breakdowns are available only for sessions recorded after this feature is installed.

## Data migration

Database version 6 adds the `time_tracking_entry` table. Existing task totals are preserved. The migration does not fabricate dates for previously accumulated time because the old database stored no session timestamps.
