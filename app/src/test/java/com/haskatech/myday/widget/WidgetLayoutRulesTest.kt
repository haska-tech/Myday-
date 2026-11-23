package com.haskatech.myday.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutRulesTest {
    @Test fun legacyLayoutUsesBothWidthAndHeight() {
        assertEquals(WidgetLayoutSize.SMALL, WidgetLayoutRules.legacyLayout(150, 220))
        assertEquals(WidgetLayoutSize.MEDIUM, WidgetLayoutRules.legacyLayout(190, 130))
        assertEquals(WidgetLayoutSize.LARGE, WidgetLayoutRules.legacyLayout(260, 190))
    }

    @Test fun responsiveBreakpointsAdaptToAvailableWidthAndHeight() {
        val sizes = WidgetLayoutRules.responsiveBreakpoints(110f, 300f, 110f, 220f)
        assertEquals(3, sizes.size)
        assertEquals(WidgetLayoutSize.SMALL, sizes[0].layout)
        assertEquals(WidgetLayoutSize.MEDIUM, sizes[1].layout)
        assertEquals(WidgetLayoutSize.LARGE, sizes[2].layout)
        assertTrue(sizes[1].widthDp > sizes[0].widthDp)
        assertTrue(sizes[2].heightDp > sizes[1].heightDp)
    }

    @Test fun fixedSizeWidgetUsesOnlyOneBreakpoint() {
        assertEquals(1, WidgetLayoutRules.responsiveBreakpoints(110f, 110f, 110f, 110f).size)
    }

    @Test fun largeMinimumSizeStartsWithLargeLayout() {
        val sizes = WidgetLayoutRules.responsiveBreakpoints(260f, 300f, 180f, 220f)
        assertEquals(WidgetLayoutSize.LARGE, sizes.first().layout)
    }
    @Test fun doesNotOfferLayoutsThatDoNotFitTheMaximumSize() {
        val compact = WidgetLayoutRules.responsiveBreakpoints(110f, 150f, 110f, 130f)
        assertEquals(1, compact.size)
        assertEquals(WidgetLayoutSize.SMALL, compact.first().layout)

        val mediumToLarge = WidgetLayoutRules.responsiveBreakpoints(180f, 260f, 110f, 180f)
        assertEquals(listOf(WidgetLayoutSize.MEDIUM, WidgetLayoutSize.LARGE), mediumToLarge.map { it.layout })
    }

}
