package com.haskatech.myday.widget

object WidgetRefreshRules {
    const val ACTION_DATE_CHANGED = "android.intent.action.DATE_CHANGED"
    const val ACTION_TIME_CHANGED = "android.intent.action.TIME_SET"
    const val ACTION_TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED"

    fun shouldRefresh(action: String?): Boolean =
        action == MyDayWidgetProvider.ACTION_REFRESH ||
        action == "android.appwidget.action.APPWIDGET_UPDATE" ||
        action == ACTION_DATE_CHANGED ||
        action == ACTION_TIME_CHANGED ||
        action == ACTION_TIMEZONE_CHANGED
}
