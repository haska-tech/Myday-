package com.haskatech.myday.data

object CategoryRules {
    fun cleanName(value: String): String? {
        val clean = value.trim()
        return clean.takeIf { it.isNotEmpty() && it.length <= 32 }
    }

    fun isValidColor(value: String): Boolean =
        value.matches(Regex("^#[0-9A-Fa-f]{6}$"))
}
