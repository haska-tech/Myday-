package com.haskatech.myday

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class MyDayDesignSystemTest {
    @Test
    fun brandPaletteUsesTheApprovedNavyLimeAndPaperValues() {
        assertEquals(Color(0xFF14243A), MyDayColors.Navy)
        assertEquals(Color(0xFFB7E445), MyDayColors.Lime)
        assertEquals(Color(0xFFF4F6F9), MyDayColors.Paper)
        assertEquals(Color(0xFF748094), MyDayColors.Muted)
    }

    @Test
    fun semanticWarningAndSurfaceTokensAreStable() {
        assertEquals(Color(0xFFFFF4E5), MyDayColors.WarningBackground)
        assertEquals(Color(0xFF8A4B08), MyDayColors.WarningText)
        assertEquals(Color(0xFFEAF2D4), MyDayColors.SoftLime)
        assertEquals(Color.White, MyDayColors.White)
        assertEquals(Color(0xFFD8E0EA), MyDayColors.OnNavyMuted)
    }

    @Test
    fun sharedTypographyDefinesConsistentPremiumHeadingAndBodyScale() {
        assertEquals(29, MyDayTypography.headlineLarge.fontSize.value.toInt())
        assertEquals(35, MyDayTypography.headlineLarge.lineHeight.value.toInt())
        assertEquals(21, MyDayTypography.titleLarge.fontSize.value.toInt())
        assertEquals(14, MyDayTypography.bodyMedium.fontSize.value.toInt())
        assertEquals(12, MyDayTypography.bodySmall.fontSize.value.toInt())
    }
}
