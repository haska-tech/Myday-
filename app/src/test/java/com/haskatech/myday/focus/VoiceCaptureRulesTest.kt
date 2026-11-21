package com.haskatech.myday.focus

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCaptureRulesTest {
    @Test
    fun recognizedTextBecomesTaskWhenInputIsEmpty() {
        assertEquals("Call the clinic", VoiceCaptureRules.appendRecognizedText("", "Call the clinic"))
    }

    @Test
    fun recognizedTextAppendsToExistingDraft() {
        assertEquals("Call the clinic tomorrow", VoiceCaptureRules.appendRecognizedText("Call the clinic", "tomorrow"))
    }

    @Test
    fun surroundingWhitespaceIsRemoved() {
        assertEquals("Buy milk", VoiceCaptureRules.appendRecognizedText("  Buy ", " milk  "))
    }

    @Test
    fun emptyRecognitionDoesNotChangeDraft() {
        assertEquals("Existing task", VoiceCaptureRules.appendRecognizedText(" Existing task ", "  "))
    }
}
