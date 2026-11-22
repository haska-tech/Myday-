package com.haskatech.myday.widget
import com.haskatech.myday.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
class WidgetTaskCompletionRulesTest {
 @Test fun pendingIntentUrisAreUniquePerTask() {
  assertTrue(WidgetTaskCompletionRules.pendingIntentUri(1L) != WidgetTaskCompletionRules.pendingIntentUri(2L))
 }
 @Test fun pendingIntentUriRejectsInvalidIds() {
  assertTrue(runCatching { WidgetTaskCompletionRules.pendingIntentUri(0L) }.isFailure)
  assertTrue(runCatching { WidgetTaskCompletionRules.pendingIntentUri(-1L) }.isFailure)
 }
 @Test fun visibleRowsIncludePrioritiesAndTodayTasksOnlyOnce() {
  val now=Instant.parse("2026-11-23T06:00:00Z").toEpochMilli()
  val priority=TaskEntity(id=1,title="Priority",isTopPriority=true)
  val ordinary=TaskEntity(id=2,title="Scheduled",scheduledAtMillis=now+60_000)
  val tomorrow=TaskEntity(id=3,title="Tomorrow",scheduledAtMillis=now+86_400_000)
  val completed=TaskEntity(id=4,title="Done",scheduledAtMillis=now,isCompleted=true)
  val snapshot=WidgetTaskSnapshot.create(listOf(priority,ordinary,tomorrow,completed),now,ZoneId.of("Africa/Kampala"))
  assertEquals(listOf(1L,2L),snapshot.visibleTasks().map{it.id})
  assertFalse(snapshot.visibleTasks().any{it.isCompleted})
 }
}
