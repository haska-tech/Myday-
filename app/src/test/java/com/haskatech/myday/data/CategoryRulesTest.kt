package com.haskatech.myday.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryRulesTest {
    @Test fun trimsNamesAndRejectsBlankOrOverlongNames() {
        assertEquals("Work", CategoryRules.cleanName("  Work  "))
        assertEquals(null, CategoryRules.cleanName("   "))
        assertEquals(null, CategoryRules.cleanName("x".repeat(33)))
    }

    @Test fun acceptsOnlySixDigitHexColours() {
        assertTrue(CategoryRules.isValidColor("#12aBcD"))
        assertFalse(CategoryRules.isValidColor("12aBcD"))
        assertFalse(CategoryRules.isValidColor("#12345"))
        assertFalse(CategoryRules.isValidColor("#GGGGGG"))
    }
}
