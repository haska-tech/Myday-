package com.haskatech.myday.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetQuickAddRulesTest {
    @Test fun opensComposerOnlyForExplicitWidgetRequest() {
        assertTrue(WidgetQuickAddRules.shouldOpenAddDialog(true))
        assertFalse(WidgetQuickAddRules.shouldOpenAddDialog(false))
    }
}
