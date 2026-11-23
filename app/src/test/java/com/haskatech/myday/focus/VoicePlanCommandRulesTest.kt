package com.haskatech.myday.focus
import com.haskatech.myday.data.SmartRecommendation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
class VoicePlanCommandRulesTest {
 private val items=listOf(SmartRecommendation(7,"Submit report",90,"High importance",1000L,60,true))
 @Test fun parsesApprovalOnly(){assertEquals(VoicePlanCommand.Approve(7),VoicePlanCommandRules.parse("approve Submit report",items));assertNull(VoicePlanCommandRules.parse("Submit report",items))}
 @Test fun parsesCorrectionAndDefer(){assertEquals(VoicePlanCommand.ChangeDuration(7,90),VoicePlanCommandRules.parse("change Submit report to 90 minutes",items));assertEquals(VoicePlanCommand.Defer(7),VoicePlanCommandRules.parse("skip Submit report",items));assertNull(VoicePlanCommandRules.parse("change Submit report to two hours",items))}
 @Test fun rejectsUnknownAndUnclearCommands(){assertNull(VoicePlanCommandRules.parse("approve unknown task",items));assertNull(VoicePlanCommandRules.parse("maybe Submit report",items))}
}
