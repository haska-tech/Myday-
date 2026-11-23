package com.haskatech.myday.widget

/** Shared intent contract for opening the task composer from a home-screen widget. */
object WidgetQuickAddRules {
    const val EXTRA_OPEN_QUICK_ADD = "com.haskatech.myday.widget.EXTRA_OPEN_QUICK_ADD"

    fun shouldOpenAddDialog(requested: Boolean): Boolean = requested
}
