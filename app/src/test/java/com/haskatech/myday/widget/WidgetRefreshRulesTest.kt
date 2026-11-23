package com.haskatech.myday.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetRefreshRulesTest {
    @Test fun refreshesOnDateClockAndTimezoneChanges() {
        assertTrue(WidgetRefreshRules.shouldRefresh(WidgetRefreshRules.ACTION_DATE_CHANGED))
        assertTrue(WidgetRefreshRules.shouldRefresh(WidgetRefreshRules.ACTION_TIME_CHANGED))
        assertTrue(WidgetRefreshRules.shouldRefresh(WidgetRefreshRules.ACTION_TIMEZONE_CHANGED))
        assertTrue(WidgetRefreshRules.shouldRefresh(MyDayWidgetProvider.ACTION_REFRESH))
        assertFalse(WidgetRefreshRules.shouldRefresh("com.example.UNRELATED"))
        assertFalse(WidgetRefreshRules.shouldRefresh(null))
    }

    @Test fun quickAddFlagOpensTheExistingTaskDialog() {
        assertTrue(WidgetQuickAddRules.shouldOpenAddDialog(true))
        assertFalse(WidgetQuickAddRules.shouldOpenAddDialog(false))
    }
}
