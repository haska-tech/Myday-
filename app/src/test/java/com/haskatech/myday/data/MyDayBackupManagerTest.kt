package com.haskatech.myday.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MyDayBackupManagerTest {
    @Test fun roundTripsTasksEntriesAndActiveSession() {
        val tasks = listOf(TaskEntity(
            id = 7, title = "Clinic review", notes = "Bring files", scheduledAtMillis = 10_000L,
            reminderAtMillis = 9_000L, recurrenceRule = "Weekly", priority = "High",
            isTopPriority = true, isCompleted = false, createdAtMillis = 1_000L,
            timeSpentMillis = 3_000L, recurrenceSeriesId = "series-7", plannedDurationMinutes = 45
        ))
        val entries = listOf(TimeTrackingEntryEntity(3, 7, 2_000L, 5_000L, 3_000L))
        val session = TimeTrackingSessionEntity(1, 7, 6_000L)
        val restored = MyDayBackupManager.decode(MyDayBackupManager.encode(tasks, entries, session))
        assertEquals(tasks, restored.tasks)
        assertEquals(entries, restored.entries)
        assertEquals(session, restored.session)
    }

    @Test fun acceptsEmptyBackupAndNullActiveSession() {
        val restored = MyDayBackupManager.decode(MyDayBackupManager.encode(emptyList(), emptyList(), null))
        assertEquals(emptyList<TaskEntity>(), restored.tasks)
        assertEquals(emptyList<TimeTrackingEntryEntity>(), restored.entries)
        assertNull(restored.session)
    }

    @Test fun rejectsUnsupportedFormatAndBrokenReferencesBeforeRestore() {
        assertThrows(IllegalArgumentException::class.java) {
            MyDayBackupManager.decode("""{"format":"Other","formatVersion":1,"tasks":[],"timeEntries":[]}""")
        }
        val broken = """{"format":"MyDayBackup","formatVersion":1,"tasks":[],"timeEntries":[{"id":1,"taskId":99,"startedAtMillis":1,"endedAtMillis":2,"durationMillis":1}],"activeSession":null}"""
        assertThrows(IllegalArgumentException::class.java) { MyDayBackupManager.decode(broken) }
    }
}
