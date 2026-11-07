package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.CppDatabase
import com.example.data.model.WidgetTip
import com.example.data.repository.CppTipRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class TipWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // AppWidgetProvider is a broadcast receiver: keep database work off the main
                // thread and bound it to less than Android's broadcast execution window.
                val tip = withTimeout(8_000L) {
                    val dao = CppDatabase.getDatabase(appContext).cppTipDao()
                    CppTipRepository(dao).initializeIfNeeded(appContext)
                    selectTip(dao.getWidgetTips(), TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis()))
                }
                ids.forEach { render(appContext, manager, it, tip) }
            } catch (error: Exception) {
                Log.w("TipWidget", "Unable to load the saved tip", error)
                ids.forEach { render(appContext, manager, it, null) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle
    ) {
        onUpdate(context, manager, intArrayOf(appWidgetId))
    }

    private fun render(context: Context, manager: AppWidgetManager, id: Int, tip: WidgetTip?) {
        val views = RemoteViews(context.packageName, R.layout.tip_widget)
        views.setTextViewText(R.id.widget_title, tip?.title ?: context.getString(R.string.widget_empty))
        views.setTextViewText(R.id.widget_standard, tip?.standard ?: "C++")
        views.setTextViewText(R.id.widget_number, tip?.let { context.getString(R.string.widget_tip_number, it.id) } ?: "")
        views.setViewVisibility(R.id.widget_number, if (tip == null) View.GONE else View.VISIBLE)
        val intent = tipIntent(context, tip?.id)
        val click = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.tip_widget_root, click)
        manager.updateAppWidget(id, views)
    }

    companion object {
        internal fun selectTip(tips: List<WidgetTip>, day: Long): WidgetTip? =
            if (tips.isEmpty()) null else tips[Math.floorMod(day, tips.size.toLong()).toInt()]

        internal fun tipIntent(context: Context, tipId: Int?): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (tipId != null) {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("cpptotw://tip/$tipId")
                }
            }

        internal fun tipIdFromIntent(intent: Intent): Int? {
            val uri = intent.data ?: return null
            if (intent.action != Intent.ACTION_VIEW || uri.scheme != "cpptotw" ||
                uri.host != "tip" || uri.pathSegments.size != 1) return null
            return uri.lastPathSegment?.toIntOrNull()?.takeIf { it > 0 }
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TipWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            context.sendBroadcast(Intent(context, TipWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            })
        }
    }
}
