package com.haskatech.myday.widget
import com.haskatech.myday.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
class WidgetTaskSnapshotTest {
 private val zone = ZoneId.of("Asia/Kolkata")
 @Test fun dateAndSelectionUseDeviceTimezone() {
  val now=Instant.parse("2026-11-22T20:00:00Z").toEpochMilli()
  val localToday=Instant.parse("2026-11-22T19:00:00Z").toEpochMilli()
  val tomorrow=Instant.parse("2026-11-23T20:30:00Z").toEpochMilli()
  val s=WidgetTaskSnapshot.create(listOf(TaskEntity(id=1,title="Today",scheduledAtMillis=localToday),TaskEntity(id=2,title="Tomorrow",scheduledAtMillis=tomorrow)),now,zone)
  assertEquals(LocalDate.of(2026,11,23),s.date); assertEquals(listOf(1L),s.todayTasks.map{it.id}); assertEquals(WidgetEmptyState.HAS_TASKS,s.emptyState)
 }
 @Test fun todayTasksAreOrderedByScheduledInstant() {
  val now=Instant.parse("2026-11-23T06:00:00Z").toEpochMilli()
  val s=WidgetTaskSnapshot.create(listOf(TaskEntity(id=1,title="Later",scheduledAtMillis=now+3600000),TaskEntity(id=2,title="Earlier",scheduledAtMillis=now+600000)),now,zone)
  assertEquals(listOf(2L,1L),s.todayTasks.map{it.id})
 }
 @Test fun completedTasksAreExcludedAndShowAllCompletedState() {
  val now=Instant.parse("2026-11-23T06:00:00Z").toEpochMilli()
  val s=WidgetTaskSnapshot.create(listOf(TaskEntity(id=1,title="Done",scheduledAtMillis=now,isCompleted=true)),now,zone)
  assertTrue(s.todayTasks.isEmpty()); assertEquals(WidgetEmptyState.ALL_COMPLETED,s.emptyState)
 }
 @Test fun noPlannedTasksAndAllCompletedAreDistinguished() {
  val now=Instant.parse("2026-11-23T06:00:00Z").toEpochMilli()
  val empty=WidgetTaskSnapshot.create(emptyList(),now,zone)
  val inbox=WidgetTaskSnapshot.create(listOf(TaskEntity(id=2,title="Inbox")),now,zone)
  assertEquals(WidgetEmptyState.ALL_COMPLETED,empty.emptyState); assertEquals(WidgetEmptyState.NONE_PLANNED,inbox.emptyState)
 }
 @Test fun topPrioritiesAreLimitedToThreeInSourceOrder() {
  val now=Instant.parse("2026-11-23T06:00:00Z").toEpochMilli()
  val tasks=(1L..4L).map{TaskEntity(id=it,title="P$it",isTopPriority=true)}
  val s=WidgetTaskSnapshot.create(tasks,now,zone)
  assertEquals(listOf(1L,2L,3L),s.topPriorities.map{it.id})
 }
}
