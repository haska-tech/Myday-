package com.haskatech.myday.data
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
@RunWith(RobolectricTestRunner::class)
class SmartPlanApprovalTest {
 private lateinit var db:MyDayDatabase
 @Before fun setUp(){db=Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(),MyDayDatabase::class.java).allowMainThreadQueries().build()}
 @After fun tearDown(){db.close()}
 @Test fun scheduleChangesOnlyAfterExplicitApproval()=runBlocking{val id=db.taskDao().insert(TaskEntity(title="Unscheduled"));assertNull(db.taskDao().getTaskById(id)!!.scheduledAtMillis);val start=java.time.LocalDate.of(2026,11,24).atTime(9,0).atZone(java.time.ZoneId.of("Africa/Kampala")).toInstant().toEpochMilli();val result=db.taskDao().applyApprovedSchedule(id,start,45);assertEquals(start,result!!.scheduledAtMillis);assertEquals(45,result.plannedDurationMinutes)}
 @Test fun rejectsSlotsOutsideWorkHoursAndBeyondDeadline(){
  val date=java.time.LocalDate.of(2026,11,24);val zone=java.time.ZoneId.of("Africa/Kampala")
  val morning=date.atTime(9,0).atZone(zone).toInstant().toEpochMilli()
  val deadline=morning+30*60000L
  val dueId=db.taskDao().insert(TaskEntity(title="Due soon",deadlineAtMillis=deadline))
  assertNull(db.taskDao().applyApprovedSchedule(dueId,morning,60))
  val evening=date.atTime(19,0).atZone(zone).toInstant().toEpochMilli()
  val eveningId=db.taskDao().insert(TaskEntity(title="Evening"))
  assertNull(db.taskDao().applyApprovedSchedule(eveningId,evening,30))
 }
 @Test fun rejectsConflictingAndCompletedTasks()=runBlocking{val start=java.time.LocalDate.of(2026,11,24).atTime(9,0).atZone(java.time.ZoneId.of("Africa/Kampala")).toInstant().toEpochMilli();val busy=db.taskDao().insert(TaskEntity(title="Busy",scheduledAtMillis=start,plannedDurationMinutes=60));val candidate=db.taskDao().insert(TaskEntity(title="Candidate"));assertNull(db.taskDao().applyApprovedSchedule(candidate,start+30000L,30));db.taskDao().setCompleted(candidate,true);assertNull(db.taskDao().applyApprovedSchedule(candidate,start+7200000L,30));assertEquals(start,db.taskDao().getTaskById(busy)!!.scheduledAtMillis)}
}
