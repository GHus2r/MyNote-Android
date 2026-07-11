package com.mynote.android.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.mynote.android.R
import com.mynote.android.data.AppDatabase
import com.mynote.android.ui.edit.EditActivity
import com.mynote.android.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class MyNoteWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) updateWidget(ctx, mgr, id)
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(ctx, mgr, id, newOptions)
        updateWidget(ctx, mgr, id)
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO)

        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(
                android.content.ComponentName(ctx, MyNoteWidget::class.java)
            )
            for (id in ids) updateWidget(ctx, mgr, id)
        }

        private enum class WidgetShape { COMPACT, CARD, WIDE, DEFAULT }

        private fun detectShape(mgr: AppWidgetManager, id: Int): WidgetShape {
            val opts = mgr.getAppWidgetOptions(id)
            val wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            val hDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40)
            return when {
                hDp < 70 -> WidgetShape.COMPACT
                wDp in 100..190 && hDp in 80..200 -> WidgetShape.CARD
                wDp >= 200 && hDp >= 100 -> WidgetShape.WIDE
                else -> WidgetShape.DEFAULT
            }
        }

        private fun updateWidget(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val shape = detectShape(mgr, id)
            val density = ctx.resources.displayMetrics.density

            val (layoutRes, needData) = when (shape) {
                WidgetShape.COMPACT -> R.layout.widget_mynote_compact to false
                WidgetShape.CARD    -> R.layout.widget_mynote_card to true
                WidgetShape.WIDE    -> R.layout.widget_mynote_wide to true
                WidgetShape.DEFAULT -> R.layout.widget_mynote to true
            }
            val views = RemoteViews(ctx.packageName, layoutRes)

            // ── 通用点击：新建笔记 ──
            val newIntent = Intent(ctx, EditActivity::class.java).apply {
                putExtra("noteId", UUID.randomUUID().toString())
                putExtra("from_widget", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val newPi = PendingIntent.getActivity(ctx, 0, newIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            // 所有布局都有新建按钮（不同 ID）
            for (btnId in intArrayOf(R.id.btn_new_note, R.id.btn_new_note_compact, R.id.btn_new_card, R.id.btn_new_wide)) {
                try { views.setOnClickPendingIntent(btnId, newPi) } catch (_: Exception) {}
            }

            // ── 通用点击：整体打开主页 ──
            val mainIntent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val mainPi = PendingIntent.getActivity(ctx, 1, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            try { views.setOnClickPendingIntent(android.R.id.background, mainPi) } catch (_: Exception) {}

            // ── 卡片型：搜索按钮 ──
            try {
                views.setOnClickPendingIntent(R.id.btn_search, mainPi)
                views.setOnClickPendingIntent(R.id.btn_search_wide, mainPi)
            } catch (_: Exception) {}

            if (!needData) {
                mgr.updateAppWidget(id, views)
                return
            }

            // ── 异步加载数据 ──
            scope.launch {
                try {
                    val db = AppDatabase.get(ctx)
                    val notes = db.noteDao().searchNotes("")
                        .sortedByDescending { it.updateTime }
                    val count = notes.size

                    when (shape) {
                        WidgetShape.CARD -> {
                            views.setTextViewText(R.id.tv_note_count, "$count 篇笔记")
                        }
                        WidgetShape.WIDE -> {
                            views.removeAllViews(R.id.notes_container_wide)
                            if (notes.isEmpty()) {
                                views.setViewVisibility(R.id.tv_empty_wide, android.view.View.VISIBLE)
                            } else {
                                views.setViewVisibility(R.id.tv_empty_wide, android.view.View.GONE)
                                for ((i, n) in notes.take(5).withIndex()) {
                                    views.addView(R.id.notes_container_wide, buildNoteItem(ctx, n, i))
                                }
                            }
                        }
                        WidgetShape.DEFAULT -> {
                            views.removeAllViews(R.id.notes_container)
                            if (notes.isEmpty()) {
                                views.setViewVisibility(R.id.tv_empty, android.view.View.VISIBLE)
                            } else {
                                views.setViewVisibility(R.id.tv_empty, android.view.View.GONE)
                                for ((i, n) in notes.take(3).withIndex()) {
                                    views.addView(R.id.notes_container, buildNoteItem(ctx, n, i))
                                }
                            }
                        }
                        else -> {}
                    }

                    mgr.updateAppWidget(id, views)
                } catch (_: Exception) {}
            }
        }

        private fun buildNoteItem(ctx: Context, note: com.mynote.android.data.entity.Note, index: Int): RemoteViews {
            val item = RemoteViews(ctx.packageName, R.layout.widget_note_item)
            val title = note.title.ifEmpty { "无标题" }
            val prefix = if (note.isPinned) "📌 " else ""
            item.setTextViewText(R.id.tv_note_title, "$prefix$title")

            val preview = note.contentText
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .let { if (it.length > 30) it.take(30) + "..." else it.ifEmpty { "无内容" } }
            item.setTextViewText(R.id.tv_note_preview, preview)

            val openIntent = Intent(ctx, EditActivity::class.java).apply {
                putExtra("noteId", note.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            item.setOnClickPendingIntent(R.id.widget_note_item_root,
                PendingIntent.getActivity(ctx, 100 + index, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            return item
        }
    }
}
