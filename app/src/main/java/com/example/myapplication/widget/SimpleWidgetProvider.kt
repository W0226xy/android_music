package com.example.myapplication.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.example.myapplication.R

/**
 * 极简测试 Widget — 只有一个 TextView，用于诊断 Widget 注册是否正常。
 */
class SimpleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("SimpleWidget", "onUpdate called, ids=${appWidgetIds.toList()}")
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.simple_widget)
            views.setTextViewText(R.id.simple_text, "Widget 工作正常!")
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
