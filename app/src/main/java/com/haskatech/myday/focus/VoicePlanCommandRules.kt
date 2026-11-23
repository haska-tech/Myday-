package com.haskatech.myday.focus
import com.haskatech.myday.data.SmartRecommendation
import java.util.Locale
sealed class VoicePlanCommand { data class Approve(val taskId:Long):VoicePlanCommand();data class ChangeDuration(val taskId:Long,val minutes:Int):VoicePlanCommand();data class Defer(val taskId:Long):VoicePlanCommand() }
object VoicePlanCommandRules {
 fun parse(spokenText:String,recommendations:List<SmartRecommendation>):VoicePlanCommand? {
  val text=spokenText.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9 ]")," ").replace(Regex("\\s+")," ")
  if(text.isBlank())return null
  val matches=recommendations.mapNotNull{item->val title=item.title.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9 ]")," ").trim();if(title.isNotEmpty()&&text.contains(title))item else null}
  val longest=matches.maxOfOrNull{it.title.length}?:return null
  val best=matches.filter{it.title.length==longest}
  if(best.size!=1)return null
  val target=best.single()
  return when{
   text.startsWith("approve ")||text.startsWith("confirm ")->VoicePlanCommand.Approve(target.taskId)
   text.startsWith("skip ")||text.startsWith("defer ")||text.startsWith("not now ")->VoicePlanCommand.Defer(target.taskId)
   text.startsWith("change ")||text.startsWith("set ")->{val minutes=Regex("(\\d{1,3})\\s*(minutes|minute|min)").find(text)?.groupValues?.get(1)?.toIntOrNull();if(minutes!=null&&minutes in 5..1440)VoicePlanCommand.ChangeDuration(target.taskId,minutes)else null}
   else->null
  }
 }
}
