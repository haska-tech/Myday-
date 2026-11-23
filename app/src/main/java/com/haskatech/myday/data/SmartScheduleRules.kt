package com.haskatech.myday.data
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
enum class SmartWarningType { OVERLAP, OUTSIDE_WORK_HOURS, DEADLINE_INSUFFICIENT, DAILY_OVERLOAD, NO_AVAILABLE_SLOT }
data class SmartPlanWarning(val type:SmartWarningType,val taskId:Long?,val taskTitle:String,val message:String)
data class SmartRecommendation(val taskId:Long,val title:String,val score:Int,val reason:String,val proposedStartMillis:Long?,val durationMinutes:Int,val changesSchedule:Boolean)
data class SmartPlan(val recommendations:List<SmartRecommendation>,val warnings:List<SmartPlanWarning>,val availableMinutesToday:Int)
/** Deterministic local planner. It proposes; only an explicit approval can write a time to Room. */
object SmartScheduleRules {
 const val DEFAULT_DURATION_MINUTES=60
 const val DEFAULT_WORKDAY_START_HOUR=8
 const val DEFAULT_WORKDAY_END_HOUR=18
 const val DEFAULT_MAX_DAILY_MINUTES=480
 private data class Busy(val start:Long,val end:Long)
 fun analyze(tasks:List<TaskEntity>,nowMillis:Long,zone:ZoneId=ZoneId.systemDefault(),workdayStartHour:Int=8,workdayEndHour:Int=18,maxDailyMinutes:Int=480,horizonDays:Int=7):SmartPlan {
  require(workdayStartHour in 0..23 && workdayEndHour in 1..23 && workdayStartHour<workdayEndHour)
  require(maxDailyMinutes>0 && horizonDays>0)
  val active=tasks.filterNot{it.isCompleted};val warnings=mutableListOf<SmartPlanWarning>();val scheduled=active.filter{it.scheduledAtMillis!=null}
  scheduled.forEach{task->
   val start=task.scheduledAtMillis!!;val end=safeEnd(start,task.plannedDurationMinutes?:DEFAULT_DURATION_MINUTES)?:return@forEach
   val localStart=Instant.ofEpochMilli(start).atZone(zone);val localEnd=Instant.ofEpochMilli(end).atZone(zone)
   if(localStart.hour<workdayStartHour||localStart.hour>=workdayEndHour||localEnd.toLocalDate()!=localStart.toLocalDate()||localEnd.hour>workdayEndHour||(localEnd.hour==workdayEndHour&&localEnd.minute>0))
    warnings+=SmartPlanWarning(SmartWarningType.OUTSIDE_WORK_HOURS,task.id,task.title,"Scheduled outside the default ${workdayStartHour}:00–${workdayEndHour}:00 planning window.")
  }
  scheduled.forEach{task->
   TaskConflictRules.findConflicts(task.scheduledAtMillis,task.plannedDurationMinutes?:DEFAULT_DURATION_MINUTES,scheduled.map{it.copy(plannedDurationMinutes=it.plannedDurationMinutes?:DEFAULT_DURATION_MINUTES)},task.id)
    .forEach{conflict->warnings+=SmartPlanWarning(SmartWarningType.OVERLAP,task.id,task.title,"Overlaps with ${conflict.title}.")}
  }
  val today=Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
  scheduled.groupBy{Instant.ofEpochMilli(it.scheduledAtMillis!!).atZone(zone).toLocalDate()}.forEach{(date,dayTasks)->
   val minutes=dayTasks.sumOf{it.plannedDurationMinutes?:DEFAULT_DURATION_MINUTES}
   if(minutes>maxDailyMinutes)warnings+=SmartPlanWarning(SmartWarningType.DAILY_OVERLOAD,null,date.toString(),"$minutes planned minutes exceed the $maxDailyMinutes-minute daily capacity.")
  }
  active.filter{it.deadlineAtMillis!=null}.forEach{task->
   val duration=task.plannedDurationMinutes?:DEFAULT_DURATION_MINUTES
   val available=freeMinutesUntil(task,active,nowMillis,task.deadlineAtMillis!!,zone,workdayStartHour,workdayEndHour)
   if(available<duration)warnings+=SmartPlanWarning(SmartWarningType.DEADLINE_INSUFFICIENT,task.id,task.title,"Only $available free planning minutes remain before the deadline; about $duration are needed.")
  }
  val recommendations=active.map{task->
   val duration=task.plannedDurationMinutes?:DEFAULT_DURATION_MINUTES;val existing=task.scheduledAtMillis
   val hasIssue=warnings.any{it.taskId==task.id&&(it.type==SmartWarningType.OVERLAP||it.type==SmartWarningType.OUTSIDE_WORK_HOURS||it.type==SmartWarningType.DEADLINE_INSUFFICIENT)}
   val proposed=if(existing!=null&&!hasIssue)existing else findSlot(task,active,nowMillis,zone,workdayStartHour,workdayEndHour,horizonDays)
   val reasons=mutableListOf<String>()
   reasons+=when(task.priority){"High"->"High importance";"Low"->"Lower importance";else->"Normal importance"}
   if(task.isTopPriority)reasons+="already marked Top 3"
   task.deadlineAtMillis?.let{val delta=it-nowMillis;reasons+=when{delta<0->"deadline overdue";delta<=86400000L->"due within 24 hours";delta<=259200000L->"due within 3 days";else->"deadline scheduled"}}?:run{reasons+="no deadline set"}
   reasons+=if(task.plannedDurationMinutes==null)"estimated at $duration minutes" else "$duration-minute task"
   if(warnings.any{it.taskId==task.id&&it.type==SmartWarningType.DEADLINE_INSUFFICIENT})reasons+="deadline may be difficult to meet"
   if(proposed==null)warnings+=SmartPlanWarning(SmartWarningType.NO_AVAILABLE_SLOT,task.id,task.title,"No suitable opening was found in the next $horizonDays days.")
   SmartRecommendation(task.id,task.title,score(task,nowMillis,duration),reasons.joinToString(" · "),proposed,duration,proposed!=null&&proposed!=existing)
  }.sortedWith(compareByDescending<SmartRecommendation>{it.score}.thenBy{it.proposedStartMillis?:Long.MAX_VALUE}).take(3)
  return SmartPlan(recommendations,warnings.distinct(),freeMinutesOnDate(null,active,today,nowMillis,zone,workdayStartHour,workdayEndHour).toInt())
 }
 private fun score(task:TaskEntity,now:Long,duration:Int):Int {
  var score=when(task.priority){"High"->40;"Low"->10;else->25};if(task.isTopPriority)score+=15
  task.deadlineAtMillis?.let{val delta=it-now;score+=when{delta<0->50;delta<=86400000L->40;delta<=259200000L->30;delta<=604800000L->20;else->5}}
  score+=when{duration<=30->10;duration<=60->7;duration<=120->3;else->0};return score
 }
 private fun findSlot(task:TaskEntity,tasks:List<TaskEntity>,now:Long,zone:ZoneId,startHour:Int,endHour:Int,horizon:Int):Long? {
  val duration=(task.plannedDurationMinutes?:DEFAULT_DURATION_MINUTES).toLong()*60000L;val deadline=task.deadlineAtMillis?:Long.MAX_VALUE
  for(offset in 0 until horizon){
   val date=Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(offset.toLong())
   val dayStart=date.atTime(startHour,0).atZone(zone).toInstant().toEpochMilli();val dayEnd=date.atTime(endHour,0).atZone(zone).toInstant().toEpochMilli()
   var cursor=maxOf(now,dayStart)
   for(block in busy(tasks,task.id,zone,date).sortedBy{it.start}){
    if(block.end<=cursor||block.start>=dayEnd)continue
    val gapEnd=minOf(block.start,dayEnd,deadline);if(cursor<=gapEnd-duration)return cursor
    cursor=maxOf(cursor,block.end)
   }
   if(cursor<=minOf(dayEnd,deadline)-duration)return cursor
  };return null
 }
 private fun freeMinutesUntil(task:TaskEntity,tasks:List<TaskEntity>,now:Long,deadline:Long,zone:ZoneId,startHour:Int,endHour:Int):Int {
  if(deadline<=now)return 0
  var date=Instant.ofEpochMilli(now).atZone(zone).toLocalDate();val last=Instant.ofEpochMilli(deadline).atZone(zone).toLocalDate();var total=0L
  while(!date.isAfter(last)){total+=freeMinutesOnDate(task.id,tasks,date,now,zone,startHour,endHour,deadline);date=date.plusDays(1)}
  return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
 }
 private fun freeMinutesOnDate(exclude:Long?,tasks:List<TaskEntity>,date:LocalDate,now:Long,zone:ZoneId,startHour:Int,endHour:Int,deadline:Long=Long.MAX_VALUE):Long {
  val from=maxOf(now,date.atTime(startHour,0).atZone(zone).toInstant().toEpochMilli());val until=minOf(deadline,date.atTime(endHour,0).atZone(zone).toInstant().toEpochMilli())
  if(until<=from)return 0L
  var cursor=from;var free=0L
  for(block in busy(tasks,exclude,zone,date).sortedBy{it.start}){val s=maxOf(block.start,from);val e=minOf(block.end,until);if(e<=s)continue;if(s>cursor)free+=s-cursor;cursor=maxOf(cursor,e)}
  if(until>cursor)free+=until-cursor;return free/60000L
 }
 private fun busy(tasks:List<TaskEntity>,exclude:Long?,zone:ZoneId,date:LocalDate):List<Busy> =
  tasks.asSequence().filter{!it.isCompleted&&it.id!=exclude&&it.scheduledAtMillis!=null}.mapNotNull{task->
   val start=task.scheduledAtMillis!!;if(Instant.ofEpochMilli(start).atZone(zone).toLocalDate()!=date)return@mapNotNull null
   val end=safeEnd(start,task.plannedDurationMinutes?:DEFAULT_DURATION_MINUTES)?:return@mapNotNull null;Busy(start,end)
  }.toList()
 private fun safeEnd(start:Long,minutes:Int):Long?{if(minutes<=0)return null;val duration=minutes.toLong()*60000L;return if(start>Long.MAX_VALUE-duration)null else start+duration}
}
