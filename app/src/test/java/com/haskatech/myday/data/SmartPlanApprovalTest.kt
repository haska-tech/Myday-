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
 @Test fun scheduleChangesOnlyAfterExplicitApproval()=runBlocking{val id=db.taskDao().insert(TaskEntity(title="Unscheduled"));assertNull(db.taskDao().getTaskById(id)!!.scheduledAtMillis);val result=db.taskDao().applyApprovedSchedule(id,100000L,45);assertEquals(100000L,result!!.scheduledAtMillis);assertEquals(45,result.plannedDurationMinutes)}
 @Test fun rejectsConflictingAndCompletedTasks()=runBlocking{val start=100000L;val busy=db.taskDao().insert(TaskEntity(title="Busy",scheduledAtMillis=start,plannedDurationMinutes=60));val candidate=db.taskDao().insert(TaskEntity(title="Candidate"));assertNull(db.taskDao().applyApprovedSchedule(candidate,start+30000L,30));db.taskDao().setCompleted(candidate,true);assertNull(db.taskDao().applyApprovedSchedule(candidate,start+7200000L,30));assertEquals(start,db.taskDao().getTaskById(busy)!!.scheduledAtMillis)}
}
