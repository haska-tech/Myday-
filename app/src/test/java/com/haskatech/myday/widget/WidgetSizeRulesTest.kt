package com.haskatech.myday.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSizeRulesTest {
    @Test fun selectsSmallMediumAndLargeUsingBothDimensions() {
        assertEquals(WidgetSizeClass.SMALL, WidgetSizeRules.choose(150, 180))
        assertEquals(WidgetSizeClass.MEDIUM, WidgetSizeRules.choose(180, 110))
        assertEquals(WidgetSizeClass.MEDIUM, WidgetSizeRules.choose(280, 120))
        assertEquals(WidgetSizeClass.LARGE, WidgetSizeRules.choose(280, 170))
    }

    @Test fun taskRowsScaleWithAvailableWidgetSize() {
        assertEquals(2, WidgetSizeRules.taskLimit(WidgetSizeClass.SMALL))
        assertEquals(5, WidgetSizeRules.taskLimit(WidgetSizeClass.MEDIUM))
        assertEquals(8, WidgetSizeRules.taskLimit(WidgetSizeClass.LARGE))
    }
}
