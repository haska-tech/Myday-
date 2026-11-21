package com.haskatech.myday.focus

object VoiceCaptureRules {
    fun appendRecognizedText(existingText: String, recognizedText: String): String {
        val existing = existingText.trim()
        val recognized = recognizedText.trim()
        return when {
            recognized.isEmpty() -> existing
            existing.isEmpty() -> recognized
            else -> "$existing $recognized"
        }
    }
}
