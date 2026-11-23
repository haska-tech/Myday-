package com.haskatech.myday.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.haskatech.myday.reminders.ReminderScheduler
import com.haskatech.myday.widget.MyDayWidgetProvider
import org.json.JSONArray
import org.json.JSONObject

/** Versioned JSON backup. Validation finishes before restore changes the database. */
object MyDayBackupManager {
    private const val FORMAT_VERSION = 1

    fun encode(tasks: List<TaskEntity>, entries: List<TimeTrackingEntryEntity>, session: TimeTrackingSessionEntity?): String =
        JSONObject().put("format", "MyDayBackup").put("formatVersion", FORMAT_VERSION)
            .put("tasks", JSONArray().apply { tasks.forEach { put(it.toJson()) } })
            .put("timeEntries", JSONArray().apply { entries.forEach { put(it.toJson()) } })
            .put("activeSession", session?.toJson() ?: JSONObject.NULL).toString(2)

    fun decode(json: String): BackupSnapshot {
        val root = JSONObject(json)
        require(root.getString("format") == "MyDayBackup") { "This is not a MyDay backup file." }
        require(root.getInt("formatVersion") == FORMAT_VERSION) { "This backup version is not supported." }
        val tasks = root.getJSONArray("tasks").toObjects { it.toTask() }
        val entries = root.getJSONArray("timeEntries").toObjects { it.toEntry() }
        val session = root.optJSONObject("activeSession")?.toSession()
        require(tasks.map { it.id }.toSet().size == tasks.size) { "Backup contains duplicate task IDs." }
        require(tasks.all { it.id > 0 && it.title.isNotBlank() && it.timeSpentMillis >= 0L &&
            (it.plannedDurationMinutes == null || it.plannedDurationMinutes > 0) }) {
            "Backup contains invalid task data."
        }
        val taskIds = tasks.map { it.id }.toSet()
        require(entries.map { it.id }.toSet().size == entries.size) { "Backup contains duplicate time-entry IDs." }
        require(entries.all { it.id > 0 && it.taskId in taskIds && it.startedAtMillis >= 0L &&
            it.endedAtMillis >= it.startedAtMillis && it.durationMillis == it.endedAtMillis - it.startedAtMillis }) {
            "Backup contains invalid time-tracking data."
        }
        require(session == null || (session.id == 1 && session.taskId in taskIds &&
            (session.startedAtMillis == null || session.startedAtMillis >= 0L))) {
            "Backup contains an invalid active time-tracking session."
        }
        return BackupSnapshot(tasks, entries, session)
    }

    suspend fun exportTo(context: Context, uri: Uri) {
        val database = MyDayDatabase.getDatabase(context)
        val snapshot = database.withTransaction {
            BackupSnapshot(database.taskDao().getAllTasks(), database.timeTrackingDao().getAllEntries(),
                database.timeTrackingDao().getSession())
        }
        val output = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Could not open the selected backup destination.")
        output.bufferedWriter(Charsets.UTF_8).use { it.write(encode(snapshot.tasks, snapshot.entries, snapshot.session)) }
    }

    suspend fun importFrom(context: Context, uri: Uri) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open the selected backup file.")
        val snapshot = input.bufferedReader(Charsets.UTF_8).use { decode(it.readText()) }
        val database = MyDayDatabase.getDatabase(context)
        val oldTasks = database.taskDao().getAllTasks()
        database.withTransaction {
            database.timeTrackingDao().clearEntries()
            database.taskDao().clearAllTasks()
            database.taskDao().insertAll(snapshot.tasks)
            if (snapshot.entries.isNotEmpty()) database.timeTrackingDao().insertEntries(snapshot.entries)
            database.timeTrackingDao().clearSession()
            snapshot.session?.let { database.timeTrackingDao().saveSession(it) }
        }
        oldTasks.forEach { ReminderScheduler.cancel(context, it.id) }
        val now = System.currentTimeMillis()
        snapshot.tasks.filter { !it.isCompleted && it.reminderAtMillis != null && it.reminderAtMillis > now }
            .forEach { ReminderScheduler.schedule(context, it.id, it.title, it.reminderAtMillis!!) }
        MyDayWidgetProvider.requestRefresh(context)
    }

    private fun TaskEntity.toJson() = JSONObject().put("id", id).put("title", title).put("notes", notes)
        .putNullableLong("scheduledAtMillis", scheduledAtMillis).putNullableLong("reminderAtMillis", reminderAtMillis)
        .put("recurrenceRule", recurrenceRule).put("priority", priority).put("isTopPriority", isTopPriority)
        .put("isCompleted", isCompleted).put("createdAtMillis", createdAtMillis).put("timeSpentMillis", timeSpentMillis)
        .put("recurrenceSeriesId", recurrenceSeriesId ?: JSONObject.NULL)
        .put("plannedDurationMinutes", plannedDurationMinutes ?: JSONObject.NULL)

    private fun TimeTrackingEntryEntity.toJson() = JSONObject().put("id", id).put("taskId", taskId)
        .put("startedAtMillis", startedAtMillis).put("endedAtMillis", endedAtMillis).put("durationMillis", durationMillis)

    private fun TimeTrackingSessionEntity.toJson() = JSONObject().put("id", id).put("taskId", taskId)
        .putNullableLong("startedAtMillis", startedAtMillis)

    private fun JSONObject.putNullableLong(key: String, value: Long?) = put(key, value ?: JSONObject.NULL)
    private fun <T> JSONArray.toObjects(mapper: (JSONObject) -> T): List<T> =
        (0 until length()).map { mapper(getJSONObject(it)) }

    private fun JSONObject.toTask() = TaskEntity(
        id = getLong("id"), title = getString("title"), notes = optString("notes", ""),
        scheduledAtMillis = optNullableLong("scheduledAtMillis"), reminderAtMillis = optNullableLong("reminderAtMillis"),
        recurrenceRule = optString("recurrenceRule", "None"), priority = optString("priority", "Normal"),
        isTopPriority = optBoolean("isTopPriority", false), isCompleted = optBoolean("isCompleted", false),
        createdAtMillis = getLong("createdAtMillis"), timeSpentMillis = getLong("timeSpentMillis"),
        recurrenceSeriesId = optNullableString("recurrenceSeriesId"),
        plannedDurationMinutes = if (isNull("plannedDurationMinutes")) null else getInt("plannedDurationMinutes")
    )
    private fun JSONObject.toEntry() = TimeTrackingEntryEntity(
        id = getLong("id"), taskId = getLong("taskId"), startedAtMillis = getLong("startedAtMillis"),
        endedAtMillis = getLong("endedAtMillis"), durationMillis = getLong("durationMillis")
    )
    private fun JSONObject.toSession() = TimeTrackingSessionEntity(
        id = getInt("id"), taskId = getLong("taskId"), startedAtMillis = optNullableLong("startedAtMillis")
    )
    private fun JSONObject.optNullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)
    private fun JSONObject.optNullableString(key: String): String? = if (isNull(key)) null else getString(key)
}

data class BackupSnapshot(
    val tasks: List<TaskEntity>,
    val entries: List<TimeTrackingEntryEntity>,
    val session: TimeTrackingSessionEntity?
)
