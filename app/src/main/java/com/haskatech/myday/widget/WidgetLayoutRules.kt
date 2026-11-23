package com.haskatech.myday.widget

enum class WidgetLayoutSize { SMALL, MEDIUM, LARGE }
data class WidgetBreakpoint(val widthDp: Float, val heightDp: Float, val layout: WidgetLayoutSize)

object WidgetLayoutRules {
    fun legacyLayout(widthDp: Int, heightDp: Int): WidgetLayoutSize = when {
        widthDp >= 240 && heightDp >= 170 -> WidgetLayoutSize.LARGE
        widthDp >= 170 && heightDp >= 110 -> WidgetLayoutSize.MEDIUM
        else -> WidgetLayoutSize.SMALL
    }

    fun responsiveBreakpoints(minWidthDp: Float, maxWidthDp: Float, minHeightDp: Float, maxHeightDp: Float): List<WidgetBreakpoint> {
        val minW = minWidthDp.coerceAtLeast(1f)
        val maxW = maxWidthDp.coerceAtLeast(minW)
        val minH = minHeightDp.coerceAtLeast(1f)
        val maxH = maxHeightDp.coerceAtLeast(minH)
        val widthRange = maxW - minW
        val heightRange = maxH - minH
        val initialLayout = legacyLayout(minW.toInt(), minH.toInt())
        val result = mutableListOf(WidgetBreakpoint(minW, minH, initialLayout))
        if (initialLayout == WidgetLayoutSize.SMALL && (widthRange > 0f || heightRange > 0f)) {
            result += WidgetBreakpoint(minW + widthRange * 0.45f, minH + heightRange * 0.25f, WidgetLayoutSize.MEDIUM)
        }
        if (initialLayout != WidgetLayoutSize.LARGE && (widthRange > 0f || heightRange > 0f)) {
            result += WidgetBreakpoint(minW + widthRange * 0.75f, minH + heightRange * 0.70f, WidgetLayoutSize.LARGE)
        }
        return result
    }
}

