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
        val result = mutableListOf(WidgetBreakpoint(minW, minH, WidgetLayoutSize.SMALL))
        val medium = WidgetBreakpoint(minW + widthRange * 0.45f, minH + heightRange * 0.25f, WidgetLayoutSize.MEDIUM)
        if (medium.widthDp > minW || medium.heightDp > minH) result += medium
        val large = WidgetBreakpoint(minW + widthRange * 0.75f, minH + heightRange * 0.70f, WidgetLayoutSize.LARGE)
        val last = result.last()
        if (large.widthDp > last.widthDp || large.heightDp > last.heightDp) result += large
        return result
    }
}

object WidgetRefreshRules {
    const val ACTION_DATE_CHANGED = "android.intent.action.DATE_CHANGED"
    const val ACTION_TIME_CHANGED = "android.intent.action.TIME_SET"
    const val ACTION_TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED"
    fun shouldRefresh(action: String?): Boolean =
        action == MyDayWidgetProvider.ACTION_REFRESH ||
        action == "android.appwidget.action.APPWIDGET_UPDATE" ||
        action == ACTION_DATE_CHANGED || action == ACTION_TIME_CHANGED || action == ACTION_TIMEZONE_CHANGED
}

object WidgetQuickAddRules {
    const val ACTION_QUICK_ADD = "com.haskatech.myday.widget.QUICK_ADD"
    fun shouldOpenAddDialog(action: String?): Boolean = action == ACTION_QUICK_ADD
}
